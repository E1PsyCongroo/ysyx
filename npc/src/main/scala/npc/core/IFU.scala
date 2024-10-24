package rvcpu.core

import rvcpu.axi4._
import rvcpu.dev._
import rvcpu.utility._
import chisel3._
import chisel3.util._

class IFUOut(xlen: Int = 32) extends Bundle {
  val pc          = Output(UInt(xlen.W))
  val instruction = Output(UInt(32.W))
}

class IFUIO(awidth: Int = 32, xlen: Int = 32) extends Bundle {
  val in     = Flipped(DecoupledIO(new WBUOut))
  val out    = DecoupledIO(new IFUOut)
  val master = new AXI4MasterIO
}

class IFU(xlen: Int = 32, PCReset: BigInt = BigInt("80000000", 16)) extends Module {
  val io = IO(new IFUIO(xlen))

  /* Handshake and get input */
  val pc = RegEnable(io.in.bits.nextPc, PCReset.U, io.in.fire)

  val arfire = io.master.arvalid && io.master.arready
  val rfire  = io.master.rvalid && io.master.rready

  val sIdle :: sSetAddr :: sFetch :: sExec :: Nil = Enum(4)
  val state                                       = RegInit(sSetAddr)
  state := MuxLookup(state, sSetAddr)(
    Seq(
      sIdle -> Mux(io.in.fire, sSetAddr, sIdle),
      sSetAddr -> Mux(arfire, sFetch, sSetAddr),
      sFetch -> Mux(rfire, sExec, sFetch),
      sExec -> Mux(io.out.fire, sIdle, sExec)
    )
  )

  val isIdle    = state === sIdle
  val isSetAddr = state === sSetAddr
  val isFetch   = state === sFetch
  val isExec    = state === sExec

  val instruction = RegEnable(io.master.rdata, Instruction.nop.bitPat.value.U, rfire)

  /* For tracer */
  import rvcpu.dev.Dev.SRAMAddr
  val SramTracer = Module(new SramTracer)
  SramTracer.io.raddr := pc
  SramTracer.io.rdata := io.master.rdata
  SramTracer.io.ren   := rfire && SRAMAddr.in(pc)
  SramTracer.io.waddr := DontCare
  SramTracer.io.wdata := DontCare
  SramTracer.io.wlen  := DontCare
  SramTracer.io.wen   := false.B

  assert(!rfire || io.master.rresp === "b00".U(2.W))
  /* IO bind */
  io.master.awvalid := false.B
  io.master.awaddr  := DontCare
  io.master.awid    := DontCare
  io.master.awlen   := 0.U
  io.master.awsize  := DontCare
  io.master.awburst := DontCare

  io.master.wvalid := false.B
  io.master.wdata  := DontCare
  io.master.wstrb  := DontCare
  io.master.wlast  := DontCare

  io.master.bready := false.B

  io.master.arvalid := isSetAddr
  io.master.araddr  := pc
  io.master.arid    := 0.U
  io.master.arlen   := 0.U
  io.master.arsize  := "b010".U
  io.master.arburst := "b01".U

  io.master.rready := isFetch

  io.in.ready             := isIdle
  io.out.valid            := isExec
  io.out.bits.pc          := pc
  io.out.bits.instruction := instruction
}
