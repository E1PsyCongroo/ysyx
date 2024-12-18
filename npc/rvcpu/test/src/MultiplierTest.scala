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

class MultiplierTest(width: Int, multiplier: => Multiplier) extends Module {
  val io  = IO(Flipped(DecoupledIO(new MultiplierIn(width))))
  val dut = Module(multiplier)
  require(dut.io.in.bits.multiplicand.getWidth == width)

  val sIdle :: sGetDutResult :: sEnd :: Nil = Enum(3)

  val state          = RegInit(sIdle)
  val isIdle         = state === sIdle
  val isGetDutResult = state === sGetDutResult
  val isEnd          = state === sEnd

  state := MuxLookup(state, sIdle)(
    Seq(
      sIdle -> Mux(dut.io.in.fire, sGetDutResult, sIdle),
      sGetDutResult -> Mux(dut.io.out.fire, sIdle, sGetDutResult)
    )
  )

  when(isIdle && io.fire) {
    assume(io.bits.signed =/= "b01".U)
  }

  val in = RegEnable(io.bits, dut.io.in.fire)
  val ref = RegEnable(
    MuxLookup(io.bits.signed, io.bits.multiplicand * io.bits.multiplier)(
      Seq(
        "b11".U -> (io.bits.multiplicand.asSInt * io.bits.multiplier.asSInt).asUInt,
        "b10".U -> (io.bits.multiplicand.asSInt * io.bits.multiplier.asUInt).asUInt,
        "b00".U -> (io.bits.multiplicand.asUInt * io.bits.multiplier.asUInt).asUInt
      )
    ),
    dut.io.in.fire
  )

  dut.io.flush := false.B
  dut.io.in <> io
  dut.io.out.ready := isGetDutResult
  val dutResult = WireDefault(dut.io.out.bits)

  when(dut.io.out.fire) {
    assert(
      ref === (dutResult.result_hi ## dutResult.result_lo),
      cf"\nsigned: ${in.signed}%b, multiplicand: ${in.multiplicand}%b, multiplier: ${in.multiplier}%b\n" +
        cf"ref_hi: ${ref(width * 2 - 1, width)}%x, dut_hi: ${dutResult.result_hi}%x\n" +
        cf"ref_lo: ${ref(width - 1, 0)}%x, dut_lo: ${dutResult.result_lo}%x\n"
    )
  }
}

class ShiftMultiplierFormalTest extends AnyFlatSpec with ChiselScalatestTester with Formal {
  behavior.of("ShiftMultiplier")
  it should "pass" in {
    val width = 8
    verify(
      new MultiplierTest(width, new ShiftMultiplier(width)),
      Seq(BoundedCheck(10), BtormcEngineAnnotation)
    )
  }
}

class Booth2MultiplierFormalTest extends AnyFlatSpec with ChiselScalatestTester with Formal {
  behavior.of("Booth2Multiplier")
  it should "pass" in {
    val width = 8
    verify(
      new MultiplierTest(width, new Booth2Multiplier(width)),
      Seq(BoundedCheck(6), BtormcEngineAnnotation)
    )
  }
}

class WallaceMultiplierFormalTest extends AnyFlatSpec with ChiselScalatestTester with Formal {
  behavior.of("WallaceMultiplier")
  it should "pass" in {
    val width = 8
    verify(
      new MultiplierTest(width, new WallaceMultiplier(width, 2)),
      Seq(BoundedCheck(4), BtormcEngineAnnotation)
    )
  }
}
