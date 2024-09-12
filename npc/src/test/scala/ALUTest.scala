import org.scalatest._

import chisel3._
import chisel3.util._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers


class ALUTest extends AnyFlatSpec with ChiselScalatestTester with Matchers {
  behavior of "ALU"
  it should "pass" in {
    test(new rvcpu.core.ALU(32)).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      dut.io.aluCtr.poke("b0000".U)
      dut.io.inA.poke(123.U)
      dut.io.inB.poke(456.U)
      dut.io.aluOut.expect((123+456).U)
    }
  }
}
