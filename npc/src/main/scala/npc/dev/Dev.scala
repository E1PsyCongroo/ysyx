package rvcpu.dev

import chisel3._
import chisel3.util._

case class Area(start: UInt, end: UInt) {
  def in(addr: UInt): Bool = {
    addr >= start && addr <= end
  }
}

object Dev {
  val memoryAddr = Area("h80000000".U, "h87ffffff".U)
  val uartAddr   = Area("ha00003f8".U, "ha00003f8".U)
  val mtimeAddr  = Area("ha0000048".U, "ha000004d".U)

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
