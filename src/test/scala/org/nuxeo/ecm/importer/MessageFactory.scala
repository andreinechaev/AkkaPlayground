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

import java.util.UUID

import org.nuxeo.ecm.message.{Data, Message}

import scala.util.Random


object MessageFactory {
  def generate(path: String): Message = {
    val rnd = Random.nextInt(100)
    val folderish = rnd > 50
    val kind = if (folderish) "Folder" else "File"

    val firstFour = UUID.randomUUID().toString.substring(0, 4)
    val title = s"${firstFour}_$kind"

    Message(title, path, null, List[Data](), kind, folderish)
  }
}
