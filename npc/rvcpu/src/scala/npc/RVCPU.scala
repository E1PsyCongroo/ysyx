package rvcpu.core

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
    // stall: Option[Bool] = None,
    flush: Option[Bool] = None
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
        rightIn.valid := !flush.get && RegNext((leftOut.fire || (!rightOut.fire && rightIn.valid)), false.B)
      }
    } else if (arch == "ooo") {
      rightIn <> Queue(leftOut, 16)
    }
  }
}

case class ICacheConfig(
  totalWidth:         Int,
  blockWidth:         Int,
  associativityWidth: Int,
  icacheNeed:         Seq[AddressSet])

case class CPUConfig(
  frequency:    BigInt,
  xlen:         Int,
  extentionE:   Boolean,
  PCReset:      BigInt,
  icacheConfig: ICacheConfig,
  sim:          Boolean)

class CPUState(cpuConfig: CPUConfig) extends Bundle {
  val ifuCycle    = UInt(64.W)
  val icacheCycle = UInt(64.W)
  val iduCycle    = UInt(64.W)
  val exuCycle    = UInt(64.W)
  val lsuCycle    = UInt(64.W)
  val wbuCycle    = UInt(64.W)

  val pc = UInt(cpuConfig.xlen.W)

  val inst           = UInt(32.W)
  val icacheNeed     = Bool()
  val icacheHit      = Bool()
  val icacheMissTime = UInt(64.W)

  val isEnd    = Bool()
  val exitCode = UInt(cpuConfig.xlen.W)

  val npc      = UInt(cpuConfig.xlen.W)
  val csrState = new CSRState(cpuConfig)

  val memRen        = Bool()
  val memWen        = Bool()
  val memAccessSize = UInt(AXI4Parameters.sizeBits.W)
  val memAddr       = UInt(cpuConfig.xlen.W)
  val memData       = UInt(cpuConfig.xlen.W)

  val gprState = new GPRState(cpuConfig)
}

class RVCPUIO(cpuConfig: CPUConfig, axiParams: AXI4BundleParameters) extends Bundle {
  val interrupt = Input(Bool())
  val masters = new Bundle {
    val icache = AXI4Bundle(axiParams)
    val lsu    = AXI4Bundle(axiParams)
  }
  val slave = Flipped(AXI4Bundle(axiParams))
  val state = if (cpuConfig.sim) Some(DecoupledIO(new CPUState(cpuConfig))) else None
}

class RVCPU(cpuConfig: CPUConfig, axiParams: AXI4BundleParameters) extends Module {
  val io = IO(new RVCPUIO(cpuConfig, axiParams))

  val IFU     = Module(new IFU(cpuConfig))
  val ICache  = Module(new ICache(cpuConfig, axiParams))
  val IDU     = Module(new IDU(cpuConfig))
  val EXU     = Module(new EXU(cpuConfig))
  val LSU     = Module(new LSU(cpuConfig, axiParams))
  val WBU     = Module(new WBU(cpuConfig))
  val RegFile = Module(new RegFile(cpuConfig))

  StageConnect(IFU.io.out, ICache.io.in, ICache.io.out, Some(EXU.io.jump))
  StageConnect(ICache.io.out, IDU.io.in, IDU.io.out, Some(EXU.io.jump))
  StageConnect(IDU.io.out, EXU.io.in, EXU.io.out)
  StageConnect(EXU.io.out, LSU.io.in, LSU.io.out)
  StageConnect(LSU.io.out, WBU.io.in, WBU.io.out)

  IFU.io.jump   := EXU.io.jump || IDU.io.fence_i
  IFU.io.nextPC := Mux(EXU.io.jump, EXU.io.nextPC, ICache.io.in.bits.pc)

  ICache.io.clear := IDU.io.fence_i

  RegFile.io.in.ra1 := IDU.io.RegFileAccess.ra1
  RegFile.io.in.ra2 := IDU.io.RegFileAccess.ra2
  def conflict(ra: UInt, old: RegFileAccess) = old.we && old.wa =/= 0.U && ra === old.wa
  val isEXURa1RAW    = conflict(IDU.io.RegFileAccess.ra1, EXU.io.RegFileAccess)
  val isEXURa2RAW    = conflict(IDU.io.RegFileAccess.ra2, EXU.io.RegFileAccess)
  val isEXUForward   = !EXU.io.in.bits.control.wbSrc(0) && EXU.io.out.valid
  val EXUForwardData = EXU.io.out.bits.alu_csr_out
  val isLSURa1RAW    = conflict(IDU.io.RegFileAccess.ra1, LSU.io.RegFileAccess)
  val isLSURa2RAW    = conflict(IDU.io.RegFileAccess.ra2, LSU.io.RegFileAccess)
  val isLSUForward   = Mux(LSU.io.in.bits.control.wbSrc.asBool, LSU.io.out.valid, LSU.io.in.valid)
  val LSUForwardData = LSU.io.out.bits.wd
  val isWBURa1RAW    = conflict(IDU.io.RegFileAccess.ra1, WBU.io.RegFileAccess)
  val isWBURa2RAW    = conflict(IDU.io.RegFileAccess.ra2, WBU.io.RegFileAccess)
  val isWBUForward   = WBU.io.in.valid
  val WBUForwardData = WBU.io.RegFileAccess.wd
  IDU.io.RegFileReturn.rd1 := MuxCase(
    RegFile.io.out.rd1,
    Seq(
      (isEXURa1RAW && isEXUForward) -> EXUForwardData,
      (isLSURa1RAW && isLSUForward) -> LSUForwardData,
      (isWBURa1RAW && isWBUForward) -> WBUForwardData
    )
  )
  IDU.io.RegFileReturn.rd2 := MuxCase(
    RegFile.io.out.rd2,
    Seq(
      (isEXURa2RAW && isEXUForward) -> EXUForwardData,
      (isLSURa2RAW && isLSUForward) -> LSUForwardData,
      (isWBURa2RAW && isWBUForward) -> WBUForwardData
    )
  )
  IDU.io.stall := ((isEXURa1RAW || isEXURa2RAW) && !isEXUForward) || ((isLSURa1RAW || isLSURa2RAW) && !isLSUForward) || ((isWBURa1RAW || isWBURa2RAW) && !isWBUForward)

  WBU.io.out.ready := true.B
  RegFile.io.in.wa := WBU.io.RegFileAccess.wa
  RegFile.io.in.we := WBU.io.RegFileAccess.we
  RegFile.io.in.wd := WBU.io.RegFileAccess.wd

  io.masters.icache <> ICache.io.master
  io.masters.lsu <> LSU.io.master
  io.slave := DontCare

  if (cpuConfig.sim) {
    val cycle = RegInit(0.U(64.W))
    cycle                  := cycle + 1.U
    IFU.io.curCycle.get    := cycle
    ICache.io.curCycle.get := cycle
    IDU.io.curCycle.get    := cycle
    EXU.io.curCycle.get    := cycle
    LSU.io.curCycle.get    := cycle
    WBU.io.curCycle.get    := cycle
    WBU.io.gprState.get    := RegFile.io.gprState.get
    io.state.get.bits      := WBU.io.out.bits.state.get
    io.state.get.valid     := WBU.io.out.valid
    WBU.io.out.ready       := io.state.get.ready
  }
}
