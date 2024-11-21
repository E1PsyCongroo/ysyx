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

class LSUOut(xlen: Int, extentionE: Boolean) extends Bundle {
  val wa     = Output(UInt(if (extentionE) 4.W else 5.W))
  val pcCom  = Output(UInt(xlen.W))
  val aluOut = Output(UInt(xlen.W))
  val memOut = Output(UInt(xlen.W))
  val csrOut = Output(UInt(xlen.W))
  val control = new Bundle {
    val regWe = Bool()
    val pcSrc = Output(UInt(PCSrcFrom.getWidth.W))
    val wbSrc = Output(UInt(WBSrcFrom.getWidth.W))
  }
}

class LSUIO(xlen: Int, extentionE: Boolean) extends Bundle {
  val in     = Flipped(DecoupledIO(new EXUOut(xlen, extentionE)))
  val out    = DecoupledIO(new LSUOut(xlen, extentionE))
  val master = new AXI4MasterIO
}

class LSU(xlen: Int, extentionE: Boolean) extends Module {
  val io = IO(new LSUIO(xlen, extentionE))

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

  val sIdle :: sSendReq :: sWaitResp :: sSendOut :: Nil = Enum(4)

  val state      = RegInit(sIdle)
  val isIdle     = state === sIdle
  val isSendReq  = state === sSendReq
  val isWaitResp = state === sWaitResp
  val isSendOut  = state === sSendOut

  state := MuxLookup(state, sIdle)(
    Seq(
      sIdle -> Mux(io.in.fire, sSendReq, sIdle),
      sSendReq -> Mux(
        memAccess,
        Mux(arfire || (awfire && wfire), sWaitResp, sSendReq),
        Mux(io.out.fire, sIdle, sSendOut)
      ),
      sWaitResp -> Mux(bfire || rfire, sSendOut, sWaitResp),
      sSendOut -> Mux(io.out.fire, sIdle, sSendOut)
    )
  )

  assert(!bfire || io.master.bresp === "b00".U(2.W))
  assert(!rfire || io.master.rresp === "b00".U(2.W))
  /* IO bind */
  io.master.awvalid := isSendReq && wen
  io.master.awaddr  := waddr
  io.master.awid    := 0.U
  io.master.awlen   := 0.U
  io.master.awsize  := size
  io.master.awburst := "b01".U

  io.master.wvalid := isSendReq && wen
  io.master.wdata  := wdata << (waddr(1, 0) << 3.U)
  io.master.wstrb  := wmask
  io.master.wlast  := isSendReq

  io.master.bready := isWaitResp

  io.master.arvalid := isSendReq && ren
  io.master.araddr  := raddr
  io.master.arid    := 0.U
  io.master.arlen   := 0.U
  io.master.arsize  := size
  io.master.arburst := "b01".U

  io.master.rready := isWaitResp

  io.in.ready               := isIdle
  io.out.valid              := isSendOut || (isSendReq && !memAccess)
  io.out.bits.wa            := in.wa
  io.out.bits.pcCom         := in.pcCom
  io.out.bits.aluOut        := in.aluOut
  io.out.bits.memOut        := RegEnable(rdata, rfire)
  io.out.bits.csrOut        := in.csrOut
  io.out.bits.control.regWe := in.control.regWe
  io.out.bits.control.pcSrc := in.control.pcSrc
  io.out.bits.control.wbSrc := in.control.wbSrc
}
