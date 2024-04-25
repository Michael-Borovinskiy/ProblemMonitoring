package ru.boro.bussaps.analyticalstreams

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.from_json
import org.apache.spark.sql.streaming.{OutputMode, Trigger}
import org.apache.spark.sql.types.{StringType, StructType}

object MonitoringEventStream {

  def main(args: Array[String]): Unit = {

    val spark: SparkSession = SparkSession.builder().master("local")
      .config("spark.sql.extensions", "io.delta.sql.DeltaSparkSessionExtension")
      .config("spark.sql.catalog.spark_catalog", "org.apache.spark.sql.delta.catalog.DeltaCatalog")
      .getOrCreate()

    import spark.implicits._

    spark.conf.set("spark.sql.session.timeZone", "Europe/Moscow")

    val schema = new StructType()
      .add("typeEvent", StringType)
      .add("kindEvent", StringType)
      .add("idClient", StringType)
      .add("dateCreate", StringType)

    val inputStream = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "localhost:9092,localhost:9093,localhost:9094")
      .option("subscribe", "monitoringserv")
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
        , ($"dateCreate" / 1000).cast("timestamp").as("date_create")
      )

    df.writeStream
      .format("delta")
      .outputMode(OutputMode.Append())
      .option("checkpointLocation", Utils.mapConf("PATH_TO_FILES") + "chkp_monitoring")
      .trigger(Trigger.ProcessingTime("20 seconds"))
      .start(Utils.mapConf("PATH_TO_FILES") + "events_monitoring")
      .awaitTermination()
  }

}
