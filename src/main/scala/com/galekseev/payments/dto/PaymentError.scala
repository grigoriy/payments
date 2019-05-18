package com.galekseev.payments.dto

import play.api.libs.json.Format

sealed trait PaymentError
sealed trait TransferError extends PaymentError
sealed trait AccountCreationError extends PaymentError

object PaymentError {
  case class AccountExists(id: AccountId) extends AccountCreationError
  case object SameAccountTransfer extends TransferError
  case class NoSuchAccount(id: AccountId) extends TransferError
  case object InsufficientFunds extends TransferError

  implicit val format: Format[PaymentError] = julienrf.json.derived.oformat()
}
