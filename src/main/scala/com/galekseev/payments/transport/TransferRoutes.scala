package com.galekseev.payments.transport

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.post
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import com.galekseev.payments.core.TransferService
import com.galekseev.payments.dto.PaymentError.{ NegativeAmount, NoSuchAccount }
import com.galekseev.payments.dto.{ AccountId, PaymentError, TransferId, TransferRequest }
import com.typesafe.scalalogging.StrictLogging
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport

class TransferRoutes(transferService: TransferService)
    extends StrictLogging
    with PlayJsonSupport
    with DiscreteExceptionHandling {

  lazy val routes: Route =
    pathPrefix("transfers") {
      concat(
        pathEnd {
          concat(

            post {
              entity(as[TransferRequest]) {
                transferReq => {
                  transferService.makeTransfer(transferReq) match {
                    case Right(transfer) =>
                      logger.info(s"Processed the transfer [$transfer]")
                      complete(transfer)
                    case Left(error) =>
                      error match {
                        case NegativeAmount | NoSuchAccount(_) =>
                          logger.info(s"Invalid transfer request [$transferReq]: [$error]")
                          complete((StatusCodes.BadRequest, error))
                        case PaymentError.InsufficientFunds =>
                          logger.info(s"Insufficient funds for the transfer [$transferReq]")
                          complete("Insufficient funds")
                      }
                  }
                }
              }
            }

            ,
            get {
              parameter('accId.as[Long]) { accId =>
                complete(transferService.getByAccount(AccountId(accId)))
              }
            }

            ,
            get { complete(transferService.get) }

          )
        },

        path(LongNumber) { id =>

          get {
            rejectEmptyResponse {
              complete(
                transferService.get(TransferId(id)).toOption
              )
            }
          }

        }
      )
    }
}
