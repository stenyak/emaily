/*
 * Copyright (c) 2009 Google Inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Modified by: dLux (the modification is marked with 'By dLux') and taton.
 */

package com.google.wave.api;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.google.wave.api.impl.ElementSerializer;
import com.google.wave.api.impl.EventDataSerializer;
import com.google.wave.api.impl.EventMessageBundle;
import com.google.wave.api.impl.EventMessageBundleSerializer;
import com.google.wave.api.impl.OperationMessageBundle;
import com.google.wave.api.impl.OperationSerializer;
import com.google.wave.api.impl.RobotMessageBundleImpl;

import com.metaparadigm.jsonrpc.JSONSerializer;
import com.metaparadigm.jsonrpc.MarshallException;
import com.metaparadigm.jsonrpc.SerializerState;
import com.metaparadigm.jsonrpc.UnmarshallException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * An abstract implementation of a Robot Servlet that handles deserialization of
 * events and serialization of operations.
 * 
 * @author scovitz@google.com (Seth Covitz)
 * @author mprasetya@google.com (Marcel Prasetya)
 */
@SuppressWarnings("serial")
public abstract class AbstractRobotServlet extends HttpServlet implements RobotServlet {
  private static final String CAPABILITIES_XML_VERSION_TAG_NAME = "w:version";
  private static final String WAVE_CAPABILITIES_XML_FILE_PATH = "_wave/capabilities.xml";
  // by dLux:
  public static final String JSON_OBJECT_REQUEST_PARAM = "jsonObject";

  private static final Logger log = Logger.getLogger(AbstractRobotServlet.class.getName());

  private static String version;
  static {
    parseVersionIdentifier();
  }

  private JSONSerializer serializer;
  private HttpServletRequest req;

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    this.req = req;
    RobotMessageBundleImpl events = deserializeEvents(req);

    // Log All Events
    for (Event event : events.getEvents()) {
      log.info(event.getType().toString() + " [" + event.getWavelet().getWaveId() + " "
          + event.getWavelet().getWaveletId());
      try {
        log.info(" " + event.getBlip().getBlipId() + "] ["
            + event.getBlip().getDocument().getText().replace("\n", "\\n") + "]");
      } catch (NullPointerException npx) {
        log.info("] [null]");
      }
    }

    processEvents(events);
    events.getOperations().setVersion(getVersion());
    serializeOperations(events.getOperations(), resp);
  }

  protected String getRobotAddress() {
    return req.getRemoteHost();
  }

  /**
   * Returns the version identifier that is specified in the capabilities.xml
   * file.
   *
   * @return A version identifier that is specified in capabilities.xml.
   */
  public static String getVersion() {
    return version;
  }

  /**
   * Parse version identifier from the capabilities.xml file, and set it to {@code version} static
   * variable.
   */
  private static void parseVersionIdentifier() {
    try {
      DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document document = builder.parse(new FileInputStream(WAVE_CAPABILITIES_XML_FILE_PATH));
      NodeList elements = document.getElementsByTagName(CAPABILITIES_XML_VERSION_TAG_NAME);
      if (elements.getLength() >= 1) {
        Node versionNode = elements.item(0);
        version = versionNode.getTextContent();
      }
    } catch (IOException e) {
      log.warning("Problem opening capabilities.xml file. Cause: " + e.getMessage());
    } catch (SAXException e) {
      log.warning("Problem parsing capabilities.xml file. Cause: " + e.getMessage());
    } catch (ParserConfigurationException e) {
      log.warning("Problem setting up XML parser. Cause: " + e.getMessage());
    }
  }

  private void serializeOperations(OperationMessageBundle operations, HttpServletResponse resp) {
    try {
      String json = serializer.toJSON(operations);
      log.info("Outgoing operations: " + json);

      try {
        StringBuilder sb = new StringBuilder();
        formatJSONObject(sb, "", new JSONObject(json));
        log.fine("Outgoing JSON object:\n" + sb.toString());
      } catch (JSONException je) {
        je.printStackTrace();
      }

      resp.setContentType("application/json");
      resp.setCharacterEncoding("utf-8");
      resp.getWriter().write(json);
      resp.setStatus(200);
    } catch (IOException iox) {
      iox.printStackTrace();
      resp.setStatus(500);
    } catch (MarshallException mx) {
      mx.printStackTrace();
      resp.setStatus(500);
    }
  }

  private RobotMessageBundleImpl deserializeEvents(HttpServletRequest req) throws IOException {
    String json = getRequestBody(req);
    log.info("Incoming events: " + json);

    JSONSerializer serializer = getJSONSerializer();
    RobotMessageBundleImpl events = null;

    try {
      JSONObject jsonObject = new JSONObject(json);

      StringBuilder sb = new StringBuilder();
      formatJSONObject(sb, "", jsonObject);
      log.fine("Incoming JSON object:\n" + sb.toString());

      // By dLux:
      req.setAttribute(JSON_OBJECT_REQUEST_PARAM, jsonObject);
      events = new RobotMessageBundleImpl((EventMessageBundle) serializer.unmarshall(
          new SerializerState(), EventMessageBundle.class, jsonObject), getRobotAddress());
    } catch (JSONException jsonx) {
      jsonx.printStackTrace();
    } catch (UnmarshallException e) {
      e.printStackTrace();
    }

    return events;
  }

  private static void formatJSONJavaObject(StringBuilder sb, String prefix, Object object)
      throws JSONException {
    if (object == null) {
      sb.append("null");
    } else if (object instanceof String) {
      sb.append('"').append(object).append('"');
    } else if (object instanceof JSONObject) {
      formatJSONObject(sb, prefix + "  ", (JSONObject) object);
    } else if (object instanceof JSONArray) {
      formatJSONArray(sb, prefix + "  ", (JSONArray) object);
    } else {
      sb.append(object.toString());
    }
  }

  private static void formatJSONObject(StringBuilder sb, String prefix, JSONObject json)
      throws JSONException {
    if (json.length() == 0) {
      sb.append("{}");
    } else {
      sb.append('{').append('\n');
      for (String name : JSONObject.getNames(json)) {
        sb.append(prefix).append('"').append(name).append("\":");
        formatJSONJavaObject(sb, prefix, json.get(name));
        sb.append(',').append('\n');
      }
      sb.append(prefix).append('}');
    }
  }

  private static void formatJSONArray(StringBuilder sb, String prefix, JSONArray array)
      throws JSONException {
    if (array.length() == 0) {
      sb.append("[]");
    } else {
      sb.append('[').append('\n');
      for (int i = 0; i < array.length(); ++i) {
        sb.append(prefix);
        formatJSONJavaObject(sb, prefix + "  ", array.get(i));
        sb.append(',').append('\n');
      }
      sb.append(prefix).append(']');
    }
  }

  /*
   * (non-Javadoc)
   * @see com.google.wave.api.RobotServlet#processEvents(com.google.wave.api.RobotMessageBundle)
   */
  @Override
  public abstract void processEvents(RobotMessageBundle events);

  private String getRequestBody(HttpServletRequest req) throws IOException {
    StringBuilder json = new StringBuilder();
    BufferedReader reader = req.getReader();
    String line;
    while ((line = reader.readLine()) != null) {
      json.append(line);
    }
    return json.toString();
  }

  private JSONSerializer getJSONSerializer() {
    if (serializer != null) {
      return serializer;
    }

    serializer = new JSONSerializer();
    try {
      serializer.registerDefaultSerializers();
      serializer.registerSerializer(new EventMessageBundleSerializer());
      serializer.registerSerializer(new EventDataSerializer());
      serializer.registerSerializer(new ElementSerializer());
      serializer.registerSerializer(new OperationSerializer());
    } catch (Exception e) {
      e.printStackTrace();
    }

    return serializer;
  }
}
