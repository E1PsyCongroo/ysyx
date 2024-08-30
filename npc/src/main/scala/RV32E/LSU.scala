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

class LSUIO(awidth:Int = 32, xlen: Int = 32) extends Bundle {
  val memAccess = Flipped(DecoupledIO(new MemAccess(awidth, xlen)))
  val memReturn = DecoupledIO(new MemReturn(xlen))
  val AXI       = Flipped(new AXILiteIO(awidth, xlen))
}

class LSU(awidth:Int = 32 ,xlen:Int = 32) extends Module {
  val io = IO(new LSUIO(awidth, xlen))
withReset(!reset.asBool) {
  val memOp = io.memAccess.bits.memOp
  val wmask = WireDefault(0.U(4.W))

  val possiblePatterns = Seq(
    MemControlPattern(memW),
    MemControlPattern(memH),
    MemControlPattern(memB),
    MemControlPattern(memHu),
    MemControlPattern(memBu),
  )
  val decodeTable = new DecodeTable(possiblePatterns, Seq(WmaskFeild))
  val mask = decodeTable.decode(memOp)(WmaskFeild)
  wmask := mask << io.memAccess.bits.waddr(1, 0)

  /* Write address channel */
  io.AXI.awvalid := io.memAccess.valid && io.memAccess.bits.wen
  io.AXI.awid    := DontCare
  io.AXI.awaddr  := io.memAccess.bits.waddr
  io.AXI.awport  := DontCare

  /* Write data channel */
  io.AXI.wvalid  := io.memAccess.valid && io.memAccess.bits.wen
  io.AXI.wdata   := io.memAccess.bits.wdata
  io.AXI.wstarb  := wmask

  /* Write response channel */
  io.AXI.bready  := true.B

  /* Read address channel */
  io.AXI.arvalid := io.memAccess.valid && io.memAccess.bits.ren
  io.AXI.arid    := DontCare
  io.AXI.araddr  := io.memAccess.bits.raddr
  io.AXI.arport  := DontCare

  /* Read data channel */
  io.AXI.rready  := io.memReturn.ready
  io.AXI.rid     := DontCare

  io.memAccess.ready      := io.AXI.awready && io.AXI.wready && io.AXI.arready
  io.memReturn.valid      := io.AXI.rvalid

  val rdata = io.AXI.rdata
  val raddr = io.memAccess.bits.raddr
  val loffset = (raddr(1) << 4.U) | (raddr(0) << 3.U)
  val lshift = rdata >> loffset

  io.memReturn.bits.rdata := MuxCase(lshift, Seq(
    memH -> Fill(16, lshift(15)) ## lshift(15, 0),
    memB -> Fill(24, lshift(7)) ## lshift(7, 0),
    memHu -> Fill(16, 0.U(1.W)) ## lshift(15, 0),
    memBu -> Fill(24, 0.U(1.W)) ## lshift(7, 0),
  ).map { case(key, data) => (memOp === key, data) })

}
}
