package com.galekseev.payments

import akka.event.Logging
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.DebuggingDirectives
import akka.http.scaladsl.server.{ExceptionHandler, Route}
import com.galekseev.payments.core._
import com.galekseev.payments.dto.{Account, AccountId, Transfer, TransferId}
import com.galekseev.payments.storage.Dao
import com.galekseev.payments.transport.{AccountRoutes, TransferRoutes}
import com.typesafe.scalalogging.Logger

import scala.util.control.NonFatal

object Config {

  lazy private implicit val accountLockService: LockService[AccountId] = new LockService[AccountId]
  lazy private implicit val transferLockService: LockService[TransferId] = new LockService[TransferId]
  lazy private val accountDao = new Dao[Account, AccountId]
  lazy private val transferDao = new Dao[Transfer, TransferId]
  lazy private val accountIdGenerator = new AccountIdGenerator
  lazy private val transferIdGenerator = new TransferIdGenerator
  lazy private val accountService = new SynchronizedAccountService(accountDao, accountIdGenerator)
  lazy private val transferService = new SynchronizedTransferService(transferDao, accountDao, transferIdGenerator)
  lazy private val logger: Logger = Logger("DiscreteExceptionHandler")

  lazy private implicit val discreteExceptionHandler: ExceptionHandler =
    ExceptionHandler {
      case NonFatal(e) =>
        logger.error(s"Failure", e)
        complete(StatusCodes.InternalServerError)
    }

  lazy val routes: Route = Route.seal(
    DebuggingDirectives.logRequestResult(("REST", Logging.DebugLevel))
      (new AccountRoutes(accountService).routes ~ new TransferRoutes(transferService).routes)
  )
}
