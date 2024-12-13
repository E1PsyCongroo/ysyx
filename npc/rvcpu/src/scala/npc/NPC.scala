package rvcpu

import rvcpu.core._
import rvcpu.axi4._
import rvcpu.dev._
import rvcpu.utility._
import chisel3._
import chisel3.util._

object NPCConfig {
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
    frequency = BigInt("50000000"),
    xlen       = 32,
    extentionE = true,
    PCReset    = BigInt("80000000", 16),
    icacheConfig = ICacheConfig(
      totalWidth         = 6,
      blockWidth         = 5,
      associativityWidth = 0,
      icacheNeed         = Dev.npc.PMEMAddr
    ),
    sim = true
  )
}

class NPC(useDPIC: Boolean) extends Module {
  val io =
    if (NPCConfig.cpuConfig.sim) Some(IO(new Bundle {
      val sync = Output(Bool())
      val simEnd = Output(Bool())
    }))
    else None
  val npc   = Module(new RVCPU(NPCConfig.cpuConfig, NPCConfig.axiParams))
  val clint = Module(new CLINT(NPCConfig.cpuConfig, NPCConfig.axiParams))
  val uart  = Module(new Uart(NPCConfig.axiParams))
  val mem   = Module(new AXI4Mem(NPCConfig.axiParams, useDPIC))

  AXI4Interconnect(
    NPCConfig.axiParams,
    Seq(npc.io.masters.lsu, npc.io.masters.icache),
    Seq(clint.io, uart.io, mem.io),
    Seq(
      Seq(Dev.npc.CLINTAddr),
      Seq(Dev.npc.UARTAddr),
      Seq(
        Dev.npc.PMEMAddr,
        Dev.npc.KDBAddr,
        Dev.npc.VGACTLAddr,
        Dev.npc.VGABUFAddr,
        Dev.npc.AUDIOCTLAddr,
        Dev.npc.AUDIOBUFAddr
      )
    ),
    Seq(true, true, true)
  )

  npc.io.slave     := DontCare
  npc.io.interrupt := false.B

  if (NPCConfig.cpuConfig.sim) {
    io.get.sync := npc.io.state.get.fire
    io.get.simEnd := npc.io.state.get.bits.isEnd
    if (useDPIC) {
      val dpic = Module(
        new DPIC(
          NPCConfig.cpuConfig,
          DPICConfig(
            useDPIC      = useDPIC,
            skipDiffArea = Seq(Dev.npc.CLINTAddr, Dev.npc.UARTAddr)
          )
        )
      )
      dpic.io <> npc.io.state.get
    } else {
      npc.io.state.get.ready := true.B
    }
  }
}
