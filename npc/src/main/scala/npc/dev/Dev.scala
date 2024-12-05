package rvcpu.dev

import chisel3._
import chisel3.util._

case class Area(start: UInt, end: UInt) {
  def in(addr: UInt): Bool = {
    addr >= start && addr <= end
  }
}

object Dev {

  val all = Area("h00000000".U, "hffffffff".U)

  object npc {
    val PMEMAddr     = Area("h80000000".U, "h87ffffff".U)
    val UARTAddr     = Area("ha00003f8".U, "ha00003f8".U)
    val CLINTAddr    = Area("ha0000048".U, "ha000004d".U)
    val KDBAddr      = Area("ha0000060".U, "ha0000063".U)
    val VGACTLAddr   = Area("ha0000100".U, "ha0000107".U)
    val VGABUFAddr   = Area("ha1000000".U, "ha11387ff".U)
    val AUDIOCTLAddr = Area("ha0000200".U, "ha0000217".U)
    val AUDIOBUFAddr = Area("ha2000000".U, "ha000ffff".U)
  }

  object ysyxsoc {
    val CLINTAddr        = Area("h02000000".U, "h0200ffff".U)
    val SRAMAddr         = Area("h0f000000".U, "h0fffffff".U)
    val UART16550Addr    = Area("h10000000".U, "h10000fff".U)
    val SPIMasterAddr    = Area("h10001000".U, "h10001fff".U)
    val GPIOAddr         = Area("h10002000".U, "h1000200f".U)
    val PS2Addr          = Area("h10011000".U, "h10011007".U)
    val MROMAddr         = Area("h20000000".U, "h20000fff".U)
    val VGAAddr          = Area("h21000000".U, "h211fffff".U)
    val FlashAddr        = Area("h30000000".U, "h3fffffff".U)
    val ChipLinkMMIOAddr = Area("h40000000".U, "h7fffffff".U)
    val PSRAMAddr        = Area("h80000000".U, "h9fffffff".U)
    val SDRAMAddr        = Area("ha0000000".U, "hbfffffff".U)
    val ChipLinkMEMAddr  = Area("hc0000000".U, "hffffffff".U)
  }

}
