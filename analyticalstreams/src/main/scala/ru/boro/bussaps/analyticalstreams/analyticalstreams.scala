package ru.boro.bussaps.analyticalstreams

import org.apache.spark.sql.streaming.{OutputMode, Trigger}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.{StringType, StructType}

object analyticalstreams {

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
      $"typeEvent"
        , $"kindEvent"
        , $"idClient"
        , ($"dateCreate"/1000).cast("timestamp").as("dateCreate")
        , $"approvedBy"
        , ($"approvedDateTime"/1000).cast("timestamp").as("approvedDateTime")
      )

    df.toJSON.writeStream
      .format("text")
      .trigger(Trigger.ProcessingTime("35 seconds"))
      .outputMode(OutputMode.Append())
      .option("path", "events_to_analytics")
      .option("checkpointLocation", "chkp")
      .start()
      .awaitTermination()

  }
}

