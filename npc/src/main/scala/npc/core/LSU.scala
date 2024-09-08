package rvcpu.core

import rvcpu.axi4._
import rvcpu.utility._
import rvcpu.dev._
import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._

import MemOp._

case class MemControlPattern(
  val memOp: BitPat
) extends DecodePattern {
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

class LSUOut(xlen: Int = 32) extends Bundle {
  val rdata     = Output(UInt(xlen.W))
}

class LSUIO(xlen: Int = 32) extends Bundle {
  val in        = Flipped(DecoupledIO(new LSUIn(xlen)))
  val out       = DecoupledIO(new LSUOut(xlen))
  val master    = new AXI4MasterIO
}

class LSU(xlen:Int = 32) extends Module {
  val io = IO(new LSUIO(xlen))
  val wmask = WireDefault(0.U(4.W))

  val possiblePatterns = Seq(
    MemControlPattern(memW),
    MemControlPattern(memH),
    MemControlPattern(memB),
    MemControlPattern(memHu),
    MemControlPattern(memBu),
    )
  val decodeTable = new DecodeTable(possiblePatterns, Seq(WmaskFeild))
  val mask = decodeTable.decode(io.in.bits.memOp)(WmaskFeild)
  wmask := mask << io.in.bits.waddr(1, 0)

  val memOp   = RegEnable(io.in.bits.memOp, io.in.fire)
  val raddr   = io.in.bits.raddr
  val loffset = RegEnable((raddr(1) << 4.U) | (raddr(0) << 3.U), io.in.fire)
  val lshift  = io.master.rdata >> loffset

  val rdata   = MuxCase(lshift, Seq(
    memH -> Fill(16, lshift(15)) ## lshift(15, 0),
    memB -> Fill(24, lshift(7)) ## lshift(7, 0),
    memHu -> Fill(16, 0.U(1.W)) ## lshift(15, 0),
    memBu -> Fill(24, 0.U(1.W)) ## lshift(7, 0),
  ).map { case(key, data) => (memOp === key, data) })

  val bfire = io.master.bvalid && io.master.bready
  val rfire = io.master.rvalid && io.master.rready

  val sIdle :: sWrite :: sRead :: Nil = Enum(3)
  val state = RegInit(sIdle)
  state := MuxLookup(state, sIdle)(Seq(
    sIdle     -> MuxCase(sIdle, Seq(
      (io.in.fire && io.in.bits.wen) -> sWrite,
      (io.in.fire && io.in.bits.ren) -> sRead,
    )),
    sWrite   -> Mux(bfire, sIdle, sWrite),
    sRead    -> Mux(rfire, sIdle, sRead),
  ))

  val isIdle    = state === sIdle
  val isWrite   = state === sWrite
  val isRead    = state === sRead

  /* IO bind */
  io.in.ready         := isIdle

  io.out.valid        := isRead && rfire
  io.out.bits.rdata   := rdata

  io.master.awvalid   := io.in.fire && io.in.bits.wen
  io.master.awaddr    := io.in.bits.waddr
  io.master.awid      := DontCare
  io.master.awlen     := 0.U
  io.master.awsize    := "b101".U
  io.master.awburst   := "b01".U

  io.master.wvalid    := io.in.fire && io.in.bits.wen
  io.master.wdata     := io.in.bits.wdata
  io.master.wstarb    := wmask
  io.master.wlast     := io.in.fire

  io.master.bready    := isWrite

  io.master.arvalid   := io.in.fire && io.in.bits.ren
  io.master.araddr    := io.in.bits.raddr
  io.master.arid      := DontCare
  io.master.arlen     := 0.U
  io.master.arsize    := "b101".U
  io.master.arbrust   := "b01".U

  io.master.rready    := isRead
}
