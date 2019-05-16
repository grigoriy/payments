package com.galekseev.payments.core

import com.galekseev.payments.dto.PaymentError.NoSuchAccount
import com.galekseev.payments.dto._

trait AccountService {

  def create(account: AccountRequest): Either[AccountCreationError, Account]

  def get(id: AccountId): Either[NoSuchAccount, Account]

  def get: Traversable[Account]
}
