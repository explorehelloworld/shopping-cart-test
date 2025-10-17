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

    val program: ZIO[Client, Nothing, List[Unit]] =
      ZIO.foreach(urls) { url =>
        fetchJsonZio(url)
          .either
          .flatMap {
            case Left(t) =>
              ZIO.succeed(println(s"$url -> error: ${Option(t.getMessage).getOrElse(t.toString)}"))
            case Right(jsonStr) =>
              parseProduct(jsonStr) match {
                case Left(parseErr) =>
                  ZIO.succeed(println(s"$url -> error: $parseErr"))
                case Right(product) =>
                  val withTax = computeTax(product).unsafeRunSync()
                  val priceStr = product.price.setScale(2, scala.math.BigDecimal.RoundingMode.HALF_UP).toString
                  val taxStr   = withTax.tax.setScale(2, scala.math.BigDecimal.RoundingMode.HALF_UP).toString
                  val totalStr = withTax.total.setScale(2, scala.math.BigDecimal.RoundingMode.HALF_UP).toString
                  ZIO.succeed(println(s"$url -> price=$priceStr, tax=$taxStr, total=$totalStr"))
              }
          }
      }

    Unsafe.unsafe { implicit u =>
      Runtime.default.unsafe
        .run(program.provide(Client.default))
        .getOrThrowFiberFailure()
    }
  }
}