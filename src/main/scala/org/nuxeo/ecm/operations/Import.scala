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
import org.nuxeo.ecm.core.api.{Blob, CoreInstance, DocumentNotFoundException}
import org.nuxeo.ecm.core.blob.{BlobManager, SimpleManagedBlob}
import org.nuxeo.ecm.message._
import org.nuxeo.runtime.api.Framework
import org.nuxeo.runtime.transaction.TransactionHelper

class Import(repoName: String, message: Message) extends Actor {

  override def receive: Receive = {
    case Start =>
      importMessage(message)
      sender() ! Success(message.title)
    case Stop =>
      println("Import stopped")
      context.stop(self)
    case str: String if message.path.contains(str) =>
      importMessage(message)
      sender() ! Success(message.title)
    case Error(reason) =>
      println(reason)
      context.stop(self)
    case undef => unhandled(undef)
  }

  def importMessage(message: Message): Unit = {
    if (!TransactionHelper.isTransactionActive) {
      TransactionHelper.startTransaction()
    }

    val session = CoreInstance.openCoreSessionSystem(repoName)
    try {
      val docModel = session.createDocumentModel(message.path, message.title, message.kind)
      docModel.setProperty("dublincore", "title", message.title)
      if (docModel.hasSchema("file")) {
        for (data <- message.data) {
          val blob = createBlobWith(data)
          docModel.setProperty("file", "content", blob)
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

  private def createBlobWith(data: Data): Blob = {
    val blobManager: BlobManager = Framework.getService(classOf[BlobManager])
    val provider: String = blobManager.getBlobProviders.keySet.iterator.next
    val info: BlobManager.BlobInfo = new BlobManager.BlobInfo
    info.key = s"$provider:${data.digest}"
    info.digest = data.digest
    info.mimeType = data.mimeType
    info.filename = data.filename
    info.encoding = data.encoding
    info.length = data.length
    new SimpleManagedBlob(info)
  }
}
