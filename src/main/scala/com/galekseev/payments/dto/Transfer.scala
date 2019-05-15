package com.galekseev.payments.dto

import java.util.UUID

import com.galekseev.payments.dto.Transfer.Status
import play.api.libs.json._

case class Transfer(id: TransferId,
                    from: AccountId,
                    to: AccountId,
                    amount: Long,
                    status: Status)
  extends HasId[TransferId]

object Transfer {

  def fromRequest(request: TransferRequest, id: TransferId, status: Status): Transfer = {
    import request._
    Transfer(id, from, to, amount, status)
  }

  sealed trait Status

  object Status {
    case object Completed extends Status {
      val writes: Writes[Completed.type] = (o: Completed.type) => Json.toJson(o.toString)
    }

    sealed trait Declined extends Status

    object Declined {
      case object InsufficientFunds extends Declined {
        val writes: Writes[InsufficientFunds.type] = (o: InsufficientFunds.type) => Json.toJson(o.toString)
      }

      val writes: Writes[Declined] = {
        case a: InsufficientFunds.type => Json.toJson(a)(InsufficientFunds.writes)
      }
    }

    implicit val writes: Writes[Status] = {
      case a: Completed.type => Json.toJson(a)(Completed.writes)
      case a: Declined       => Json.toJson(a)(Declined.writes)
    }
  }

  implicit val writes: OWrites[Transfer] = Json.writes[Transfer]
}

case class TransferId(id: Long) extends AnyVal

object TransferId {
  implicit val format: Format[TransferId] = new Format[TransferId] {
    override def reads(json: JsValue): JsResult[TransferId] =
      json.validate[Long].map(TransferId(_))
    override def writes(o: TransferId): JsValue = Json.toJson(o.id)
  }

  implicit val ordering: Ordering[TransferId] = Ordering.by(_.id)
}

case class TransferRequest(requestId: UUID, from: AccountId, to: AccountId, amount: Long)

object TransferRequest {
  implicit val format: Format[TransferRequest] = Json.format[TransferRequest]
}
