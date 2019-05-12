package com.galekseev.payments.core

import com.galekseev.payments.dto.PaymentError.{ InsufficientFunds, NegativeAmount, NoSuchAccount }
import com.galekseev.payments.dto._

trait AccountService {

  def create(account: AccountRequest): Either[AccountCreationError, AccountId]

  def get(id: AccountId): Either[NoSuchAccount, Account]

  def deposit(id: AccountId, amountCents: Long): Either[DepositError, Account]

  def withdraw(id: AccountId, amountCents: Long): Either[WithdrawalError, Account]
}

object AccountService {

  def validateAmount(amount: Long): Either[NegativeAmount.type, Unit] =
    Either.cond(amount > 0, (), NegativeAmount)

  def enoughFunds(account: Account, amount: Long): Either[InsufficientFunds.type, Unit] =
    Either.cond(account.amount >= amount, (), InsufficientFunds)
}
