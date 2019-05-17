package com.galekseev.payments.dto

import com.galekseev.payments.dto.Amount.NonNegativeBigInt
import eu.timepit.refined.api.{ Refined, Validate }
import eu.timepit.refined.numeric.NonNegative
import eu.timepit.refined.refineV
import play.api.libs.json._

case class Amount(cents: NonNegativeBigInt) {
  // scalastyle:off

  @SuppressWarnings(Array("org.wartremover.warts.EitherProjectionPartial"))
  def +(other: Amount): Amount =
    Amount(refineV[NonNegative](cents.value + other.cents.value).right.get)

  def -(other: Amount): Option[Amount] =
    refineV[NonNegative](cents.value - other.cents.value).toOption.map(Amount(_))

  // scalastyle:on
}

object Amount {
  type NonNegativeBigInt = BigInt Refined NonNegative

  implicit val format: Format[Amount] = Json.format

  implicit def refinedReads[T, P](implicit reads: Reads[T], v: Validate[T, P]): Reads[T Refined P] =
    Reads[T Refined P] { json =>
      reads
        .reads(json)
        .flatMap { t: T =>
          refineV[P](t) match {
            case Left(error)  => JsError(error)
            case Right(value) => JsSuccess(value)
          }
        }
    }

  implicit def refinedWrites[T, P](implicit writes: Writes[T]): Writes[T Refined P] =
    writes.contramap(_.value)
}
