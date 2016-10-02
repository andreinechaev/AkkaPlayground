/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * Contributors:
 *     anechaev
 */

package org.nuxeo.ecm.operations
import akka.actor.Actor

case class Success(title: String)
case class Resp()

class ImportSupervisor(repository: String) extends Actor{
  import akka.actor.SupervisorStrategy.{Escalate, Restart}
  import akka.actor.{ActorRef, OneForOneStrategy, Props}
  import org.nuxeo.ecm.core.api.DocumentNotFoundException
  import org.nuxeo.ecm.message.{Message, Start, Stop}

  var counter: Int = 0

  override val supervisorStrategy = OneForOneStrategy(maxNrOfRetries = 100) {
    case _: DocumentNotFoundException => Restart
    case _: Exception => Escalate
  }

  override def receive: Receive = {
    case Start => println("Supervisor Started")
    case message: Message =>
      val ref = context.actorOf(Props(new Import(repository, message)))
      ref ! Start
      for (r <- context.children if message.folderish) {
        r ! message.title
      }
    case Resp => sender ! counter
    case Success(_) => counter+=1
    case Stop =>
      context.stop(self)
      println("Supervisor stopped")
  }
}
