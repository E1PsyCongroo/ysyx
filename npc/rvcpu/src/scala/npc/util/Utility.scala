package rvcpu.utility

import rvcpu._
import rvcpu.core._
import chisel3._
import chisel3.util._

object LSFR {
  def width = 8.W
  def apply(next: Bool): UInt = {
    val reg    = RegInit(1.U(width))
    val newBit = reg(4) ^ reg(3) ^ reg(2) ^ reg(0)
    reg := Mux(next, Mux(reg === 0.U, 1.U, newBit ## reg(7, 1)), reg)
    reg
  }
}
