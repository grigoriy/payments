package com.galekseev.payments
import org.scalatest.{ Matchers, WordSpecLike }
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

/**
  * Example:
  *
  * val container = Gen.containerOf[List, Int](Gen.oneOf(1, 2, 3))
  * val pickNItems = Gen.pick(2, 1 to 3)
  * val zeroToNItems = Gen.someOf(1 to 3)
  * val oneToNItems = Gen.atLeastOne(1 to 3)
  * val shuffledZeroToNItems = zeroToNItems.map(Random.shuffle(_))
  *
  * "hasThreeSum" when {
  *     "given collections that have a matching three-sum" should {
  *       "return true" in {
  *         forAll(minSuccessful(1000), sizeRange(PosZInt(20))) {
  *           (ints: Vector[Int], sum: Long) =>
  *             whenever(
  *               (for {
  *                 a <- ints
  *                 b <- ints
  *                 c <- ints
  *                 if a.toLong + b.toLong - c.toLong == sum
  *               } yield ()).nonEmpty
  *             ) {
  *               hasThreeSum(ints, sum) shouldEqual true
  *             }
  *         }
  *       }
  *     }
  *   }
  */
trait PropertiesBasedSpec extends ScalaCheckDrivenPropertyChecks with WordSpecLike with Matchers
