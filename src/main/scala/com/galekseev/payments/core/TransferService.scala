package com.galekseev.payments.core

import java.time.OffsetDateTime

import com.galekseev.payments.dto.PaymentError.{NoSuchAccount, SameAccountTransfer}
import com.galekseev.payments.dto.Transfer.Status.{Completed, Declined}
import com.galekseev.payments.dto._
import com.galekseev.payments.storage.Dao
import com.typesafe.scalalogging.StrictLogging

import scala.util.control.NonFatal

trait TransferService {
  def makeTransfer(transfer: TransferRequest): Either[TransferError, Transfer]
  def get: Traversable[Transfer]
}

class SynchronizedTransferService(transferDao: Dao[Transfer, TransferId],
                                  accountDao: Dao[Account, AccountId],
                                  idGenerator: TransferIdGenerator
                                 )(implicit accountLockService: LockService[AccountId])
  extends TransferService with StrictLogging {

  override def makeTransfer(request: TransferRequest): Either[TransferError, Transfer] =
    for {
      _         <- validateRequest(request)
      fromId    = request.from
      toId      = request.to
      transfer  <- locked(fromId, toId, () =>
        for {
          from      <- accountDao.get(fromId).toRight(NoSuchAccount(fromId))
          to        <- accountDao.get(toId).toRight(NoSuchAccount(toId))
        } yield doTransfer(from, to, request.amount, request.description)
      )
    } yield transfer

  private def validateRequest(request: TransferRequest): Either[SameAccountTransfer.type, TransferRequest] =
    Either.cond(request.from != request.to, request, SameAccountTransfer)

  private def locked[A](from: AccountId, to: AccountId, f: () => A): A =
    accountLockService.callWithWriteLocks(Seq(from, to), f)

  @SuppressWarnings(Array("org.wartremover.warts.Product", "org.wartremover.warts.Serializable"))
  private def doTransfer(from: Account, to: Account, amount: Amount, description: Option[String]): Transfer =
    withRollback(() => {

      val status =
        (from.balance - amount).map(newFromBalance => {
          saveUpdated(from.copy(balance = newFromBalance))
          saveUpdated(to.copy(balance = to.balance + amount))
          Completed
        }).getOrElse(
          Declined.InsufficientFunds
        )
      saveTransfer(Transfer(idGenerator.generate(), from.id, to.id, amount, description, status, OffsetDateTime.now()))

    },
      from,
      to,
      amount
    )

  @SuppressWarnings(Array("org.wartremover.warts.Throw"))
  private def withRollback[A](f: () => A, from: Account, to: Account, amount: Amount): A =
    try { f() } catch {
      case NonFatal(e) =>
        logger.error(s"Failed to transfer [$amount] from [${from.id}] to [${to.id}]. Rolling back ...", e)
        Seq(from, to).foreach(account =>
          accountDao.update(account).foreach(saved => logger.info(s"Rolled back $saved"))
        )
        throw e
    }

  @SuppressWarnings(Array("org.wartremover.warts.Throw"))
  private def saveUpdated(account: Account): Unit =
    accountDao.update(account).orElse(
      throw new RuntimeException(s"Account [${account.id}] has disappeared while locked (must not happen)")
    ).foreach(_ => ())

  @SuppressWarnings(Array("org.wartremover.warts.Throw"))
  private def saveTransfer(transfer: Transfer): Transfer =
    transferDao.add(transfer)
      .getOrElse(throw new RuntimeException("Failed to generate a unique transfer ID (must not happen"))

  override def get: Traversable[Transfer] =
    transferDao.get
      .toSeq.sorted((x: Transfer, y: Transfer) => -x.processingTimestamp.compareTo(y.processingTimestamp))
}
