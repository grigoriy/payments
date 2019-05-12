package com.galekseev.payments.core

import com.galekseev.payments.dto.{ Transfer, TransferError, TransferRequest }

trait TransferService {

  def makeTransfer(transfer: TransferRequest): Either[TransferError, Transfer]
}
