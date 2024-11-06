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

// class IFU(xlen: Int = 32, PCReset: BigInt = BigInt("80000000", 16)) extends Module {
//   val io = IO(new IFUIO(xlen))

//   /* Handshake and get input */
//   val pc = RegEnable(io.in.bits.nextPc, PCReset.U, io.in.fire)

//   val arfire = io.master.arvalid && io.master.arready
//   val rfire  = io.master.rvalid && io.master.rready

//   val sIdle :: sSetAddr :: sFetch :: sExec :: Nil = Enum(4)

//   val state     = RegInit(sSetAddr)
//   val isIdle    = state === sIdle
//   val isSetAddr = state === sSetAddr
//   val isFetch   = state === sFetch
//   val isExec    = state === sExec

//   state := MuxLookup(state, sSetAddr)(
//     Seq(
//       sIdle -> Mux(io.in.fire, sSetAddr, sIdle),
//       sSetAddr -> Mux(arfire, sFetch, sSetAddr),
//       sFetch -> Mux(rfire, sExec, sFetch),
//       sExec -> Mux(io.out.fire, sIdle, sExec)
//     )
//   )

//   val instruction = RegEnable(io.master.rdata, Instruction.nop.bitPat.value.U, rfire)

//   assert(!rfire || io.master.rresp === "b00".U(2.W))
//   /* IO bind */
//   io.master.awvalid := false.B
//   io.master.awaddr  := DontCare
//   io.master.awid    := DontCare
//   io.master.awlen   := 0.U
//   io.master.awsize  := DontCare
//   io.master.awburst := DontCare

//   io.master.wvalid := false.B
//   io.master.wdata  := DontCare
//   io.master.wstrb  := DontCare
//   io.master.wlast  := DontCare

//   io.master.bready := false.B

//   io.master.arvalid := isSetAddr
//   io.master.araddr  := pc
//   io.master.arid    := 0.U
//   io.master.arlen   := 0.U
//   io.master.arsize  := "b010".U
//   io.master.arburst := "b01".U

//   io.master.rready := isFetch

//   io.in.ready             := isIdle
//   io.out.valid            := isExec
//   io.out.bits.pc          := pc
//   io.out.bits.instruction := instruction
// }

class IFU(awidth: Int = 32, xlen: Int = 32, PCReset: BigInt = BigInt("80000000", 16)) extends Module {
  val io     = IO(new IFUIO(awidth, xlen))
  val ICache = Module(new ICache(awidth, xlen, 4, 16, 16, !Dev.SRAMAddr.in(_)))

  val pc          = RegEnable(io.in.bits.nextPc, PCReset.U, io.in.fire)
  val instruction = RegEnable(ICache.io.out.bits.rdata, Instruction.nop.bitPat.value.U, ICache.io.out.fire)

  val sIdle :: sSetAddr :: sFetch :: sExec :: Nil = Enum(4)

  val state     = RegInit(sSetAddr)
  val isIdle    = state === sIdle
  val isSetAddr = state === sSetAddr
  val isFetch   = state === sFetch
  val isExec    = state === sExec

  state := MuxLookup(state, sSetAddr)(
    Seq(
      sIdle -> Mux(io.in.fire, sSetAddr, sIdle),
      sSetAddr -> Mux(ICache.io.in.fire, sFetch, sSetAddr),
      sFetch -> Mux(ICache.io.out.fire, sExec, sFetch),
      sExec -> Mux(io.out.fire, sIdle, sExec)
    )
  )

  /* IO bind */
  ICache.io.in.valid      := isSetAddr
  ICache.io.in.bits.raddr := pc
  ICache.io.out.ready     := isFetch
  io.master <> ICache.io.master

  io.in.ready := isIdle

  io.out.valid            := isExec
  io.out.bits.pc          := pc
  io.out.bits.instruction := instruction
}
