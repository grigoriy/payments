package com.galekseev.payments.dto

import play.api.libs.json.Format

sealed trait PaymentError
sealed trait TransferError extends PaymentError
sealed trait AccountCreationError extends PaymentError

object PaymentError {
  case object NegativeAmount extends AccountCreationError
  case object SameAccountTransfer extends TransferError
  case object InsufficientFunds extends TransferError
  case class AccountExists(id: AccountId) extends AccountCreationError
  case class NoSuchAccount(id: AccountId) extends TransferError
  case class NoSuchTransfer(id: TransferId) extends PaymentError

  implicit val format: Format[PaymentError] = julienrf.json.derived.oformat()
}
