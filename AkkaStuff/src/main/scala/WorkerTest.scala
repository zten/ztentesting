import akka.actor._
import akka.actor.Actor._

/**
 * Work with some workers to do some "interesting" work
 *
 * User: dubba
 * Date: 10/27/11
 */

sealed trait WorkerRequest

case class StartWork(workCollector: ActorRef) extends WorkerRequest

sealed trait WorkerResponse

case class WorkDone(ref: ActorRef) extends WorkerResponse

sealed trait WorkCollectorRequest

case class WorkUnit(str: String) extends WorkCollectorRequest

case class FinishedWorking() extends WorkCollectorRequest

sealed trait WorkCollectorResponse

case class FinishedWork(work: List[String]) extends WorkCollectorResponse

sealed trait WorkSupervisorResponse

case class Work(work: List[String]) extends WorkSupervisorResponse

case class Failure(reason: String) extends WorkSupervisorResponse

object WorkerTest extends App {
  val supervisor = actorOf[WorkSupervisor]

  supervisor.start()

  val work = supervisor ? StartWorkers(10)

  work map {
    f => f match {
      case response: WorkSupervisorResponse => response match {
        case Work(x) =>
          x.foreach(line => println(line))
          supervisor.stop()
        case Failure(reason) =>
          println(reason)
          supervisor.stop()
      }
    }
  }

}


class WorkCollector extends Actor {
  val workBuilder = List.newBuilder[String]

  override def receive = {
    case message: WorkCollectorRequest => message match {
      case WorkUnit(str) => {
        println("I got a unit of work")
        workBuilder += str
      }

      // we're really getting the finished work with this message
      case FinishedWorking() => {
        self.tryReply(FinishedWork(workBuilder.result()))
        self.stop()
      }
    }

  }
}

class Worker extends Actor {
  override def receive = {
    case request: WorkerRequest => request match {
      case StartWork(workCollector) => {
        val myNumber = (scala.math.random * 2000).toLong
        Thread.sleep(myNumber)

        val rolled = (scala.math.random * 20).toLong
        if (rolled > 18) {
          // fail instead of succeed
          //workSupervisor ! WorkFailed(self)
          self.tryReply(WorkFailed(self, "Failure chance happened; rolled " + rolled))
        } else {
          workCollector tryTell WorkUnit(self.uuid + ": produced " + myNumber)
          self.tryReply(WorkDone(self))
        }
        self.stop()
      }
    }

  }
}

sealed trait WorkSupervisorRequest

case class StartWorkers(actorCount: Int) extends WorkSupervisorRequest

case class WorkFailed(ref: ActorRef, reason: String) extends WorkSupervisorRequest

class WorkSupervisor extends Actor {
  var workers = 0
  val workCollector = actorOf[WorkCollector]
  var successes = 0
  var startChannel: UntypedChannel = null

  override def receive = {
    case workRequest: WorkerResponse => workRequest match {
      case WorkDone(ref) => {
        successes += 1
        if (successes == workers) {
          val futureWork = workCollector ? FinishedWorking()

          futureWork map {
            case response: WorkCollectorResponse => response match {
              case FinishedWork(work) => {
                workCollector.stop()
                startChannel tryTell Work(work)
              }
            }
          }



        }

      }

    }

    case request: WorkSupervisorRequest => request match {
      case WorkFailed(ref, reason) => {
        println("Work failed")
        workCollector.stop()
        startChannel tryTell Failure("Actor " + ref.uuid + " couldn't finish because: " + reason)
        self.stop()
      }

      case StartWorkers(actorCount) => {
        startChannel = self.channel

        workCollector.start()

        for (val i <- 1 to actorCount) {
          val newActor = actorOf[Worker]
          newActor.start()
          newActor ! StartWork(workCollector)

        }

        workers = actorCount

      }
    }


  }
}