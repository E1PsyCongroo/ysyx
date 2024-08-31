package rvcpu.core

import chisel3._
import chisel3.util._

class WBUOut(xlen: Int = 32) extends Bundle {
  val nextPc  = Output(UInt(xlen.W))

}

class WBUIO(xlen: Int = 32, extentionE: Boolean = true) extends Bundle {
  val in  = Flipped(DecoupledIO(new EXUOut))
  val out = DecoupledIO(new WBUOut)
  val RegFileAccess = new Bundle {
    val wa = Output(UInt(if (extentionE) 4.W else 5.W))
    val we = Output(Bool())
    val wd = Output(UInt(xlen.W))
  }
}

class WBU(xlen: Int = 32, extentionE: Boolean = true) extends Module {
  val io = IO(new WBUIO(xlen, extentionE))

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

  io.in.ready               := io.out.ready
  io.out.valid              := io.in.fire
  io.out.bits.nextPc        := pcSrc
  io.RegFileAccess.wa       := io.in.bits.wa
  io.RegFileAccess.we       := io.in.bits.control.regWe && io.out.valid
  io.RegFileAccess.wd       := wbSrc
}
