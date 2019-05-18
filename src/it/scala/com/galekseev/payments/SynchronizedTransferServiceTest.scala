package com.galekseev.payments

import java.util.concurrent.{CountDownLatch, Executors, TimeUnit}

import com.galekseev.payments.dto.Amount.NonNegativeBigInt
import com.galekseev.payments.dto._
import com.typesafe.scalalogging.StrictLogging
import eu.timepit.refined.auto._
import eu.timepit.refined.numeric.NonNegative
import eu.timepit.refined.refineV
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{Matchers, WordSpec}

import scala.util.Random

// scalastyle:off magic.number
class SynchronizedTransferServiceTest extends WordSpec with Matchers with ScalaFutures with StrictLogging {

  "SynchronizedTransferService" when {

    "invoked serially" should {
      "preserve the total amount of money in the bank" in {
        val config = new TestConfig()
        val accountService = config.accountService
        val transferService = config.transferService
        val numAccounts = 10
        val numTransferRequests = 100
        val accounts: Seq[Account] = (1 to numAccounts)
          .map(_ => accountService.create(AccountRequest(Amount(randomNonNegativeBigInt))))
          .filter(_.isRight).map(_.right.get)
        val totalAmount = accounts.map(_.balance).reduce(_ + _)

        (1 to numTransferRequests).map(_ => transferService.makeTransfer(TransferRequest(
          accounts(Random.nextInt(accounts.size)).id,
          accounts(Random.nextInt(accounts.size)).id,
          Amount(randomNonNegativeBigInt)
        )))

        val updatedAccounts = accountService.get
        assert(updatedAccounts.map(_.balance).reduce(_ + _) === totalAmount)
      }
    }

    "invoked in parallel" should {
      "preserve the total amount of money in the bank" in {
        val config = new TestConfig()
        val accountService = config.accountService
        val transferService = config.transferService
        val numAccounts = 10
        val numTransferRequests = 1000
        val accounts: Seq[Account] = (1 to numAccounts).map(_ =>
          accountService.create(AccountRequest(Amount(randomNonNegativeBigInt)))
        ).filter(_.isRight).map(_.right.get)
        val totalAmount = accounts.map(_.balance).reduce(_ + _)
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
              val randomFromHundred = Random.nextInt(100)
              if (randomFromHundred > 98)
                transferService.makeTransfer(transferRequest.copy(from = AccountId(-1)))
              else if (randomFromHundred > 96)
                transferService.makeTransfer(transferRequest.copy(to = AccountId(-1)))
              else if (randomFromHundred > 94)
                accountService.create(AccountRequest(Amount(BigInt(0L)))).map(newAccount =>
                  transferService.makeTransfer(transferRequest.copy(to = newAccount.id))
                )
              transferService.makeTransfer(transferRequest)
              ()
            }
          }
        ))
        latch.countDown()
        executor.awaitTermination(30, TimeUnit.SECONDS)
        executor.shutdown()

        val updatedAccounts = accountService.get
        assert(updatedAccounts.map(_.balance).reduce(_ + _) === totalAmount)
      }
    }

  }

  private def randomNonNegativeBigInt: NonNegativeBigInt =
    refineV[NonNegative](BigInt(Random.nextInt())) match {
      case Right(n) => n
      case Left(_) => randomNonNegativeBigInt
    }
}
