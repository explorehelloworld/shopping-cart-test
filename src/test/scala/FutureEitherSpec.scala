// src/test/scala/FutureEitherSpec.scala
//package com.example.utils

import com.example.utils.FutureEither
import munit.FunSuite

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

final class FutureEitherSpec extends FunSuite {

  implicit val ec: ExecutionContext = ExecutionContext.global

  // Small helpers
  private def feRight[L, R](r: R): FutureEither[L, R] =
    FutureEither(Future.successful(Right(r)))

  private def feLeft[L, R](l: L): FutureEither[L, R] =
    FutureEither(Future.successful(Left(l)))

  test("map on Right transforms the value") {
    val fe = feRight[String, Int](2).map(_ + 3)
    fe.future.map(res => assertEquals(res, Right(5)))
  }

  test("map on Left keeps the Left as is") {
    val fe = feLeft[String, Int]("err").map(_ + 1)
    fe.future.map(res => assertEquals(res, Left("err")))
  }

  test("flatMap should chain on Right") {
    val fe = feRight[String, Int](2).flatMap(v => feRight[String, Int](v * 4))
    fe.future.map(res => assertEquals(res, Right(8)))
  }

  test("flatMap should short-circuit on Left and not evaluate the function") {
    var called = false
    val fe = feLeft[String, Int]("boom").flatMap { v =>
      called = true
      feRight[String, Int](v + 1)
    }
    fe.future.map { res =>
      assertEquals(res, Left("boom"))
      assertEquals(called, false)
    }
  }

  test("flatMap propagates Left returned by the function") {
    val fe = feRight[String, Int](10).flatMap(_ => feLeft[String, Int]("nope"))
    fe.future.map(res => assertEquals(res, Left("nope")))
  }

  test("for-comprehension combines two Right values") {
    val combined =
      for {
        a <- feRight[String, Int](3)
        b <- feRight[String, Int](7)
      } yield a + b

    combined.future.map(res => assertEquals(res, Right(10)))
  }

  test("for-comprehension yields Left when one step is Left") {
    val combined =
      for {
        a <- feRight[String, Int](3)
        b <- feLeft[String, Int]("failed")
      } yield a + b

    combined.future.map(res => assertEquals(res, Left("failed")))
  }

  test("FutureEither with a failed Future fails the whole computation") {
    val ex = new RuntimeException("boom")
    val failing = FutureEither[String, Int](Future.failed(ex))
    failing.future.transform {
      case Failure(e) =>
        assertEquals(e.getMessage, "boom")
        Success(assert(cond = true))
      case Success(_) =>
        Success(fail("Expected the Future to fail"))
    }
  }
}