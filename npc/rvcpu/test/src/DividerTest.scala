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

class DividerTest(width: Int, divider: => Divider) extends Module {
  val io  = IO(Flipped(DecoupledIO(new DividerIn(width))))
  val dut = Module(divider)
  require(dut.io.in.bits.dividend.getWidth == width)

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

  val in = RegEnable(io.bits, dut.io.in.fire)
  val refQuotient = RegEnable(
    Mux(
      io.bits.signed,
      (io.bits.dividend.asSInt / io.bits.divisor.asSInt).asUInt,
      (io.bits.dividend.asUInt / io.bits.divisor.asUInt).asUInt
    ),
    dut.io.in.fire
  )(width - 1, 0)
  val refRemainder = RegEnable(
    Mux(
      io.bits.signed,
      (io.bits.dividend.asSInt % io.bits.divisor.asSInt).asUInt,
      (io.bits.dividend.asUInt % io.bits.divisor.asUInt).asUInt
    ),
    dut.io.in.fire
  )

  dut.io.flush := false.B
  dut.io.in <> io
  dut.io.out.ready := isGetDutResult
  val dutResult = WireDefault(dut.io.out.bits)

  when(dut.io.out.fire) {
    when(in.divisor === 0.U) {
      assert(
        (dutResult.quotient === Fill(width, 1.U(1.W))) && dutResult.remainder === in.dividend,
        cf"\nsigned: ${in.signed}%b, dividend: ${in.dividend}%b, divisor: ${in.divisor}%b\n" +
          cf"refQuotient: ${Fill(width, 1.U(1.W))}%x, dutQuotient: ${dutResult.quotient}%x\n" +
          cf"refRemainder: ${in.dividend}%x, dutRemainder: ${dutResult.remainder}%x\n"
      )
    }.otherwise {
      assert(
        (refQuotient === dutResult.quotient) && (refRemainder === dutResult.remainder),
        cf"\nsigned: ${in.signed}%b, dividend: ${in.dividend}%b, divisor: ${in.divisor}%b\n" +
          cf"refQuotient: ${refQuotient}%x, dutQuotient: ${dutResult.quotient}%x\n" +
          cf"refRemainder: ${refRemainder}%x, dutRemainder: ${dutResult.remainder}%x\n"
      )
    }
  }
}

class BoothDividerFormalTest extends AnyFlatSpec with ChiselScalatestTester with Formal {
  behavior.of("BoothDivider")
  it should "pass" in {
    val width = 8
    verify(
      new DividerTest(width, new BoothDivider(width)),
      Seq(BoundedCheck(10), BtormcEngineAnnotation)
    )
  }
}
