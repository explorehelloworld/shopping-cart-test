package com.example

import cats.effect.IO
import io.circe.generic.auto._
import io.circe.parser.decode
import zio._
import zio.http.{Client, Request, URL}
import scala.concurrent.ExecutionContext

object ProductTaxCalculator {

  final case class Product(name: String, price: BigDecimal)
  final case class ProductWithTax(name: String, price: BigDecimal, tax: BigDecimal, total: BigDecimal)

  //fetch from set of urls given in the readme.md file as part of assignemnt
  def fetchJsonZio(urlStr: String): zio.ZIO[zio.http.Client, Throwable, String] = {
    zio.ZIO.scoped {
      for {
        url  <- zio.ZIO.fromEither(zio.http.URL.decode(urlStr))
          .mapError(e => new IllegalArgumentException(s"Invalid URL: $urlStr ($e)"))
        res  <- zio.http.Client.request(zio.http.Request.get(url))
        _    <- if (res.status.isSuccess) zio.ZIO.unit
        else zio.ZIO.fail(new RuntimeException(s"couldnt call given url from HTTP module: ${res.status.code} for $urlStr"))
        body <- res.body.asString
      } yield body
    }
  }

  def computeTax(product: Product): IO[ProductWithTax] = IO {
    val tax = (product.price * BigDecimal("0.125")).setScale(2, BigDecimal.RoundingMode.UP)
    val total = product.price + tax // 0.125 can be read from config. we will config it later
    ProductWithTax(product.name, product.price, tax, total)
  }

  def parseProduct(jsonStr: String): Either[String, Product] = {
    decode[Product](jsonStr) match {
      case Right(p) => Right(p)
      case Left(e)  => Left(s"Invalid JSON: ${Option(e.getMessage).getOrElse(e.toString)}")
    }
  }

  def fetchJsonIO(url: String)(implicit ec: ExecutionContext): IO[String] = {
    val future = Unsafe.unsafe { implicit u =>
      Runtime.default.unsafe.runToFuture(
        fetchJsonZio(url).provide(Client.default)
      )
    }
    IO.fromFuture(IO.pure(future))
  }

  def fetchAndParse(url: String)(implicit ec: ExecutionContext): IO[Either[String, Product]] = {
    fetchJsonIO(url).attempt.map {
      case Right(jsonStr) => parseProduct(jsonStr) match {
        case Left(value) => Left(s"Malformed JSON: $value")
        case Right(value) => Right(Product(value.name, value.price))
      }
      case Left(err)      => Left(s"Fetch failed for $url: ${err.getMessage}")
    }
  }
}