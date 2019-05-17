package com.galekseev.payments.core.synched

import com.galekseev.payments.core.AccountService
import com.galekseev.payments.dto.PaymentError.{ AccountExists, NoSuchAccount }
import com.galekseev.payments.dto._
import com.galekseev.payments.storage.synched.Dao

class SynchronizedAccountService(dao: Dao[Account, AccountId], idGenerator: AccountIdGenerator)(
  implicit accountLockService: LockService[AccountId]
) extends AccountService {

  import accountLockService.{ callWithAllReadLocks, callWithReadLocks, callWithWriteLocks }

  override def create(request: AccountRequest): Either[AccountExists, Account] = {
    val id = idGenerator.generate()
    callWithWriteLocks(Seq(id), () =>
      dao
        .add(Account(id, request.amount))
        .toRight(AccountExists(id))
    )
  }

  override def get(id: AccountId): Either[NoSuchAccount, Account] =
    callWithReadLocks(Seq(id), () =>
      dao.get(id).toRight(NoSuchAccount(id))
    )

  override def get: Traversable[Account] =
    callWithAllReadLocks(() =>
      dao.get
    )
}
