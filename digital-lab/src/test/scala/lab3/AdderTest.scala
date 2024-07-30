package scala.lab3

import org.scalatest._

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers


class AdderTest extends AnyFlatSpec with ChiselScalatestTester with Matchers {
  behavior of "Adder"
  it should "work correctly for all inputs" in {
    test(new Adder(4)) { c =>
      for (i <- -8 to 7) {
        for (j <- -8 to 7) {
          // 测试加法
          c.io.ina.poke(i.S)
          c.io.inb.poke(j.S)
          c.io.addSub.poke(false.B)
          c.clock.step(1)

          // 计算预期结果并确保在范围内
          val expectedSum = (i + j) match {
            case sum if sum < -8 => sum + 16
            case sum if sum > 7  => sum - 16
            case sum             => sum
          }
          val expectedOverflow = ((i >= 0 && j >= 0 && expectedSum < 0) || (i < 0 && j < 0 && expectedSum >= 0))
          c.io.result.expect(expectedSum.S)
          c.io.overflow.expect(expectedOverflow.B)

          // 测试减法
          c.io.ina.poke(i.S)
          c.io.inb.poke(j.S)
          c.io.addSub.poke(true.B)
          c.clock.step(1)

          // 计算预期结果并确保在范围内
          val expectedDiff = (i - j) match {
            case diff if diff < -8 => diff + 16
            case diff if diff > 7  => diff - 16
            case diff              => diff
          }
          val expectedOverflowSub = ((i >= 0 && j < 0 && expectedDiff < 0) || (i < 0 && j >= 0 && expectedDiff >= 0))
          c.io.result.expect(expectedDiff.S)
          c.io.overflow.expect(expectedOverflowSub.B)
        }
      }
    }
  }
}
