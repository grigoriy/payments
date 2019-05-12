package com.galekseev.payments.core.synched

import java.util.concurrent.atomic.AtomicLong

import com.galekseev.payments.dto.{ AccountId, TransferId }

trait IdGenerator[A] {
  def generate(): A
}

class AccountIdGenerator extends IdGenerator[AccountId] {
  private val nextId = new AtomicLong(1)

  override def generate(): AccountId =
    AccountId(nextId.getAndIncrement())
}

class TransferIdGenerator extends IdGenerator[TransferId] {
  private val nextId = new AtomicLong(1)

  override def generate(): TransferId =
    TransferId(nextId.getAndIncrement())
}
