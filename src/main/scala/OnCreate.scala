import play.api.libs.json._
import sys.process._

case class FiletypeAction(filetype: String, commandTokens: List[String]) {
  import play.api.libs.json._

  implicit val ftaWrites = Json.writes[FiletypeAction]

  def toJson = Json.toJson(this)

  def execute: String = Process(commandTokens).!!.trim
}

object FiletypeAction {
  implicit val fta = Json.format[FiletypeAction]

  def fromJson(json: String): FiletypeAction = Json.fromJson[FiletypeAction](Json.parse(json)).get
}

object OnCreate extends App {
  import com.beachape.filemanagement.RegistryTypes._
  import com.beachape.filemanagement.Messages._
  import java.nio.file.StandardWatchEventKinds._
  import collection.JavaConverters._

  implicit val system = akka.actor.ActorSystem("actorSystem")
  val fileMonitorActor = system.actorOf(com.beachape.filemanagement.MonitorActor())

  val config = com.typesafe.config.ConfigFactory.parseResources("config.json")
  val x = config.entrySet.asScala
  println(s"config.entrySet = $x")

  val createCallback: Callback = { path =>
    config.entrySet.asScala.foreach { case entry =>
      val filetype: String = entry.getKey
      if (path endsWith filetype) {
        val commandTokens: List[String] = config.getStringList(filetype).asScala.toList
        println(s"$path was created so the following should be executed: $commandTokens")
        val result = FiletypeAction(filetype, commandTokens).execute
        println(result)
      } else {
        println(s"$path does not end with $filetype so action was not triggered.")
      }
    }
  }

  val watchedDirectory = if (args.nonEmpty) args(0) else new java.io.File("").getAbsolutePath
  val watchedPath = java.nio.file.Paths get watchedDirectory
  println(s"Watching $watchedDirectory")

  fileMonitorActor ! RegisterCallback(
    ENTRY_CREATE,
    modifier = None,
    recursive = false,
    path = watchedPath,
    createCallback)
}
