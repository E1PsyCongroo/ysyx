package rvcpu

import rvcpu._
import rvcpu.core._
import rvcpu.utility._
import chisel3._
import chisel3.util._

class RVCPUIO(awidth:Int = 32, xlen: Int = 32) extends Bundle {
  val pc              = Output(UInt(xlen.W))
  val IFUAXIManager   = Flipped(new AXILiteSubordinateIO(awidth, xlen))
  val LSUAXIManager   = Flipped(new AXILiteSubordinateIO(awidth, xlen))
}

class RVCPU (
  awidth: Int = 32,
  xlen: Int = 32,
  extentionE: Boolean = true,
  PCReset: BigInt = BigInt("80000000", 16)
) extends Module {
  val io = IO(new RVCPUIO(xlen))

  val IFU = Module(new IFU(xlen, PCReset))
  val IDU = Module(new IDU(xlen, extentionE))
  val EXU = Module(new EXU(xlen))
  val LSU = Module(new LSU(awidth, xlen))
  val WBU = Module(new WBU(xlen, extentionE))
  EXU.io.AXIManager <> LSU.io.AXISubordinate
  LSU.io.memOp  := EXU.io.memOp

  val RegFile    = Module(new RegFile(xlen, if (extentionE) 4 else 5))
  RegFile.io.ra1 := IDU.io.RegFileAccess.ra1
  RegFile.io.ra2 := IDU.io.RegFileAccess.ra2
  IDU.io.RegFileReturn.rd1 := RegFile.io.rd1
  IDU.io.RegFileReturn.rd2 := RegFile.io.rd2
  RegFile.io.wa  := WBU.io.RegFileAccess.wa
  RegFile.io.we  := WBU.io.RegFileAccess.we
  RegFile.io.wd  := WBU.io.RegFileAccess.wd

  StageConnect(IFU.io.out, IDU.io.in)
  StageConnect(IDU.io.out, EXU.io.in)
  StageConnect(EXU.io.out, WBU.io.in)
  StageConnect(WBU.io.out, IFU.io.in)

  io.pc             := IFU.io.pc
  // io.IFUAXIManager  <> IFU.io.AXIManager
  // io.LSUAXIManager  <> LSU.io.AXIManager

  io.IFUAXIManager.awvalid  := IFU.io.AXIManager.awvalid
  IFU.io.AXIManager.awready := io.IFUAXIManager.awready
  io.IFUAXIManager.awid     := IFU.io.AXIManager.awid
  io.IFUAXIManager.awaddr   := IFU.io.AXIManager.awaddr
  io.IFUAXIManager.awport   := IFU.io.AXIManager.awport
  io.IFUAXIManager.wvalid   := IFU.io.AXIManager.wvalid
  IFU.io.AXIManager.wready  := io.IFUAXIManager.wready
  io.IFUAXIManager.wdata    := IFU.io.AXIManager.wdata
  io.IFUAXIManager.wstarb   := IFU.io.AXIManager.wstarb
  IFU.io.AXIManager.bvalid  := io.IFUAXIManager.bvalid
  io.IFUAXIManager.bready   := IFU.io.AXIManager.bready
  IFU.io.AXIManager.bid     := io.IFUAXIManager.bid
  IFU.io.AXIManager.bresp   := io.IFUAXIManager.bresp
  io.IFUAXIManager.arvalid  := IFU.io.AXIManager.arvalid
  IFU.io.AXIManager.arready := io.IFUAXIManager.arready
  io.IFUAXIManager.arid     := IFU.io.AXIManager.arid
  io.IFUAXIManager.araddr   := IFU.io.AXIManager.araddr
  io.IFUAXIManager.arport   := IFU.io.AXIManager.arport
  IFU.io.AXIManager.rvalid  := io.IFUAXIManager.rvalid
  io.IFUAXIManager.rready   := IFU.io.AXIManager.rready
  IFU.io.AXIManager.rid     := io.IFUAXIManager.rid
  IFU.io.AXIManager.rdata   := io.IFUAXIManager.rdata
  IFU.io.AXIManager.rresp   := io.IFUAXIManager.rresp

  io.LSUAXIManager.awvalid  := LSU.io.AXIManager.awvalid
  LSU.io.AXIManager.awready := io.LSUAXIManager.awready
  io.LSUAXIManager.awid     := LSU.io.AXIManager.awid
  io.LSUAXIManager.awaddr   := LSU.io.AXIManager.awaddr
  io.LSUAXIManager.awport   := LSU.io.AXIManager.awport
  io.LSUAXIManager.wvalid   := LSU.io.AXIManager.wvalid
  LSU.io.AXIManager.wready  := io.LSUAXIManager.wready
  io.LSUAXIManager.wdata    := LSU.io.AXIManager.wdata
  io.LSUAXIManager.wstarb   := LSU.io.AXIManager.wstarb
  LSU.io.AXIManager.bvalid  := io.LSUAXIManager.bvalid
  io.LSUAXIManager.bready   := LSU.io.AXIManager.bready
  LSU.io.AXIManager.bid     := io.LSUAXIManager.bid
  LSU.io.AXIManager.bresp   := io.LSUAXIManager.bresp
  io.LSUAXIManager.arvalid  := LSU.io.AXIManager.arvalid
  LSU.io.AXIManager.arready := io.LSUAXIManager.arready
  io.LSUAXIManager.arid     := LSU.io.AXIManager.arid
  io.LSUAXIManager.araddr   := LSU.io.AXIManager.araddr
  io.LSUAXIManager.arport   := LSU.io.AXIManager.arport
  LSU.io.AXIManager.rvalid  := io.LSUAXIManager.rvalid
  io.LSUAXIManager.rready   := LSU.io.AXIManager.rready
  LSU.io.AXIManager.rid     := io.LSUAXIManager.rid
  LSU.io.AXIManager.rdata   := io.LSUAXIManager.rdata
  LSU.io.AXIManager.rresp   := io.LSUAXIManager.rresp
}

object StageConnect {
  var arch = "multi"

  def apply[T <: Data](left: DecoupledIO[T], right: DecoupledIO[T]) = {
    if (arch == "single")   {
      right.bits  := left.bits
      left.ready  := true.B
      right.valid := true.B
    }
    else if (arch == "multi") {
      right <> left
    }
    else if (arch == "pipeline") {
      right <> RegEnable(left, left.fire)
    }
    else if (arch == "ooo") {
      right <> Queue(left, 16)
    }
  }
}

class NPC (
  awidth: Int = 32,
  xlen: Int = 32,
  extentionE: Boolean = true,
  PCReset: BigInt = BigInt("80000000", 16)
) extends Module {
  val RVCPU           = Module(new RVCPU(awidth, xlen, extentionE, PCReset))
  val AXILiteArbiter  = Module(new AXILiteArbiter(awidth, xlen, 2))
  val AXILiteMem      = Module(new AXILiteMem(awidth, xlen, 4))
  AXILiteMem.reset    := !reset.asBool

  RVCPU.io.IFUAXIManager <> AXILiteArbiter.io.AXISubordinates(0)
  RVCPU.io.LSUAXIManager <> AXILiteArbiter.io.AXISubordinates(1)
  AXILiteArbiter.io.AXIMananger <> AXILiteMem.io
}
