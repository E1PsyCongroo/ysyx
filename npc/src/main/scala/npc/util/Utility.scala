package rvcpu.utility

import chisel3._
import chisel3.util._

class AdderOut(width: Int) extends Bundle {
  val carry    = Output(Bool())
  val zero     = Output(Bool())
  val overflow = Output(Bool())
  val result   = Output(UInt(width.W))
}

object Adder {
  def apply(ina: UInt, inb: UInt, addSub: Bool): AdderOut = {
    require(ina.getWidth == inb.getWidth)
    val width  = ina.getWidth
    val out    = Wire(new AdderOut(width))
    val tA     = ina
    val tB     = inb
    val tnoCin = (Fill(width, addSub) ^ tB)
    val result = Wire(UInt((width + 1).W))
    result       := tA +& tnoCin +& addSub
    out.result   := result(width - 1, 0)
    out.carry    := result(width).asBool
    out.zero     := ~(out.result.orR)
    out.overflow := (tA(width - 1) === tnoCin(width - 1)) && (result(width - 1) =/= tA(width - 1))
    out
  }
}

object Shifter {
  def apply(in: UInt, shamt: UInt, isLeft: Bool, isArith: Bool): UInt = {
    val width = in.getWidth
    require(width == (1 << shamt.getWidth))
    val sll = in << shamt
    val srl = in >> shamt
    val sra = (in.asSInt >> shamt).asUInt
    val out = Wire(UInt(width.W))
    out := Mux(isLeft, sll, Mux(isArith, sra, srl))
    out
  }
}

object BarrelShifter {
  def apply(in: UInt, shamt: UInt, isLeft: Bool, isArith: Bool): UInt = {
    val w = in.getWidth
    require(w == (1 << shamt.getWidth))
    val leftIn = Mux(isArith, in(w - 1), false.B) // 右移时从左边移入的位
    def layer(din: Seq[Bool], n: Int): Seq[Bool] = { // 描述第n级选择器如何排布
      val s = 1 << n // 需要移动的位数
      def shiftRight(i: Int) = if (i + s >= w) leftIn else din(i + s) // 描述右移时第i位输出
      def shiftLeft(i:  Int) = if (i < s) false.B else din(i - s) // 描述左移时第i位输出
      val sel = Cat(isLeft, shamt(n)) // 将移位方向和移位量作为选择器的选择信号
      din.zipWithIndex.map {
        case (b, i) => // 对于每一位输入b,
          VecInit(b, shiftRight(i), b, shiftLeft(i))(sel)
      } // 都从4种输入中选择一种作为输出
    }
    def barrelshift(din: Seq[Bool], k: Int): Seq[Bool] = // 描述有k级的桶形移位器如何排布
      if (k == 0) din // 若移位器只有0级, 则结果和输入相同
      // 否则实例化一个有k-1级的桶形移位器和第k-1级选择器, 并将后者的输出作为前者的输入
      else barrelshift(layer(din, k - 1), k - 1)
    val out = Wire(UInt(w.W))
    out := Cat(barrelshift(in.asBools, log2Up(w)).reverse) // 实例化一个有log2(w)级的桶形移位器
    out
  }
}

object LSFR {
  def width = 8.W
  def apply(next: Bool): UInt = {
    val reg    = RegInit(1.U(width))
    val newBit = reg(4) ^ reg(3) ^ reg(2) ^ reg(0)
    reg := Mux(next, Mux(reg === 0.U, 1.U, newBit ## reg(7, 1)), reg)
    reg
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

class Tracer extends BlackBox with HasBlackBoxResource {
  val io = IO(new Bundle {
    val raddr = Input(UInt(32.W))
    val rlen  = Input(UInt(32.W))
    val rdata = Input(UInt(32.W))
    val ren   = Input(Bool())
    val waddr = Input(UInt(32.W))
    val wlen  = Input(UInt(32.W))
    val wdata = Input(UInt(32.W))
    val wen   = Input(Bool())
  })
  addResource("/Tracer.sv")
}

class TracerDataFetch extends BlackBox with HasBlackBoxResource {
  val io = IO(new Bundle {
    val clock  = Input(Clock())
    val reset  = Input(Bool())
    val start  = Input(Bool())
    val finish = Input(Bool())
  })
  addResource("/TracerDataFetch.sv")
}

class InstTracer extends BlackBox with HasBlackBoxResource {
  val io = IO(new Bundle {
    val clock      = Input(Clock())
    val npc        = Input(UInt(32.W))
    val inst       = Input(UInt(32.W))
    val exec_cycle = Input(UInt(64.W))
    val en         = Input(Bool())
  })
  addResource("/InstTracer.sv")
}

class CacheCount extends BlackBox with HasBlackBoxResource {
  val io = IO(new Bundle {
    val clock        = Input(Clock())
    val enable       = Input(Bool())
    val hit          = Input(UInt(32.W))
    val access_cycle = Input(UInt(32.W))
    val miss_penalty = Input(UInt(32.W))
  })
  addResource("/CacheCount.sv")
}

class CacheTracer extends Module {
  val io = IO(new Bundle {
    val cacheNeed         = Input(Bool())
    val cacheHit          = Input(Bool())
    val cacheAccessStart  = Input(Bool())
    val cacheAccessFinish = Input(Bool())
    val cacheFetchStart   = Input(Bool())
    val cacheFetchFinish  = Input(Bool())
  })

  val access           = RegInit(false.B)
  val accessCount      = RegInit(0.U(32.W))
  val fetch            = RegInit(false.B)
  val missPenaltyCount = RegInit(0.U(32.W))

  access := MuxCase(
    access,
    Seq(
      io.cacheAccessStart -> true.B,
      io.cacheAccessFinish -> false.B
    )
  )

  accessCount := MuxCase(
    0.U,
    Seq(
      io.cacheAccessFinish -> 0.U,
      access -> (accessCount + 1.U)
    )
  )

  fetch := MuxCase(
    fetch,
    Seq(
      io.cacheFetchStart -> true.B,
      io.cacheFetchFinish -> false.B
    )
  )

  missPenaltyCount := MuxCase(
    0.U,
    Seq(
      fetch -> (missPenaltyCount + 1.U)
    )
  )

  val cacheTracer = Module(new CacheCount)
  cacheTracer.io.clock        := clock
  cacheTracer.io.enable       := io.cacheAccessFinish && io.cacheNeed
  cacheTracer.io.hit          := io.cacheHit
  cacheTracer.io.access_cycle := accessCount - missPenaltyCount + 1.U
  cacheTracer.io.miss_penalty := missPenaltyCount
}

class EndControlIO extends Bundle {
  val clock = Input(Clock())
  val isEnd = Input(Bool())
  val code  = Input(UInt(32.W))
}

class EndControl extends BlackBox with HasBlackBoxResource {
  val io = IO(new EndControlIO)
  addResource("/EndControl.sv")
}

object EndControl {
  def apply(clock: Clock, isEnd: Bool, exitCode: UInt): Unit = {
    val module = Module(new EndControl)
    module.io.clock := clock;
    module.io.isEnd := isEnd;
    module.io.code  := exitCode;
  }
}
