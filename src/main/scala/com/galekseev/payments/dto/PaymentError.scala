package com.galekseev.payments.dto

import play.api.libs.json.{Format, __}

sealed trait PaymentError
sealed trait TransferError extends PaymentError
sealed trait AccountCreationError extends PaymentError

object PaymentError {
  final case class AccountExists(accountId: AccountId) extends AccountCreationError
  final case object SameAccountTransfer extends TransferError
  final case class NoSuchAccount(accountId: AccountId) extends TransferError
  final case object InsufficientFunds extends TransferError

  implicit val format: Format[PaymentError] = julienrf.json.derived.flat.oformat((__ \ "type").format[String])
}
