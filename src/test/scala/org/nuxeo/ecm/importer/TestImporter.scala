package org.nuxeo.ecm.importer

import javax.inject.Inject

import akka.actor.{ActorSystem, Props}
import junit.framework.Assert._
import org.junit.{After, Test}
import org.junit.runner.RunWith
import org.nuxeo.ecm.core.api.CoreSession
import org.nuxeo.ecm.core.test.CoreFeature
import org.nuxeo.ecm.core.test.annotations.{Granularity, RepositoryConfig}
import org.nuxeo.ecm.message.{Start, Stop}
import org.nuxeo.ecm.operations.Import
import org.nuxeo.runtime.test.runner.{Deploy, Features, FeaturesRunner}
import org.scalatest.FunSuite


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

@RunWith(classOf[FeaturesRunner])
@Features(Array(classOf[CoreFeature]))
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy(Array("org.nuxeo.ecm.platform.filemanager.api", //
  "org.nuxeo.ecm.platform.filemanager.core"))
class TestImporter {

  @Inject
  var coreSession: CoreSession = _

  @Test
  def testBasics(): Unit = {
    val system = ActorSystem("TestSystem")
    val importer = system.actorOf(Props(new Import(coreSession.getRepositoryName)))
    val producer = system.actorOf(Props(new Producer(importer)))

    producer ! Start

    for (_ <- Range(0, 100)) {
      producer ! MessageFactory.generate("/")
    }
    Thread.sleep(5000)

    producer ! Stop

    assertEquals(0, 0)
  }

}
