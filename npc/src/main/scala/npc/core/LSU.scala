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

class LSUOut(xlen: Int, extentionE: Boolean, sim: Boolean) extends Bundle {
  val wa     = Output(UInt(if (extentionE) 4.W else 5.W))
  val aluOut = Output(UInt(xlen.W))
  val memOut = Output(UInt(xlen.W))
  val csrOut = Output(UInt(xlen.W))
  val control = new Bundle {
    val regWe = Bool()
    val wbSrc = Output(UInt(WBSrcFrom.getWidth.W))
  }

  val nextPC     = if (sim) Some(Output(UInt(xlen.W))) else None
  val inst       = if (sim) Some(Output(UInt(32.W))) else None
  val memAccess  = if (sim) Some(Output(Bool())) else None
  val fetchCycle = if (sim) Some(Output(UInt(64.W))) else None
  val isEnd      = if (sim) Some(Output(Bool())) else None
  val exitCode   = if (sim) Some(Output(UInt(32.W))) else None
}

class LSUIO(xlen: Int, extentionE: Boolean, sim: Boolean) extends Bundle {
  val in     = Flipped(DecoupledIO(new EXUOut(xlen, extentionE, sim)))
  val out    = DecoupledIO(new LSUOut(xlen, extentionE, sim))
  val master = new AXI4MasterIO
  val RegFileAccess = new Bundle {
    val wa = Output(UInt(if (extentionE) 4.W else 5.W))
    val we = Output(Bool())
  }
}

class LSU(xlen: Int, extentionE: Boolean, sim: Boolean) extends Module {
  val io = IO(new LSUIO(xlen, extentionE, sim))

  val in        = WireDefault(io.in.bits)
  val wen       = in.control.memWen
  val ren       = in.control.memRen
  val memOp     = in.control.memOp
  val raddr     = in.aluOut
  val waddr     = in.aluOut
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
  val lshift  = io.master.rdata >> loffset

  val rdata = MuxCase(
    lshift,
    Seq(
      memH -> Fill(16, lshift(15)) ## lshift(15, 0),
      memB -> Fill(24, lshift(7)) ## lshift(7, 0),
      memHu -> Fill(16, 0.U(1.W)) ## lshift(15, 0),
      memBu -> Fill(24, 0.U(1.W)) ## lshift(7, 0)
    ).map { case (key, data) => (memOp === key, data) }
  )

  val awfire = io.master.awvalid && io.master.awready
  val wfire  = io.master.wvalid && io.master.wready
  val bfire  = io.master.bvalid && io.master.bready
  val arfire = io.master.arvalid && io.master.arready
  val rfire  = io.master.rvalid && io.master.rready

  val sSendReq :: sWaitResp :: Nil = Enum(2)

  val state      = RegInit(sSendReq)
  val isSendReq  = state === sSendReq
  val isWaitResp = state === sWaitResp

  state := MuxLookup(state, sSendReq)(
    Seq(
      sSendReq -> Mux(memAccess && (arfire || (awfire && wfire)), sWaitResp, sSendReq),
      sWaitResp -> Mux(io.out.fire, sSendReq, sWaitResp)
    )
  )

  assert(!bfire || io.master.bresp === "b00".U(2.W))
  assert(!rfire || io.master.rresp === "b00".U(2.W))
  /* IO bind */
  io.master.awvalid := isSendReq && io.in.valid && wen
  io.master.awaddr  := waddr
  io.master.awid    := 0.U
  io.master.awlen   := 0.U
  io.master.awsize  := size
  io.master.awburst := "b01".U

  io.master.wvalid := isSendReq && io.in.valid && wen
  io.master.wdata  := wdata << (waddr(1, 0) << 3.U)
  io.master.wstrb  := wmask
  io.master.wlast  := isSendReq

  io.master.bready := io.out.ready

  io.master.arvalid := isSendReq && io.in.valid && ren
  io.master.araddr  := raddr
  io.master.arid    := 0.U
  io.master.arlen   := 0.U
  io.master.arsize  := size
  io.master.arburst := "b01".U

  io.master.rready := io.out.ready

  io.in.ready               := !io.in.valid || io.out.fire
  io.out.valid              := io.in.valid && Mux(ren, io.master.rvalid, Mux(wen, io.master.bvalid, true.B))
  io.out.bits.wa            := in.wa
  io.out.bits.aluOut        := in.aluOut
  io.out.bits.memOut        := rdata
  io.out.bits.csrOut        := in.csrOut
  io.out.bits.control.regWe := in.control.regWe
  io.out.bits.control.wbSrc := in.control.wbSrc
  io.RegFileAccess.wa       := in.wa
  io.RegFileAccess.we       := in.control.regWe && io.in.valid
  if (sim) {
    io.out.bits.nextPC.get     := in.nextPC.get
    io.out.bits.inst.get       := in.inst.get
    io.out.bits.memAccess.get  := memAccess
    io.out.bits.fetchCycle.get := in.fetchCycle.get
    io.out.bits.isEnd.get      := in.isEnd.get
    io.out.bits.exitCode.get   := in.exitCode.get
  }
}
