package com.galekseev.payments

import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.http.scaladsl.unmarshalling.FromEntityUnmarshaller
import com.galekseev.payments.dto.PaymentError.{NoSuchAccount, SameAccountTransfer}
import com.galekseev.payments.dto.Transfer.Status.Completed
import com.galekseev.payments.dto.Transfer.Status.Declined.InsufficientFunds
import com.galekseev.payments.dto._
import com.typesafe.scalalogging.StrictLogging
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import eu.timepit.refined.auto._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Assertion, Matchers, WordSpec}
import play.api.libs.json.Json.{obj, toJsObject}
import play.api.libs.json._

// scalastyle:off magic.number
class EndToEndSpec
  extends WordSpec
    with Matchers
    with ScalaFutures
    with ScalatestRouteTest
    with PlayJsonSupport
    with StrictLogging {

  private val routes = new TestConfig().routes
  private val accountsUri = "/accounts"
  private val transfersUri = "/transfers"
  private var account_1: Account = _
  private var account_2: Account = _
  private var account_3: Account = _
  private var insufficientFundsTransfer: Transfer = _
  private var zeroTransfer: Transfer = _
  private var completedTransfer: Transfer = _

  "MoneyTransferRoutes" should {
    "create accounts and transfer money between them" in {

      // the order is important because these tests depend on the mutable state
      testAccountCreation
      testNegativeAmountTransfer
      testNonexistentSenderTransfer
      testNonexistentReceiverTransfer
      testNonexistentSenderAndReceiverTransfer
      testSameAccountTransfer
      testInsufficientFundsTransfer
      testZeroTransfer
      testProperTransfer

      lazy val accountRequest = AccountRequest(Amount(BigInt(10L)))
      lazy val negativeAmountAccountRequest = toJsObject(accountRequest) ++ obj("balance" -> Json.toJson(-10L))
      lazy val transferAmount = Amount(BigInt(4L))
      lazy val negativeTransferRequest =
        toJsObject(TransferRequest(account_1.id, account_2.id, transferAmount)) ++ obj("amount" -> Json.toJson(-4L))
      lazy val nonExistentAccountId = AccountId(Long.MaxValue)

      def checkAccounts(expected: Seq[Account]): Assertion =
        checkSeq(expected, accountsUri)

      def checkTransfers(expected: Seq[Transfer]): Assertion =
        checkSeq(expected, transfersUri)

      def checkSeq[A](expected: Seq[A], uri: String)(implicit ev: FromEntityUnmarshaller[Seq[A]]): Assertion =
        Get(uri = uri) ~> routes ~> check {
          status should ===(StatusCodes.OK)
          entityAs[Seq[A]] shouldBe expected
        }

      def testNonexistentAccountTransfer(request: TransferRequest): Assertion = {
        Post(transfersUri).withEntity(Marshal(request).to[MessageEntity].futureValue) ~> routes ~> check {
          status should ===(StatusCodes.BadRequest)
          entityAs[PaymentError] should === (NoSuchAccount(nonExistentAccountId))
        }

        checkAccounts(Seq(account_1, account_2, account_3))
        checkTransfers(Seq.empty)
      }

      lazy val testAccountCreation: Assertion = {
        checkAccounts(Seq.empty)
        checkTransfers(Seq.empty)

        Post(accountsUri).withEntity(Marshal(accountRequest).to[MessageEntity].futureValue) ~> routes ~> check {
          status should ===(StatusCodes.OK)
          account_1 = entityAs[Account]
          account_1.balance should ===(accountRequest.balance)
        }

        Post(accountsUri).withEntity(Marshal(accountRequest).to[MessageEntity].futureValue) ~> routes ~> check {
          status should ===(StatusCodes.OK)
          account_2 = entityAs[Account]
          account_2.balance should ===(accountRequest.balance)
        }

        Post(accountsUri).withEntity(Marshal(accountRequest).to[MessageEntity].futureValue) ~> routes ~> check {
          status should ===(StatusCodes.OK)
          account_3 = entityAs[Account]
          account_3.balance should ===(accountRequest.balance)
        }

        Post(accountsUri).withEntity(Marshal(negativeAmountAccountRequest).to[MessageEntity].futureValue) ~> routes ~> check {
          status should ===(StatusCodes.BadRequest)
        }

        checkAccounts(Seq(account_1, account_2, account_3))
      }

      lazy val testNegativeAmountTransfer: Assertion = {
        Post(transfersUri).withEntity(Marshal(negativeTransferRequest).to[MessageEntity].futureValue) ~> routes ~> check {
          status should === (StatusCodes.BadRequest)
        }

        checkAccounts(Seq(account_1, account_2, account_3))
        checkTransfers(Seq.empty)
      }

      lazy val testNonexistentSenderTransfer: Assertion =
        testNonexistentAccountTransfer(TransferRequest(nonExistentAccountId, account_2.id, transferAmount))

      lazy val testNonexistentReceiverTransfer: Assertion =
        testNonexistentAccountTransfer(TransferRequest(account_1.id, nonExistentAccountId, transferAmount))

      lazy val testNonexistentSenderAndReceiverTransfer: Assertion = {
        val request = TransferRequest(nonExistentAccountId, nonExistentAccountId, transferAmount)
        Post(transfersUri).withEntity(Marshal(request).to[MessageEntity].futureValue) ~> routes ~> check {
          status should ===(StatusCodes.BadRequest)
          entityAs[PaymentError] should === (SameAccountTransfer)
        }

        checkAccounts(Seq(account_1, account_2, account_3))
        checkTransfers(Seq.empty)
      }

      lazy val testSameAccountTransfer: Assertion = {
        val request = TransferRequest(account_1.id, account_1.id, transferAmount)
        Post(transfersUri).withEntity(Marshal(request).to[MessageEntity].futureValue) ~> routes ~> check {
          status should ===(StatusCodes.BadRequest)
          entityAs[PaymentError] should === (SameAccountTransfer)
        }

        checkAccounts(Seq(account_1, account_2, account_3))
        checkTransfers(Seq.empty)
      }

      lazy val testInsufficientFundsTransfer: Assertion = {
        val insufficientFundsTransferRequest = TransferRequest(account_1.id, account_3.id, Amount(BigInt(Long.MaxValue)))
        Post(transfersUri).withEntity(Marshal(insufficientFundsTransferRequest).to[MessageEntity].futureValue) ~> routes ~> check {
          status should === (StatusCodes.OK)
          insufficientFundsTransfer = entityAs[Transfer]
          insufficientFundsTransfer.from should === (insufficientFundsTransferRequest.from)
          insufficientFundsTransfer.to should === (insufficientFundsTransferRequest.to)
          insufficientFundsTransfer.amount should === (insufficientFundsTransferRequest.amount)
          insufficientFundsTransfer.status should === (InsufficientFunds)
        }

        checkAccounts(Seq(account_1, account_2, account_3))
        checkTransfers(Seq(insufficientFundsTransfer))
      }

      lazy val testZeroTransfer: Assertion = {
        val transferRequest = TransferRequest(account_1.id, account_2.id, Amount(BigInt(0L)))
        Post(transfersUri).withEntity(Marshal(transferRequest).to[MessageEntity].futureValue) ~> routes ~> check {
          status should ===(StatusCodes.OK)
          zeroTransfer = entityAs[Transfer]
          zeroTransfer.from should ===(transferRequest.from)
          zeroTransfer.to should ===(transferRequest.to)
          zeroTransfer.amount should ===(transferRequest.amount)
          zeroTransfer.status should ===(Completed)
        }

        checkAccounts(Seq(
          account_1,
          account_2,
          account_3
        ))

        checkTransfers(Seq(
          insufficientFundsTransfer,
          zeroTransfer
        ))
      }

      lazy val testProperTransfer: Assertion = {
        val transferRequest = TransferRequest(account_1.id, account_2.id, transferAmount)
        Post(transfersUri).withEntity(Marshal(transferRequest).to[MessageEntity].futureValue) ~> routes ~> check {
          status should ===(StatusCodes.OK)
          completedTransfer = entityAs[Transfer]
          completedTransfer.from should ===(transferRequest.from)
          completedTransfer.to should ===(transferRequest.to)
          completedTransfer.amount should ===(transferRequest.amount)
          completedTransfer.status should ===(Completed)
        }

        checkAccounts(Seq(
          account_1.copy(balance = (account_1.balance - transferAmount).get),
          account_2.copy(balance = account_2.balance + transferAmount),
          account_3
        ))

        checkTransfers(Seq(
          insufficientFundsTransfer,
          zeroTransfer,
          completedTransfer
        ))
      }

    }
  }
}
