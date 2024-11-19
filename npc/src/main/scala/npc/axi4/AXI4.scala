package rvcpu.axi4

import rvcpu.dev._
import chisel3._
import chisel3.util._

object TransactionResponse extends ChiselEnum {
  val okey   = Value("b00".U)
  val exokay = Value("b01".U)
  val slverr = Value("b10".U)
  val decerr = Value("b11".U)
}

class AXI4MasterIO extends AXI4LiteMasterIO {
  /* Write address channel */
  val awid    = Output(UInt(4.W))
  val awlen   = Output(UInt(8.W))
  val awsize  = Output(UInt(3.W))
  val awburst = Output(UInt(2.W))

  /* Write data channel */
  val wlast = Output(Bool())

  /* Write response channel */
  val bid = Input(UInt(4.W))

  /* Read address channel */
  val arid    = Output(UInt(4.W))
  val arlen   = Output(UInt(8.W))
  val arsize  = Output(UInt(3.W))
  val arburst = Output(UInt(2.W))

  /* Read data channel */
  val rlast = Input(Bool())
  val rid   = Input(UInt(4.W))
}

class AXI4SlaveIO extends AXI4LiteSlaveIO {
  /* Write address channel */
  val awid    = Input(UInt(4.W))
  val awlen   = Input(UInt(8.W))
  val awsize  = Input(UInt(3.W))
  val awburst = Input(UInt(2.W))

  /* Write data channel */
  val wlast = Input(Bool())

  /* Write response channel */
  val bid = Output(UInt(4.W))

  /* Read address channel */
  val arid    = Input(UInt(4.W))
  val arlen   = Input(UInt(8.W))
  val arsize  = Input(UInt(3.W))
  val arburst = Input(UInt(2.W))

  /* Read data channel */
  val rlast = Output(Bool())
  val rid   = Output(UInt(4.W))
}

object AXI4 {
  def none: AXI4MasterIO = {
    val none = Wire(new AXI4MasterIO)
    /* Write address channel */
    none.awready := false.B
    none.awvalid := false.B
    none.awaddr  := DontCare
    none.awid    := DontCare
    none.awlen   := DontCare
    none.awsize  := DontCare
    none.awburst := DontCare
    /* Write data channel */
    none.wready := false.B
    none.wvalid := false.B
    none.wdata  := DontCare
    none.wstrb  := DontCare
    none.wlast  := DontCare
    /* Write response channel */
    none.bready := false.B
    none.bvalid := false.B
    none.bresp  := DontCare
    none.bid    := DontCare
    /* Read address channel */
    none.arready := false.B
    none.arvalid := false.B
    none.araddr  := DontCare
    none.arid    := DontCare
    none.arlen   := DontCare
    none.arsize  := DontCare
    none.arburst := DontCare
    /* Read data channel */
    none.rready := false.B
    none.rvalid := false.B
    none.rresp  := DontCare
    none.rdata  := DontCare
    none.rlast  := DontCare
    none.rid    := DontCare
    none
  }
}

class AXI4Arbiter(masterNum: Int) extends Module {
  val io = IO(new Bundle {
    val masters = Vec(masterNum, Flipped(new AXI4MasterIO))
    val slave   = new AXI4MasterIO
  })

  val sIdle :: sWaitWrite :: sWaitRead :: sReset :: Nil = Enum(4)

  val isIdle      = state === sIdle
  val isWaitWrite = state === sWaitWrite
  val isWaitRead  = state === sWaitRead
  val isReset     = state === sReset
  val isWait      = isWaitWrite || isWaitRead

  val state = RegInit(sReset)

  val writeRequests      = VecInit(io.masters.map(master => (master.awvalid || master.wvalid))).asUInt
  val readRequests       = VecInit(io.masters.map(master => master.arvalid)).asUInt
  val requests           = writeRequests | readRequests
  val selected           = PriorityEncoder(requests)
  val isWriteTransaction = writeRequests(selected)
  val isReadTransaction  = readRequests(selected)
  assert(!(isWriteTransaction && isReadTransaction))
  val selectedReg    = RegEnable(selected, 0.U, isIdle)
  val selectedMaster = io.masters(selectedReg)
  val bfire          = io.slave.bvalid && io.slave.bready
  val rfire          = io.slave.rvalid && io.slave.rready

  val waitPrevTrans = withReset(false.B)(Reg(Bool()))
  waitPrevTrans := MuxCase(
    waitPrevTrans,
    Seq(
      reset.asBool -> isWait,
      (isReset && (bfire || rfire)) -> false.B
    )
  )

  state := MuxLookup(state, sIdle)(
    Seq(
      sIdle -> MuxCase(
        sIdle,
        Seq(
          isWriteTransaction -> sWaitWrite,
          isReadTransaction -> sWaitRead
        )
      ),
      sWaitWrite -> Mux(bfire, sIdle, sWaitWrite),
      sWaitRead -> Mux(rfire, sIdle, sWaitRead),
      sReset -> Mux(waitPrevTrans, sReset, sIdle)
    )
  )

  AXI4.none <> io.slave
  for (i <- 0 until masterNum) {
    io.masters(i) <> AXI4.none
    when(isWait && i.U === selectedReg) {
      io.masters(i) <> io.slave
    }
  }
}

object AXI4Arbiter {
  def apply(masters: Seq[AXI4MasterIO]) = {
    val Arbiter = Module(new AXI4Arbiter(masters.length))
    for (i <- 0 until masters.length) {
      masters(i) <> Arbiter.io.masters(i)
    }
    Arbiter.io.slave
  }
}

class AXI4Xbar(slaves: Seq[Area]) extends Module {
  val length = slaves.length
  val io = IO(new Bundle {
    val master = Flipped(new AXI4MasterIO)
    val slaves = Vec(length, new AXI4MasterIO)
  })

  val sIdle :: sWaitWaddr :: sWaitBresp :: sWaitRresp :: Nil = Enum(4)

  val isIdle      = state === sIdle
  val isWaitWaddr = state === sWaitWaddr
  val isWaitBresp = state === sWaitBresp
  val isWaitRresp = state === sWaitRresp

  val state = RegInit(sIdle)

  val isWaddrTransaction = io.master.awvalid
  val isWdataTransaction = io.master.wvalid
  val isReadTransaction  = io.master.arvalid
  assert(!((isWaddrTransaction || isWdataTransaction) && isReadTransaction))

  val awfire = io.master.awvalid && io.master.awready
  val wfire  = io.master.wvalid && io.master.wready
  val bfire  = io.master.bvalid && io.master.bready
  val arfire = io.master.arvalid && io.master.arready
  val rfire  = io.master.rvalid && io.master.rready

  state := MuxLookup(state, sIdle)(
    Seq(
      sIdle -> MuxCase(
        sIdle,
        Seq(
          isWaddrTransaction -> sWaitBresp,
          isWdataTransaction -> sWaitWaddr,
          isReadTransaction -> sWaitRresp
        )
      ),
      sWaitWaddr -> Mux(isWaddrTransaction, sWaitBresp, sWaitWaddr),
      sWaitBresp -> Mux(bfire, sIdle, sWaitBresp),
      sWaitRresp -> Mux(rfire, sIdle, sWaitRresp)
    )
  )

  val transAddr = RegInit(0.U(32.W))
  transAddr := MuxCase(
    transAddr,
    Seq(
      (isIdle && isReadTransaction) -> io.master.araddr,
      (isIdle && isWaddrTransaction) -> io.master.awaddr,
      (isWaitWaddr && isWaddrTransaction) -> io.master.awaddr
    )
  )
  val areas       = VecInit(slaves.map(area => VecInit(area.start, area.end)))
  val validSlaves = VecInit(areas.map(area => (transAddr >= area(0) && transAddr <= area(1)))).asUInt
  val haveSlave   = validSlaves.orR && (isWaitBresp || isWaitRresp)
  val slaveSelect = WireDefault(Mux(haveSlave, PriorityEncoder(validSlaves), 0.U))

  io.master <> AXI4.none
  for (i <- 0 until slaves.length) {
    AXI4.none <> io.slaves(i)
  }

  when(haveSlave) {
    io.master <> io.slaves(slaveSelect)
  }
}

object AXI4Xbar {
  def apply(
    master: AXI4MasterIO,
    slaves: Seq[(Area, AXI4SlaveIO)]
  ) = {
    val Xbar = Module(new AXI4Xbar(slaves.map(data => data._1)))
    master <> Xbar.io.master
    for (i <- 0 until slaves.length) {
      Xbar.io.slaves(i) <> slaves(i)._2
    }
  }
}

class AXI4Interconnect(fanInNum: Int, fanOutSeq: Seq[UInt => Bool]) extends Module {
  val io = IO(new Bundle {
    val fanIn  = Vec(fanInNum, Flipped(new AXI4MasterIO))
    val fanOut = Vec(fanOutSeq.size, new AXI4MasterIO)
  })

  val sIdle :: sWaitWdata :: sWaitBresp :: sWaitRresp :: Nil = Enum(4)

  val state       = RegInit(sIdle)
  val isIdle      = state === sIdle
  val isWaitWdata = state === sWaitWdata
  val isWaitBresp = state === sWaitBresp
  val isWaitRresp = state === sWaitRresp

  val waddrRequests = VecInit(io.fanIn.map(in => in.awvalid)).asUInt
  val wdataRequests = VecInit(io.fanIn.map(in => in.wvalid)).asUInt
  val readRequests  = VecInit(io.fanIn.map(in => in.arvalid)).asUInt
  val requests      = waddrRequests | wdataRequests | readRequests

  val selected           = PriorityEncoder(requests)
  val selectedReg        = RegEnable(selected, isIdle)
  val isWaddrTransaction = waddrRequests(selected)
  val isWdataTransaction = wdataRequests(selected)
  val isReadTransaction  = readRequests(selected)
  assert(!((isWaddrTransaction || isWdataTransaction) && isReadTransaction))

  val awfire = io.fanIn(selectedReg).awvalid && io.fanIn(selectedReg).awready
  val wfire  = io.fanIn(selectedReg).wvalid && io.fanIn(selectedReg).wready
  val bfire  = io.fanIn(selectedReg).bvalid && io.fanIn(selectedReg).bready
  val arfire = io.fanIn(selectedReg).arvalid && io.fanIn(selectedReg).arready
  val rfire  = io.fanIn(selectedReg).rvalid && io.fanIn(selectedReg).rready && io.fanIn(selectedReg).rlast

  state := MuxLookup(state, sIdle)(
    Seq(
      sIdle -> MuxCase(
        sIdle,
        Seq(
          (isWaddrTransaction && isWdataTransaction) -> sWaitBresp,
          isWaddrTransaction -> sWaitWdata,
          isReadTransaction -> sWaitRresp
        )
      ),
      sWaitWdata -> Mux(wfire, sWaitBresp, sWaitWdata),
      sWaitBresp -> Mux(bfire, sIdle, sWaitBresp),
      sWaitRresp -> Mux(rfire, sIdle, sWaitRresp)
    )
  )

  val transAddr = WireDefault(
    MuxCase(
      0.U,
      Seq(
        (isIdle && isReadTransaction) -> io.fanIn(selected).araddr,
        (isIdle && isWaddrTransaction) -> io.fanIn(selected).awaddr
      )
    )
  )
  val matches     = VecInit(fanOutSeq.map(_(transAddr))).asUInt
  val fanOutValid = isWaitWdata || isWaitBresp || isWaitRresp
  val outSelect   = RegEnable(PriorityEncoder(matches), isIdle)

  for (i <- 0 until fanInNum) {
    io.fanIn(i) <> AXI4.none
  }
  for (i <- 0 until fanOutSeq.size) {
    AXI4.none <> io.fanOut(i)
  }

  when(fanOutValid) {
    io.fanIn(selectedReg) <> io.fanOut(outSelect)
  }
}

object AXI4Interconnect {
  def apply(
    fanIn: Seq[AXI4MasterIO],
    fanOutArea: Seq[UInt => Bool],
    fanOut: Seq[AXI4MasterIO]
  ) = {
    val AXI4Interconnect = Module(new AXI4Interconnect(fanIn.length, fanOutArea))
    for (i <- 0 until fanIn.length) {
      AXI4Interconnect.io.fanIn(i) <> fanIn(i)
    }
    for (i <- 0 until fanOut.length) {
      AXI4Interconnect.io.fanOut(i) <> fanOut(i)
    }
  }
}
