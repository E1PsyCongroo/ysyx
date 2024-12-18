import org.scalatest._

import rvcpu._
import rvcpu.core._
import rvcpu.axi4._
import rvcpu.dev._
import rvcpu.utility._
import chisel3._
import chisel3.util._
import chiseltest._
import chiseltest.formal._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class AdderTest extends Module {
  val width = 4
  val io = IO(new Bundle {
    val ina    = Input(UInt(width.W))
    val inb    = Input(UInt(width.W))
    val addSub = Input(Bool())
  })
  val rcaResult   = RippleCarryAdder(io.ina, io.inb, io.addSub)
  val adderResult = Adder(io.ina, io.inb, io.addSub)

  assert(rcaResult === adderResult, p"in: ${io}, adder: ${adderResult}, rca: ${rcaResult}\n")
}

class AdderFormalTest extends AnyFlatSpec with ChiselScalatestTester with Formal {
  behavior.of("Adder")
  it should "pass" in {
    verify(new AdderTest, Seq(BoundedCheck(2), BtormcEngineAnnotation))
  }
}
