package com.galekseev.payments

import java.util.UUID

import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.galekseev.payments.core.synched._
import com.galekseev.payments.dto.Transfer.Status.Completed
import com.galekseev.payments.dto.Transfer.Status.Declined.InsufficientFunds
import com.galekseev.payments.dto._
import com.galekseev.payments.storage.synched.Dao
import com.galekseev.payments.transport.{AccountRoutes, TransferRoutes}
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}
import play.api.libs.json._

// scalastyle:off magic.number
class EndToEndSpec
  extends WordSpec
    with Matchers
    with ScalaFutures
    with ScalatestRouteTest
    with PlayJsonSupport {

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
  lazy private val routes = accountRoutes ~ transferRoutes

  "MoneyTransferRoutes" should {

    "create 2 accounts and transfer money between them" in {

      val account_1 = AccountRequest(10L)
      val account_2 = AccountRequest(20L)
      val transferRequest = TransferRequest(UUID.randomUUID(), AccountId(1L), AccountId(2L), 4L)
      val accountEntity_1 = Marshal(account_1).to[MessageEntity].futureValue
      val accountEntity_2 = Marshal(account_2).to[MessageEntity].futureValue
      val transferRequestEntity = Marshal(transferRequest).to[MessageEntity].futureValue

      Get(uri = "/accounts/1") ~> routes ~> check {
        status should ===(StatusCodes.NotFound)
      }

      Get(uri = "/accounts/2") ~> routes ~> check {
        status should ===(StatusCodes.NotFound)
      }

      Post("/accounts").withEntity(accountEntity_1) ~> routes ~> check {
        status should ===(StatusCodes.OK)
      }

      Post("/accounts").withEntity(accountEntity_2) ~> routes ~> check {
        status should ===(StatusCodes.OK)
      }

      Get(uri = "/accounts/1") ~> routes ~> check {
        status should ===(StatusCodes.OK)
        entityAs[Account] should ===(Account(AccountId(1L), account_1.amount))
      }

      Get(uri = "/accounts/2") ~> routes ~> check {
        status should ===(StatusCodes.OK)
        entityAs[Account] should ===(Account(AccountId(2L), account_2.amount))
      }

      Post("/transfers").withEntity(transferRequestEntity) ~> transferRoutes ~> check {
        status should ===(StatusCodes.OK)
        entityAs[Transfer] should ===(Transfer.fromRequest(transferRequest, TransferId(1L), Completed))
      }

      Get(uri = "/accounts/1") ~> routes ~> check {
        status should ===(StatusCodes.OK)
        entityAs[Account] should ===(Account(AccountId(1L), account_1.amount - 4))
      }

      Get(uri = "/accounts/2") ~> routes ~> check {
        status should ===(StatusCodes.OK)
        entityAs[Account] should ===(Account(AccountId(2L), account_2.amount + 4))
      }
    }

  }
}

object EndToEndSpec {

  val failureReads: Reads[Transfer.Status.Declined] = {
    case JsString("InsufficientFunds") => JsSuccess(InsufficientFunds)
    case other => JsError(s"Cannot deserialise Transfer.Status.Declined from [$other]")
  }

  implicit val statusReads: Reads[Transfer.Status] = {
    case JsString("Completed") => JsSuccess(Completed)
    case other  => failureReads.reads(other)
  }

  implicit val transferReads: Reads[Transfer] = Json.reads[Transfer]
}
