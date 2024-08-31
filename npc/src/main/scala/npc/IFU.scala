package rvcpu.core

import rvcpu.dev._
import rvcpu.utility._
import chisel3._
import chisel3.util._

class IFUOut(xlen: Int = 32) extends Bundle{
  val pc          = Output(UInt(xlen.W))
  val instruction = Output(UInt(32.W))
}

class IFUIO(awidth:Int = 32, xlen: Int = 32) extends Bundle {
  val pc          = Output(UInt(xlen.W))
  val in          = Flipped(DecoupledIO(new WBUOut))
  val out         = DecoupledIO(new IFUOut)
  val AXIManager  = Flipped(new AXILiteSubordinateIO(awidth, xlen))
}

class IFU(xlen: Int = 32, PCReset: BigInt = BigInt("80000000", 16)) extends Module {
  val io = IO(new IFUIO(xlen))

  val arfire  = io.AXIManager.arvalid && io.AXIManager.arready
  val rfire   = io.AXIManager.rvalid && io.AXIManager.rready

  val sFetch :: sExec :: sSetAddr :: Nil = Enum(3)
  val state = RegInit(sSetAddr)
  state := MuxLookup(state, sSetAddr)(Seq(
    sSetAddr  -> Mux(arfire, sFetch, sSetAddr),
    sFetch    -> Mux(rfire, sExec, sFetch),
    sExec     -> Mux(io.in.fire, sSetAddr, sExec),
  ))

  val isFetch   = state === sFetch
  val isExec    = state === sExec
  val isSetAddr = state === sSetAddr

  val pc          = RegEnable(io.in.bits.nextPc, PCReset.U, io.in.fire)
  val instruction = RegEnable(io.AXIManager.rdata, Instruction.nop.bitPat.value.U, rfire)

  /* IO bind */
  /* Write address channel */
  io.AXIManager.awvalid   := false.B
  io.AXIManager.awid      := DontCare
  io.AXIManager.awaddr    := DontCare
  io.AXIManager.awport    := DontCare

  /* Write data channel */
  io.AXIManager.wvalid    := false.B
  io.AXIManager.wdata     := DontCare
  io.AXIManager.wstarb    := DontCare

  /* Write response channel */
  io.AXIManager.bready    := true.B

  /* Read address channel */
  io.AXIManager.arvalid   := isSetAddr
  io.AXIManager.arid      := DontCare
  io.AXIManager.araddr    := pc
  io.AXIManager.arport    := DontCare

  /* Read data channel */
  io.AXIManager.rready    := isFetch

  io.pc                   := pc
  io.in.ready             := isExec
  io.out.valid            := isExec
  io.out.bits.pc          := pc
  io.out.bits.instruction := instruction
}
