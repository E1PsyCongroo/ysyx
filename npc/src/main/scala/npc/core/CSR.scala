package rvcpu.core

import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._
import java.lang.module.ModuleDescriptor.Requires

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

object CSRControl {
  def apply(xlen: Int, csrCtr: UInt, csrAddr: UInt, csrIn: UInt, pc: UInt): UInt = {
    require(csrCtr.getWidth == CSRCtr.getWidth)
    require(csrAddr.getWidth == CSRAddress.getWidth)
    require(xlen >= 32)
    val out = Wire(UInt(xlen.W))

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
            case CSRAddress.mcause => WireDefault(0x11.U(xlen.W))
            case _                 => Reg(UInt(xlen.W))
          }
        )
      )
      .toMap

    val csrRead = WireDefault(MuxLookup(csrAddr, 0.U)(csrs.toSeq.map {
      case (addr, csr) => (addr.value.asUInt, csr)
    }))

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

    val mepc = csrs(CSRAddress.mepc)
    mepc := Mux(csrCtr === CSRCtr.csrExcept, pc, Mux(csrAddr === CSRAddress.mepc, csrWrite, mepc))

    val commonCsrs = Seq(
      CSRAddress.mtvec
    )
    for (commCsr <- commonCsrs) {
      csrs(commCsr) := Mux(csrAddr === commCsr, csrWrite, csrs(commCsr))
    }

    val mcause     = csrs(CSRAddress.mcause)
    val mtvec      = csrs(CSRAddress.mtvec)
    val exceptAddr = Mux(mtvec(0), mtvec + (mcause << 2.U), mtvec)
    out := MuxCase(
      csrRead,
      Seq(
        CSRCtr.csrNone -> 0.U,
        CSRCtr.csrExcept -> exceptAddr,
        CSRCtr.csrMret -> csrs(CSRAddress.mepc)
      ).map { case (key, data) => (key === csrCtr, data) }
    )
    out
  }
}
