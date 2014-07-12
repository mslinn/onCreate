case class FiletypeAction(filetype: String, commandTokens: List[String])

object OnCreate extends App {
  import collection.JavaConverters._
  import collection.mutable
  import com.typesafe.config.{Config, ConfigObject}
  import com.beachape.filemanagement.RegistryTypes._
  import com.beachape.filemanagement.Messages._
  import java.nio.file.Path
  import java.nio.file.StandardWatchEventKinds._

  implicit val system = akka.actor.ActorSystem("actorSystem")
  val fileMonitorActor = system.actorOf(com.beachape.filemanagement.MonitorActor())

  def handleActions(path: Path, config: Config) = {
    import sys.process._
    val filetype: String = config.getString("filetype")
    if (path.toString.endsWith(filetype)) {
      val commandTokens: mutable.Buffer[String] = config.getStringList("commandTokens").asScala
      val command = commandTokens.map(_.replaceAll("\\$f", path.toString))
      println(s"$path was created so the following is about to be executed: $command")
      val result = Process(command).!!.trim
      println(result)
    } else {
      println(s"$path does not end with $filetype so action was not triggered.")
    }
  }

  val config = com.typesafe.config.ConfigFactory.parseResources("config.json")
  val createCallback: Callback = { path =>
    val am = config.getList("actionMap")
    am.asScala.foreach { case configObject: ConfigObject =>
      val actionKeys: mutable.Set[String] = configObject.unwrapped.keySet.asScala
      handleActions(path, configObject.toConfig)
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
