// package rvcpu

// import rvcpu.core._
// import rvcpu.axi4._
// import rvcpu.dev._
// import rvcpu.fpga._
// import rvcpu.utility._
// import chisel3._
// import chisel3.util._

// class FPGA_RVCPUIO(awidth: Int, xlen: Int) extends Bundle {
//   val interrupt = Input(Bool())
//   val master    = new AXI4MasterIO
// }

// class FPGA_RVCPU(awidth: Int, xlen: Int, extentionE: Boolean, PCReset: BigInt, sim: Boolean) extends Module {
//   val io = IO(new FPGA_RVCPUIO(awidth, xlen))

//   val needCache: UInt => Bool = Dev.fpga.BRAMAddr.in
//   val IFU     = Module(new IFU(xlen, PCReset))
//   val ICache  = Module(new ICache(awidth, xlen, 6, 5, 0, needCache, sim))
//   val IDU     = Module(new IDU(xlen, extentionE, sim))
//   val EXU     = Module(new EXU(xlen, extentionE, sim))
//   val LSU     = Module(new LSU(xlen, extentionE, sim))
//   val WBU     = Module(new WBU(xlen, extentionE, sim))
//   val RegFile = Module(new RegFile(xlen, if (extentionE) 4 else 5))
//   val CLINT   = Module(new CLINT(awidth, xlen))

//   StageConnect(IFU.io.out, ICache.io.in, ICache.io.out, Some(EXU.io.jump))
//   StageConnect(ICache.io.out, IDU.io.in, IDU.io.out, Some(EXU.io.jump))
//   StageConnect(IDU.io.out, EXU.io.in, EXU.io.out)
//   StageConnect(EXU.io.out, LSU.io.in, LSU.io.out)
//   StageConnect(LSU.io.out, WBU.io.in, WBU.io.out)

//   IFU.io.jump   := EXU.io.jump || IDU.io.fence_i
//   IFU.io.nextPC := Mux(EXU.io.jump, EXU.io.nextPC, ICache.io.in.bits.pc)

//   ICache.io.clear := IDU.io.fence_i

//   RegFile.io.in.ra1 := IDU.io.RegFileAccess.ra1
//   RegFile.io.in.ra2 := IDU.io.RegFileAccess.ra2
//   def conflict(ra: UInt, old: RegFileAccess) = old.we && old.wa =/= 0.U && ra === old.wa
//   val isEXURa1RAW    = conflict(IDU.io.RegFileAccess.ra1, EXU.io.RegFileAccess)
//   val isEXURa2RAW    = conflict(IDU.io.RegFileAccess.ra2, EXU.io.RegFileAccess)
//   val isEXUForward   = !EXU.io.in.bits.control.wbSrc(0) && EXU.io.out.valid
//   val EXUForwardData = EXU.io.out.bits.alu_csr_Out
//   val isLSURa1RAW    = conflict(IDU.io.RegFileAccess.ra1, LSU.io.RegFileAccess)
//   val isLSURa2RAW    = conflict(IDU.io.RegFileAccess.ra2, LSU.io.RegFileAccess)
//   val isLSUForward   = Mux(LSU.io.in.bits.control.wbSrc.asBool, LSU.io.out.valid, LSU.io.in.valid)
//   val LSUForwardData = LSU.io.out.bits.wd
//   val isWBURa1RAW    = conflict(IDU.io.RegFileAccess.ra1, WBU.io.RegFileAccess)
//   val isWBURa2RAW    = conflict(IDU.io.RegFileAccess.ra2, WBU.io.RegFileAccess)
//   val isWBUForward   = WBU.io.in.valid
//   val WBUForwardData = WBU.io.RegFileAccess.wd
//   IDU.io.RegFileReturn.rd1 := MuxCase(
//     RegFile.io.out.rd1,
//     Seq(
//       (isEXURa1RAW && isEXUForward) -> EXUForwardData,
//       (isLSURa1RAW && isLSUForward) -> LSUForwardData,
//       (isWBURa1RAW && isWBUForward) -> WBUForwardData
//     )
//   )
//   IDU.io.RegFileReturn.rd2 := MuxCase(
//     RegFile.io.out.rd2,
//     Seq(
//       (isEXURa2RAW && isEXUForward) -> EXUForwardData,
//       (isLSURa2RAW && isLSUForward) -> LSUForwardData,
//       (isWBURa2RAW && isWBUForward) -> WBUForwardData
//     )
//   )
//   IDU.io.stall := ((isEXURa1RAW || isEXURa2RAW) && !isEXUForward) || ((isLSURa1RAW || isLSURa2RAW) && !isLSUForward) || ((isWBURa1RAW || isWBURa2RAW) && !isWBUForward)

//   WBU.io.out.ready := true.B
//   RegFile.io.in.wa := WBU.io.RegFileAccess.wa
//   RegFile.io.in.we := WBU.io.RegFileAccess.we
//   RegFile.io.in.wd := WBU.io.RegFileAccess.wd

//   val OutDevAddr = Area(Dev.fpga.BRAMAddr.start, Dev.all.end)
//   AXI4Interconnect(
//     Seq(LSU.io.master, ICache.io.master),
//     Seq(CLINT.io, io.master),
//     Seq(Dev.fpga.CLINTAddr, OutDevAddr),
//     Seq(true, false)
//   )
//   if (sim) {
//     val cycle = RegInit(0.U(64.W))
//     cycle                  := cycle + 1.U
//     ICache.io.curCycle.get := cycle

//     when(WBU.io.out.fire && WBU.io.out.bits.isEnd.get) {
//       printf("SIM END!")
//     }
//   }
// }
