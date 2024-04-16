package ru.boro.bussaps.analyticalstreams

import org.apache.spark.sql.streaming.{OutputMode, Trigger}
import org.apache.spark.sql.{Dataset, Row, SparkSession}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.{StringType, StructType}

import scala.io.Source
import scala.util.Using

object analyticalstreams {

  private val mapConf: Map[String, String] = Using(Source.fromResource("application.yml")) { src =>
    src.getLines.flatMap(s => s.split(":")).grouped(2)
      .map(x => (x.head.trim, x(1).trim))
      .toMap
  }.get

  def main(args: Array[String]): Unit = {

    val spark: SparkSession = SparkSession.builder().master("local")
      .getOrCreate()

    import spark.implicits._

    spark.conf.set("spark.sql.session.timeZone", "Europe/Moscow")

    val schema = new StructType()
      .add("typeEvent", StringType)
      .add("kindEvent", StringType)
      .add("idClient", StringType)
      .add("dateCreate", StringType)
      .add("approvedBy", StringType)
      .add("approvedDateTime", StringType)

    val input_stream = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "localhost:9092,localhost:9093,localhost:9094")
      .option("subscribe", "analyticalserv")
      .load()

    val df = input_stream.select(
      $"value".cast(StringType).as("value"))
      .map(value => value.mkString)
      .select(from_json($"value", schema).as("data"))
      .select($"data.*")
      .select(
      $"typeEvent".as("type_event")
        , $"kindEvent".as("kind_event")
        , $"idClient".as("id_client")
        , ($"dateCreate"/1000).cast("timestamp").as("date_create")
        , $"approvedBy".as("approved_by")
        , ($"approvedDateTime"/1000).cast("timestamp").as("approved_datetime")
      )

    df.writeStream
      .foreachBatch { (batchDF: Dataset[Row], batchId: Long) =>
        postgres_sink(batchDF, batchId)
      }
      .trigger(Trigger.ProcessingTime("35 seconds"))
      .start()
      .awaitTermination()

  }

  def postgres_sink(data_frame: Dataset[Row], batchId: Long): Unit = {


    val df = data_frame.persist()

    df.write
      .format("jdbc")
      .option("url", "jdbc:postgresql://0.0.0.0:15432/")
      .option("dbtable", "analytical_event")
      .option("user", mapConf("POSTGRES_USER"))
      .option("password", mapConf("POSTGRES_PASSWORD"))
      .option("driver", "org.postgresql.Driver")
      .mode("append")
      .save

    df.unpersist()
  }

}

