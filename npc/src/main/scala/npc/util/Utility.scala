package rvcpu.utility

import chisel3._
import chisel3.util._

class Adder(width: Int) extends Module {
  val io = IO(new Bundle {
    val ina      = Input(SInt(width.W))
    val inb      = Input(SInt(width.W))
    val addSub   = Input(Bool())
    val carry    = Output(Bool())
    val zero     = Output(Bool())
    val overflow = Output(Bool())
    val result   = Output(SInt(width.W))
  })
  val tA     = io.ina.asUInt
  val tB     = io.inb.asUInt
  val tnoCin = (Fill(width, io.addSub.asUInt) ^ tB)
  val result = Wire(UInt((width + 1).W))
  result      := tA +& tnoCin +& io.addSub.asUInt
  io.result   := result(width - 1, 0).asSInt
  io.carry    := result(width).asBool
  io.zero     := ~(io.result.asUInt.orR)
  io.overflow := (tA(width - 1) === tnoCin(width - 1)) && (result(width - 1) =/= tA(width - 1))
}

class BarrelShift(w: Int) extends Module {
  val io = IO(new Bundle {
    val in = Input(UInt(w.W))
    val shamt = Input(UInt(log2Up(w).W))
    val isLeft = Input(Bool())
    val isArith = Input(Bool())
    val out = Output(UInt(w.W))
  })
  val leftIn = Mux(io.isArith, io.in(w-1), false.B) // 右移时从左边移入的位
  def layer(din: Seq[Bool], n: Int): Seq[Bool] = { // 描述第n级选择器如何排布
    val s = 1 << n   // 需要移动的位数
    def shiftRight(i: Int) = if (i + s >= w) leftIn else din(i + s) // 描述右移时第i位输出
    def shiftLeft (i: Int) = if (i < s) false.B else din(i - s) // 描述左移时第i位输出
    val sel = Cat(io.isLeft, io.shamt(n)) // 将移位方向和移位量作为选择器的选择信号
    din.zipWithIndex.map{ case (b, i) =>                // 对于每一位输入b,
      VecInit(b, shiftRight(i), b, shiftLeft(i))(sel) } // 都从4种输入中选择一种作为输出
  }
  def barrelshift(din: Seq[Bool], k: Int): Seq[Bool] = // 描述有k级的桶形移位器如何排布
    if (k == 0) din  // 若移位器只有0级, 则结果和输入相同
    // 否则实例化一个有k-1级的桶形移位器和第k-1级选择器, 并将后者的输出作为前者的输入
    else barrelshift(layer(din, k - 1), k - 1)
  io.out := Cat(barrelshift(io.in.asBools, log2Up(w)).reverse) // 实例化一个有log2(w)级的桶形移位器
}

class LSFR extends Module {
  val io = IO(new Bundle {
    val next = Input(Bool())
    val out = Output(UInt(8.W))
  })
  val reg    = RegInit(1.U(8.W))
  val newBit = reg(4) ^ reg(3) ^ reg(2) ^ reg(0)
  reg    := Mux(io.next, Mux(reg === 0.U, 1.U, newBit ## reg(7, 1)), reg)
  io.out := reg
}

object LSFR {
  def apply(next: Bool) = {
    val lsfr = Module(new LSFR)
    lsfr.io.next  := next
    lsfr.io.out
  }
}

class SkipDifftest extends BlackBox with HasBlackBoxResource {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val skip  = Input(Bool())
  })
  addResource("/SkipDifftest.sv")
}

object SkipDifftest {
  def apply(clock: Clock, skip: Bool) = {
    val SkipDifftest = Module(new SkipDifftest)
    SkipDifftest.io.clock := clock
    SkipDifftest.io.skip  := skip
  }
}

class SramTracer extends BlackBox with HasBlackBoxResource {
  val io = IO(new Bundle {
    val raddr = Input(UInt(32.W))
    val rdata = Input(UInt(32.W))
    val ren   = Input(Bool())
    val waddr = Input(UInt(32.W))
    val wdata = Input(UInt(32.W))
    val wlen  = Input(UInt(32.W))
    val wen   = Input(Bool())
  })
  addResource("/SramTracer.sv")
}

