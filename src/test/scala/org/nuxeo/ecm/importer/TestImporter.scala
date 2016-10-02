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

package org.nuxeo.ecm.importer

import java.util.concurrent.TimeUnit
import javax.inject.Inject

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.google.common.base.Stopwatch
import junit.framework.Assert._
import org.junit.Test
import org.junit.runner.RunWith
import org.nuxeo.ecm.core.api.CoreSession
import org.nuxeo.ecm.core.test.CoreFeature
import org.nuxeo.ecm.core.test.annotations.{Granularity, RepositoryConfig}
import org.nuxeo.ecm.message._
import org.nuxeo.ecm.operations.{ImportSupervisor, Resp}
import org.nuxeo.runtime.test.runner.{Deploy, Features, FeaturesRunner}

import scala.concurrent.Await
import scala.concurrent.duration._


@RunWith(classOf[FeaturesRunner])
@Features(Array(classOf[CoreFeature]))
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy(Array("org.nuxeo.ecm.platform.filemanager.api", //
  "org.nuxeo.ecm.platform.filemanager.core"))
class TestImporter {

  @Inject
  var coreSession: CoreSession = _

  val num = 1000

  @Test
  def testBasics(): Unit = {
    val system = ActorSystem("TestSystem")
    val supervisor = system.actorOf(Props(new ImportSupervisor(coreSession.getRepositoryName)))
    val producer = system.actorOf(Props(new Producer(supervisor)))

    producer ! Start

    for (_ <- Range(0, num)) {
      producer ! MessageFactory.generate("/")
    }

    Thread.sleep(num * 10)

    implicit val timeout = Timeout(5 seconds)
    val future = supervisor ? Resp
    val result = Await.result(future, timeout.duration).asInstanceOf[Int]

    producer ! Stop
    assertEquals(num, result)
  }

  @Test
  def testRouter(): Unit = {
    val system = ActorSystem("RouterTestSystem")
    val supervisor = system.actorOf(Props(new Watcher(50)))

    val repo = coreSession.getRepositoryName
    val containers = (0 until num)
      .map(_ => MessageContainer(MessageFactory.generate("/"), repo))

    val watch = Stopwatch.createStarted()
    (0 until num).par
      .foreach( i => {
      supervisor ! containers.apply(i)
    })

    implicit val timeout = Timeout(60 seconds)
    val future = supervisor ? Resp
    val result = Await.result(future, timeout.duration).asInstanceOf[Int]

    val speed = result / (watch.elapsed(TimeUnit.MILLISECONDS) / 1000.0)
    println(f"Import speed: $speed%.2f")
    assertEquals(num, result)
  }

}
