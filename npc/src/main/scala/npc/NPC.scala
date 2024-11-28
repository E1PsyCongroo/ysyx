package rvcpu

import rvcpu.core._
import rvcpu.axi4._
import rvcpu.dev._
import rvcpu.utility._
import chisel3._
import chisel3.util._

object StageConnect {
  var arch = "pipeline"

  def apply[T <: Data](
    leftOut:  DecoupledIO[T],
    rightIn:  DecoupledIO[T],
    rightOut: DecoupledIO[T],
    flush:    Option[Bool] = None
  ) = {
    if (arch == "single") {
      rightIn.bits  := leftOut.bits
      leftOut.ready := true.B
      rightIn.valid := true.B
    } else if (arch == "multi") {
      rightIn <> leftOut
    } else if (arch == "pipeline") {
      leftOut.ready := rightIn.ready
      rightIn.bits  := RegEnable(leftOut.bits, leftOut.fire)
      if (flush.isEmpty) {
        rightIn.valid := RegNext(leftOut.fire || (!rightOut.fire && rightIn.valid), false.B)
      } else {
        rightIn.valid := RegNext(!flush.get && (leftOut.fire || (!rightOut.fire && rightIn.valid)), false.B)
      }
    } else if (arch == "ooo") {
      rightIn <> Queue(leftOut, 16)
    }
  }
}

class NPC(
  awidth:     Int     = 32,
  xlen:       Int     = 32,
  extentionE: Boolean = true,
  PCReset:    BigInt  = BigInt("80000000", 16),
  sim:        Boolean = true)
    extends Module {

  val needCache: UInt => Bool = Dev.memoryAddr.in
  val IFU     = Module(new IFU(xlen, PCReset))
  val ICache  = Module(new ICache(awidth, xlen, 6, 5, 0, needCache, sim))
  val IDU     = Module(new IDU(xlen, extentionE, sim))
  val EXU     = Module(new EXU(xlen, extentionE, sim))
  val LSU     = Module(new LSU(xlen, extentionE, sim))
  val WBU     = Module(new WBU(xlen, extentionE, sim))
  val RegFile = Module(new RegFile(xlen, if (extentionE) 4 else 5))
  val AXI4Mem = Module(new AXI4Mem(awidth, xlen))
  val CLINT   = Module(new CLINT(awidth, xlen, Dev.mtimeAddr))
  val Uart    = Module(new Uart(awidth, xlen))

  StageConnect(IFU.io.out, ICache.io.in, ICache.io.out, Some(EXU.io.jump))
  StageConnect(ICache.io.out, IDU.io.in, IDU.io.out, Some(EXU.io.jump))
  StageConnect(IDU.io.out, EXU.io.in, EXU.io.out)
  StageConnect(EXU.io.out, LSU.io.in, LSU.io.out)
  StageConnect(LSU.io.out, WBU.io.in, WBU.io.out)

  IFU.io.jump   := EXU.io.jump
  IFU.io.nextPC := EXU.io.nextPC

  ICache.io.clear := IDU.io.fence_i

  RegFile.io.ra1 := IDU.io.RegFileAccess.ra1
  RegFile.io.ra2 := IDU.io.RegFileAccess.ra2
  def conflict(ra: UInt, wa: UInt, we: Bool) = we && wa =/= 0.U && ra === wa
  val isEXURa1RAW    = conflict(IDU.io.RegFileAccess.ra1, EXU.io.RegFileAccess.wa, EXU.io.RegFileAccess.we)
  val isEXURa2RAW    = conflict(IDU.io.RegFileAccess.ra2, EXU.io.RegFileAccess.wa, EXU.io.RegFileAccess.we)
  val isEXUForward   = !EXU.io.in.bits.control.wbSrc(0) && EXU.io.out.valid
  val EXUForwardData = EXU.io.out.bits.alu_csr_Out
  val isLSURa1RAW    = conflict(IDU.io.RegFileAccess.ra1, LSU.io.RegFileAccess.wa, LSU.io.RegFileAccess.we)
  val isLSURa2RAW    = conflict(IDU.io.RegFileAccess.ra2, LSU.io.RegFileAccess.wa, LSU.io.RegFileAccess.we)
  val isLSUForward   = Mux(LSU.io.in.bits.control.wbSrc.asBool, LSU.io.out.valid, LSU.io.in.valid)
  val LSUForwardData = LSU.io.out.bits.wd
  val isWBURa1RAW    = conflict(IDU.io.RegFileAccess.ra1, WBU.io.RegFileAccess.wa, WBU.io.RegFileAccess.we)
  val isWBURa2RAW    = conflict(IDU.io.RegFileAccess.ra2, WBU.io.RegFileAccess.wa, WBU.io.RegFileAccess.we)
  val isWBUForward   = WBU.io.in.valid
  val WBUForwardData = WBU.io.RegFileAccess.wd
  IDU.io.RegFileReturn.rd1 := MuxCase(
    RegFile.io.rd1,
    Seq(
      (isEXURa1RAW && isEXUForward) -> EXUForwardData,
      (isLSURa1RAW && isLSUForward) -> LSUForwardData,
      (isWBURa1RAW && isWBUForward) -> WBUForwardData
    )
  )
  IDU.io.RegFileReturn.rd2 := MuxCase(
    RegFile.io.rd2,
    Seq(
      (isEXURa2RAW && isEXUForward) -> EXUForwardData,
      (isLSURa2RAW && isLSUForward) -> LSUForwardData,
      (isWBURa2RAW && isWBUForward) -> WBUForwardData
    )
  )
  IDU.io.stall := ((isEXURa1RAW || isEXURa2RAW) && !isEXUForward) || ((isLSURa1RAW || isLSURa2RAW) && !isLSUForward) || ((isWBURa1RAW || isWBURa2RAW) && !isWBUForward) || EXU.io.jump

  WBU.io.out.ready := true.B
  RegFile.io.wa    := WBU.io.RegFileAccess.wa
  RegFile.io.we    := WBU.io.RegFileAccess.we
  RegFile.io.wd    := WBU.io.RegFileAccess.wd

  AXI4Interconnect(
    Seq(LSU.io.master, ICache.io.master),
    Seq(CLINT.io, Uart.io, AXI4Mem.io),
    Seq(Dev.mtimeAddr.in, Dev.uartAddr.in, addr => !Dev.mtimeAddr.in(addr) && !Dev.uartAddr.in(addr)),
  )

  if (sim) {
    val cycle = RegInit(0.U(64.W))
    cycle := cycle + 1.U

    ICache.io.curCycle.get := cycle

    val InstTracer = Module(new InstTracer)
    InstTracer.io.clock      := clock
    InstTracer.io.npc        := WBU.io.out.bits.nextPC.get
    InstTracer.io.inst       := WBU.io.out.bits.inst.get
    InstTracer.io.exec_cycle := cycle - WBU.io.out.bits.fetchCycle.get
    InstTracer.io.en         := WBU.io.out.fire

    EndControl(clock, WBU.io.out.fire && WBU.io.out.bits.isEnd.get, WBU.io.out.bits.exitCode.get)

    val devs = Seq(
      Dev.mtimeAddr,
      Dev.uartAddr
    )
    val needSkipDifftest = devs.map { dev => dev.in(WBU.io.out.bits.memAddr.get) }.foldLeft(false.B)(_ || _)
    SkipDifftest(clock, WBU.io.out.fire && WBU.io.out.bits.memAccess.get && needSkipDifftest)

    val TracerDataFetch = Module(new TracerDataFetch)
    TracerDataFetch.io.clock  := clock
    TracerDataFetch.io.reset  := reset
    TracerDataFetch.io.start  := LSU.io.master.arvalid
    TracerDataFetch.io.finish := LSU.io.master.rvalid && LSU.io.master.rready

    val CacheTracer = Module(new CacheTracer)
    CacheTracer.io.cacheNeed         := ICache.io.trace.get.need
    CacheTracer.io.cacheHit          := ICache.io.trace.get.hit
    CacheTracer.io.cacheAccessStart  := IFU.io.out.fire
    CacheTracer.io.cacheAccessFinish := ICache.io.out.fire
    CacheTracer.io.cacheFetchStart   := ICache.io.master.arvalid
    CacheTracer.io.cacheFetchFinish  := ICache.io.master.rvalid && ICache.io.master.rlast && ICache.io.master.rready
  }
}
