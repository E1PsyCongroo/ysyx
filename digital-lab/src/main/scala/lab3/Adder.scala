package scala.lab3

import circt.stage.ChiselStage
import chisel3._
import chisel3.util._

class Adder(width: Int) extends Module {
  val io = IO(new Bundle {
    val ina = Input(SInt(width.W))
    val inb = Input(SInt(width.W))
    val addSub = Input(Bool())
    val carry = Output(Bool())
    val zero = Output(Bool())
    val overflow = Output(Bool())
    val result = Output(SInt(width.W))
  })
  val tA = io.ina.asUInt
  val tB = io.inb.asUInt
  val tnoCin = (Fill(width, io.addSub.asUInt) ^ tB)
  val result = Wire(UInt((width+1).W))
  result := tA +& tnoCin +& io.addSub.asUInt
  io.result := result(width-1, 0).asSInt
  io.carry := result(width).asBool
  io.zero := ~(io.result.asUInt.orR)
  io.overflow := (tA(width-1) === tnoCin(width-1)) && (result(width-1) =/= tA(width-1))
}