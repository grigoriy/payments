package com.galekseev.payments

import java.util.UUID

import akka.event.Logging
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.DebuggingDirectives
import akka.http.scaladsl.server.{ExceptionHandler, Route}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.galekseev.payments.core.synched._
import com.galekseev.payments.dto.PaymentError.{NegativeAmount, NoSuchAccount}
import com.galekseev.payments.dto.Transfer.Status.Completed
import com.galekseev.payments.dto.Transfer.Status.Declined.InsufficientFunds
import com.galekseev.payments.dto._
import com.galekseev.payments.storage.synched.Dao
import com.galekseev.payments.transport.{AccountRoutes, TransferRoutes}
import com.typesafe.scalalogging.StrictLogging
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json._

import scala.util.control.NonFatal

// scalastyle:off magic.number
class EndToEndSpec
  extends WordSpec
    with Matchers
    with ScalaFutures
    with ScalatestRouteTest
    with PlayJsonSupport
    with StrictLogging {

  import EndToEndSpec._

  lazy implicit private val accountLockService: LockService[AccountId] = new LockService[AccountId]
  lazy implicit private val transferLockService: LockService[TransferId] = new LockService[TransferId]
  lazy private val accountDao = new Dao[Account, AccountId]
  lazy private val transferDao = new Dao[Transfer, TransferId]
  lazy private val accountIdGenerator = new AccountIdGenerator
  lazy private val transferIdGenerator = new TransferIdGenerator
  lazy private val accountService = new SynchronizedAccountService(accountDao, accountIdGenerator)
  lazy private val transferService = new SynchronizedTransferService(transferDao, accountService, accountDao, transferIdGenerator)
  lazy private val accountRoutes = new AccountRoutes(accountService).routes
  lazy private val transferRoutes = new TransferRoutes(transferService).routes

  implicit val discreteExceptionHandler: ExceptionHandler =
    ExceptionHandler {
      case NonFatal(e) =>
        logger.error(s"Failure", e)
        complete(StatusCodes.InternalServerError)
    }
  lazy val routes: Route = DebuggingDirectives.logRequestResult(("REST", Logging.DebugLevel))(
    Route.seal(
      accountRoutes ~ transferRoutes
    )
  )

  "MoneyTransferRoutes" should {

    "create 2 accounts and transfer money between them" in {

      val accountRequest_1 = AccountRequest(10L)
      val accountRequest_2 = AccountRequest(20L)
      val accountRequest_3 = AccountRequest(30L)
      var account_1 = Account(AccountId(-1L), accountRequest_1.amount)
      var account_2 = Account(AccountId(-1L), accountRequest_2.amount)
      var account_3 = Account(AccountId(-1L), accountRequest_3.amount)
      val negativeTransferAmount = -4L
      val positiveTransferAmount = 4L
      var insufficientFundsTransferId = TransferId(-1L)
      var transferId = TransferId(-1L)

      Get(uri = "/accounts") ~> routes ~> check {
        status should ===(StatusCodes.OK)
        entityAs[Seq[Account]] shouldBe empty
      }

      Get(uri = "/transfers") ~> routes ~> check {
        status should ===(StatusCodes.OK)
        entityAs[Seq[Transfer]] shouldBe empty
      }

      Get(uri = "/accounts/1") ~> routes ~> check {
        status should ===(StatusCodes.NotFound)
      }

      Get(uri = "/accounts/2") ~> routes ~> check {
        status should ===(StatusCodes.NotFound)
      }

      Post("/accounts").withEntity(Marshal(accountRequest_1).to[MessageEntity].futureValue) ~> routes ~> check {
        status should ===(StatusCodes.OK)
        account_1 = Account(entityAs[AccountId], accountRequest_1.amount)
      }

      Post("/accounts").withEntity(Marshal(accountRequest_2).to[MessageEntity].futureValue) ~> routes ~> check {
        status should ===(StatusCodes.OK)
        account_2 = Account(entityAs[AccountId], accountRequest_2.amount)
      }

      Post("/accounts").withEntity(Marshal(accountRequest_3).to[MessageEntity].futureValue) ~> routes ~> check {
        status should ===(StatusCodes.OK)
        account_3 = Account(entityAs[AccountId], accountRequest_3.amount)
      }

      Get(uri = "/accounts") ~> routes ~> check {
        status should ===(StatusCodes.OK)
        entityAs[Seq[Account]] should ===(Seq(account_1, account_2, account_3))
      }

      Get(uri = s"/accounts/${account_1.id.id}") ~> routes ~> check {
        status should ===(StatusCodes.OK)
        entityAs[Account] should ===(account_1)
      }

      Get(uri = s"/accounts/${account_2.id.id}") ~> routes ~> check {
        status should ===(StatusCodes.OK)
        entityAs[Account] should ===(account_2)
      }

      Get(uri = s"/accounts/${account_3.id.id}") ~> routes ~> check {
        status should ===(StatusCodes.OK)
        entityAs[Account] should ===(account_3)
      }

      val negativeTransferRequest = TransferRequest(UUID.randomUUID(), account_1.id, account_2.id, negativeTransferAmount)
      Post("/transfers").withEntity(Marshal(negativeTransferRequest).to[MessageEntity].futureValue) ~> transferRoutes ~> check {
        status should ===(StatusCodes.BadRequest)
        val error = entityAs[String]
        error should ===(NegativeAmount.toString)
      }

      Get(uri = s"/accounts/${account_1.id.id}") ~> routes ~> check {
        status should ===(StatusCodes.OK)
        entityAs[Account] should ===(account_1)
      }

      Get(uri = s"/accounts/${account_2.id.id}") ~> routes ~> check {
        status should ===(StatusCodes.OK)
        entityAs[Account] should ===(account_2)
      }

      Get(uri = s"/accounts/${account_3.id.id}") ~> routes ~> check {
        status should ===(StatusCodes.OK)
        entityAs[Account] should ===(account_3)
      }

      Get(uri = "/transfers") ~> routes ~> check {
        status should ===(StatusCodes.OK)
        entityAs[Seq[Transfer]] shouldBe empty
      }

      val nonExistentAccountId = AccountId(4L)
      val nonExistentSenderTransferRequest = TransferRequest(UUID.randomUUID(), nonExistentAccountId, account_2.id, positiveTransferAmount)
      Post("/transfers").withEntity(Marshal(nonExistentSenderTransferRequest).to[MessageEntity].futureValue) ~> transferRoutes ~> check {
        status should ===(StatusCodes.BadRequest)
        val error = entityAs[NoSuchAccount]
        error should === (NoSuchAccount(nonExistentAccountId))
      }

      Get(uri = s"/accounts/${account_1.id.id}") ~> routes ~> check {
        status should ===(StatusCodes.OK)
        entityAs[Account] should ===(account_1)
      }

      Get(uri = s"/accounts/${account_2.id.id}") ~> routes ~> check {
        status should ===(StatusCodes.OK)
        entityAs[Account] should ===(account_2)
      }

      Get(uri = s"/accounts/${account_3.id.id}") ~> routes ~> check {
        status should ===(StatusCodes.OK)
        entityAs[Account] should ===(account_3)
      }

      Get(uri = "/transfers") ~> routes ~> check {
        status should ===(StatusCodes.OK)
        entityAs[Seq[Transfer]] shouldBe empty
      }

      val nonExistentReceiverTransferRequest = TransferRequest(UUID.randomUUID(), account_1.id, nonExistentAccountId, positiveTransferAmount)
      Post("/transfers").withEntity(Marshal(nonExistentReceiverTransferRequest).to[MessageEntity].futureValue) ~> transferRoutes ~> check {
        status should ===(StatusCodes.BadRequest)
        val error = entityAs[NoSuchAccount]
        error should === (NoSuchAccount(nonExistentAccountId))
      }

      Get(uri = s"/accounts/${account_1.id.id}") ~> routes ~> check {
        status should ===(StatusCodes.OK)
        entityAs[Account] should ===(account_1)
      }

      Get(uri = s"/accounts/${account_2.id.id}") ~> routes ~> check {
        status should ===(StatusCodes.OK)
        entityAs[Account] should ===(account_2)
      }

      Get(uri = s"/accounts/${account_3.id.id}") ~> routes ~> check {
        status should ===(StatusCodes.OK)
        entityAs[Account] should ===(account_3)
      }

      Get(uri = "/transfers") ~> routes ~> check {
        status should ===(StatusCodes.OK)
        entityAs[Seq[Transfer]] shouldBe empty
      }

      val insufficientFundsTransferRequest = TransferRequest(UUID.randomUUID(), account_1.id, account_3.id, Long.MaxValue)
      Post("/transfers").withEntity(Marshal(insufficientFundsTransferRequest).to[MessageEntity].futureValue) ~> transferRoutes ~> check {
        status should ===(StatusCodes.OK)
        val transfer = entityAs[Transfer]
        insufficientFundsTransferId = transfer.id
        transfer should ===(Transfer.fromRequest(insufficientFundsTransferRequest, insufficientFundsTransferId, InsufficientFunds))
      }

      Get(uri = s"/accounts/${account_1.id.id}") ~> routes ~> check {
        status should ===(StatusCodes.OK)
        entityAs[Account] should ===(account_1)
      }

      Get(uri = s"/accounts/${account_2.id.id}") ~> routes ~> check {
        status should ===(StatusCodes.OK)
        entityAs[Account] should ===(account_2)
      }

      Get(uri = s"/accounts/${account_3.id.id}") ~> routes ~> check {
        status should ===(StatusCodes.OK)
        entityAs[Account] should ===(account_3)
      }

      Get(uri = "/transfers") ~> routes ~> check {
        status should ===(StatusCodes.OK)
        entityAs[Seq[Transfer]] shouldBe Seq(Transfer.fromRequest(insufficientFundsTransferRequest, insufficientFundsTransferId, InsufficientFunds))
      }

      val transferRequest = TransferRequest(UUID.randomUUID(), account_1.id, account_2.id, positiveTransferAmount)
      Post("/transfers").withEntity(Marshal(transferRequest).to[MessageEntity].futureValue) ~> transferRoutes ~> check {
        status should ===(StatusCodes.OK)
        val transfer: Transfer = entityAs[Transfer]
        transferId = transfer.id
        transfer should ===(Transfer.fromRequest(transferRequest, transferId, Completed))
      }

      val account_1_updated = account_1.copy(amount = account_1.amount - positiveTransferAmount)
      Get(uri = s"/accounts/${account_1.id.id}") ~> routes ~> check {
        status should ===(StatusCodes.OK)
        entityAs[Account] should ===(account_1_updated)
      }

      val account_2_updated = account_2.copy(amount = account_2.amount + positiveTransferAmount)
      Get(uri = s"/accounts/${account_2.id.id}") ~> routes ~> check {
        status should ===(StatusCodes.OK)
        entityAs[Account] should ===(account_2_updated)
      }

      Get(uri = s"/accounts/${account_3.id.id}") ~> routes ~> check {
        status should ===(StatusCodes.OK)
        entityAs[Account] should ===(account_3)
      }

      Get(uri = "/accounts") ~> routes ~> check {
        status should ===(StatusCodes.OK)
        entityAs[Seq[Account]] should ===(Seq(
          account_1_updated, account_2_updated, account_3
        ))
      }

      Get(uri = "/transfers") ~> routes ~> check {
        status should ===(StatusCodes.OK)
        entityAs[Seq[Transfer]] should ===(Seq(
          Transfer.fromRequest(insufficientFundsTransferRequest, insufficientFundsTransferId, InsufficientFunds),
          Transfer.fromRequest(transferRequest, transferId, Completed)
        ))
      }

      Get(uri = s"/transfers?accId=${account_1.id.id}") ~> routes ~> check {
        status should ===(StatusCodes.OK)
        entityAs[Seq[Transfer]] should ===(Seq(
          Transfer.fromRequest(insufficientFundsTransferRequest, insufficientFundsTransferId, InsufficientFunds),
          Transfer.fromRequest(transferRequest, transferId, Completed)
        ))
      }

      Get(uri = s"/transfers?accId=${account_2.id.id}") ~> routes ~> check {
        status should ===(StatusCodes.OK)
        entityAs[Seq[Transfer]] should ===(Seq(
          Transfer.fromRequest(transferRequest, transferId, Completed)
        ))
      }

      Get(uri = s"/transfers?accId=${account_3.id.id}") ~> routes ~> check {
        status should ===(StatusCodes.OK)
        entityAs[Seq[Transfer]] should ===(Seq(
          Transfer.fromRequest(insufficientFundsTransferRequest, insufficientFundsTransferId, InsufficientFunds)
        ))
      }

    }
  }
}

object EndToEndSpec {

  val failureReads: Reads[Transfer.Status.Declined] = {
    case JsString("InsufficientFunds") => JsSuccess(InsufficientFunds)
    case other => JsError(s"Cannot deserialize Transfer.Status.Declined from [$other]")
  }
  implicit val statusReads: Reads[Transfer.Status] = {
    case JsString("Completed") => JsSuccess(Completed)
    case other  => failureReads.reads(other)
  }
  implicit val transferReads: Reads[Transfer] = Json.reads[Transfer]
  implicit val noSuchAccouontReads: Reads[NoSuchAccount] = Json.reads[NoSuchAccount]
}
