package com.galekseev.payments.core.synched

import com.galekseev.payments.core.AccountService.validateAmount
import com.galekseev.payments.core.{AccountService, TransferService}
import com.galekseev.payments.dto.PaymentError.{NegativeAmount, NoSuchAccount, NoSuchTransfer}
import com.galekseev.payments.dto.Transfer.Status.{Completed, Declined}
import com.galekseev.payments.dto._
import com.galekseev.payments.storage.synched.Dao
import com.typesafe.scalalogging.StrictLogging

import scala.util.control.NonFatal

class SynchronizedTransferService(transferDao: Dao[Transfer, TransferId],
                                  accountService: AccountService,
                                  accountDao: Dao[Account, AccountId],
                                  idGenerator: TransferIdGenerator
                                 )(implicit accountLockService: LockService[AccountId],
                                   transferLockService: LockService[TransferId])
  extends TransferService with StrictLogging {

  override def makeTransfer(request: TransferRequest): Either[TransferError, Transfer] =
    for {
      _        <- validateAmount(request.amount)
      transfer <- doTransfer(request)
    } yield transfer

  @SuppressWarnings(Array("org.wartremover.warts.Throw", "org.wartremover.warts.OptionPartial"))
  private def doTransfer(request: TransferRequest): Either[TransferError, Transfer] = {
    val id = idGenerator.generate()
    accountLockService.callWithWriteLocks(Seq(request.from, request.to), () =>
      transferLockService.callWithWriteLocks(Seq(id), () => {
        val from = accountService.get(request.from)
        val to = accountService.get(request.to)
        try {
          (for {
            _ <- accountService.withdraw(request.from, request.amount)
            _ <- accountService.deposit(request.to, request.amount)
          } yield {
            Transfer.fromRequest(request, id, Completed)
          }).left
            .flatMap {
              case e @ (NegativeAmount | NoSuchAccount(request.from)) =>
                Left(e)
              case e @ NoSuchAccount(_) =>
                from.foreach(accountDao.update)
                Left(e)
              case PaymentError.InsufficientFunds =>
                Right(Transfer.fromRequest(request, id, Declined.InsufficientFunds))
            }
            .map(transferDao.add(_).get)
        } catch {
          case NonFatal(e) =>
            logger.error(s"Failed to transfer [${request.amount}] from [$from to [$to]. Rolling back ...", e)
            // todo: use AccountDao instead of AccountService everywhere in the class if we use it here anyway ?
            from.foreach(accountDao.update)
            to.foreach(accountDao.update)
            throw e
        }
      })
    )
  }

  override def get: Traversable[Transfer] =
    transferLockService.callWithAllReadLocks(() =>
      transferDao.get
    )

  override def getByAccount(accountId: AccountId): Traversable[Transfer] =
    get.filter(transfer =>
      Set(transfer.from, transfer.to).contains(accountId)
    )

  override def get(id: TransferId): Either[NoSuchTransfer, Transfer] =
    transferLockService.callWithReadLocks(Seq(id), () =>
      transferDao.get(id).toRight(NoSuchTransfer(id))
    )
}
