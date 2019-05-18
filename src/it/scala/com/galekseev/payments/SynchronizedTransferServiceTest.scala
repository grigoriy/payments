package com.galekseev.payments

import java.util.concurrent.{CountDownLatch, Executors, TimeUnit}

import com.galekseev.payments.core.synched._
import com.galekseev.payments.dto.Amount.NonNegativeBigInt
import com.galekseev.payments.dto._
import com.galekseev.payments.storage.synched.Dao
import com.typesafe.scalalogging.StrictLogging
import eu.timepit.refined.numeric.NonNegative
import eu.timepit.refined.refineV
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}

import scala.util.Random

class SynchronizedTransferServiceTest extends WordSpec with Matchers with ScalaFutures with StrictLogging {

  "SynchronizedTransferService" when {

    "invoked serially" should {
      "preserve the total amount of money in the bank" in {
        implicit val accountLockService: LockService[AccountId] = new LockService[AccountId]
        implicit val transferLockService: LockService[TransferId] = new LockService[TransferId]
        val accountDao = new Dao[Account, AccountId]
        val transferDao = new Dao[Transfer, TransferId]
        val accountIdGenerator = new AccountIdGenerator
        val transferIdGenerator = new TransferIdGenerator
        val accountService = new SynchronizedAccountService(accountDao, accountIdGenerator)
        val transferService = new SynchronizedTransferService(transferDao, accountDao, transferIdGenerator)
        val numAccounts = 10
        val numTransferRequests = 100

        val accounts: Seq[Account] = (1 to numAccounts)
          .map(_ => accountService.create(AccountRequest(Amount(randomNonNegativeBigInt))))
          .filter(_.isRight).map(_.right.get)
        val totalAmount = accounts.map(_.amount).reduce(_ + _)
//        logger.info(s"Accounts: ${accounts.mkString("\n")}")
//        logger.info(s"Total amount: $totalAmount")

        (1 to numTransferRequests).map(_ => transferService.makeTransfer(TransferRequest(
          accounts(Random.nextInt(accounts.size)).id,
          accounts(Random.nextInt(accounts.size)).id,
          Amount(randomNonNegativeBigInt)
        )))
//          .foreach(transfer => logger.info(s"$transfer"))

        val updatedAccounts = accountService.get
//        logger.info(s"Updated accounts:\n${updatedAccounts.mkString("\n")}")
        assert(updatedAccounts.map(_.amount).reduce(_ + _) === totalAmount)
      }
    }

    "invoked in parallel" should {
      "preserve the total amount of money in the bank" in {
        implicit val accountLockService: LockService[AccountId] = new LockService[AccountId]
        implicit val transferLockService: LockService[TransferId] = new LockService[TransferId]
        val accountDao = new Dao[Account, AccountId]
        val transferDao = new Dao[Transfer, TransferId]
        val accountIdGenerator = new AccountIdGenerator
        val transferIdGenerator = new TransferIdGenerator
        val accountService = new SynchronizedAccountService(accountDao, accountIdGenerator)
        val transferService = new SynchronizedTransferService(transferDao, accountDao, transferIdGenerator)
        val numAccounts = 10
        val numTransferRequests = 1000

        val accounts: Seq[Account] = (1 to numAccounts)
          .map(_ => accountService.create(AccountRequest(Amount(randomNonNegativeBigInt))))
          .filter(_.isRight).map(_.right.get)
        val totalAmount = accounts.map(_.amount).reduce(_ + _)
//                logger.info(s"Accounts: ${accounts.mkString("\n")}")
//                logger.info(s"Total amount: $totalAmount")

        val executor = Executors.newCachedThreadPool()
        val latch = new CountDownLatch(1)
        (1 to numTransferRequests).map(_ =>
          TransferRequest(
            accounts(Random.nextInt(accounts.size)).id,
            accounts(Random.nextInt(accounts.size)).id,
            Amount(randomNonNegativeBigInt)
          )
        ).map(transferRequest => executor.submit(
          new Runnable {
            override def run(): Unit = {
              latch.await()
//              val transfer = transferService.makeTransfer(transferRequest)
//              logger.info(s"$transfer")
              transferService.makeTransfer(transferRequest)
              ()
            }
          }
        ))
        latch.countDown()
        executor.awaitTermination(30, TimeUnit.SECONDS)
        executor.shutdown()

        val updatedAccounts = accountService.get
//        logger.info(s"Updated accounts:\n${updatedAccounts.mkString("\n")}")

        assert(updatedAccounts.map(_.amount).reduce(_ + _) === totalAmount)
      }
    }

  }

  private def randomNonNegativeBigInt: NonNegativeBigInt =
    refineV[NonNegative](BigInt(Random.nextInt())) match {
      case Right(n) => n
      case Left(_) => randomNonNegativeBigInt
    }
}
