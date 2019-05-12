package com.galekseev.payments

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.complete
import akka.http.scaladsl.server.ExceptionHandler
import com.typesafe.scalalogging.{ Logger, StrictLogging }

import scala.util.control.NonFatal

package object transport {

  trait DiscreteExceptionHandling extends StrictLogging {
    implicit val discreteExceptionHandler: ExceptionHandler =
      ExceptionHandler {
        case NonFatal(e) =>
          logger.error(s"Failure", e)
          complete(StatusCodes.InternalServerError)
      }

    protected def logger: Logger
  }
}
