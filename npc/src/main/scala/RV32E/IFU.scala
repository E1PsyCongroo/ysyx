package RVCPU

import chisel3._
import chisel3.util._

class IFUOut(xlen: Int = 32) extends Bundle{
  val pc          = Output(UInt(xlen.W))
  val instruction = Output(UInt(32.W))
  val wa          = Output(UInt(5.W))
  val wbSrc       = Output(UInt(xlen.W))
  val control     = new Bundle {
    val regWe = Bool()
  }
}

class IFUIO(xlen: Int = 32) extends Bundle {
  val pc          = Output(UInt(xlen.W))
  val in          = Flipped(DecoupledIO(new WBUOut))
  val out         = DecoupledIO(new IFUOut)
}

class IFetch(xlen: Int = 32) extends BlackBox with HasBlackBoxResource {
  val io = IO(new Bundle {
    val clock       = Input(Clock())
    val reset       = Input(Reset())
    val pc          = Input(UInt(xlen.W))
    val avalid      = Input(Bool())
    val aready      = Output(Bool())
    val instruction = Output(UInt(32.W))
    val dvalid      = Output(Bool())
    val dready      = Input(Bool())
  })
  addResource("/IFetch.sv")
}

class IFU(xlen: Int = 32, PCReset: BigInt = BigInt("80000000", 16)) extends Module {
  val io = IO(new IFUIO(xlen))

  val nextPc = Mux(reset.asBool, PCReset.U, io.in.bits.nextPc)
  val IFetch = Module(new IFetch(xlen))
  val IFetchSetAddrFire = IFetch.io.avalid && IFetch.io.aready
  val IFetchFire = IFetch.io.dvalid && IFetch.io.dready

  val sSetAddr :: sFetch :: sExec :: Nil = Enum(3)
  val state = RegInit(sFetch)
  state := MuxLookup(state, sSetAddr)(Seq(
    sSetAddr  -> Mux(IFetchSetAddrFire, sFetch, sSetAddr),
    sFetch    -> Mux(IFetchFire, sExec, sFetch),
    sExec     -> Mux(io.in.fire && IFetchSetAddrFire, sFetch, sExec),
  ))

  val isSetAddr = state === sSetAddr
  val isFetch   = state === sFetch
  val isExec    = state === sExec

  val PC = RegEnable(nextPc, PCReset.U, io.in.fire)

  IFetch.io.clock   := clock
  IFetch.io.reset   := reset
  IFetch.io.pc      := nextPc
  IFetch.io.avalid  := io.in.fire
  IFetch.io.dready  := isFetch
  val instruction   = RegEnable(IFetch.io.instruction, IFetchFire)

  io.pc := PC

  io.in.ready               := isExec
  io.out.valid              := isExec
  io.out.bits.pc            := PC
  io.out.bits.instruction   := instruction
  io.out.bits.wa            := io.in.bits.wa
  io.out.bits.wbSrc         := io.in.bits.wbSrc
  io.out.bits.control.regWe := io.in.bits.control.regWe
}
