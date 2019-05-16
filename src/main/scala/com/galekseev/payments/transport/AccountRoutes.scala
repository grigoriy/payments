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
    pathPrefix("accounts") {
      concat(
        pathEnd {
          concat(

            post {
              entity(as[AccountRequest]) {
                account =>
                  accountService.create(account) match {
                    case Right(acc) =>
                      logger.info(s"Created account [$acc]")
                      complete(acc)
                    case Left(NegativeAmount) =>
                      logger.info(s"Could not create an account [$account]: negative initial deposit")
                      complete((StatusCodes.BadRequest, "Initial deposit cannot be negative"))
                    case Left(AccountExists(id)) =>
                      logger.info(s"Could not create an account [$account] with id [$id]: conflicting IDs")
                      complete((StatusCodes.InternalServerError, "Conflicting IDs"))
                  }
              }
            }

            ,
            get { complete(accountService.get) }

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
}
