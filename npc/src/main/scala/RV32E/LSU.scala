package RVCPU

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

class LSUIO extends Bundle {
  val avalid  = Input(Bool())
  val aready  = Output(Bool())
  val memOp   = Input(UInt(MemOp.getWidth.W))
  val raddr   = Input(UInt(32.W))
  val wen     = Input(Bool())
  val waddr   = Input(UInt(32.W))
  val wdata   = Input(UInt(32.W))

  val dvalid  = Output(Bool())
  val dready  = Input(Bool())
  val rdata   = Output(UInt(32.W))
}

class LSU extends Module {
  val io = IO(new LSUIO)

  val wmask = WireDefault(0.U(4.W))
  val Mem = Module(new Mem)
  Mem.io.clock  := clock
  Mem.io.reset  := reset

  Mem.io.avalid := io.avalid
  io.aready     := Mem.io.aready
  Mem.io.raddr  := io.raddr
  Mem.io.wen    := io.wen
  Mem.io.waddr  := io.waddr
  Mem.io.wdata  := io.wdata
  Mem.io.wmask  := wmask

  Mem.io.dready := io.dready
  io.dvalid     := Mem.io.dvalid
  val rdata = Mem.io.rdata

  val possiblePatterns = Seq(
    MemControlPattern(memW),
    MemControlPattern(memH),
    MemControlPattern(memB),
    MemControlPattern(memHu),
    MemControlPattern(memBu),
  )
  val decodeTable = new DecodeTable(possiblePatterns, Seq(WmaskFeild))
  val mask = decodeTable.decode(io.memOp)(WmaskFeild)

  wmask := mask << io.waddr(1, 0)

  val loffset = (io.raddr(1) << 4.U) | (io.raddr(0) << 3.U)
  val lshift = rdata >> loffset
  io.rdata := MuxCase(lshift, Seq(
    memH -> Fill(16, lshift(15)) ## lshift(15, 0),
    memB -> Fill(24, lshift(7)) ## lshift(7, 0),
    memHu -> Fill(16, 0.U(1.W)) ## lshift(15, 0),
    memBu -> Fill(24, 0.U(1.W)) ## lshift(7, 0),
  ).map { case(key, data) => (io.memOp === key, data) })
}
