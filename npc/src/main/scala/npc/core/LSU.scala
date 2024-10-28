package rvcpu.core

import rvcpu.axi4._
import rvcpu.utility._
import rvcpu.dev._
import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._

import MemOp._

case class MemControlPattern(
  val memOp: BitPat)
    extends DecodePattern {
  require(memOp.getWidth == MemOp.getWidth)
  def bitPat: BitPat = memOp
}

object WmaskFeild extends DecodeField[MemControlPattern, UInt] {
  def name = "Wmask Feild"
  def chiselType: UInt = UInt(4.W)
  def genTable(op: MemControlPattern): BitPat = {
    op.bitPat(1, 0).rawString match {
      case "10" => BitPat("b1111")
      case "01" => BitPat("b0011")
      case "00" => BitPat("b0001")
    }
  }
}

object SizeFeild extends DecodeField[MemControlPattern, UInt] {
  def name = "Size Feild"
  def chiselType: UInt = UInt(3.W)
  def genTable(op: MemControlPattern): BitPat = {
    op.bitPat(1, 0).rawString match {
      case "10" => BitPat("b010")
      case "01" => BitPat("b001")
      case "00" => BitPat("b000")
    }
  }
}

class LSUOut(xlen: Int = 32) extends Bundle {
  val bresp = Output(UInt(2.W))
  val rdata = Output(UInt(xlen.W))
}

class LSUIO(xlen: Int = 32) extends Bundle {
  val in     = Flipped(DecoupledIO(new LSUIn(xlen)))
  val out    = DecoupledIO(new LSUOut(xlen))
  val master = new AXI4MasterIO
}

class LSU(xlen: Int = 32) extends Module {
  val io = IO(new LSUIO(xlen))

  val in    = RegEnable(io.in.bits, io.in.fire)
  val wmask = WireDefault(0.U(4.W))

  val possiblePatterns = Seq(
    MemControlPattern(memW),
    MemControlPattern(memH),
    MemControlPattern(memB),
    MemControlPattern(memHu),
    MemControlPattern(memBu)
  )
  val decodeTable = new DecodeTable(possiblePatterns, Seq(WmaskFeild, SizeFeild))
  val mask        = decodeTable.decode(in.memOp)(WmaskFeild)
  wmask := mask << in.waddr(1, 0)
  val size = decodeTable.decode(in.memOp)(SizeFeild)

  val raddr   = in.raddr
  val loffset = WireDefault(raddr(1, 0) << 3.U)
  val lshift  = io.master.rdata >> loffset

  val rdata = MuxCase(
    lshift,
    Seq(
      memH -> Fill(16, lshift(15)) ## lshift(15, 0),
      memB -> Fill(24, lshift(7)) ## lshift(7, 0),
      memHu -> Fill(16, 0.U(1.W)) ## lshift(15, 0),
      memBu -> Fill(24, 0.U(1.W)) ## lshift(7, 0)
    ).map { case (key, data) => (in.memOp === key, data) }
  )

  val awfire = io.master.awvalid && io.master.awready
  val wfire  = io.master.wvalid && io.master.wready
  val bfire  = io.master.bvalid && io.master.bready
  val arfire = io.master.arvalid && io.master.arready
  val rfire  = io.master.rvalid && io.master.rready

  val sIdle :: sSetWrite :: sSetWaddr :: sSetWdata :: sWaitBresp :: sSetRaddr :: sWaitRead :: sSendOut :: Nil = Enum(8)
  val state                                                                                                   = RegInit(sIdle)
  state := MuxLookup(state, sIdle)(
    Seq(
      sIdle -> MuxCase(
        sIdle,
        Seq(
          (io.in.fire && io.in.bits.wen) -> sSetWrite,
          (io.in.fire && io.in.bits.ren) -> sSetRaddr
        )
      ),
      sSetWrite -> MuxCase(
        sSetWrite,
        Seq(
          (awfire && wfire) -> sWaitBresp,
          awfire -> sSetWdata,
          wfire -> sSetWaddr
        )
      ),
      sSetWaddr -> Mux(awfire, sWaitBresp, sSetWaddr),
      sSetWdata -> Mux(wfire, sWaitBresp, sSetWdata),
      sWaitBresp -> MuxCase(
        sWaitBresp,
        Seq(
          (bfire && io.out.fire) -> sIdle,
          bfire -> sSendOut
        )
      ),
      sSetRaddr -> Mux(arfire, sWaitRead, sSetRaddr),
      sWaitRead -> MuxCase(
        sWaitRead,
        Seq(
          (rfire && io.out.fire) -> sIdle,
          rfire -> sSendOut
        )
      ),
      sSendOut -> Mux(io.out.fire, sIdle, sSendOut)
    )
  )

  val isIdle      = state === sIdle
  val isSetWrite  = state === sSetWrite
  val isSetWaddr  = state === sSetWaddr
  val isSetWdata  = state === sSetWdata
  val isWaitBresp = state === sWaitBresp
  val isSetRaddr  = state === sSetRaddr
  val isWaitRead  = state === sWaitRead
  val isSendOut   = state === sSendOut

  /* For tracer */
  // import rvcpu.dev.Dev
  // val devs = Seq(Dev.CLINTAddr, Dev.UART16550Addr, Dev.SPIMasterAddr, Dev.GPIOAddr, Dev.PS2Addr, Dev.VGAAddr, Dev.ChipLinkMEMAddr)
  // SkipDifftest(
  //   clock,
  //   io.out.fire &&
  //     (devs.map(dev => dev.in(in.waddr) || dev.in(in.raddr)).foldLeft(false.B)(_ || _))
  // );
  val SramTracer = Module(new SramTracer)
  SramTracer.io.raddr := in.raddr
  SramTracer.io.rdata := io.out.bits.rdata
  SramTracer.io.ren   := io.out.fire && in.ren && Dev.SDRAMAddr.in(in.raddr)
  SramTracer.io.waddr := in.waddr
  SramTracer.io.wdata := in.wdata
  SramTracer.io.wlen := MuxCase(
    4.U(32.W),
    Seq(
      memB -> 1.U(32.W),
      memH -> 2.U(32.W),
      memW -> 4.U(32.W),
      memBu -> 1.U(32.W),
      memHu -> 2.U(32.W)
    ).map { case (key, data) => (key === in.memOp, data) }
  )
  SramTracer.io.wen := io.out.fire && in.wen && Dev.SDRAMAddr.in(in.waddr)

  assert((io.master.araddr & ~((1.U(32.W) << size) >> 1.U)) === io.master.araddr, "%x %x", io.master.araddr, size)
  assert((io.master.awaddr & ~((1.U(32.W) << size) >> 1.U)) === io.master.awaddr, "%x %x", io.master.awaddr, size)
  assert(!bfire || io.master.bresp === "b00".U(2.W))
  assert(!rfire || io.master.rresp === "b00".U(2.W))
  /* IO bind */
  io.in.ready := isIdle

  io.out.valid      := isSendOut
  io.out.bits.bresp := RegEnable(io.master.bresp, bfire)
  io.out.bits.rdata := RegEnable(rdata, rfire)

  io.master.awvalid := isSetWrite || isSetWaddr
  io.master.awaddr  := in.waddr
  io.master.awid    := 0.U
  io.master.awlen   := 0.U
  io.master.awsize  := size
  io.master.awburst := "b01".U

  io.master.wvalid := isSetWrite || isSetWdata
  io.master.wdata  := in.wdata << (in.waddr(1, 0) << 3.U)
  io.master.wstrb  := wmask
  io.master.wlast  := isSetWrite || isSetWdata

  io.master.bready := isWaitBresp

  io.master.arvalid := isSetRaddr
  io.master.araddr  := in.raddr
  io.master.arid    := 0.U
  io.master.arlen   := 0.U
  io.master.arsize  := size
  io.master.arburst := "b01".U

  io.master.rready := isWaitRead
}
