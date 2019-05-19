package com.galekseev.payments.storage

import java.util.concurrent.ConcurrentHashMap

import com.galekseev.payments.core.LockService
import com.galekseev.payments.dto.HasId
import com.typesafe.scalalogging.StrictLogging

import scala.collection.JavaConverters._

class Dao[A <: HasId[Id], Id](implicit lockService: LockService[Id]) extends StrictLogging {

  import lockService._

  private val idToA = new ConcurrentHashMap[Id, A]()

  /**
    * @param a new entity
    * @return Some[A] if added, None if not (found an existing entity with such ID)
    */
  def add(a: A): Option[A] =
    callWithWriteLocks(Seq(a.id), () =>
      Option(idToA.putIfAbsent(a.id, a)) match {
        case Some(_) => None
        case None    => Some(a)
      }
    )

  def get: Traversable[A] =
    callWithAllReadLocks(() =>
      idToA.values().asScala
    )

  def get(id: Id): Option[A] =
    callWithReadLocks(Seq(id), () =>
      Option(idToA.get(id))
    )

  /**
    * @param a modified entity
    * @return Some[A] if updated, None if not (no entity with such ID found)
    */
  def update(a: A): Option[A] =
    callWithWriteLocks(Seq(a.id), () =>
      Option(idToA.replace(a.id, a))
        .map(_ => a)
    )
}
