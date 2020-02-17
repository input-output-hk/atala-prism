package io.iohk.node.operations

import java.time.LocalDate

import io.iohk.node.operations.path.ValueAtPath
import io.iohk.node.{geud_node => proto}

import scala.util.Try

object ParsingUtils {

  def parseDate(date: ValueAtPath[proto.Date]): Either[ValidationError, LocalDate] = {
    for {
      year <- date.child(_.year, "year").parse { year =>
        Either.cond(year > 0, year, "Year needs to be specified as positive value")
      }
      month <- date.child(_.month, "month").parse { month =>
        Either.cond(month >= 1 && month <= 12, month, "Month has to be specified and between 1 and 12")
      }
      parsedDate <- date.child(_.day, "day").parse { day =>
        Try(LocalDate.of(year, month, day)).toEither.left
          .map(_ => "Day has to be specified and a proper day in the month")
      }
    } yield parsedDate
  }

}
