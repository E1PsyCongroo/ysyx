package rvcpu.utility

import chisel3._
import chisel3.util._
import coursier.core.shaded.geny.Generator.End

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
    val in      = Input(UInt(w.W))
    val shamt   = Input(UInt(log2Up(w).W))
    val isLeft  = Input(Bool())
    val isArith = Input(Bool())
    val out     = Output(UInt(w.W))
  })
  val leftIn = Mux(io.isArith, io.in(w - 1), false.B) // 右移时从左边移入的位
  def layer(din: Seq[Bool], n: Int): Seq[Bool] = { // 描述第n级选择器如何排布
    val s = 1 << n // 需要移动的位数
    def shiftRight(i: Int) = if (i + s >= w) leftIn else din(i + s) // 描述右移时第i位输出
    def shiftLeft(i:  Int) = if (i < s) false.B else din(i - s) // 描述左移时第i位输出
    val sel = Cat(io.isLeft, io.shamt(n)) // 将移位方向和移位量作为选择器的选择信号
    din.zipWithIndex.map {
      case (b, i) => // 对于每一位输入b,
        VecInit(b, shiftRight(i), b, shiftLeft(i))(sel)
    } // 都从4种输入中选择一种作为输出
  }
  def barrelshift(din: Seq[Bool], k: Int): Seq[Bool] = // 描述有k级的桶形移位器如何排布
    if (k == 0) din // 若移位器只有0级, 则结果和输入相同
    // 否则实例化一个有k-1级的桶形移位器和第k-1级选择器, 并将后者的输出作为前者的输入
    else barrelshift(layer(din, k - 1), k - 1)
  io.out := Cat(barrelshift(io.in.asBools, log2Up(w)).reverse) // 实例化一个有log2(w)级的桶形移位器
}

class LSFR extends Module {
  val io = IO(new Bundle {
    val next = Input(Bool())
    val out  = Output(UInt(8.W))
  })
  val reg    = RegInit(1.U(8.W))
  val newBit = reg(4) ^ reg(3) ^ reg(2) ^ reg(0)
  reg    := Mux(io.next, Mux(reg === 0.U, 1.U, newBit ## reg(7, 1)), reg)
  io.out := reg
}

object LSFR {
  def apply(next: Bool) = {
    val lsfr = Module(new LSFR)
    lsfr.io.next := next
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

class CacheCount extends BlackBox with HasBlackBoxResource {
  val io = IO(new Bundle {
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
