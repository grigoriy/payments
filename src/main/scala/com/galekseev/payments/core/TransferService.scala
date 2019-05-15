package com.galekseev.payments.core

import com.galekseev.payments.dto.PaymentError.NoSuchTransfer
import com.galekseev.payments.dto._

trait TransferService {

  def makeTransfer(transfer: TransferRequest): Either[TransferError, Transfer]

  def get: Traversable[Transfer]

  def get(id: TransferId): Either[NoSuchTransfer, Transfer]

  def getByAccount(accountId: AccountId): Traversable[Transfer]
}
