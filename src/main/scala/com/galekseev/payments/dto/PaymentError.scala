package com.galekseev.payments.dto

import play.api.libs.json.{ Json, OWrites, Writes }

sealed trait PaymentError
sealed trait TransferError extends PaymentError
sealed trait WithdrawalError extends TransferError
sealed trait DepositError extends TransferError
sealed trait AccountCreationError extends PaymentError

object PaymentError {
  case object NegativeAmount extends WithdrawalError with DepositError with AccountCreationError {
    val writes: Writes[NegativeAmount.type] = (o: NegativeAmount.type) => Json.toJson(o.toString)
  }

  case class AccountExists(id: AccountId) extends AccountCreationError
  object AccountExists {
    val writes: OWrites[AccountExists] = Json.writes[AccountExists]
  }

  case class NoSuchAccount(id: AccountId) extends WithdrawalError with DepositError
  object NoSuchAccount {
    val writes: OWrites[NoSuchAccount] = Json.writes[NoSuchAccount]
  }

  case object InsufficientFunds extends WithdrawalError {
    val writes: Writes[InsufficientFunds.type] = (o: InsufficientFunds.type) => Json.toJson(o.toString)
  }

  case class NoSuchTransfer(id: TransferId) extends PaymentError
  object NoSuchTransfer {
    val writes: OWrites[NoSuchTransfer] = Json.writes[NoSuchTransfer]
  }

  implicit val writes: Writes[PaymentError] = {
    case a: NegativeAmount.type    => NegativeAmount.writes.writes(a)
    case a: AccountExists          => AccountExists.writes.writes(a)
    case a: NoSuchAccount          => NoSuchAccount.writes.writes(a)
    case a: InsufficientFunds.type => InsufficientFunds.writes.writes(a)
    case a: NoSuchTransfer         => NoSuchTransfer.writes.writes(a)
  }
}
