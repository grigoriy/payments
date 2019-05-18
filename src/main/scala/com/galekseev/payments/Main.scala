package com.galekseev.payments

import akka.actor.{ActorSystem, Terminated}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

object Main extends App with StrictLogging {

  lazy private val conf = ConfigFactory.load()

  implicit val system: ActorSystem = ActorSystem("MoneyTransferServer")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContext = system.dispatcher

  private val serverBinding: Future[Http.ServerBinding] = Http().bindAndHandle(
    Config.routes,
    conf.getString("host"),
    conf.getInt("port")
  )

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
