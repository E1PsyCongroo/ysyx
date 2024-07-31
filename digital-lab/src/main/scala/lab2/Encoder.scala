package scala.lab2.Encoder

import circt.stage.ChiselStage
import chisel3._
import chisel3.util._

class Encoder(inWidth: Int) extends Module {
  require(inWidth > 0)
  val outWidth = log2Ceil(inWidth)
  val io = IO(new Bundle {
    val in  = Input(UInt(inWidth.W))
    val out = Output(UInt(outWidth.W))
  })
  val v = Wire(Vec(inWidth, UInt(outWidth.W)))
  v(0) := 0.U
  for (i <- 1 until inWidth) {
    v(i) := Mux(io.in(i), i.U, 0.U) | v(i - 1)
  }
  io.out := v(inWidth - 1)
}

object Encoder {
  def apply(inWidth: Int, in: UInt) = {
    val encoder = Module(new Encoder(inWidth))
    encoder.io.in := in
    encoder.io.out
  }
}

class PriorityArbiter(width: Int) extends Module {
  val io = IO(new Bundle {
    val in  = Input(UInt(width.W))
    val out = Output(UInt(width.W))
  })
  val grant      = VecInit.fill(width)(false.B)
  val notGranted = VecInit.fill(width)(false.B)
  grant(width - 1)      := io.in(width - 1).asBool
  notGranted(width - 1) := !grant(width - 1)
  for (i <- width - 1 to 1 by -1) {
    grant(i - 1)      := io.in(i - 1).asBool && notGranted(i)
    notGranted(i - 1) := !grant(i - 1) && notGranted(i)
  }
  io.out := grant.asUInt
}

object PriorityArbiter {
  def apply(width: Int, in: UInt) = {
    val arbiter = Module(new PriorityArbiter(width))
    arbiter.io.in := in
    arbiter.io.out
  }
}

class PriorityEncoder(inWidth: Int) extends Module {
  val outWidth = log2Ceil(inWidth)
  val io = IO(new Bundle {
    val in  = Input(UInt(inWidth.W))
    val out = Output(UInt(outWidth.W))
  })
  io.out := Encoder(inWidth, PriorityArbiter(inWidth, io.in))
}

object PriorityEncoder {
  def apply(inWidth: Int, in: UInt) = {
    val priorityEncoder = Module(new PriorityEncoder(inWidth))
    priorityEncoder.io.in := in
    priorityEncoder.io.out
  }
}
