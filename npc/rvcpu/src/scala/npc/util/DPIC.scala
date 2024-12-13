package rvcpu.utility

import rvcpu._
import rvcpu.core._
import rvcpu.axi4._
import rvcpu.dev._
import chisel3._
import chisel3.util._
import os.stat

class StateSyncer extends BlackBox with HasBlackBoxResource {
  val io = IO(new Bundle {
    val clock              = Input(Clock())
    val reset              = Input(Reset())
    val en                 = Input(Bool())
    val icacheCostCycle    = Input(UInt(64.W))
    val icacheMissCost     = Input(UInt(64.W))
    val iduCostCycle       = Input(UInt(64.W))
    val exuCostCycle       = Input(UInt(64.W))
    val lsuCostCycle       = Input(UInt(64.W))
    val wbuCostCycle       = Input(UInt(64.W))
    val pc                 = Input(UInt(32.W))
    val npc                = Input(UInt(32.W))
    val inst               = Input(UInt(32.W))
    val icacheNeed         = Input(Bool())
    val icacheHit          = Input(Bool())
    val skipDifftest       = Input(Bool())
    val simEnd             = Input(Bool())
    val exitCode           = Input(UInt(32.W))
    val csrState           = Input(Vec(6, UInt(32.W)))
    val memRen             = Input(Bool())
    val memWen             = Input(Bool())
    val memAccessSize      = Input(UInt(3.W))
    val memAddr            = Input(UInt(32.W))
    val memData            = Input(UInt(32.W))
    val gprState           = Input(Vec(32, UInt(32.W)))
  })
  addResource("/StateSyncer.sv")
}

case class DPICConfig(
  useDPIC:      Boolean,
  skipDiffArea: Seq[AddressSet])

class DPIC(cpuConfig: CPUConfig, dpicConfig: DPICConfig) extends Module {
  val io          = IO(Flipped(DecoupledIO(new CPUState(cpuConfig))))
  val stateSyncer = Module(new StateSyncer)

  val state            = io.bits
  val csrState         = state.csrState
  val gprState         = state.gprState
  val needSkipDifftest = dpicConfig.skipDiffArea.map { dev => dev.contains(state.memAddr) }.foldLeft(false.B)(_ || _)

  stateSyncer.io.clock              := clock
  stateSyncer.io.reset              := reset
  stateSyncer.io.en                 := io.fire
  stateSyncer.io.icacheCostCycle    := state.icacheCycle - state.ifuCycle
  stateSyncer.io.icacheMissCost     := state.icacheMissTime
  stateSyncer.io.iduCostCycle       := state.iduCycle - state.icacheCycle
  stateSyncer.io.exuCostCycle       := state.exuCycle - state.iduCycle
  stateSyncer.io.lsuCostCycle       := state.lsuCycle - state.exuCycle
  stateSyncer.io.wbuCostCycle       := state.wbuCycle - state.lsuCycle
  stateSyncer.io.pc                 := state.pc
  stateSyncer.io.npc                := state.npc
  stateSyncer.io.inst               := state.inst
  stateSyncer.io.icacheNeed         := state.icacheNeed
  stateSyncer.io.icacheHit          := state.icacheHit
  stateSyncer.io.skipDifftest       := (state.memRen || state.memWen) && needSkipDifftest
  stateSyncer.io.simEnd             := state.isEnd
  stateSyncer.io.exitCode           := state.exitCode
  stateSyncer.io.csrState           := csrState.csrs
  stateSyncer.io.memRen             := state.memRen
  stateSyncer.io.memWen             := state.memWen
  stateSyncer.io.memAccessSize      := state.memAccessSize
  stateSyncer.io.memAddr            := state.memAddr
  stateSyncer.io.memData            := state.memData
  if (cpuConfig.extentionE) {
    stateSyncer.io.gprState := gprState.gprs ++ VecInit.fill(16)(0.U)
  } else {
    stateSyncer.io.gprState := gprState.gprs
  }

  io.ready := true.B
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
    val mtvec      = Input(UInt(32.W))
    val mepc       = Input(UInt(32.W))
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
      (io.cacheAccessStart && io.cacheAccessFinish) -> access,
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
  cacheTracer.io.enable       := (access || io.cacheAccessStart) && io.cacheAccessFinish && io.cacheNeed
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
