package com.galekseev.payments.transport

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.MethodDirectives.{get, post}
import akka.http.scaladsl.server.directives.RouteDirectives.complete
import com.galekseev.payments.core.AccountService
import com.galekseev.payments.dto.AccountRequest
import com.galekseev.payments.dto.PaymentError.AccountExists
import com.typesafe.scalalogging.StrictLogging
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport

class AccountRoutes(accountService: AccountService)
    extends StrictLogging
    with PlayJsonSupport
    with DiscreteExceptionHandling {

  lazy val routes: Route =
    path("accounts") {
          concat(

            post {
              entity(as[AccountRequest]) { account =>
                accountService.create(account) match {
                  case Right(acc) =>
                    logger.info(s"Created account [$acc]")
                    complete(acc)
                  case Left(AccountExists(id)) =>
                    logger.error(s"Could not create an account [$account] with id [$id]: conflicting IDs")
                    complete((StatusCodes.InternalServerError, "Conflicting IDs"))
                }
              }
            }

            , get { complete(accountService.get) }
          )

    }
}
