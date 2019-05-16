package com.galekseev.payments.core.synched

import com.galekseev.payments.core.TransferService
import com.galekseev.payments.dto.PaymentError.{InsufficientFunds, NoSuchAccount, NoSuchTransfer}
import com.galekseev.payments.dto.Transfer.Status.{Completed, Declined}
import com.galekseev.payments.dto._
import com.galekseev.payments.storage.synched.Dao
import com.typesafe.scalalogging.StrictLogging

import scala.util.control.NonFatal

class SynchronizedTransferService(
  transferDao: Dao[Transfer, TransferId],
  accountDao: Dao[Account, AccountId],
  idGenerator: TransferIdGenerator
)(implicit accountLockService: LockService[AccountId],
  transferLockService: LockService[TransferId])
    extends TransferService with StrictLogging {

  @SuppressWarnings(Array("org.wartremover.warts.Throw", "org.wartremover.warts.OptionPartial", "org.wartremover.warts.Product", "org.wartremover.warts.Serializable"))
  override def makeTransfer(request: TransferRequest): Either[TransferError, Transfer] = {
    val id = idGenerator.generate()
    accountLockService.callWithWriteLocks(Seq(request.from, request.to), () =>
      transferLockService.callWithWriteLocks(Seq(id), () => {
        val maybeFrom = accountDao.get(request.from)
        val maybeTo = accountDao.get(request.to)
        try {
          (for {
            from <- maybeFrom.toRight(NoSuchAccount(request.from))
            to <- maybeTo.toRight(NoSuchAccount(request.to))
            newFromAmount <- (from.amount - request.amount).toRight(InsufficientFunds)
            _ <- accountDao.update(from.copy(amount = newFromAmount)).toRight(throw new RuntimeException("Should not happen"))
            _ <- accountDao.update(to.copy(amount = to.amount + request.amount)).toRight(throw new RuntimeException("Should not happen"))
          } yield {
            Transfer.fromRequest(request, id, Completed)
          })
            .left
            .flatMap {
              case e: NoSuchAccount =>
                Left(e)
              case PaymentError.InsufficientFunds =>
                Right(Transfer.fromRequest(request, id, Declined.InsufficientFunds))
            }
            .map(transferDao.add(_).get)
        } catch {
          case NonFatal(e) =>
            logger.error(s"Failed to transfer [${request.amount}] from [${request.from} to [${request.to}]. Rolling back ...", e)
            maybeFrom.foreach(accountDao.update)
            maybeTo.foreach(accountDao.update)
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
