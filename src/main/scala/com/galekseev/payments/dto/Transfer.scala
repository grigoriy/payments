package com.galekseev.payments.dto

import java.time.OffsetDateTime
import java.util.UUID

import com.galekseev.payments.dto.Transfer.Status
import com.galekseev.payments.dto.Transfer.Status.Declined.InsufficientFunds
import play.api.libs.json._

case class Transfer(id: TransferId,
                    from: AccountId,
                    to: AccountId,
                    amount: Amount,
                    description: Option[String],
                    status: Status,
                    processingTimestamp: OffsetDateTime)
  extends HasId[TransferId]
object Transfer {

  sealed trait Status
  object Status {
    final case object Completed extends Status
    sealed trait Declined extends Status
    object Declined {
      case object InsufficientFunds extends Declined
    }

    implicit val format: Format[Status] = new Format[Status] {
      private val CompletedString = Completed.toString
      private val InsufficientFundsString = InsufficientFunds.toString
      override def writes(o: Status): JsValue = JsString(o.toString)
      override def reads(json: JsValue): JsResult[Status] =
        json.validate[String].map {
          case `CompletedString` => Completed
          case `InsufficientFundsString` => InsufficientFunds
        }
    }
  }

  implicit val format: OFormat[Transfer] = Json.format
}

final case class TransferId(id: UUID) extends AnyVal
object TransferId {
  implicit val format: Format[TransferId] = new Format[TransferId] {
    override def reads(json: JsValue): JsResult[TransferId] = json.validate[UUID].map(TransferId(_))
    override def writes(o: TransferId): JsValue = Json.toJson(o.id)
  }

  implicit val ordering: Ordering[TransferId] = Ordering.by(_.id)
}

final case class TransferRequest(from: AccountId, to: AccountId, amount: Amount, description: Option[String])
object TransferRequest {
  implicit val format: OFormat[TransferRequest] = Json.format[TransferRequest]
}
