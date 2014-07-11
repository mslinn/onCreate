object OnCreate extends App {
  import akka.actor.ActorSystem
  import com.beachape.filemanagement.MonitorActor
  import com.beachape.filemanagement.RegistryTypes._
  import com.beachape.filemanagement.Messages._
  import java.nio.file.Paths
  import java.nio.file.StandardWatchEventKinds._

  implicit val system = ActorSystem("actorSystem")
  val fileMonitorActor = system.actorOf(MonitorActor())

  val createCallback: Callback = { path =>
    println(s"File was created: $path")
  }

  val watchedDirectory = if (args.nonEmpty) args(0) else new java.io.File(".").getAbsolutePath
  val watchedPath = Paths get watchedDirectory

  fileMonitorActor ! RegisterCallback(
    ENTRY_CREATE,
    modifier = None,
    recursive = false,
    path = watchedPath,
    createCallback)
}
