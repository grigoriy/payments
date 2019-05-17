package com.galekseev.payments

import java.util.UUID

import com.galekseev.payments.core.synched._
import com.galekseev.payments.dto.Amount.NonNegativeBigInt
import com.galekseev.payments.dto._
import com.galekseev.payments.storage.synched.Dao
import com.typesafe.scalalogging.StrictLogging
import eu.timepit.refined.numeric.NonNegative
import eu.timepit.refined.refineV
import org.scalatest.{Matchers, WordSpec}

import scala.util.Random

class SynchronizedTransferServiceTest extends WordSpec with Matchers with StrictLogging {
  lazy implicit private val accountLockService: LockService[AccountId] = new LockService[AccountId]
  lazy implicit private val transferLockService: LockService[TransferId] = new LockService[TransferId]
  lazy private val accountDao = new Dao[Account, AccountId]
  lazy private val transferDao = new Dao[Transfer, TransferId]
  lazy private val accountIdGenerator = new AccountIdGenerator
  lazy private val transferIdGenerator = new TransferIdGenerator
  lazy private val accountService = new SynchronizedAccountService(accountDao, accountIdGenerator)
  lazy private val transferService = new SynchronizedTransferService(transferDao, accountDao, transferIdGenerator)

  "SynchronizedTransferService" when {

    "invoked serially" should {
      "preserve the total amount of money in the bank" in {
        val accounts: Seq[Account] = (1 to 10)
          .map(_ => accountService.create(AccountRequest(Amount(randomNonNegativeBigInt))))
          .filter(_.isRight).map(_.right.get)
        val totalAmount = accounts.map(_.amount).reduce(_ + _)
//        logger.info(s"Accounts: ${accounts.mkString("\n")}")
//        logger.info(s"Total amount: $totalAmount")

        (1 to 100).map(_ => transferService.makeTransfer(TransferRequest(
          UUID.randomUUID(),
          accounts(Random.nextInt(accounts.size)).id,
          accounts(Random.nextInt(accounts.size)).id,
          Amount(randomNonNegativeBigInt)
        )))
//          .foreach(transfer => logger.info(s"$transfer"))

        val updatedAccounts = accountService.get
//        logger.info(s"Updated accounts: ${updatedAccounts.mkString("\n")}")
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
