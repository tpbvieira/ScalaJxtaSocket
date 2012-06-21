//import akka.actor.{ Actor, ActorRef, Channel }
//import akka.dispatch._
//import akka.c
//
//import com.twitter.json.Json
//
//class SocketActor(
//  serviceHost: String, 
//  servicePort: Int, 
//  numberOfActors: Int,
//  maxLineLength: Int) extends Actor with Consumer {
//
//  type TASKS = List[Map[String, String]]
//  type MapString = Map[String, String]
//
//  private lazy val lbActor = Actor.actorOf(new LoadBalancerActor(numberOfActors)).start
//
//  def endpointUri = "netty:tcp://%s:%d?textline=true&decoderMaxLineLength=%d" format (serviceHost, servicePort, maxLineLength)
//
//  override def autoack = false 
//
//  def receive = {
//    case msg: Message => {
//      val data = msg.bodyAs[String]
//      try {
//        Logger.log("Received data: %s" format data)
//        process(data)
//      } catch {
//        case e: Exception => {
//          Logger.log("Failed from SocketActor: %s" format e.toString)
//          self.reply("[\"Failed\"]")
//          e.printStackTrace
//        }
//      }
//    }
//  }
//
//  private def process(data: String) {
//    val parameters: MapString = Json.parse(data).asInstanceOf[MapString]
//    val tasks: TASKS = parameters.getOrElse("tasks", Map()).asInstanceOf[TASKS]
//    val replyTimes = if (parameters.getOrElse("reply", true).asInstanceOf[Boolean]) tasks.size else 0
//    val needReply = replyTimes > 0 
//    var observer: ActorRef = null
//
//    if (needReply) {
//      observer = Actor.actorOf(new Observer(self.channel, replyTimes)).start
//    } else {
//      self.reply(Ack)
//    }
//
//    tasks.foreach(task => {
//      val params = task.getOrElse("params", Map()).asInstanceOf[MapString]
//      lbActor ! HttpRequestData(
//        observer,
//        task.getOrElse("method", "get").asInstanceOf[String], 
//        task("path"), 
//        params,
//        needReply 
//      )
//    })
//  }
//}