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
  val instruction = Input(UInt(32.W))
  val in          = Flipped(DecoupledIO(new WBUOut))
  val out         = DecoupledIO(new IFUOut)
}

class IFU(xlen: Int = 32, PCReset: BigInt = BigInt("80000000", 16)) extends Module {
  val io = IO(new IFUIO(xlen))

  val nextPc = Mux(reset.asBool, PCReset.U, io.in.bits.nextPc)
  val PC = RegEnable(nextPc, io.in.fire)
  io.pc := nextPc

  val sReceive :: sTransmit :: Nil = Enum(2)
  val state = RegInit(sReceive)
  state := MuxLookup(state, sReceive)(Seq(
    sReceive  -> Mux(io.in.fire & !io.out.fire, sTransmit, sReceive),
    sTransmit -> Mux(io.out.fire & !io.in.fire, sReceive, sTransmit)
  ))

  io.in.ready := true.B // TODO

  io.out.valid              := true.B // TODO
  io.out.bits.pc            := PC
  io.out.bits.instruction   := io.instruction
  io.out.bits.wa            := io.in.bits.wa
  io.out.bits.wbSrc         := io.in.bits.wbSrc
  io.out.bits.control.regWe := io.in.bits.control.regWe
}
