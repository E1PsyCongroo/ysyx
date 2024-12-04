package rvcpu.core

import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._

object CSR {
  object PrivMode {
    def getWidth = 2
    val U_Mode   = BitPat("b00");
    val S_Mode   = BitPat("b01");
    val M_Mode   = BitPat("b11");
  }

  object CSRAddress {
    def getWidth  = 12
    val mstatus   = BitPat(0x300.U)
    val mtvec     = BitPat(0x305.U)
    val mepc      = BitPat(0x341.U)
    val mcause    = BitPat(0x342.U)
    val mvendorid = BitPat(0xf11.U)
    val marchid   = BitPat(0xf12.U)
  }
  import CSRAddress._
  val CSRRegs = Seq(
    mstatus,
    mtvec,
    mepc,
    mcause,
    mvendorid,
    marchid
  )
}
import CSR._

class CSRMstatus extends Bundle {
  val sd    = UInt(1.W)
  val wpri0 = UInt(8.W)
  val tsr   = UInt(1.W)
  val tw    = UInt(1.W)
  val tvm   = UInt(1.W)
  val mxr   = UInt(1.W)
  val sum   = UInt(1.W)
  val mprv  = UInt(1.W)
  val xs    = UInt(2.W)
  val fs    = UInt(2.W)
  val mpp   = UInt(2.W)
  val vs    = UInt(2.W)
  val spp   = UInt(1.W)
  val mpie  = UInt(1.W)
  val ube   = UInt(1.W)
  val spie  = UInt(1.W)
  val wpri1 = UInt(1.W)
  val mie   = UInt(1.W)
  val wpri2 = UInt(1.W)
  val sie   = UInt(1.W)
  val wpri3 = UInt(1.W)
}

class CSROut(xlen: Int, sim: Boolean) extends Bundle {
  val out   = UInt(xlen.W)
  val mtvec = if (sim) Some(UInt(xlen.W)) else None
  val mepc  = if (sim) Some(UInt(xlen.W)) else None
}

object CSRControl {
  def apply(xlen: Int, sim: Boolean, csrCtr: UInt, csrAddr: UInt, csrIn: UInt, pc: UInt): CSROut = {
    require(csrCtr.getWidth == CSRCtr.getWidth)
    require(csrAddr.getWidth == CSRAddress.getWidth)
    require(xlen >= 32)
    val out = Wire(new CSROut(xlen, sim))

    // 机器只支持 M 模式
    val priv = WireDefault(PrivMode.M_Mode.value.U(PrivMode.getWidth.W))
    val csrs = CSRRegs
      .map(csr =>
        (
          csr,
          csr match {
            // 机器不支持中断，仅支持 M 模式
            case CSRAddress.mstatus   => WireDefault(0x1800.U(xlen.W))
            case CSRAddress.mvendorid => WireDefault(0x79737978.U(xlen.W))
            case CSRAddress.marchid   => WireDefault(0x0.U(xlen.W))
            // 仅支持 ECALL 异常
            case CSRAddress.mcause => WireDefault(11.U(xlen.W))
            case _                 => Reg(UInt(xlen.W))
          }
        )
      )
      .toMap

    val csrRead = Wire(UInt(xlen.W))
    csrRead := MuxCase(DontCare, csrs.toSeq.map { case (addr, csr) => (csrAddr === addr, csr) })
    val csrSet   = WireDefault(csrRead | csrIn)
    val csrClear = WireDefault(csrRead & (~csrIn))
    val csrWrite = WireDefault(
      MuxCase(
        csrRead,
        Seq(
          CSRCtr.csrNone -> csrRead,
          CSRCtr.csrRW -> csrIn,
          CSRCtr.csrRS -> csrSet,
          CSRCtr.csrRC -> csrClear
        ).map { case (key, data) => (key === csrCtr, data) }
      )
    )

    val mepc     = csrs(CSRAddress.mepc)
    val nextMepc = Mux(csrCtr === CSRCtr.csrExcept, pc, Mux(csrAddr === CSRAddress.mepc, csrWrite, mepc))
    mepc := nextMepc

    val mcause    = csrs(CSRAddress.mcause)
    val mtvec     = csrs(CSRAddress.mtvec)
    val nextMtvec = Mux(csrAddr === CSRAddress.mtvec, csrWrite, csrs(CSRAddress.mtvec))
    mtvec := nextMtvec

    val tvec       = mtvec(xlen - 1, 2) ## "b00".U(2.W)
    val exceptAddr = Mux(mtvec(0), tvec + (mcause << 2.U), tvec)
    out.out := MuxCase(
      csrRead,
      Seq(
        CSRCtr.csrNone -> 0.U,
        CSRCtr.csrExcept -> exceptAddr,
        CSRCtr.csrMret -> csrs(CSRAddress.mepc)
      ).map { case (key, data) => (key === csrCtr, data) }
    )
    if (sim) {
      out.mtvec.get := nextMtvec
      out.mepc.get  := nextMepc
    }
    out
  }
}
