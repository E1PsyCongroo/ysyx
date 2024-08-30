package RVCPU

import util._
import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._

class AXILiteSubordinateIO(awidth:Int = 32, dwidth: Int = 32) extends Bundle {
  /* Write address channel */
  val awvalid = Input(Bool())
  val awready = Output(Bool())
  val awid    = Input(UInt(0.W))
  val awaddr  = Input(UInt(awidth.W))
  val awport  = Input(UInt(3.W))

  /* Write data channel */
  val wvalid  = Input(Bool())
  val wready  = Output(Bool())
  val wdata   = Input(UInt(dwidth.W))
  val wstarb  = Input(UInt((dwidth/8).W))

  /* Write response channel */
  val bvalid  = Output(Bool())
  val bready  = Input(Bool())
  val bid     = Output(UInt(0.W))
  val bresp   = Output(UInt(2.W))

  /* Read address channel */
  val arvalid = Input(Bool())
  val arready = Output(Bool())
  val arid    = Input(UInt(0.W))
  val araddr  = Input(UInt(awidth.W))
  val arport  = Input(UInt(3.W))

  /* Read data channel */
  val rvalid  = Output(Bool())
  val rready  = Input(Bool())
  val rid     = Output(UInt(0.W))
  val rdata   = Output(UInt(dwidth.W))
  val rresp   = Output(UInt(2.W))
}

object TransactionResponse extends ChiselEnum{
  val okey    = Value("b00".U)
  val exokay  = Value("b01".U)
  val slverr  = Value("b10".U)
  val decerr  = Value("b11".U)
}


class AXILiteArbiter(awidth: Int = 32, dwidth: Int = 32, managerNum: Int) extends Module {
  val io = IO(new Bundle {
    val AXISubordinates   = Vec(managerNum, new AXILiteSubordinateIO(awidth, dwidth))
    val AXIMananger       = Flipped(new AXILiteSubordinateIO(awidth, dwidth))
  })

  val writeRequests       = VecInit(io.AXISubordinates.map(
    manager => (manager.awvalid || manager.wvalid)
  )).asUInt
  val readRequests        = VecInit(io.AXISubordinates.map(
    manager => manager.arvalid
  )).asUInt
  val requests            = writeRequests | readRequests
  val selected            = PriorityEncoder(requests)
  val isWriteTransaction  = writeRequests(selected)
  val isReadTransaction   = readRequests(selected)
  assert(!(isWriteTransaction && isReadTransaction))
  val selectedReg         = RegEnable(selected, isWriteTransaction || isReadTransaction)
  val selectedManager     = io.AXISubordinates(selectedReg)
  val bfire               = selectedManager.bvalid && selectedManager.bready
  val rfire               = selectedManager.rvalid && selectedManager.rready

  val sIdle :: sWaitWrite :: sWaitRead :: Nil = Enum(3)
  val state = RegInit(sIdle)
  state := MuxLookup(state, sIdle)(Seq(
    sIdle       -> MuxCase(sIdle, Seq(
      isWriteTransaction  -> sWaitWrite,
      isReadTransaction   -> sWaitRead,
    )),
    sWaitWrite  -> Mux(bfire, sIdle, sWaitWrite),
    sWaitRead   -> Mux(rfire, sIdle, sWaitRead),
  ))

  val isIdle      = state === sIdle
  val isWaitWrite = state === sWaitWrite
  val isWaitRead  = state === sWaitRead


  val nonResp     = Wire(new AXILiteSubordinateIO(awidth, dwidth))
  /* Write address channel */
  nonResp.awready := false.B

  /* Write data channel */
  nonResp.wready  := false.B

  /* Write response channel */
  nonResp.bvalid  := false.B
  nonResp.bid     := DontCare
  nonResp.bresp   := DontCare

  /* Read address channel */
  nonResp.arready := false.B

  /* Read data channel */
  nonResp.rvalid  := false.B
  nonResp.rid     := DontCare
  nonResp.rdata   := DontCare
  nonResp.rresp   := DontCare

  val nonTrans      = Wire(Flipped(new AXILiteSubordinateIO(awidth, dwidth)))
  /* Write address channel */
  nonTrans.awvalid  := false.B
  nonTrans.awid     := DontCare
  nonTrans.awaddr   := DontCare
  nonTrans.awport   := DontCare

  /* Write data channel */
  nonTrans.wvalid   := false.B
  nonTrans.wdata    := DontCare
  nonTrans.wstarb   := DontCare

  /* Write response channel */
  nonTrans.bready   := false.B

  /* Read address channel */
  nonTrans.arvalid  := false.B
  nonTrans.arid     := DontCare
  nonTrans.araddr   := DontCare
  nonTrans.arport   := DontCare

  /* Read data channel */
  nonTrans.rready   := false.B

  nonTrans <> nonResp

  for (i <- 0 until managerNum) {
    io.AXISubordinates(i) <> nonResp
  }

  when (isWaitWrite || isWaitRead) {
    io.AXISubordinates(selectedReg) <> io.AXIMananger
  }.otherwise {
    nonTrans <> io.AXIMananger
  }
}
