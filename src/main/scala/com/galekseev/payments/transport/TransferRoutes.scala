package com.galekseev.payments.transport

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.post
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import com.galekseev.payments.core.TransferService
import com.galekseev.payments.dto.TransferRequest
import com.typesafe.scalalogging.StrictLogging
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport

class TransferRoutes(transferService: TransferService)
  extends StrictLogging
    with PlayJsonSupport {

  lazy val routes: Route =
    path("transfers") {
      post(entity(as[TransferRequest]) (transfer)) ~
      get(complete(transferService.get))
    }

  private def transfer(transferRequest: TransferRequest) = {
    transferService.makeTransfer(transferRequest) match {
      case Right(transfer) =>
        logger.info(s"Processed the transfer [$transfer]")
        complete(transfer)
      case Left(error) =>
        logger.info(s"Invalid transfer request [$transferRequest]: [$error]")
        complete((StatusCodes.BadRequest, error))
    }
  }
}
