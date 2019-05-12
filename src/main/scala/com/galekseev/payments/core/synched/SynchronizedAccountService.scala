package com.galekseev.payments.core.synched

import com.galekseev.payments.core.AccountService
import com.galekseev.payments.core.AccountService.{ enoughFunds, validateAmount }
import com.galekseev.payments.dto.PaymentError.{ AccountExists, NoSuchAccount }
import com.galekseev.payments.dto._
import com.galekseev.payments.storage.synched.Dao

class SynchronizedAccountService(dao: Dao[Account, AccountId], idGenerator: AccountIdGenerator)(
  implicit accountLockService: LockService[AccountId]
) extends AccountService {

  import accountLockService.{ callWithReadLocks, callWithWriteLocks }

  override def create(request: AccountRequest): Either[AccountCreationError, AccountId] =
    for {
      _ <- validateAmount(request.amount)
      id = idGenerator.generate()
      account <- callWithWriteLocks(
        Seq(id),
        () =>
          dao
            .add(Account(id, request.amount))
            .toRight(AccountExists(id))
      )
    } yield account.id

  override def get(id: AccountId): Either[NoSuchAccount, Account] =
    callWithReadLocks(Seq(id), () => dao.get(id).toRight(NoSuchAccount(id)))

  @SuppressWarnings(Array("org.wartremover.warts.Product", "org.wartremover.warts.Serializable"))
  override def deposit(id: AccountId, amountCents: Long): Either[DepositError, Account] =
    for {
      _ <- validateAmount(amountCents)
      updatedAccount <- callWithWriteLocks(
        Seq(id),
        () =>
          for {
            account <- get(id)
            updatedAcc <- dao
              .update(account.copy(amount = account.amount + amountCents))
              .toRight(NoSuchAccount(id))
          } yield updatedAcc
      )
    } yield updatedAccount

  @SuppressWarnings(Array("org.wartremover.warts.Product", "org.wartremover.warts.Serializable"))
  override def withdraw(id: AccountId, amountCents: Long): Either[WithdrawalError, Account] =
    for {
      _ <- validateAmount(amountCents)
      updatedAccount <- callWithWriteLocks(
        Seq(id),
        () =>
          for {
            account <- get(id)
            _       <- enoughFunds(account, amountCents)
            updatedAcc <- dao
              .update(account.copy(amount = account.amount - amountCents))
              .toRight(NoSuchAccount(id))
          } yield updatedAcc
      )
    } yield updatedAccount
}
