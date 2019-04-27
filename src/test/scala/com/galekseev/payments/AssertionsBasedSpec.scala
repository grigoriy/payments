package com.galekseev.payments
import org.scalatest.{ Matchers, WordSpecLike }

/*
 * Example:
 *
 * "entity (in this context/state)" when {
 *   "interacted with (in this way)" should {
 *     "react (in this way)" in {
 *
 *       assert(1 === 1)
 *     }
 *   }
 * }
 */
trait AssertionsBasedSpec extends WordSpecLike with Matchers
