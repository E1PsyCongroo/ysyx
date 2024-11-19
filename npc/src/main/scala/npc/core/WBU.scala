package rvcpu.core

import chisel3._
import chisel3.util._

class WBUOut(xlen: Int) extends Bundle {
  val nextPc = Output(UInt(xlen.W))
}

class WBUIO(xlen: Int, extentionE: Boolean) extends Bundle {
  val in  = Flipped(DecoupledIO(new EXUOut(xlen)))
  val out = DecoupledIO(new WBUOut(xlen))
  val RegFileAccess = new Bundle {
    val wa = Output(UInt(if (extentionE) 4.W else 5.W))
    val we = Output(Bool())
    val wd = Output(UInt(xlen.W))
  }
}

class WBU(xlen: Int, extentionE: Boolean) extends Module {
  val io = IO(new WBUIO(xlen, extentionE))

  val in      = RegEnable(io.in.bits, io.in.fire)
  val wa      = in.wa
  val pcCom   = in.pcCom
  val aluOut  = in.aluOut
  val memOut  = in.memOut
  val csrOut  = in.csrOut
  val control = in.control

  val sIdle :: sWriteBack :: Nil = Enum(2)

  val state       = RegInit(sIdle)
  val isIdle      = state === sIdle
  val isWriteBack = state === sWriteBack

  state := MuxLookup(state, sIdle)(
    Seq(
      sIdle -> Mux(io.in.fire, sWriteBack, sIdle),
      sWriteBack -> Mux(io.out.fire, sIdle, sWriteBack)
    )
  )

  val pcSrc = MuxCase(
    pcCom,
    Seq(
      PCSrcFrom.fromCom -> pcCom,
      PCSrcFrom.fromCSR -> csrOut
    ).map { case (key, data) => (key === control.pcSrc, data) }
  )

  val wbSrc = MuxCase(
    aluOut,
    Seq(
      WBSrcFrom.fromALU -> aluOut,
      WBSrcFrom.fromMem -> memOut,
      WBSrcFrom.fromCSR -> csrOut
    ).map { case (key, data) => (control.wbSrc === key, data) }
  )

  io.in.ready         := isIdle
  io.out.valid        := isWriteBack
  io.out.bits.nextPc  := pcSrc
  io.RegFileAccess.wa := wa
  io.RegFileAccess.we := control.regWe && isWriteBack
  io.RegFileAccess.wd := wbSrc
}
