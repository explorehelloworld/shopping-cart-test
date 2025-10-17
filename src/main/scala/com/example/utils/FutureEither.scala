
package com.example.utils

import scala.concurrent.{ExecutionContext, Future}

case class FutureEither[L, R](future: Future[Either[L, R]]) {
  def map[B](f: R => B)(implicit ec: ExecutionContext): FutureEither[L, B] =
    FutureEither(future.map(_.map(f)))

  def flatMap[B](f: R => FutureEither[L, B])(implicit ec: ExecutionContext): FutureEither[L, B] =
    FutureEither(future.flatMap {
      case Left(l) => Future.successful(Left(l))
      case Right(r) => f(r).future
    })
}