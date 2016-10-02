/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  Contributors:
 *      Andrei Nechaev
 */

package org.nuxeo.ecm.message

import akka.actor.SupervisorStrategy.{Escalate, Restart}
import akka.actor.{Actor, ActorLogging, ActorRef, OneForOneStrategy, Props, SupervisorStrategy, Terminated}
import akka.routing.{ActorRefRoutee, RoundRobinRoutingLogic, Router}
import org.nuxeo.ecm.core.api.{CoreInstance, DocumentNotFoundException}
import org.nuxeo.ecm.operations.Resp
import org.nuxeo.runtime.transaction.TransactionHelper

case class MessageContainer(message: Message, repo: String)

class Worker(batch: Int = 30) extends Actor with ActorLogging {

  var messages = Seq[MessageContainer]()
  var received = 0

  override def receive: Receive = {
    case container: MessageContainer =>
        try {
          importMessage(container.message, container.repo)
        } catch {
          case e: Exception =>
            throw e
        }
    case e: Error =>
      log.info(s"Error occurred $e")
      sender ! e
  }

  def importMessage(message: Message, to: String): Unit = {
    if (!TransactionHelper.isTransactionActive) {
      TransactionHelper.startTransaction()
    }

    val session = CoreInstance.openCoreSessionSystem(to)
    try {
      val docModel = session.createDocumentModel(message.path, message.title, message.kind)
      docModel.setProperty("dublincore", "title", message.title)
      if (docModel.hasSchema("file")) {
        for (data <- message.data) {
//          val blob = createBlobWith(data)
//          docModel.setProperty("file", "content", blob)
        }
      }
      session.createDocument(docModel)
    } catch {
      case e: DocumentNotFoundException =>
        throw e
      case e: Exception =>
        println(e)
    } finally {
      session.close()
    }

    TransactionHelper.commitOrRollbackTransaction()
  }
}

class Watcher(size: Int) extends Actor {

  var router = {
    val routees = Vector.fill(size) {
      val w = context.actorOf(Props(new Worker()))
      context.watch(w)
      ActorRefRoutee(w)
    }

    Router(RoundRobinRoutingLogic(), routees)
  }

  var sent = 0

  override def supervisorStrategy(): SupervisorStrategy = OneForOneStrategy(maxNrOfRetries = 100) {
    case _: DocumentNotFoundException => Restart
    case _: Exception => Escalate
  }

  override def receive: Receive = {
    case container: MessageContainer =>
      sent += 1
      router.route(container, sender)
    case Resp =>
      sender ! sent
    case Terminated(a) =>
      router.removeRoutee(a)
      val w = context.actorOf(Props(new Worker()))
      context.watch(w)
      router = router.addRoutee(w)
  }
}
