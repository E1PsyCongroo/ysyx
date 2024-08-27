package RVCPU

import circt.stage.ChiselStage
import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._
import chisel3.experimental.dataview._

object CSR {
  object PrivMode {
    def getWidth = 2
    val U_Mode = BitPat("b00");
    val S_Mode = BitPat("b01");
    val M_Mode = BitPat("b11");
  }

  object CSRAddress {
    def getWidth = 12
    val mstatus = BitPat(0x300.U)
    val mtvec   = BitPat(0x305.U)
    val mepc    = BitPat(0x341.U)
    val mcause  = BitPat(0x342.U)
  }
  import CSRAddress._
  val CSRRegs = Seq(
    mstatus,
    mtvec,
    mepc,
    mcause
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

// object CSRMstatus {
//   implicit val mstatusView: DataView[UInt, CSRMstatus] = DataView[UInt, CSRMstatus](
//     _ => new CSRMstatus,
//     _(31)     -> _.sd,
//     _(22)     -> _.tsr,
//     _(21)     -> _.tw,
//     _(20)     -> _.tvm,
//     _(19)     -> _.mxr,
//     _(18)     -> _.sum,
//     _(17)     -> _.mprv,
//     _(16, 15) -> _.xs,
//     _(14, 13) -> _.fs,
//     _(12, 11) -> _.mpp,
//     _(10, 9)  -> _.vs,
//     _(8)      -> _.spp,
//     _(7)      -> _.mpie,
//     _(6)      -> _.ube,
//     _(5)      -> _.spie,
//     _(3)      -> _.mie,
//     _(1)      -> _.sie
//   )
// }

class CSRControlIO(xlen: Int) extends Bundle {
  val csrCtr  = Input(UInt(CSRCtr.getWidth.W))
  val csrAddr = Input(UInt(CSRAddress.getWidth.W))
  val csrIn   = Input(UInt(xlen.W))
  val csrOut  = Output(UInt(xlen.W))
  /* Excpetion */
  val epc     = Input(UInt(xlen.W))
}

class CSRControl(xlen: Int) extends Module {
  require(xlen >= 32)
  val io              = IO(new CSRControlIO(xlen))
  val priv            = RegInit(PrivMode.M_Mode.value.U(PrivMode.getWidth.W))

  val csrs            = CSRRegs.map(csr =>
    (csr, if (csr == CSRAddress.mstatus) RegInit(0x1800.U(xlen.W)) else RegInit(0.U(xlen.W)))
  ).toMap
  val csrRead         = WireDefault(MuxCase(0.U, csrs.toSeq.map{ case(key, data) => (key === io.csrAddr, data) }))
  val csrSet          = WireDefault(csrRead | io.csrIn)
  val csrClear        = WireDefault(csrRead & (~io.csrIn))
  val csrCommonWrite  = WireDefault(MuxCase(csrRead, Seq(
    CSRCtr.csrRW -> io.csrIn,
    CSRCtr.csrRS -> csrSet,
    CSRCtr.csrRC -> csrClear,
  ).map { case (key, data) => (key === io.csrCtr, data) }))

  // val mstatus         = csrs(CSRAddress.mstatus).viewAs[CSRMstatus]
  val mstatus         = csrs(CSRAddress.mstatus).asTypeOf(new CSRMstatus)
  val mstatusEcallWr  = WireInit(mstatus)
  mstatusEcallWr.mpie := mstatus.mie
  mstatusEcallWr.mie  := 0.U
  mstatusEcallWr.mpp  := priv
  val mstatusMretWr   = WireDefault(mstatus)
  mstatusMretWr.mpp   := PrivMode.U_Mode.value.U
  mstatusMretWr.mie   := mstatus.mpie
  mstatusMretWr.mpie  := 1.U
  csrs(CSRAddress.mstatus)  := MuxCase(csrCommonWrite, Seq(
    CSRCtr.csrNone  -> mstatus.asUInt,
    CSRCtr.csrEcall -> mstatusEcallWr.asUInt,
    CSRCtr.csrMret  -> mstatusMretWr.asUInt,
  ).map { case (key, data) => (key === io.csrCtr, data) })

  val mcause          = csrs(CSRAddress.mcause)
  val mcauseEcallWr   = WireDefault(MuxCase(2.U, Seq(
    PrivMode.M_Mode -> 11.U,
    PrivMode.S_Mode -> 9.U,
    PrivMode.U_Mode -> 8.U,
  ).map { case (key, data) => (key === priv, data) }))
  mcause              := MuxCase(csrCommonWrite, Seq(
    CSRCtr.csrNone  -> mcause,
    CSRCtr.csrEcall -> mcauseEcallWr,
    CSRCtr.csrMret  -> mcause,
  ).map{ case (key, data) => (key === io.csrCtr, data) })

  val mepc            = csrs(CSRAddress.mepc)
  mepc                := MuxCase(csrCommonWrite, Seq(
    CSRCtr.csrNone  -> mepc,
    CSRCtr.csrEcall -> io.epc,
    CSRCtr.csrMret  -> mepc,
  ).map{ case (key, data) => (key === io.csrCtr, data) })

  val commonCsrs      = Seq(
    CSRAddress.mtvec
  )
  for (commCsr <- commonCsrs) {
    csrs(commCsr)     := csrCommonWrite
  }

  priv                := MuxCase(priv, Seq(
    CSRCtr.csrEcall -> PrivMode.M_Mode.value.U,
    CSRCtr.csrMret  -> mstatus.mpp,
  ).map { case (key, data) => (key === io.csrCtr, data) })
  io.csrOut           := MuxCase(csrRead, Seq(
    CSRCtr.csrNone  -> 0.U,
    CSRCtr.csrEcall -> csrs(CSRAddress.mtvec),
    CSRCtr.csrMret  -> csrs(CSRAddress.mepc),
  ).map { case (key, data) => (key === io.csrCtr, data) })
}

object CSRControl extends App {
  val firtoolOptions = Array("--lowering-options=" + List(
    // make yosys happy
    // see https://github.com/llvm/circt/blob/main/docs/VerilogGeneration.md
    "disallowLocalVariables",
    "disallowPackedArrays",
    "locationInfoStyle=wrapInAtSquareBracket"
  ).reduce(_ + "," + _))
  ChiselStage.emitSystemVerilogFile(new CSRControl(xlen=32), Array("--target-dir", "generate"), firtoolOptions)
}
