package com.galekseev.payments.core.synched

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.{ ReadWriteLock, ReentrantReadWriteLock }

import com.galekseev.payments.core.synched.LockService.LockType

class LockService[A](implicit conv: A => Ordered[A]) {

  import com.galekseev.payments.core.synched.LockService.LockType.{ Read, Write }

  private val locks = new ConcurrentHashMap[A, ReadWriteLock]()

  def callWithReadLocks[B](ids: Seq[A], f: () => B): B = {
    withLocks(ids, Read, f)
  }

  def callWithWriteLocks[B](ids: Seq[A], f: () => B): B = {
    withLocks(ids, Write, f)
  }

  private def withLocks[B](ids: Seq[A], lockType: LockType, f: () => B): B = {
    val locks = ids.sorted.map(
      lockType match {
        case Read  => getLock(_).readLock()
        case Write => getLock(_).writeLock()
      }
    )
    locks.foreach(_.lock())
    try {
      f()
    } finally {
      locks.reverse.foreach(_.unlock())
    }
  }

  private def getLock(id: A): ReadWriteLock = {
    val lock = new ReentrantReadWriteLock(false)
    Option(locks.putIfAbsent(id, lock)).getOrElse(lock)
  }
}

object LockService {
  private sealed trait LockType
  private object LockType {
    final case object Read extends LockType
    final case object Write extends LockType
  }
}
