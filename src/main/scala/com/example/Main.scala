package com.example

import cats.effect.unsafe.implicits.global
import com.example.ProductTaxCalculator._
import zio.{Runtime, Unsafe, ZIO}
import zio.http.Client

object Main {
  def main(args: Array[String]): Unit = {
    val urls: List[String] =
      List(
        "https://raw.githubusercontent.com/mattjanks16/shopping-cart-test-data/main/cornflakes.json"
      )

    val program: ZIO[Client, Nothing, Unit] = ???

    Unsafe.unsafe { implicit u =>
      Runtime.default.unsafe
        .run(program.provide(Client.default))
        .getOrThrowFiberFailure()
    }
  }
}