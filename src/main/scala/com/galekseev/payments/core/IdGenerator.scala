package com.galekseev.payments.core

import java.util.UUID

import com.galekseev.payments.dto.{AccountId, TransferId}

trait IdGenerator[A] {
  def generate(): A
}

class AccountIdGenerator extends IdGenerator[AccountId] {

  override def generate(): AccountId =
    AccountId(UUID.randomUUID())
}

class TransferIdGenerator extends IdGenerator[TransferId] {

  override def generate(): TransferId =
    TransferId(UUID.randomUUID())
}
