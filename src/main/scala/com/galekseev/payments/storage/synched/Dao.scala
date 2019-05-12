package com.galekseev.payments.storage.synched

import java.util.concurrent.ConcurrentHashMap

import com.galekseev.payments.core.synched.LockService
import com.galekseev.payments.dto.HasId
import com.typesafe.scalalogging.StrictLogging

class Dao[A <: HasId[Id], Id](implicit lockService: LockService[Id]) extends StrictLogging {
  private val idToA = new ConcurrentHashMap[Id, A]()

  /**
    * @param a new entity
    * @return Some[A] if added, None if not (found an existing entity with such ID)
    */
  def add(a: A): Option[A] =
    lockService.callWithWriteLocks(
      Seq(a.id),
      () =>
        Option(idToA.putIfAbsent(a.id, a)) match {
          case Some(_) => None
          case None    => Some(a)
        }
    )

  def get(id: Id): Option[A] =
    lockService.callWithReadLocks(Seq(id), () => Option(idToA.get(id)))

  /**
    * @param a modified entity
    * @return Some[A] if updated, None if not (no entity with such ID found)
    */
  def update(a: A): Option[A] =
    lockService.callWithWriteLocks(
      Seq(a.id),
      () =>
        Option(idToA.replace(a.id, a))
          .map(_ => a)
    )
}
