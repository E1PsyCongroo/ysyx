package rvcpu.utility

import rvcpu._
import rvcpu.core._
import chisel3._
import chisel3.util._
import scala.math._

class DividerIn(dwidth: Int) extends Bundle {
  val signed   = Bool
  val dividend = UInt(dwidth.W)
  val divisor  = UInt(dwidth.W)
}

class DividerOut(dwidth: Int) extends Bundle {
  val quotient  = UInt(dwidth.W)
  val remainder = UInt(dwidth.W)
}

class DividerIO(dwidth: Int) extends Bundle {
  val flush = Input(Bool())
  val in    = Flipped(DecoupledIO(new MultiplierIn(dwidth)))
  val out   = DecoupledIO(new MultiplierOut(dwidth))
}

abstract class Divider(dwidth: Int) extends Module {
  val io = IO(new DividerIO(dwidth))
}

class BoothDivider(dwidth: Int) extends Divider(dwidth) {
  require(dwidth % 2 == 0)

  val in = io.in.bits
  def GenPartialProduct(y_add: Bool, y: Bool, y_sub: Bool, multiplicand: UInt): UInt = {
    val width               = multiplicand.getWidth
    val sel_negative        = WireDefault(y_add & (y & ~y_sub | ~y & y_sub))
    val sel_positive        = WireDefault(~y_add & (y & ~y_sub | ~y & y_sub))
    val sel_double_negative = WireDefault(y_add & ~y & ~y_sub)
    val sel_double_positive = WireDefault(~y_add & y & y_sub)
    val x                   = multiplicand(width - 1) ## multiplicand ## 0.U
    val partialProduct = for (i <- 1 to width + 1) yield {
      WireDefault(
        (sel_negative & ~x(i)) | (sel_double_negative & ~x(i - 1)) |
          (sel_positive & x(i)) | (sel_double_positive & x(i - 1))
      )
    }
    val result = Cat(partialProduct.toSeq.reverse) + (sel_negative || sel_double_negative)
    require(result.getWidth == width + 1)
    result
  }
  val multiplicandSigned = RegEnable(in.signed(1), io.in.fire)
  val multiplierWidth    = dwidth + 2
  // 乘数双符号/零扩展 和 最低位补0 需要 3 bits
  val result     = Reg(UInt((dwidth + multiplierWidth + 1).W))
  val nextResult = Wire(result.cloneType)
  // 被乘数符号/零扩展 需要 1 bit
  val multiplicand_r = RegEnable(Mux(in.signed(1), in.multiplicand(dwidth - 1), 0.U) ## in.multiplicand, io.in.fire)
  // 部分积 宽度为 dwidth + 2(可能的左移与符号扩展)
  val partialProduct = WireDefault(GenPartialProduct(result(2), result(1), result(0), multiplicand_r))
  val cycleCounter   = Reg(UInt(log2Ceil(multiplierWidth / 2).W))
  val last           = WireDefault(cycleCounter === (multiplierWidth / 2 - 1).U)

  val sIdle :: sMul :: sSend :: Nil = Enum(3)

  val state  = RegInit(sIdle)
  val isIdle = state === sIdle
  val isMul  = state === sMul
  val isSend = state === sSend
  state := Mux(
    io.flush,
    sIdle,
    MuxLookup(state, sIdle)(
      Seq(
        sIdle -> Mux(io.in.fire, sMul, sIdle),
        sMul -> Mux(last, sSend, sMul),
        sSend -> Mux(io.out.fire, sIdle, sSend)
      )
    )
  )

  result := MuxCase(
    DontCare,
    Seq(
      io.in.fire -> Fill(2, Mux(in.signed(0), in.multiplier(dwidth - 1), 0.U(1.W))) ## in.multiplier ## 0.U(1.W),
      isMul -> nextResult,
      isSend -> result
    )
  )
  nextResult := (partialProduct + (Fill(2, result(dwidth + multiplierWidth)) ## result(
    dwidth + multiplierWidth,
    multiplierWidth + 1
  ))) ## result(multiplierWidth, 2)
  cycleCounter := MuxCase(
    DontCare,
    Seq(
      io.in.fire -> 0.U,
      isMul -> (cycleCounter + 1.U)
    )
  )

  io.in.ready           := isIdle
  io.out.valid          := isSend && !io.flush
  io.out.bits.result_lo := result(dwidth, 1)
  io.out.bits.result_hi := result(dwidth * 2, dwidth + 1)
}
