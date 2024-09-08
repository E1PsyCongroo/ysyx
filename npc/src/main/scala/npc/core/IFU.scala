package rvcpu.core

import rvcpu.axi4._
import rvcpu.dev._
import rvcpu.utility._
import chisel3._
import chisel3.util._

class IFUOut(xlen: Int = 32) extends Bundle{
  val pc          = Output(UInt(xlen.W))
  val instruction = Output(UInt(32.W))
}

class IFUIO(awidth:Int = 32, xlen: Int = 32) extends Bundle {
  val in          = Flipped(DecoupledIO(new WBUOut))
  val out         = DecoupledIO(new IFUOut)
  val master   = new AXI4MasterIO
}

class IFU(xlen: Int = 32, PCReset: BigInt = BigInt("80000000", 16)) extends Module {
  val io = IO(new IFUIO(xlen))

  val arfire  = io.master.arvalid && io.master.arready
  val rfire   = io.master.rvalid && io.master.rready

  val sFetch :: sExec :: sSetAddr :: Nil = Enum(3)
  val state = RegInit(sSetAddr)
  state := MuxLookup(state, sSetAddr)(Seq(
    sSetAddr  -> Mux(arfire, sFetch, sSetAddr),
    sFetch    -> Mux(rfire, sExec, sFetch),
    sExec     -> Mux(io.out.fire, sSetAddr, sExec),
  ))

  val isFetch   = state === sFetch
  val isExec    = state === sExec
  val isSetAddr = state === sSetAddr

  val pc          = RegEnable(io.in.bits.nextPc, PCReset.U, io.in.fire)
  val instruction = RegEnable(io.master.rdata, Instruction.nop.bitPat.value.U, rfire)

  /* IO bind */
  io.master.awvalid    := false.B
  io.master.awaddr     := DontCare
  io.master.awid       := DontCare
  io.master.awlen      := 0.U
  io.master.awsize     := "b101".U
  io.master.awburst    := "b01".U

  io.master.wvalid     := false.B
  io.master.wdata      := DontCare
  io.master.wstarb     := DontCare
  io.master.wlast      := DontCare

  io.master.bready     := false.B

  io.master.arvalid    := isSetAddr
  io.master.araddr     := pc
  io.master.arid       := DontCare
  io.master.arlen      := 0.U
  io.master.arsize     := "b101".U
  io.master.arbrust    := "b01".U

  io.master.rready     := isFetch

  io.in.ready             := isExec
  io.out.valid            := isExec
  io.out.bits.pc          := pc
  io.out.bits.instruction := instruction
}
