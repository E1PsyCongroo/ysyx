package rvcpu.core

import rvcpu.axi4._
import rvcpu.utility._
import rvcpu.dev._
import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._

import MemOp._

case class MemControlPattern(
  val memOp: BitPat)
    extends DecodePattern {
  require(memOp.getWidth == MemOp.getWidth)
  def bitPat: BitPat = memOp
}

object WmaskFeild extends DecodeField[MemControlPattern, UInt] {
  def name = "Wmask Feild"
  def chiselType: UInt = UInt(4.W)
  def genTable(op: MemControlPattern): BitPat = {
    op.bitPat(1, 0).rawString match {
      case "10" => BitPat("b1111")
      case "01" => BitPat("b0011")
      case "00" => BitPat("b0001")
    }
  }
}

object SizeFeild extends DecodeField[MemControlPattern, UInt] {
  def name = "Size Feild"
  def chiselType: UInt = UInt(3.W)
  def genTable(op: MemControlPattern): BitPat = {
    op.bitPat(1, 0).rawString match {
      case "10" => BitPat("b010")
      case "01" => BitPat("b001")
      case "00" => BitPat("b000")
    }
  }
}

class LSUOut(cpuConfig: CPUConfig) extends Bundle {
  val wa = UInt(if (cpuConfig.extentionE) 4.W else 5.W)
  val wd = UInt(cpuConfig.xlen.W)
  val control = new Bundle {
    val regWe = Bool()
  }

  val state = if (cpuConfig.sim) Some(new CPUState(cpuConfig)) else None
}

class LSUIO(cpuConfig: CPUConfig, params: AXI4BundleParameters) extends Bundle {
  require(params.addrBits == cpuConfig.xlen)
  require(params.dataBits == cpuConfig.xlen)
  require(params.lenPrenset)
  require(params.sizePresent)
  require(params.burstPresent)
  require(params.brespPresent)
  require(params.rlastPresent)
  require(params.rrespPresent)
  val in            = Flipped(DecoupledIO(new EXUOut(cpuConfig)))
  val out           = DecoupledIO(new LSUOut(cpuConfig))
  val master        = AXI4Bundle(params)
  val RegFileAccess = Flipped(new RegFileAccess(cpuConfig))

  val curCycle = if (cpuConfig.sim) Some(Input(UInt(64.W))) else None
}

class LSU(cpuConfig: CPUConfig, params: AXI4BundleParameters) extends Module {
  val io = IO(new LSUIO(cpuConfig, params))

  val in        = WireDefault(io.in.bits)
  val wen       = in.control.memWen
  val ren       = in.control.memRen
  val memOp     = in.control.memOp
  val raddr     = in.alu_csr_out
  val waddr     = in.alu_csr_out
  val wdata     = in.wdata
  val memAccess = wen | ren

  val possiblePatterns = Seq(
    MemControlPattern(memW),
    MemControlPattern(memH),
    MemControlPattern(memB),
    MemControlPattern(memHu),
    MemControlPattern(memBu)
  )
  val decodeTable = new DecodeTable(possiblePatterns, Seq(WmaskFeild, SizeFeild))
  val mask        = decodeTable.decode(memOp)(WmaskFeild)
  val wmask       = mask << waddr(1, 0)
  val size        = decodeTable.decode(memOp)(SizeFeild)

  val loffset = WireDefault(raddr(1, 0) << 3.U)
  val lshift  = io.master.r.bits.data >> loffset

  val rdata = MuxCase(
    lshift,
    Seq(
      memH -> Fill(16, lshift(15)) ## lshift(15, 0),
      memB -> Fill(24, lshift(7)) ## lshift(7, 0),
      memHu -> Fill(16, 0.U(1.W)) ## lshift(15, 0),
      memBu -> Fill(24, 0.U(1.W)) ## lshift(7, 0)
    ).map { case (key, data) => (memOp === key, data) }
  )

  val awfire = io.master.aw.fire
  val wfire  = io.master.w.fire
  val bfire  = io.master.b.fire
  val arfire = io.master.ar.fire
  val rfire  = io.master.r.fire
  val rlast  = io.master.r.bits.last

  val sSendReq :: sWaitAWfire :: sWaitWfire :: sWaitResp :: Nil = Enum(4)

  val state        = RegInit(sSendReq)
  val isSendReq    = state === sSendReq
  val isWaitAWfire = state === sWaitAWfire
  val isWaitWfire  = state === sWaitWfire
  val isWaitResp   = state === sWaitResp

  state := MuxLookup(state, sSendReq)(
    Seq(
      sSendReq -> MuxCase(
        sSendReq,
        Seq(
          arfire -> sWaitResp,
          (awfire && wfire) -> sWaitResp,
          awfire -> sWaitWfire,
          wfire -> sWaitAWfire
        )
      ),
      sWaitAWfire -> Mux(awfire, sWaitResp, sWaitAWfire),
      sWaitWfire -> Mux(wfire, sWaitResp, sWaitWfire),
      sWaitResp -> Mux(io.out.fire, sSendReq, sWaitResp)
    )
  )

  /* IO bind */
  io.master.aw.valid      := (isSendReq || isWaitAWfire) && io.in.valid && wen
  io.master.aw.bits       := DontCare
  io.master.aw.bits.addr  := waddr
  io.master.aw.bits.id    := 1.U
  io.master.aw.bits.len   := 0.U
  io.master.aw.bits.size  := size
  io.master.aw.bits.burst := "b01".U

  io.master.w.valid     := (isSendReq || isWaitWfire) && io.in.valid && wen
  io.master.w.bits      := DontCare
  io.master.w.bits.data := wdata << (waddr(1, 0) << 3.U)
  io.master.w.bits.strb := wmask
  io.master.w.bits.last := isSendReq

  io.master.b.ready := io.out.ready

  io.master.ar.valid      := isSendReq && io.in.valid && ren
  io.master.ar.bits       := DontCare
  io.master.ar.bits.addr  := raddr
  io.master.ar.bits.id    := 1.U
  io.master.ar.bits.len   := 0.U
  io.master.ar.bits.size  := size
  io.master.ar.bits.burst := "b01".U

  io.master.r.ready := io.out.ready

  io.in.ready               := !io.in.valid || io.out.fire
  io.out.valid              := io.in.valid && Mux(ren, io.master.r.valid, Mux(wen, io.master.b.valid, true.B))
  io.out.bits.wa            := in.wa
  io.out.bits.wd            := Mux(in.control.wbSrc.asBool, rdata, in.alu_csr_out)
  io.out.bits.control.regWe := in.control.regWe
  io.RegFileAccess.ra1      := DontCare
  io.RegFileAccess.ra2      := DontCare
  io.RegFileAccess.wa       := in.wa
  io.RegFileAccess.we       := in.control.regWe && io.in.valid
  io.RegFileAccess.wd       := DontCare

  if (cpuConfig.sim) {
    assert(!bfire || io.master.b.bits.resp === AXI4Parameters.RESP_OKAY)
    assert(!rfire || io.master.r.bits.resp === AXI4Parameters.RESP_OKAY)

    io.out.bits.state.get               := in.state.get
    io.out.bits.state.get.lsuCycle      := io.curCycle.get
    io.out.bits.state.get.memRen        := ren
    io.out.bits.state.get.memWen        := wen
    io.out.bits.state.get.memAccessSize := 1.U << size
    io.out.bits.state.get.memData       := Mux(wen, io.master.w.bits.data, io.master.r.bits.data)
    io.out.bits.state.get.memAddr       := in.alu_csr_out
  }
}
