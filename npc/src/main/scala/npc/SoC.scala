package rvcpu

import rvcpu.core._
import rvcpu.axi4._
import rvcpu.dev._
import rvcpu.utility._
import chisel3._
import chisel3.util._

class RVCPUIO(awidth: Int = 32, xlen: Int = 32) extends Bundle {
  val interrupt = Input(Bool())
  val master    = new AXI4MasterIO
  val slave     = Flipped(new AXI4MasterIO)
}

class RVCPU(
  awidth:     Int     = 32,
  xlen:       Int     = 32,
  extentionE: Boolean = true,
  PCReset:    BigInt  = BigInt("30000000", 16),
  sim:        Boolean = true)
    extends Module {
  val io = IO(new RVCPUIO(xlen))

  val needCache: UInt => Bool = addr =>
    Dev.MROMAddr.in(addr) || Dev.FlashAddr.in(addr) || Dev.ChipLinkMEMAddr.in(addr) || Dev.PSRAMAddr.in(
      addr
    ) || Dev.SDRAMAddr.in(addr)
  val IFU     = Module(new IFU(xlen, PCReset, sim))
  val ICache  = Module(new ICache(awidth, xlen, 6, 5, 0, needCache, sim))
  val IDU     = Module(new IDU(xlen, extentionE, sim))
  val EXU     = Module(new EXU(xlen, extentionE, sim))
  val LSU     = Module(new LSU(xlen, extentionE, sim))
  val WBU     = Module(new WBU(xlen, extentionE, sim))
  val RegFile = Module(new RegFile(xlen, if (extentionE) 4 else 5))
  val CLINT   = Module(new CLINT(awidth, xlen, Dev.CLINTAddr))

  StageConnect(IFU.io.out, ICache.io.in, ICache.io.out, Some(EXU.io.jump))
  StageConnect(ICache.io.out, IDU.io.in, IDU.io.out, Some(EXU.io.jump))
  StageConnect(IDU.io.out, EXU.io.in, EXU.io.out)
  StageConnect(EXU.io.out, LSU.io.in, LSU.io.out)
  StageConnect(LSU.io.out, WBU.io.in, WBU.io.out)

  IFU.io.jump   := EXU.io.jump
  IFU.io.nextPC := EXU.io.nextPC

  ICache.io.flush := EXU.io.jump
  ICache.io.clear := IDU.io.fence_i

  RegFile.io.ra1 := IDU.io.RegFileAccess.ra1
  RegFile.io.ra2 := IDU.io.RegFileAccess.ra2
  def conflict(ra: UInt, wa: UInt, we: Bool) = we && wa =/= 0.U && ra === wa
  val isEXURa1RAW = conflict(IDU.io.RegFileAccess.ra1, EXU.io.RegFileAccess.wa, EXU.io.RegFileAccess.we)
  val isEXURa2RAW = conflict(IDU.io.RegFileAccess.ra2, EXU.io.RegFileAccess.wa, EXU.io.RegFileAccess.we)
  val isEXUForward = MuxCase(
    false.B,
    Seq(
      WBSrcFrom.fromALU -> EXU.io.out.valid,
      WBSrcFrom.fromCSR -> EXU.io.out.valid
    ).map { case (key, data) => (EXU.io.in.bits.control.wbSrc === key, data) }
  )
  val EXUForwardData = MuxCase(
    DontCare,
    Seq(
      WBSrcFrom.fromALU -> EXU.io.out.bits.aluOut,
      WBSrcFrom.fromCSR -> EXU.io.out.bits.csrOut
    ).map { case (key, data) => (EXU.io.in.bits.control.wbSrc === key, data) }
  )
  val isLSURa1RAW = conflict(IDU.io.RegFileAccess.ra1, LSU.io.RegFileAccess.wa, LSU.io.RegFileAccess.we)
  val isLSURa2RAW = conflict(IDU.io.RegFileAccess.ra2, LSU.io.RegFileAccess.wa, LSU.io.RegFileAccess.we)
  val isLSUForward = MuxCase(
    false.B,
    Seq(
      WBSrcFrom.fromALU -> LSU.io.in.valid,
      WBSrcFrom.fromCSR -> LSU.io.in.valid,
      WBSrcFrom.fromMem -> LSU.io.out.valid
    ).map { case (key, data) => (LSU.io.in.bits.control.wbSrc === key, data) }
  )
  val LSUForwardData = MuxCase(
    DontCare,
    Seq(
      WBSrcFrom.fromALU -> LSU.io.in.bits.aluOut,
      WBSrcFrom.fromCSR -> LSU.io.in.bits.csrOut,
      WBSrcFrom.fromMem -> LSU.io.out.bits.memOut
    ).map { case (key, data) => (LSU.io.in.bits.control.wbSrc === key, data) }
  )
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
    Seq(Dev.CLINTAddr.in, !Dev.CLINTAddr.in(_)),
    Seq(CLINT.io, io.master)
  )
  io.slave <> AXI4.none

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
      Dev.CLINTAddr,
      Dev.UART16550Addr,
      Dev.SPIMasterAddr,
      Dev.GPIOAddr,
      Dev.PS2Addr,
      Dev.VGAAddr,
      Dev.ChipLinkMEMAddr
    )
    val needSkipDifftest = devs.map { dev => dev.in(WBU.io.in.bits.aluOut) }.foldLeft(false.B)(_ || _)
    SkipDifftest(clock, WBU.io.out.fire && WBU.io.out.bits.memAccess.get && needSkipDifftest)

    val ICacheArfire = ICache.io.master.arvalid && ICache.io.master.arready
    val ICacheRfire  = ICache.io.master.rvalid && ICache.io.master.rready
    val ICacheTracer = Module(new Tracer)
    ICacheTracer.io.raddr := RegEnable(ICache.io.master.araddr, ICacheArfire)
    ICacheTracer.io.rlen  := RegEnable(1.U << ICache.io.master.arsize, ICacheArfire)
    ICacheTracer.io.rdata := ICache.io.master.rdata
    ICacheTracer.io.ren   := ICacheRfire
    ICacheTracer.io.waddr := DontCare
    ICacheTracer.io.wdata := DontCare
    ICacheTracer.io.wlen  := DontCare
    ICacheTracer.io.wen   := false.B

    val LSUArfire = LSU.io.master.arvalid && LSU.io.master.arready
    val LSURfire  = LSU.io.master.rvalid && LSU.io.master.rready
    val LSUAwfire = LSU.io.master.awvalid && LSU.io.master.awready
    val LSUWfire  = LSU.io.master.wvalid && LSU.io.master.wready
    val LSUBfire  = LSU.io.master.bvalid && LSU.io.master.bready
    val LSUTracer = Module(new Tracer)
    LSUTracer.io.raddr := RegEnable(LSU.io.master.araddr, LSUArfire)
    LSUTracer.io.rlen  := RegEnable(1.U << LSU.io.master.arsize, LSUArfire)
    LSUTracer.io.rdata := LSU.io.master.rdata
    LSUTracer.io.ren   := LSURfire
    LSUTracer.io.waddr := RegEnable(LSU.io.master.awaddr, LSUAwfire)
    LSUTracer.io.wdata := RegEnable(LSU.io.master.wdata, LSUWfire)
    LSUTracer.io.wlen  := RegEnable(1.U << LSU.io.master.awsize, LSUAwfire)
    LSUTracer.io.wen   := LSUBfire

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
