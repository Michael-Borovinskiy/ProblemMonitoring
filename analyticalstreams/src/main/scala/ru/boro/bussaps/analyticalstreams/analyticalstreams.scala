package ru.boro.bussaps.analyticalstreams

import org.apache.spark.sql.streaming.OutputMode
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._

object analyticalstreams {

  def main(args: Array[String]): Unit = {

    val spark: SparkSession = SparkSession.builder().master("local")
      .getOrCreate()

    import spark.implicits._

    spark.conf.set("spark.sql.session.timeZone", "Europe/Moscow")

    val input_stream = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "localhost:9092,localhost:9093,localhost:9094")
      .option("subscribe", "analyticalserv")
      .load()

    val df = input_stream.selectExpr("CAST(key AS STRING)", "CAST(value AS STRING)")
      .as[(String, String)]
      .withColumn("s_dttm", current_timestamp)

    df.toJSON.writeStream
      .format("json")
      .outputMode(OutputMode.Append())
      .option("path", "events_to_analytics")
      .option("checkpointLocation", "chkp")
      .start()
      .awaitTermination()

  }
}

