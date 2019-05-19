package com.galekseev.payments.core

import com.galekseev.payments.dto.PaymentError.AccountExists
import com.galekseev.payments.dto.{Account, AccountCreationError, AccountRequest, _}
import com.galekseev.payments.storage.Dao

trait AccountService {
  def create(account: AccountRequest): Either[AccountCreationError, Account]
  def get: Traversable[Account]
}

class SynchronizedAccountService(dao: Dao[Account, AccountId], idGenerator: AccountIdGenerator)(
  implicit accountLockService: LockService[AccountId]
) extends AccountService {

  import accountLockService.{callWithAllReadLocks, callWithWriteLocks}

  override def create(request: AccountRequest): Either[AccountExists, Account] = {
    val id = idGenerator.generate()
    callWithWriteLocks(Seq(id), () =>
      dao.add(Account(id, request.balance))
        .toRight(AccountExists(id))
    )
  }

  override def get: Traversable[Account] =
    callWithAllReadLocks(() =>
      dao.get
    )
}
