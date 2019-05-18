package com.galekseev.payments.core

import com.galekseev.payments.dto.{Account, AccountCreationError, AccountRequest}

trait AccountService {

  def create(account: AccountRequest): Either[AccountCreationError, Account]

  def get: Traversable[Account]
}
