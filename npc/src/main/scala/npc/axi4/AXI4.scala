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

  val state       = RegInit(sIdle)
  val isIdle      = state === sIdle
  val isWaitWaddr = state === sWaitWaddr
  val isWaitBresp = state === sWaitBresp
  val isWaitRresp = state === sWaitRresp

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

  val sWriteIdle :: sWaitBresp :: Nil = Enum(2)
  val sReadIdle :: sWaitRresp :: Nil  = Enum(2)

  val writeState  = RegInit(sWriteIdle)
  val isWriteIdle = writeState === sWriteIdle
  val isWaitBresp = writeState === sWaitBresp

  val readState   = RegInit(sReadIdle)
  val isReadIdle  = readState === sReadIdle
  val isWaitRresp = readState === sWaitRresp

  val writeRequests = VecInit(io.fanIn.map(in => in.awvalid)).asUInt
  val readRequests  = VecInit(io.fanIn.map(in => in.arvalid)).asUInt

  val writeSelected    = PriorityEncoder(writeRequests)
  val writeSelectedReg = RegEnable(writeSelected, isWriteIdle)
  val write            = writeRequests(writeSelected)

  val readSelected    = PriorityEncoder(readRequests)
  val readSelectedReg = RegEnable(readSelected, isReadIdle)
  val read            = readRequests(readSelected)

  val awfire = io.fanIn(writeSelectedReg).awvalid && io.fanIn(writeSelectedReg).awready
  val wfire  = io.fanIn(writeSelectedReg).wvalid && io.fanIn(writeSelectedReg).wready
  val bfire  = io.fanIn(writeSelectedReg).bvalid && io.fanIn(writeSelectedReg).bready
  val arfire = io.fanIn(readSelectedReg).arvalid && io.fanIn(readSelectedReg).arready
  val rfire  = io.fanIn(readSelectedReg).rvalid && io.fanIn(readSelectedReg).rready
  val rlast  = io.fanIn(readSelectedReg).rlast

  writeState := MuxLookup(writeState, sWriteIdle)(
    Seq(
      sWriteIdle -> Mux(write, sWaitBresp, sWriteIdle),
      sWaitBresp -> Mux(bfire, sWriteIdle, sWaitBresp)
    )
  )

  readState := MuxLookup(readState, sReadIdle)(
    Seq(
      sReadIdle -> Mux(read, sWaitRresp, sReadIdle),
      sWaitRresp -> Mux(rfire && rlast, sReadIdle, sWaitRresp)
    )
  )

  val awaddr         = io.fanIn(writeSelected).awaddr
  val wmatches       = VecInit(fanOutSeq.map(_(awaddr))).asUInt
  val writeOutSelect = RegEnable(PriorityEncoder(wmatches), isWriteIdle)

  val araddr        = io.fanIn(readSelected).araddr
  val rmatches      = VecInit(fanOutSeq.map(_(araddr))).asUInt
  val readOutSelect = RegEnable(PriorityEncoder(rmatches), isReadIdle)

  for (i <- 0 until fanInNum) {
    io.fanIn(i) <> AXI4.none
  }
  for (i <- 0 until fanOutSeq.size) {
    AXI4.none <> io.fanOut(i)
  }

  when(isWaitBresp) {
    io.fanIn(writeSelectedReg).awready := io.fanOut(writeOutSelect).awready
    io.fanOut(writeOutSelect).awvalid  := io.fanIn(writeSelectedReg).awvalid
    io.fanOut(writeOutSelect).awaddr   := io.fanIn(writeSelectedReg).awaddr
    io.fanOut(writeOutSelect).awid     := io.fanIn(writeSelectedReg).awid
    io.fanOut(writeOutSelect).awlen    := io.fanIn(writeSelectedReg).awlen
    io.fanOut(writeOutSelect).awsize   := io.fanIn(writeSelectedReg).awsize
    io.fanOut(writeOutSelect).awburst  := io.fanIn(writeSelectedReg).awburst
    io.fanIn(writeSelectedReg).wready  := io.fanOut(writeOutSelect).wready
    io.fanOut(writeOutSelect).wvalid   := io.fanIn(writeSelectedReg).wvalid
    io.fanOut(writeOutSelect).wdata    := io.fanIn(writeSelectedReg).wdata
    io.fanOut(writeOutSelect).wstrb    := io.fanIn(writeSelectedReg).wstrb
    io.fanOut(writeOutSelect).wlast    := io.fanIn(writeSelectedReg).wlast
    io.fanOut(writeOutSelect).bready   := io.fanIn(writeSelectedReg).bready
    io.fanIn(writeSelectedReg).bvalid  := io.fanOut(writeOutSelect).bvalid
    io.fanIn(writeSelectedReg).bresp   := io.fanOut(writeOutSelect).bresp
    io.fanIn(writeSelectedReg).bid     := io.fanOut(writeOutSelect).bid
  }

  when(isWaitRresp) {
    io.fanIn(readSelectedReg).arready := io.fanOut(readOutSelect).arready
    io.fanOut(readOutSelect).arvalid  := io.fanIn(readSelectedReg).arvalid
    io.fanOut(readOutSelect).araddr   := io.fanIn(readSelectedReg).araddr
    io.fanOut(readOutSelect).arid     := io.fanIn(readSelectedReg).arid
    io.fanOut(readOutSelect).arlen    := io.fanIn(readSelectedReg).arlen
    io.fanOut(readOutSelect).arsize   := io.fanIn(readSelectedReg).arsize
    io.fanOut(readOutSelect).arburst  := io.fanIn(readSelectedReg).arburst
    io.fanOut(readOutSelect).rready   := io.fanIn(readSelectedReg).rready
    io.fanIn(readSelectedReg).rvalid  := io.fanOut(readOutSelect).rvalid
    io.fanIn(readSelectedReg).rresp   := io.fanOut(readOutSelect).rresp
    io.fanIn(readSelectedReg).rdata   := io.fanOut(readOutSelect).rdata
    io.fanIn(readSelectedReg).rlast   := io.fanOut(readOutSelect).rlast
    io.fanIn(readSelectedReg).rid     := io.fanOut(readOutSelect).rid
  }
}

// object AXI4Interconnect {
//   def apply(
//     fanIn:      Seq[AXI4MasterIO],
//     fanOut:     Seq[AXI4MasterIO],
//     fanOutArea: Seq[UInt => Bool]
//   ) = {
//     val AXI4Interconnect = Module(new AXI4Interconnect(fanIn.length, fanOutArea))
//     for (i <- 0 until fanIn.length) {
//       AXI4Interconnect.io.fanIn(i) <> fanIn(i)
//     }
//     for (i <- 0 until fanOut.length) {
//       AXI4Interconnect.io.fanOut(i) <> fanOut(i)
//     }
//   }
// }

object AXI4Interconnect {
  def apply(
    fanIn:       Seq[AXI4MasterIO],
    fanOut:      Seq[AXI4MasterIO],
    fanOutArea:  Seq[Area],
    fanOutTrans: Seq[Boolean]
  ) = {
    val fanInNum  = fanIn.length
    val fanOutNum = fanOut.length
    require(fanOutNum == fanOutArea.length)
    require(fanOutNum == fanOutTrans.length)

    val sWriteIdle :: sWaitBresp :: Nil = Enum(2)
    val sReadIdle :: sWaitRresp :: Nil  = Enum(2)

    val writeState  = RegInit(sWriteIdle)
    val isWriteIdle = writeState === sWriteIdle
    val isWaitBresp = writeState === sWaitBresp

    val readState   = RegInit(sReadIdle)
    val isReadIdle  = readState === sReadIdle
    val isWaitRresp = readState === sWaitRresp

    val writeRequests = VecInit(fanIn.map(in => in.awvalid)).asUInt
    val readRequests  = VecInit(fanIn.map(in => in.arvalid)).asUInt

    val writeSelected    = PriorityEncoder(writeRequests)
    val writeSelectedReg = RegEnable(writeSelected, isWriteIdle)
    val write            = writeRequests =/= 0.U

    val readSelected    = PriorityEncoder(readRequests)
    val readSelectedReg = RegEnable(readSelected, isReadIdle)
    val read            = readRequests =/= 0.U

    val awaddr = WireDefault(fanIn(0).awaddr)
    val araddr = WireDefault(fanIn(0).araddr)
    val awfire = WireDefault(fanIn(0).awvalid && fanIn(0).awready)
    val wfire  = WireDefault(fanIn(0).wvalid && fanIn(0).wready)
    val bfire  = WireDefault(fanIn(0).bvalid && fanIn(0).bready)
    val arfire = WireDefault(fanIn(0).arvalid && fanIn(0).arready)
    val rfire  = WireDefault(fanIn(0).rvalid && fanIn(0).rready)
    val rlast  = WireDefault(fanIn(0).rlast)

    for (i <- 0 until fanIn.length) {
      when(writeSelected === i.U) {
        awaddr := fanIn(i).awaddr
      }
      when(writeSelectedReg === i.U) {
        awfire := fanIn(i).awvalid && fanIn(i).awready
        wfire  := fanIn(i).wvalid && fanIn(i).wready
        bfire  := fanIn(i).bvalid && fanIn(i).bready
      }
      when(readSelected === i.U) {
        araddr := fanIn(i).araddr
      }
      when(readSelectedReg === i.U) {
        arfire := fanIn(i).arvalid && fanIn(i).arready
        rfire  := fanIn(i).rvalid && fanIn(i).rready
        rlast  := fanIn(i).rlast
      }
    }

    val wmatches       = VecInit(fanOutArea.map(_.in(awaddr))).asUInt
    val writeOutSelect = RegEnable(Mux1H((0 until fanOutNum).map { i => (wmatches(i), i.U) }.toSeq), isWriteIdle)

    val rmatches      = VecInit(fanOutArea.map(_.in(araddr))).asUInt
    val readOutSelect = RegEnable(Mux1H((0 until fanOutNum).map { i => (rmatches(i), i.U) }.toSeq), isReadIdle)

    writeState := MuxLookup(writeState, sWriteIdle)(
      Seq(
        sWriteIdle -> Mux(write, sWaitBresp, sWriteIdle),
        sWaitBresp -> Mux(bfire, sWriteIdle, sWaitBresp)
      )
    )

    readState := MuxLookup(readState, sReadIdle)(
      Seq(
        sReadIdle -> Mux(read, sWaitRresp, sReadIdle),
        sWaitRresp -> Mux(rfire && rlast, sReadIdle, sWaitRresp)
      )
    )

    for (i <- 0 until fanInNum) {
      fanIn(i) <> AXI4.none
    }
    for (i <- 0 until fanOutNum) {
      AXI4.none <> fanOut(i)
    }

    for (i <- 0 until fanInNum) {
      for (j <- 0 until fanOutNum) {
        when(writeSelectedReg === i.U && writeOutSelect === j.U && isWaitBresp) {
          fanIn(i).awready  := fanOut(j).awready
          fanOut(j).awvalid := fanIn(i).awvalid
          if (fanOutTrans(j)) {
            fanOut(j).awaddr  := fanIn(i).awaddr - fanOutArea(j).start
          } else {
            fanOut(j).awaddr := fanIn(i).awaddr
          }
          fanOut(j).awid    := fanIn(i).awid
          fanOut(j).awlen   := fanIn(i).awlen
          fanOut(j).awsize  := fanIn(i).awsize
          fanOut(j).awburst := fanIn(i).awburst
          fanIn(i).wready   := fanOut(j).wready
          fanOut(j).wvalid  := fanIn(i).wvalid
          fanOut(j).wdata   := fanIn(i).wdata
          fanOut(j).wstrb   := fanIn(i).wstrb
          fanOut(j).wlast   := fanIn(i).wlast
          fanOut(j).bready  := fanIn(i).bready
          fanIn(i).bvalid   := fanOut(j).bvalid
          fanIn(i).bresp    := fanOut(j).bresp
          fanIn(i).bid      := fanOut(j).bid
        }
        when(readSelectedReg === i.U && readOutSelect === j.U && isWaitRresp) {
          fanIn(i).arready  := fanOut(j).arready
          fanOut(j).arvalid := fanIn(i).arvalid
          if (fanOutTrans(j)) {
            fanOut(j).araddr := fanIn(i).araddr - fanOutArea(j).start
          } else {
            fanOut(j).araddr := fanIn(i).araddr
          }
          fanOut(j).arid    := fanIn(i).arid
          fanOut(j).arlen   := fanIn(i).arlen
          fanOut(j).arsize  := fanIn(i).arsize
          fanOut(j).arburst := fanIn(i).arburst
          fanOut(j).rready  := fanIn(i).rready
          fanIn(i).rvalid   := fanOut(j).rvalid
          fanIn(i).rresp    := fanOut(j).rresp
          fanIn(i).rdata    := fanOut(j).rdata
          fanIn(i).rlast    := fanOut(j).rlast
          fanIn(i).rid      := fanOut(j).rid
        }
      }
    }
  }
}
