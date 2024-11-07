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

  val IFU = Module(new IFU(awidth, xlen, PCReset))
  val IDU = Module(new IDU(xlen, extentionE))
  val EXU = Module(new EXU(xlen))
  val LSU = Module(new LSU(xlen))
  val WBU = Module(new WBU(xlen, extentionE))
  EXU.io.LSUIn <> LSU.io.in
  LSU.io.out <> EXU.io.LSUOut

  val ICache = Module(
    new ICache(
      awidth,
      xlen,
      4,
      16,
      1,
      addr =>
        Dev.MROMAddr.in(addr) || Dev.FlashAddr.in(addr) || Dev.ChipLinkMEMAddr.in(addr) || Dev.PSRAMAddr.in(
          addr
        ) || Dev.SDRAMAddr.in(addr),
      sim
    )
  )
  ICache.io.in <> IFU.io.ICacheIn
  ICache.io.out <> IFU.io.ICacheOut

  val RegFile = Module(new RegFile(xlen, if (extentionE) 4 else 5))
  RegFile.io.ra1           := IDU.io.RegFileAccess.ra1
  RegFile.io.ra2           := IDU.io.RegFileAccess.ra2
  IDU.io.RegFileReturn.rd1 := RegFile.io.rd1
  IDU.io.RegFileReturn.rd2 := RegFile.io.rd2
  RegFile.io.wa            := WBU.io.RegFileAccess.wa
  RegFile.io.we            := WBU.io.RegFileAccess.we
  RegFile.io.wd            := WBU.io.RegFileAccess.wd

  val CLINT = Module(new CLINT(awidth, xlen, Dev.CLINTAddr))

  StageConnect(IFU.io.out, IDU.io.in)
  StageConnect(IDU.io.out, EXU.io.in)
  StageConnect(EXU.io.out, WBU.io.in)
  StageConnect(WBU.io.out, IFU.io.in)

  val AXI4Interconnect = Module(new AXI4Interconnect(2, Seq(Dev.CLINTAddr.in, !Dev.CLINTAddr.in(_))))
  AXI4Interconnect.io.fanIn(0) <> ICache.io.master
  AXI4Interconnect.io.fanIn(1) <> LSU.io.master
  AXI4Interconnect.io.fanOut(0) <> CLINT.io
  AXI4Interconnect.io.fanOut(1) <> io.master
  io.slave <> AXI4.none

  if (sim) {
    EndControl(clock, IDU.io.isEnd, IDU.io.exitCode)

    val devs = Seq(
      Dev.CLINTAddr,
      Dev.UART16550Addr,
      Dev.SPIMasterAddr,
      Dev.GPIOAddr,
      Dev.PS2Addr,
      Dev.VGAAddr,
      Dev.ChipLinkMEMAddr
    )
    SkipDifftest(
      clock,
      LSU.io.in.fire &&
        (devs.map(dev => dev.in(LSU.io.in.bits.waddr) || dev.in(LSU.io.in.bits.raddr)).foldLeft(false.B)(_ || _))
    );

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
    LSUTracer.io.wlen  := RegEnable(LSU.io.master.awsize, LSUAwfire)
    LSUTracer.io.wen   := LSUBfire

    val TracerDataFetch = Module(new TracerDataFetch)
    TracerDataFetch.io.clock  := clock
    TracerDataFetch.io.reset  := reset
    TracerDataFetch.io.start  := LSU.io.in.fire && LSU.io.in.bits.ren
    TracerDataFetch.io.finish := LSU.io.out.fire

    ICache.io.hit.get.ready := true.B
    val CacheTracer = Module(new CacheTracer)
    CacheTracer.io.cacheHit          := RegEnable(ICache.io.hit.get.bits, ICache.io.hit.get.fire)
    CacheTracer.io.cacheAccessStart  := ICache.io.in.fire
    CacheTracer.io.cacheAccessFinish := ICache.io.out.fire
    CacheTracer.io.cacheFetchStart   := ICache.io.master.arvalid
    CacheTracer.io.cacheFetchFinish  := ICache.io.master.rvalid && ICache.io.master.rready
  }
}

object StageConnect {
  var arch = "multi"

  def apply[T <: Data](left: DecoupledIO[T], right: DecoupledIO[T]) = {
    if (arch == "single") {
      right.bits  := left.bits
      left.ready  := true.B
      right.valid := true.B
    } else if (arch == "multi") {
      right <> left
    } else if (arch == "pipeline") {
      right <> RegEnable(left, left.fire)
    } else if (arch == "ooo") {
      right <> Queue(left, 16)
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
  val IFU = Module(new IFU(awidth, xlen, PCReset))
  val IDU = Module(new IDU(xlen, extentionE))
  val EXU = Module(new EXU(xlen))
  val LSU = Module(new LSU(xlen))
  val WBU = Module(new WBU(xlen, extentionE))
  EXU.io.LSUIn <> LSU.io.in
  LSU.io.out <> EXU.io.LSUOut

  val ICache = Module(new ICache(awidth, xlen, 4, 16, 1, Dev.memoryAddr.in, sim))
  ICache.io.in <> IFU.io.ICacheIn
  ICache.io.out <> IFU.io.ICacheOut

  val RegFile = Module(new RegFile(xlen, if (extentionE) 4 else 5))
  RegFile.io.ra1           := IDU.io.RegFileAccess.ra1
  RegFile.io.ra2           := IDU.io.RegFileAccess.ra2
  IDU.io.RegFileReturn.rd1 := RegFile.io.rd1
  IDU.io.RegFileReturn.rd2 := RegFile.io.rd2
  RegFile.io.wa            := WBU.io.RegFileAccess.wa
  RegFile.io.we            := WBU.io.RegFileAccess.we
  RegFile.io.wd            := WBU.io.RegFileAccess.wd

  val AXI4Mem = Module(new AXI4Mem(awidth, xlen))
  val CLINT   = Module(new CLINT(awidth, xlen, Dev.mtimeAddr))
  val Uart    = Module(new Uart(awidth, xlen))

  StageConnect(IFU.io.out, IDU.io.in)
  StageConnect(IDU.io.out, EXU.io.in)
  StageConnect(EXU.io.out, WBU.io.in)
  StageConnect(WBU.io.out, IFU.io.in)

  val AXI4Interconnect = Module(
    new AXI4Interconnect(
      2,
      Seq(Dev.mtimeAddr.in, Dev.uartAddr.in, addr => !Dev.mtimeAddr.in(addr) && !Dev.uartAddr.in(addr))
    )
  )
  AXI4Interconnect.io.fanIn(0) <> ICache.io.master
  AXI4Interconnect.io.fanIn(1) <> LSU.io.master
  AXI4Interconnect.io.fanOut(0) <> CLINT.io
  AXI4Interconnect.io.fanOut(1) <> Uart.io
  AXI4Interconnect.io.fanOut(2) <> AXI4Mem.io

  if (sim) {
    EndControl(clock, IDU.io.isEnd, IDU.io.exitCode)

    val devs = Seq(
      Dev.mtimeAddr,
      Dev.uartAddr
    )
    SkipDifftest(
      clock,
      LSU.io.in.fire &&
        (devs.map(dev => dev.in(LSU.io.in.bits.waddr) || dev.in(LSU.io.in.bits.raddr)).foldLeft(false.B)(_ || _))
    );

    val TracerDataFetch = Module(new TracerDataFetch)
    TracerDataFetch.io.clock  := clock
    TracerDataFetch.io.reset  := reset
    TracerDataFetch.io.start  := LSU.io.in.fire && LSU.io.in.bits.ren
    TracerDataFetch.io.finish := LSU.io.out.fire

    ICache.io.hit.get.ready := true.B
    val CacheTracer = Module(new CacheTracer)
    CacheTracer.io.cacheHit          := RegEnable(ICache.io.hit.get.bits, ICache.io.hit.get.fire)
    CacheTracer.io.cacheAccessStart  := ICache.io.in.fire
    CacheTracer.io.cacheAccessFinish := ICache.io.out.fire
    CacheTracer.io.cacheFetchStart   := ICache.io.master.arvalid
    CacheTracer.io.cacheFetchFinish  := ICache.io.master.rvalid && ICache.io.master.rready
  }
}
