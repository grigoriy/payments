package com.galekseev.payments.transport

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.{get, post}
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import com.galekseev.payments.core.AccountService
import com.galekseev.payments.dto.PaymentError.{AccountExists, NegativeAmount}
import com.galekseev.payments.dto.{AccountId, AccountRequest}
import com.typesafe.scalalogging.StrictLogging
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport

class AccountRoutes(accountService: AccountService)
    extends StrictLogging
    with PlayJsonSupport
    with DiscreteExceptionHandling {

  lazy val routes: Route =
    Route.seal(
      pathPrefix("accounts") {
        concat(
          pathEnd {
            concat(

              post {
                entity(as[AccountRequest]) {
                  account =>
                    accountService.create(account) match {
                      case Right(id) =>
                        logger.info(s"Created account [$account]")
                        complete(id)
                      case Left(NegativeAmount) =>
                        logger.info(s"Could not create an account [$account]: negative initial deposit")
                        complete((StatusCodes.BadRequest, "Initial deposit cannot be negative"))
                      case Left(AccountExists(_)) =>
                        logger.info(s"Could not create an account [$account]: conflicting IDs")
                        complete((StatusCodes.BadRequest, "Conflicting IDs"))
                    }
                }
              }

            )
          },
          path(LongNumber) { id =>

              get {
                rejectEmptyResponse {
                  complete(
                    accountService.get(AccountId(id)).toOption
                  )
                }
              }

          }
        )
      }
    )
}
