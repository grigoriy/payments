package com.galekseev.payments

import akka.actor.{ ActorSystem, Terminated }
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{ ExceptionHandler, Route }
import akka.http.scaladsl.server.directives.DebuggingDirectives
import akka.stream.ActorMaterializer
import com.galekseev.payments.core.synched._
import com.galekseev.payments.dto.{ Account, AccountId, Transfer, TransferId }
import com.galekseev.payments.storage.synched.Dao
import com.galekseev.payments.transport.{ AccountRoutes, TransferRoutes }
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.duration.Duration
import scala.concurrent.{ Await, ExecutionContext, Future }
import scala.util.control.NonFatal
import scala.util.{ Failure, Success }

object Main extends App with StrictLogging {

  val conf = ConfigFactory.load()

  implicit val system: ActorSystem = ActorSystem("MoneyTranserServer")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = system.dispatcher

  lazy implicit private val accountLockService: LockService[AccountId] = new LockService[AccountId]
  lazy implicit private val transferLockService: LockService[TransferId] = new LockService[TransferId]
  lazy private val accountDao = new Dao[Account, AccountId]
  lazy private val transferDao = new Dao[Transfer, TransferId]
  lazy private val accountIdGenerator = new AccountIdGenerator
  lazy private val transferIdGenerator = new TransferIdGenerator
  lazy private val accountService = new SynchronizedAccountService(accountDao, accountIdGenerator)
  lazy private val transferService =
    new SynchronizedTransferService(transferDao, accountService, accountDao, transferIdGenerator)

  implicit val discreteExceptionHandler: ExceptionHandler =
    ExceptionHandler {
      case NonFatal(e) =>
        logger.error(s"Failure", e)
        complete(StatusCodes.InternalServerError)
    }
  lazy val routes: Route = DebuggingDirectives.logRequestResult(("REST", Logging.DebugLevel))(
    Route.seal(
      new AccountRoutes(accountService).routes ~ new TransferRoutes(transferService).routes
    )
  )

  val host = conf.getString("host")
  val port = conf.getInt("port")
  val serverBinding: Future[Http.ServerBinding] = Http().bindAndHandle(routes, host, port)

  serverBinding.onComplete {
    case Success(bound) =>
      logger.info(s"Server online at http://${bound.localAddress.getHostString}:${bound.localAddress.getPort}/")
    case Failure(e) =>
      logger.error(s"Server could not start!", e)
      system
        .terminate()
        .onComplete(terminated => logger.info(s"The system terminated: $terminated"))
  }

  private val terminated: Terminated = Await.result(system.whenTerminated, Duration.Inf)
  logger.info(s"The system terminated: $terminated")
}
