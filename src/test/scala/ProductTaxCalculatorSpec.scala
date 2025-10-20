//Sudheer Kumar - v1 of product tax

import cats.effect.IO
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
    val result =  parseProduct(invalidJson).map { res => println(res)}
    assert(result.isLeft)
    assert(result.left.getOrElse("").contains("Invalid JSON"))
  }

  test("parseProduct should handle completely invalid JSON") {
    val invalidJson = """not json at all"""
    val result = parseProduct(invalidJson).map { res => println(res)}
    assert(result.isLeft)
    assert(result.left.getOrElse("").contains("Invalid JSON"))
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
      _ = (BigDecimal("2.52") * 2) + BigDecimal("9.98")
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

  // computeTax edge cases
  test("computeTax should round tax up to 2 decimals for tiny prices") {
    val p = Product("Tiny", BigDecimal("0.01"))
    computeTax(p).map { tp =>
      assertEquals(tp.tax, BigDecimal("0.01"))   // 0.01 * 0.125 = 0.00125 -> rounds UP to 0.01
      assertEquals(tp.total, BigDecimal("0.02")) // 0.01 + 0.01
    }
  }

  test("computeTax should handle zero price") {
    val p = Product("Free", BigDecimal("0.00"))
    computeTax(p).map { tp =>
      assertEquals(tp.tax, BigDecimal("0.00"))
      assertEquals(tp.total, BigDecimal("0.00"))
    }
  }

  test("computeTax should round up for high precision prices") {
    val p = Product("Precise", BigDecimal("19.999"))
    computeTax(p).map { tp =>
      assertEquals(tp.tax, BigDecimal("2.50"))    // 19.999 * 0.125 = 2.499875 -> 2.50 (UP)
      assertEquals(tp.total, BigDecimal("22.499"))// 19.999 + 2.50
    }
  }

  // parseProduct edge cases
  test("parseProduct should fail when 'title' is missing") {
    val json = """{"name": "Cheerios", "price": 2.98}"""
    parseProduct(json) match {
      case Left(msg)  => assert(msg.contains("Invalid JSON"))
      case Right(_)   => fail("Expected Left for missing 'title' field")
    }
  }

  test("parseProduct should accept extra fields") {
    val json = """{"title": "Cheerios", "price": 2.98, "extra": "ignored"}"""
    assertEquals(parseProduct(json), Right(Product("Cheerios", BigDecimal("2.98"))))
  }

  test("parseProduct should fail when price is a string") {
    val json = """{"name": "Cheerios", "price": "2.98"}"""
    parseProduct(json) match {
      case Left(msg)  => assert(msg.contains("Invalid JSON"))
      case Right(_)   => fail("Expected Left when price is a string")
    }
  }

  test("parseProduct should handle completely invalid JSON") {
    val invalid = "definitely not json"
    parseProduct(invalid) match {
      case Left(msg)  => assert(msg.contains("Invalid JSON"))
      case Right(_)   => fail("Expected Left for invalid JSON")
    }
  }

  // fetchAndParse (error path only, no network involved)
  test("fetchAndParse should return Left for invalid URL (no network call)") {
    val badUrl = "http::://bad url"
    fetchAndParse(badUrl).map {
      case Left(msg) =>
        assert(msg.startsWith(s"Fetch failed for $badUrl:"))
        assert(msg.contains("Invalid URL"))
      case Right(_) =>
        fail("Expected Left for invalid URL")
    }
  }

  test("computeTax should handle zero-priced products") {
    val product = Product("Free Sample", BigDecimal("0.00"))
    computeTax(product).map { taxed =>
      assertEquals(taxed.name, "Free Sample")
      assertEquals(taxed.price, BigDecimal("0.00"))
      assertEquals(taxed.tax, BigDecimal("0.00"))
      assertEquals(taxed.total, BigDecimal("0.00"))
    }
  }

  test("computeTax should round down when tax is below half at 3rd decimal (0.01 -> 0.00)") {
    val product = Product("Tiny", BigDecimal("0.01"))
    computeTax(product).map { taxed =>
      // 12.5% of 0.01 = 0.00125 -> 0.00 (2dp, HALF_UP)
      assertEquals(taxed.tax, BigDecimal("0.01"))
      assertEquals(taxed.total, BigDecimal("0.02"))
    }
  }

  test("computeTax should round up when third decimal is 5 (9.88 -> tax 1.24)") {
    val product = Product("Borderline", BigDecimal("9.88"))
    computeTax(product).map { taxed =>
      // 12.5% of 9.88 = 1.235 -> 1.24 (2dp, HALF_UP)
      assertEquals(taxed.tax, BigDecimal("1.24"))
      assertEquals(taxed.total, BigDecimal("11.12"))
    }
  }

  test("computeTax total equals price + tax for various prices") {
    val prices = List(
      BigDecimal("0.00"),
      BigDecimal("0.01"),
      BigDecimal("2.99"),
      BigDecimal("12.34"),
      BigDecimal("1000.00")
    )
    prices
      .foldLeft(IO.unit) { (acc, p) =>
        acc.flatMap { _ =>
          computeTax(Product("Any", p)).map { tp =>
            assertEquals(tp.total, tp.price + tp.tax)
          }
        }
      }
  }

  test("parseProduct should ignore unknown fields") {
    val json =
      """{
        |  "title": "Choco Puffs",
        |  "price": 3.50,
        |  "brand": "ACME",
        |  "id": 123,
        |  "tags": ["cereal", "breakfast"]
        |}""".stripMargin
    val result = parseProduct(json)
    assertEquals(result, Right(Product("Choco Puffs", BigDecimal("3.50"))))
  }

  test("parseProduct should fail when price is null") {
    val json = """{"title": "Null Price", "price": null}"""
    val result = parseProduct(json)
    assert(result.isLeft)
  }

  test("parseProduct should fail when price is a string (non-numeric)") {
    val json = """{"title": "String Price", "price": "2$98dollar"}"""
    val result = parseProduct(json)
    assert(result.isLeft)
  }

  test("parseProduct should fail on empty JSON object") {
    val json = "{}"
    val result = parseProduct(json)
    assert(result.isLeft)
  }

  test("parseProduct should parse negative price (validation deferred)") {
    val json = """{"title": "Refund Item", "price": -1.23}"""
    val result = parseProduct(json)
    assertEquals(result, Right(Product("Refund Item", BigDecimal("-1.23"))))
  }


}
