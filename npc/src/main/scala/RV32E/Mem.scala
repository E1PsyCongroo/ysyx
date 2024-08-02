package RV32E

import chisel3._
import chisel3.util._
import MemOp._
import chisel3.util.experimental.decode._

class MemIO extends Bundle {
  val valid = Input(Bool())
  val raddr = Input(UInt(32.W))
  val rdata = Output(UInt(32.W))

  val wen = Input(Bool())
  val waddr = Input(UInt(32.W))
  val wdata = Input(UInt(32.W))
  val wmask = Input(UInt(4.W))
}

class Mem extends BlackBox with HasBlackBoxResource {
  val io = IO(new MemIO{
    val clock = Input(Clock())
  } )
  addResource("/Mem.sv")
}

class MemControlIO extends Bundle {
  val valid = Input(Bool())
  val memOp = Input(UInt(3.W))

  val raddr = Input(UInt(32.W))
  val rdata = Output(UInt(32.W))

  val wen = Input(Bool())
  val waddr = Input(UInt(32.W))
  val wdata = Input(UInt(32.W))
}

class MemControl extends Module {
  val io = IO(new MemControlIO)

  val wmask = WireDefault(0.U(4.W))
  val mem = Module(new Mem)
  mem.io.clock := clock
  mem.io.valid := io.valid
  mem.io.raddr := io.raddr
  val rdata = mem.io.rdata

  mem.io.wen := io.wen
  mem.io.waddr := io.waddr
  mem.io.wdata := io.wdata
  mem.io.wmask := wmask

  case class MemControlPattern(
    val memOp: BitPat
  ) extends DecodePattern {
    def bitPat: BitPat = memOp
  }

  object MemControlFeild extends DecodeField[MemControlPattern, UInt] {
    def name = "Mem Control"
    def chiselType: UInt = UInt(4.W)
    def genTable(op: MemControlPattern): BitPat = {
      op.bitPat(1, 0).rawString match {
        case "10" => BitPat("b1111")
        case "01" => BitPat("b0011")
        case "00" => BitPat("b0001")
      }
    }
  }

  val possiblePatterns = Seq(
    MemControlPattern(memW),
    MemControlPattern(memH),
    MemControlPattern(memB),
    MemControlPattern(memHu),
    MemControlPattern(memBu),
  )
  val decodeTable = new DecodeTable(possiblePatterns, Seq(MemControlFeild))
  val mask = decodeTable.decode(io.memOp)(MemControlFeild)

  wmask := mask << io.waddr(1, 0)

  val loffset = (io.raddr(1) << 4.U) | (io.raddr(0) << 3.U)
  val lshift = rdata >> loffset
  io.rdata := MuxLookup(io.memOp, rdata)(Seq(
    memH.value.U -> Fill(16, lshift(15)) ## lshift(15, 0),
    memB.value.U -> Fill(24, lshift(7)) ## lshift(7, 0),
    memHu.value.U -> Fill(16, 0.U(1.W)) ## lshift(15, 0),
    memBu.value.U -> Fill(24, 0.U(1.W)) ## lshift(7, 0),
  ))
}