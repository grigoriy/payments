package com.galekseev.payments.dto

import play.api.libs.json.{ Format, JsResult, JsValue, Json }

case class Account(id: AccountId, amount: Long) extends HasId[AccountId]

object Account {
  implicit val format: Format[Account] = Json.format
}

case class AccountId(id: Long) extends AnyVal

object AccountId {
  implicit val format: Format[AccountId] = new Format[AccountId] {
    override def reads(json: JsValue): JsResult[AccountId] = json.validate[Long].map(AccountId(_))
    override def writes(o: AccountId): JsValue = Json.toJson(o.id)
  }

  implicit val ordering: Ordering[AccountId] = Ordering.by(_.id)
}

case class AccountRequest(amount: Long)

object AccountRequest {
  implicit val format: Format[AccountRequest] = Json.format
}
