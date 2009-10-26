/* Copyright (c) 2009 Google Inc.
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
 */
package com.google.wave.extensions.emaily.email;

import java.io.IOException;

import org.apache.james.mime4j.message.BodyPart;
import org.apache.james.mime4j.message.Entity;
import org.apache.james.mime4j.message.Multipart;
import org.apache.james.mime4j.message.TextBody;

import com.google.inject.Singleton;
import com.google.wave.extensions.emaily.util.StrUtil;

/**
 * Mail utilities.
 * 
 * @author taton
 */
@Singleton
public class MailUtil {
  /**
   * Formats a MIME message into a text-only message.
   * 
   * @param sb The StringBuilder to append the text content to.
   * @param entity The mail entity to format as a text-only content.
   * @return True if the entity could be formatted as a text content, false
   *         otherwise (e.g. if this is an HTML document, an image or a base64
   *         encoded binary content).
   */
  public boolean mimeEntityToText(StringBuilder sb, Entity entity) {
    if (entity.isMultipart()) {
      Multipart multipart = (Multipart) entity.getBody();
      // TODO(taton) Figure out how to handle the message preamble and epilogue?
      if (entity.getMimeType().equals("multipart/alternative")) {
        for (BodyPart part : multipart.getBodyParts()) {
          StringBuilder subsb = new StringBuilder();
          if (mimeEntityToText(subsb, part)) {
            sb.append(subsb.toString());
            return true;
          }
        }
        sb.append("No suitable formatting alternative for this email content");
        return false;
      } else {
        for (BodyPart part : multipart.getBodyParts()) {
          mimeEntityToText(sb, part);
        }
        return true;
      }
    } else if (entity.getBody() instanceof TextBody) {
      TextBody body = (TextBody) entity.getBody();
      try {
        sb.append(StrUtil.readContent(body.getReader()));
        return true;
      } catch (IOException ioe) {
        sb.append(ioe.toString());
        return false;
      }
    } else {
      sb.append("Unknown content type: [").append(entity.getClass().getName()).append("]\n");
      return false;
    }
  }
}
