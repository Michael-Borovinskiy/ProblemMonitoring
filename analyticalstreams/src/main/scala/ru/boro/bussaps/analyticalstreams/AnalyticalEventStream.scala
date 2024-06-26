package ru.boro.bussaps.analyticalstreams

import org.apache.spark.sql.streaming.{OutputMode, Trigger}
import org.apache.spark.sql.{Dataset, Row, SparkSession}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types.{StringType, StructType}


object AnalyticalEventStream {

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

    val inputStream = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "localhost:9092,localhost:9093,localhost:9094")
      .option("subscribe", "analyticalserv")
      .load()

    val df = inputStream.select(
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

  def postgres_sink(dataFrame: Dataset[Row], batchId: Long): Unit = {


    val df = dataFrame.persist()

    df.write
      .format("jdbc")
      .option("url", "jdbc:postgresql://0.0.0.0:15432/")
      .option("dbtable", "analytical_event")
      .option("user", Utils.mapConf("POSTGRES_USER"))
      .option("password", Utils.mapConf("POSTGRES_PASSWORD"))
      .option("driver", "org.postgresql.Driver")
      .mode("append")
      .save

    df.unpersist()
  }

}

