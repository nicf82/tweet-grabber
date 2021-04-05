package net.carboninter

import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.matchers.should.Matchers._

class ApplicationTest extends AnyWordSpecLike {

  "Parsing with an html table row" should {

    "work for a data row" in {

      val h = """<tr><td style='mso-number-format:d\/m\/yyyy;'>2019-09-25</td><td>IRE</td><td>SLIGO </td><td>GTY</td><td>Handicap Chase</td><td>3m�f</td><td></td><td>5:05pm</td><td></td><td>BISHOPS QUARTER</td><td>94</td><td>7</td><td>2</td><td style='mso-number-format:\@'>11-4</td><td>Paul Cawley</td><td>Patrick G Kelly</td><td>T</td><td>9</td><td>17.00</td><td>22.00</td><td>25.00</td><td>900.00</td><td>11.63</td><td>25.00</td><td>13.00</td><td>PU</td><td></td><td>12</td><td style=color:#cc0000;>-10.00</td><td style=color:#cc0000;>-10.00</td><td style=color:#cc0000;>-10.00</td><td style='color:#cc0000;'>-3</td><td>-</td><td style='color:#cc0000;'>-14.3</td><td>-</td></tr>"""
      val i = h.split("""[<][^>]*[>]\s*[<][^>]*[>]""").filter(_!="")

      i.toList shouldBe List("2019-09-25", "IRE", "SLIGO ", "GTY", "Handicap Chase", "3m�f", "5:05pm", "BISHOPS QUARTER",
        "94", "7", "2", "11-4", "Paul Cawley", "Patrick G Kelly", "T", "9", "17.00", "22.00", "25.00",
        "900.00", "11.63", "25.00", "13.00", "PU", "12", "-10.00", "-10.00", "-10.00", "-3", "-", "-14.3", "-")

    }
  }
}
