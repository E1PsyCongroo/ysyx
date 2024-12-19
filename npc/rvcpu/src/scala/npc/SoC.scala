package rvcpu

import rvcpu.core._
import rvcpu.axi4._
import rvcpu.dev._
import rvcpu.utility._
import chisel3._
import chisel3.util._
import chisel3.experimental.dataview._

object SoCConfig {
  val axiParams = AXI4BundleParameters(
    addrBits          = 32,
    dataBits          = 32,
    idBits            = 4,
    regionPresent     = false,
    wlastPresent      = true,
    rlastPresent      = true,
    sizePresent       = true,
    lenPrenset        = true,
    burstPresent      = true,
    wstrbPresent      = true,
    brespPresent      = true,
    rrespPresent      = true,
    cachePrenset      = false,
    protPrensent      = false,
    qosPrensent       = false,
    exclusiveAccesses = false
  )
  val cpuConfig = CPUConfig(
    frequency  = BigInt("50000000"),
    xlen       = 32,
    extentionE = true,
    PCReset    = BigInt("30000000", 16),
    icacheConfig = ICacheConfig(
      totalWidth         = 6,
      blockWidth         = 5,
      associativityWidth = 0,
      icacheNeed = Seq(
        Dev.ysyxsoc.MROMAddr,
        Dev.ysyxsoc.FlashAddr,
        Dev.ysyxsoc.ChipLinkMEMAddr,
        Dev.ysyxsoc.PSRAMAddr,
        Dev.ysyxsoc.SDRAMAddr
      )
    ),
    sim = true
  )
}

class SoCCoreIO extends Bundle {
  val interrupt = Input(Bool())
  val master    = StandardAXI4Bundle(SoCConfig.axiParams)
  val slave     = Flipped(StandardAXI4Bundle(SoCConfig.axiParams))
}

class SoCCore(useDPIC: Boolean) extends Module {
  val io    = IO((new SoCCoreIO))
  val core  = Module(new RVCPU(SoCConfig.cpuConfig, SoCConfig.axiParams))
  val clint = Module(new CLINT(SoCConfig.cpuConfig, SoCConfig.axiParams))
  AXI4Interconnect(
    SoCConfig.axiParams,
    Seq(core.io.masters.lsu, core.io.masters.icache),
    Seq(clint.io, io.master.viewAs[AXI4Bundle]),
    Seq(
      Seq(Dev.ysyxsoc.CLINTAddr),
      Seq(Dev.ALL)
    ),
    Seq(true, true)
  )

  core.io.slave <> io.slave.viewAs[AXI4Bundle]
  core.io.interrupt := io.interrupt

  if (SoCConfig.cpuConfig.sim) {
    if (useDPIC) {
      val dpic = Module(
        new DPIC(
          SoCConfig.cpuConfig,
          DPICConfig(
            useDPIC = useDPIC,
            skipDiffArea = Seq(
              Dev.ysyxsoc.CLINTAddr,
              Dev.ysyxsoc.UART16550Addr,
              Dev.ysyxsoc.SPIMasterAddr,
              Dev.ysyxsoc.GPIOAddr,
              Dev.ysyxsoc.PS2Addr,
              Dev.ysyxsoc.VGAAddr,
              Dev.ysyxsoc.ChipLinkMEMAddr
            )
          )
        )
      )
      dpic.io <> core.io.state.get
    } else {
      core.io.state.get.ready := true.B
    }
  }
}
