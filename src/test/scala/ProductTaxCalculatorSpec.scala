//Sudheer Kumar - v1 of product tax
package com.example

import com.example.ProductTaxCalculator.{Product, computeTax, fetchAndParse, parseProduct}
import munit.CatsEffectSuite
import scala.concurrent.ExecutionContext

class ProductTaxCalculatorSpec extends CatsEffectSuite {

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  test("computeTax should calculate correct tax and total") {
    val product = Product("Test Product", BigDecimal("10.00"))
    computeTax(product).map { taxedProduct =>
      assertEquals(taxedProduct.name, "Test Product")
      assertEquals(taxedProduct.price, BigDecimal("10.00"))
      assertEquals(taxedProduct.tax, BigDecimal("1.25")) // 12.5% of 10.00 = 1.25
      assertEquals(taxedProduct.total, BigDecimal("11.25"))
    }
  }

  test("parseProduct should parse valid JSON") {
    val validJson = """{"title": "Cheerios", "price": 2.98}"""
    val result = parseProduct(validJson)
    assertEquals(result, Right(Product("Cheerios", BigDecimal("2.98"))))
  }

  test("parseProduct should handle malformed JSON") {
    val invalidJson = """{"title": "Cheerios"}""" // missing price
    val result = parseProduct(invalidJson)
    assert(result.isLeft)
    assert(result.left.getOrElse("").contains("Malformed JSON"))
  }

  test("parseProduct should handle completely invalid JSON") {
    val invalidJson = """not json at all"""
    val result = parseProduct(invalidJson)
    assert(result.isLeft)
    assert(result.left.getOrElse("").contains("Malformed JSON"))
  }

  test("parseProduct should handle empty JSON") {
    val emptyJson = ""
    val result = parseProduct(emptyJson)
    assert(result.isLeft)
  }

  test("fetchAndParse should handle invalid URL") {
    val invalidUrl = "not-a-url"
    fetchAndParse(invalidUrl).map { result =>
      assert(result.isLeft)
      assert(result.left.getOrElse("").contains("Fetch failed"))
    }
  }

  test("fetchAndParse should handle unreachable URL") {
    val unreachableUrl = "https://this-domain-does-not-exist-12345.com/test.json"
    fetchAndParse(unreachableUrl).map { result =>
      assert(result.isLeft)
      assert(result.left.getOrElse("").contains("Fetch failed"))
    }
  }

  test("multiple products tax calculation matches sample") {
    val cornflakes = Product("cornflakes", BigDecimal("2.52"))
    val weetabix = Product("weetabix", BigDecimal("9.98"))

    for {
      taxedCornflakes <- computeTax(cornflakes)
      taxedWeetabix <- computeTax(weetabix)
      subtotal = (BigDecimal("2.52") * 2) + BigDecimal("9.98")
      actualSubtotal = (taxedCornflakes.price * 2) + taxedWeetabix.price
      actualTax = (taxedCornflakes.tax * 2) + taxedWeetabix.tax
      actualTotal = actualSubtotal + actualTax

    } yield {
      assertEquals(actualSubtotal, BigDecimal("15.02"))
      assertEquals(actualTax, BigDecimal("1.89"))
      assertEquals(actualTotal, BigDecimal("16.91"))
    }
  }

  test("computeTax should round tax up correctly") {
    val product = Product("Test", BigDecimal("1.00"))
    computeTax(product).map { result =>
      assertEquals(result.tax, BigDecimal("0.13"))
      assertEquals(result.total, BigDecimal("1.13"))
    }
  }
}
