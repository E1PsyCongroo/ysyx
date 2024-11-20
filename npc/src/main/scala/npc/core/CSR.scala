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

class CSRControlIO(xlen: Int) extends Bundle {
  val csrCtr  = Input(UInt(CSRCtr.getWidth.W))
  val csrAddr = Input(UInt(CSRAddress.getWidth.W))
  val csrIn   = Input(UInt(xlen.W))
  val csrOut  = Output(UInt(xlen.W))
  /* Excpetion */
  val epc = Input(UInt(xlen.W))
}

class CSRControl(xlen: Int) extends Module {
  require(xlen >= 32)
  val io   = IO(new CSRControlIO(xlen))
  val priv = RegInit(PrivMode.M_Mode.value.U(PrivMode.getWidth.W))

  val csrs = CSRRegs
    .map(csr =>
      (
        csr,
        csr match {
          case CSRAddress.mstatus   => RegInit(0x1800.U(xlen.W))
          case CSRAddress.mvendorid => WireDefault(0x79737978.U(xlen.W))
          case CSRAddress.marchid   => WireDefault(0x0.U(xlen.W))
          case _                    => Reg(UInt(xlen.W))
        }
      )
    )
    .toMap
  val csrRead  = WireDefault(MuxLookup(io.csrAddr, 0.U)(csrs.toSeq.map { case (addr, csr) => (addr.value.asUInt, csr) }))
  val csrSet   = WireDefault(csrRead | io.csrIn)
  val csrClear = WireDefault(csrRead & (~io.csrIn))
  val csrCommonWrite = WireDefault(
    MuxCase(
      csrRead,
      Seq(
        CSRCtr.csrRW -> io.csrIn,
        CSRCtr.csrRS -> csrSet,
        CSRCtr.csrRC -> csrClear
      ).map { case (key, data) => (key === io.csrCtr, data) }
    )
  )

  val mstatus = csrs(CSRAddress.mstatus).asTypeOf(new CSRMstatus)
  val mstatusEn = (io.csrAddr === CSRAddress.mstatus) || (io.csrCtr === CSRCtr.csrEcall) ||
    (io.csrCtr === CSRCtr.csrMret)
  val mstatusEcallWr = WireInit(mstatus)
  mstatusEcallWr.mpie := mstatus.mie
  mstatusEcallWr.mie  := 0.U
  mstatusEcallWr.mpp  := priv
  val mstatusMretWr = WireDefault(mstatus)
  mstatusMretWr.mpp  := PrivMode.U_Mode.value.U
  mstatusMretWr.mie  := mstatus.mpie
  mstatusMretWr.mpie := 1.U
  csrs(CSRAddress.mstatus) := Mux(
    mstatusEn,
    MuxCase(
      csrCommonWrite,
      Seq(
        CSRCtr.csrNone -> mstatus.asUInt,
        CSRCtr.csrEcall -> mstatusEcallWr.asUInt,
        CSRCtr.csrMret -> mstatusMretWr.asUInt
      ).map { case (key, data) => (key === io.csrCtr, data) }
    ),
    mstatus.asUInt
  )

  val mcause = csrs(CSRAddress.mcause)
  val mcauseEn = (io.csrAddr === CSRAddress.mcause) || (io.csrCtr === CSRCtr.csrEcall) ||
    (io.csrCtr === CSRCtr.csrMret)
  val mcauseEcallWr = WireDefault(
    MuxCase(
      2.U,
      Seq(
        PrivMode.M_Mode -> 11.U,
        PrivMode.S_Mode -> 9.U,
        PrivMode.U_Mode -> 8.U
      ).map { case (key, data) => (key === priv, data) }
    )
  )
  mcause := Mux(
    mcauseEn,
    MuxCase(
      csrCommonWrite,
      Seq(
        CSRCtr.csrNone -> mcause,
        CSRCtr.csrEcall -> mcauseEcallWr,
        CSRCtr.csrMret -> mcause
      ).map { case (key, data) => (key === io.csrCtr, data) }
    ),
    mcause
  )

  val mepc   = csrs(CSRAddress.mepc)
  val mepcEn = (io.csrAddr === CSRAddress.mepc) || (io.csrCtr === CSRCtr.csrEcall)
  mepc := Mux(
    mepcEn,
    MuxCase(
      csrCommonWrite,
      Seq(
        CSRCtr.csrNone -> mepc,
        CSRCtr.csrEcall -> io.epc,
        CSRCtr.csrMret -> mepc
      ).map { case (key, data) => (key === io.csrCtr, data) }
    ),
    mepc
  )

  val commonCsrs = Seq(
    CSRAddress.mtvec
  )
  for (commCsr <- commonCsrs) {
    csrs(commCsr) := Mux(io.csrAddr === commCsr, csrCommonWrite, csrs(commCsr))
  }

  priv := MuxCase(
    priv,
    Seq(
      CSRCtr.csrEcall -> PrivMode.M_Mode.value.U,
      CSRCtr.csrMret -> mstatus.mpp
    ).map { case (key, data) => (key === io.csrCtr, data) }
  )
  io.csrOut := MuxCase(
    csrRead,
    Seq(
      CSRCtr.csrNone -> 0.U,
      CSRCtr.csrEcall -> csrs(CSRAddress.mtvec),
      CSRCtr.csrMret -> csrs(CSRAddress.mepc)
    ).map { case (key, data) => (key === io.csrCtr, data) }
  )
}
