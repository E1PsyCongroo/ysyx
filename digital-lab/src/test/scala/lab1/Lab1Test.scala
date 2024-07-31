import org.scalatest._

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class Lab1Test extends AnyFlatSpec with ChiselScalatestTester {
  behavior.of("Lab1")
  it should "select correct input" in {
    test(new Lab1) { dut =>
      dut.io.X0.poke(0.U)
      dut.io.X1.poke(1.U)
      dut.io.X2.poke(2.U)
      dut.io.X3.poke(3.U)
      dut.io.Y.poke(0.U)
      dut.io.F.expect(0.U)
      dut.io.Y.poke(1.U)
      dut.io.F.expect(1.U)
      dut.io.Y.poke(2.U)
      dut.io.F.expect(2.U)
      dut.io.Y.poke(3.U)
      dut.io.F.expect(3.U)
    }
  }
}
