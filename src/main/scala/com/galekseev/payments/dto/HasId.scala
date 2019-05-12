package com.galekseev.payments.dto

trait HasId[Id] {
  def id: Id
}
