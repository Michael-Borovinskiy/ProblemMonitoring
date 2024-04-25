package ru.boro.bussaps.analyticalstreams

import scala.io.Source
import scala.util.Using

object Utils {

  val mapConf: Map[String, String] = Using(Source.fromResource("application.yml")) { src =>
    src.getLines.flatMap(s => s.split(":")).grouped(2)
      .map(x => (x.head.trim, x(1).trim))
      .toMap
  }.get

}
