package org.nuxeo.ecm.message

import org.nuxeo.ecm.core.api.Blob
import org.nuxeo.ecm.core.api.blobholder.BlobHolder


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

import scala.collection.JavaConversions._


case object Start
case object Stop
case class Error(reason: String)

case class Message(
                    title: String,
                    path: String,
                    properties: Map[String, Serializable],
                    data: List[Data],
                    kind: String,
                    folderish: Boolean = false
                  )

case class Data(
               filename: String,
               digest: String,
               mimeType: String,
               encoding: String,
               length: Long,
               dataPaths: List[String] = null
               )


object Data {
  def apply(blob: Blob): Data = {
    new Data(
      blob.getFilename,
      blob.getDigest,
      blob.getMimeType,
      blob.getEncoding,
      blob.getLength)
  }

  def dataFromBlob(holder: BlobHolder): Option[List[Data]] = {
    val list: List[Data] = if(holder.getBlob != null) {
      List(Data.apply(holder.getBlob))
    } else {
      holder.getBlobs.toList.map((b: Blob) => Data.apply(b))
    }

    Option(list)
  }
}