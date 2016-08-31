package org.nuxeo.ecm.importer

import akka.actor.{Actor, ActorRef}
import org.nuxeo.ecm.message.{Error, Message, Start, Stop}

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

class Producer(actor: ActorRef) extends Actor {


  override def receive: Receive = {
    case Start =>
      println("Producer started")
      actor ! Start
    case Stop =>
      println("Producer stop")
      actor ! Stop
      context.stop(self)
    case m: Message => actor ! m
    case Error(reason) =>
      actor ! Error(reason)
      context.stop(self)
    case s: String => println(s)
    case undef => println(s"${this.getClass.getCanonicalName} Couldn't recognize the message: $undef")
  }

  def send(message: Message): Unit = {
    actor ! Message
  }
}