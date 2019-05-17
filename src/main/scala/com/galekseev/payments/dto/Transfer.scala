package com.galekseev.payments.dto

import com.galekseev.payments.dto.Transfer.Status
import play.api.libs.json._

case class Transfer(id: TransferId,
                    from: AccountId,
                    to: AccountId,
                    amount: Amount,
                    status: Status)
  extends HasId[TransferId]
object Transfer {

  def fromRequest(request: TransferRequest, id: TransferId, status: Status): Transfer = {
    import request._
    Transfer(id, from, to, amount, status)
  }

  sealed trait Status
  object Status {
    case object Completed extends Status
    sealed trait Declined extends Status
    object Declined {
      case object InsufficientFunds extends Declined
    }

    implicit val format: Format[Status] = julienrf.json.derived.oformat()
  }

  implicit val format: Format[Transfer] = julienrf.json.derived.oformat()
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

case class TransferRequest(from: AccountId, to: AccountId, amount: Amount)
object TransferRequest {
  implicit val format: OFormat[TransferRequest] = Json.format[TransferRequest]
}
