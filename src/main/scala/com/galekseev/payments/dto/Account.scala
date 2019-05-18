package com.galekseev.payments.dto

import play.api.libs.json._

final case class Account(id: AccountId, balance: Amount) extends HasId[AccountId]
object Account {
  implicit val format: Format[Account] = Json.format
}

final case class AccountId(id: Long) extends AnyVal
object AccountId {
  implicit val format: Format[AccountId] = new Format[AccountId] {
    override def reads(json: JsValue): JsResult[AccountId] = json.validate[Long].map(AccountId(_))
    override def writes(o: AccountId): JsValue = Json.toJson(o.id)
  }

  implicit val ordering: Ordering[AccountId] = Ordering.by(_.id)
}

final case class AccountRequest(balance: Amount)
object AccountRequest {
  implicit val format: OFormat[AccountRequest] = Json.format
}
