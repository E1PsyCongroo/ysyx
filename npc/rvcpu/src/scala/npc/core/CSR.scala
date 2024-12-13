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

class CSRState(cpuConfig: CPUConfig) extends Bundle {
  val csrs = Vec(6, UInt(cpuConfig.xlen.W))
}

class CSRIn(cpuConfig: CPUConfig) extends Bundle {
  val csrCtr  = Input(UInt(CSRCtr.getWidth.W))
  val csrAddr = Input(UInt(12.W))
  val csrIn   = Input(UInt(cpuConfig.xlen.W))
  val pc      = Input(UInt(cpuConfig.xlen.W))
}

class CSROut(cpuConfig: CPUConfig) extends Bundle {
  val csrout = Output(UInt(cpuConfig.xlen.W))
}

class CSRControl(cpuConfig: CPUConfig) extends Module {
  val io = IO(new Bundle {
    val in       = new CSRIn(cpuConfig)
    val out      = new CSROut(cpuConfig)
    val csrState = if (cpuConfig.sim) Some(new CSRState(cpuConfig)) else None
  })
  // 机器只支持 M 模式
  val priv = WireDefault(PrivMode.M_Mode.value.U(PrivMode.getWidth.W))
  val csrs = CSRRegs
    .map(csr =>
      (
        csr,
        csr match {
          // 机器不支持中断，仅支持 M 模式
          case CSRAddress.mstatus   => WireDefault(0x1800.U(cpuConfig.xlen.W))
          case CSRAddress.mvendorid => WireDefault(0x79737978.U(cpuConfig.xlen.W))
          case CSRAddress.marchid   => WireDefault(0x0.U(cpuConfig.xlen.W))
          // 仅支持 ECALL 异常
          case CSRAddress.mcause => WireDefault(11.U(cpuConfig.xlen.W))
          case _                 => Reg(UInt(cpuConfig.xlen.W))
        }
      )
    )
    .toMap

  val csrRead = Wire(UInt(cpuConfig.xlen.W))
  csrRead := MuxCase(DontCare, csrs.toSeq.map { case (addr, csr) => (io.in.csrAddr === addr, csr) })
  val csrSet   = WireDefault(csrRead | io.in.csrIn)
  val csrClear = WireDefault(csrRead & (~io.in.csrIn))
  val csrWrite = WireDefault(
    MuxCase(
      csrRead,
      Seq(
        CSRCtr.csrNone -> csrRead,
        CSRCtr.csrRW -> io.in.csrIn,
        CSRCtr.csrRS -> csrSet,
        CSRCtr.csrRC -> csrClear
      ).map { case (key, data) => (key === io.in.csrCtr, data) }
    )
  )

  val mepc = csrs(CSRAddress.mepc)
  val nextMepc =
    Mux(io.in.csrCtr === CSRCtr.csrExcept, io.in.pc, Mux(io.in.csrAddr === CSRAddress.mepc, csrWrite, mepc))
  mepc := nextMepc

  val mcause    = csrs(CSRAddress.mcause)
  val mtvec     = csrs(CSRAddress.mtvec)
  val nextMtvec = Mux(io.in.csrAddr === CSRAddress.mtvec, csrWrite, csrs(CSRAddress.mtvec))
  mtvec := nextMtvec

  val tvec       = mtvec(cpuConfig.xlen - 1, 2) ## "b00".U(2.W)
  val exceptAddr = Mux(mtvec(0), tvec + (mcause << 2.U), tvec)
  io.out.csrout := MuxCase(
    csrRead,
    Seq(
      CSRCtr.csrNone -> 0.U,
      CSRCtr.csrExcept -> exceptAddr,
      CSRCtr.csrMret -> csrs(CSRAddress.mepc)
    ).map { case (key, data) => (key === io.in.csrCtr, data) }
  )

  if (cpuConfig.sim) {
    io.csrState.get.csrs(0) := csrs(CSRAddress.mstatus)
    io.csrState.get.csrs(1) := nextMtvec
    io.csrState.get.csrs(2) := nextMepc
    io.csrState.get.csrs(3) := csrs(CSRAddress.mcause)
    io.csrState.get.csrs(4) := csrs(CSRAddress.mvendorid)
    io.csrState.get.csrs(5) := csrs(CSRAddress.marchid)
  }
}
