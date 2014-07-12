case class FiletypeAction(filetype: String, commandTokens: List[String], soundStart: String="", soundEnd: String="")

object OnCreate extends App {
  import collection.JavaConverters._
  import collection.mutable
  import com.typesafe.config.{Config, ConfigObject}
  import com.beachape.filemanagement.RegistryTypes._
  import com.beachape.filemanagement.Messages._
  import java.io.File
  import java.nio.file.Path
  import java.nio.file.StandardWatchEventKinds._

  implicit val system = akka.actor.ActorSystem("actorSystem")
  val fileMonitorActor = system.actorOf(com.beachape.filemanagement.MonitorActor())

  def playSound(fileName: String): Unit = {
    import java.net.URL
    import javax.sound.sampled._
    import scala.concurrent.Await
    import scala.concurrent.duration._

    try {
      import scala.concurrent.Promise
      val audioIn: AudioInputStream = {
        if (fileName.startsWith("http:"))
          AudioSystem.getAudioInputStream(new URL(fileName) )
        else {
          val file: File = new File(getClass.getClassLoader.getResource(fileName).getFile)
          println(s"Reading from ${file.getCanonicalPath}")
          AudioSystem.getAudioInputStream(file)
        }
      }
      val clip = AudioSystem.getClip
      clip.open(audioIn)
      clip.start()
      val promise = Promise[String]()
      clip.addLineListener(new LineListener { // wait until sound has finished playing
        def update(event: LineEvent): Unit = {
          if (event.getType == LineEvent.Type.STOP) {
            event.getLine.close()
            promise.success("done")
          }
        }
      })
      Await.result(promise.future, Duration.Inf)
    } catch {
      case e: Exception =>
        println(e)
    }
  }

  def handleActions(path: Path, config: Config) = {
    import sys.process._
    val filetype: String = config.getString("filetype")
    if (path.toString.endsWith(filetype)) {
      val commandTokens: mutable.Buffer[String] = config.getStringList("commandTokens").asScala
      val command = commandTokens.map(_.replaceAll("\\$f", path.toString))
      println(s"$path was created so the following is about to be executed: $command")
      playSound("ascending.wav")
      val startMillis = System.currentTimeMillis
      val result = try {
        Process(command).!
      } catch {
        case e: Exception =>
          println(e.getMessage)
      }
      val endMillis = System.currentTimeMillis
      if (endMillis-startMillis<250) Thread.sleep(250)
      playSound(if (result==0) "descending.wav" else "problem.wav")
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

  val watchedFile = new File(if (args.nonEmpty) args(0) else "").getCanonicalFile
  val watchedDirectory = watchedFile.getCanonicalPath
  if (!watchedFile.exists) {
    println(s"Fatal: $watchedDirectory does not exist.")
    sys.exit(-1)
  }
  println(s"Watching $watchedDirectory")

  val x = watchedFile.toPath

  fileMonitorActor ! RegisterCallback(
    ENTRY_CREATE,
    modifier = None,
    recursive = true,
    path = watchedFile.toPath,
    createCallback)
}
