package RVCPU

import chisel3._
import chisel3.util._

class WBUOut(xlen: Int = 32) extends Bundle {
  val nextPc  = Output(UInt(xlen.W))
  val wa      = Output(UInt(5.W))
  val wbSrc   = Output(UInt(xlen.W))
  val control = new Bundle {
    val regWe = Bool()
  }
}

class WBUIO(xlen: Int = 32) extends Bundle {
  val in  = Flipped(DecoupledIO(new EXUOut))
  val out = DecoupledIO(new WBUOut)
}

class WBU(xlen: Int = 32) extends Module {
  val io = IO(new WBUIO(xlen))

  val sReceive :: sTransmit :: Nil = Enum(2)
  val state = RegInit(sReceive)
  state := MuxLookup(state, sReceive)(Seq(
    sReceive  -> Mux(io.in.fire & !io.out.fire, sTransmit, sReceive),
    sTransmit -> Mux(io.out.fire & !io.in.fire, sReceive, sTransmit)
  ))

  val pcCom   = io.in.bits.pcCom
  val aluOut  = io.in.bits.aluOut
  val memOut  = io.in.bits.memOut
  val csrOut  = io.in.bits.csrOut
  val control = io.in.bits.control

  val pcSrc           = MuxCase(pcCom, Seq(
    PCSrcFrom.fromCom -> pcCom,
    PCSrcFrom.fromCSR -> csrOut,
  ).map{ case (key, data) => (key === control.pcSrc, data) })

  val wbSrc = MuxCase(aluOut, Seq(
    WBSrcFrom.fromALU -> aluOut,
    WBSrcFrom.fromMem -> memOut,
    WBSrcFrom.fromCSR -> csrOut,
  ).map{ case(key, data) => (control.wbSrc === key, data) })

  io.in.ready               := io.out.ready // TODO
  io.out.valid              := io.in.fire // TODO
  io.out.bits.nextPc        := pcSrc
  io.out.bits.wa            := io.in.bits.wa
  io.out.bits.wbSrc         := wbSrc
  io.out.bits.control.regWe := io.in.bits.control.regWe
}
