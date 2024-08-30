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
  val AXISubordinate  = new AXILiteSubordinateIO(awidth, xlen)
  val memOp           = Input(UInt(MemOp.getWidth.W))
  val AXIManager      = Flipped(new AXILiteSubordinateIO(awidth, xlen))
}

class LSU(awidth:Int = 32 ,xlen:Int = 32) extends Module {
  val io = IO(new LSUIO(awidth, xlen))
withReset(!reset.asBool) {
  val wmask = WireDefault(0.U(4.W))

  val possiblePatterns = Seq(
    MemControlPattern(memW),
    MemControlPattern(memH),
    MemControlPattern(memB),
    MemControlPattern(memHu),
    MemControlPattern(memBu),
    )
  val decodeTable = new DecodeTable(possiblePatterns, Seq(WmaskFeild))
  val mask = decodeTable.decode(io.memOp)(WmaskFeild)
  wmask := mask << io.AXISubordinate.awaddr(1, 0)

  val arfire  = io.AXIManager.arvalid && io.AXIManager.arready
  val memOp   = RegEnable(io.memOp, arfire)
  val raddr   = RegEnable(io.AXISubordinate.araddr, arfire)
  val loffset = (raddr(1) << 4.U) | (raddr(0) << 3.U)
  val lshift  = io.AXIManager.rdata >> loffset

  val rdata   = MuxCase(lshift, Seq(
    memH -> Fill(16, lshift(15)) ## lshift(15, 0),
    memB -> Fill(24, lshift(7)) ## lshift(7, 0),
    memHu -> Fill(16, 0.U(1.W)) ## lshift(15, 0),
    memBu -> Fill(24, 0.U(1.W)) ## lshift(7, 0),
  ).map { case(key, data) => (memOp === key, data) })

  /* IO bind */
  /* Write address channel */
  io.AXIManager.awvalid := io.AXISubordinate.awvalid
  io.AXISubordinate.awready  := io.AXIManager.awready
  io.AXIManager.awid    := DontCare
  io.AXIManager.awaddr  := io.AXISubordinate.awaddr
  io.AXIManager.awport  := DontCare

  /* Write data channel */
  io.AXIManager.wvalid  := io.AXISubordinate.wvalid
  io.AXISubordinate.wready   := io.AXIManager.wready
  io.AXIManager.wdata   := io.AXISubordinate.wdata
  io.AXIManager.wstarb  := wmask

  /* Write response channel */
  io.AXISubordinate.bvalid   := io.AXIManager.bvalid
  io.AXIManager.bready  := io.AXISubordinate.bready
  io.AXISubordinate.bid      := DontCare
  io.AXISubordinate.bresp    := io.AXIManager.bresp

  /* Read address channel */
  io.AXIManager.arvalid := io.AXISubordinate.arvalid
  io.AXISubordinate.arready  := io.AXIManager.arready
  io.AXIManager.arid    := DontCare
  io.AXIManager.araddr  := io.AXISubordinate.araddr
  io.AXIManager.arport  := DontCare

  /* Read data channel */
  io.AXISubordinate.rvalid   := io.AXIManager.rvalid
  io.AXIManager.rready  := io.AXISubordinate.rready
  io.AXISubordinate.rid      := DontCare
  io.AXISubordinate.rdata    := rdata
  io.AXISubordinate.rresp    := io.AXIManager.rready

}
}
