package com.google.wave.extensions.emaily.util;

import java.io.BufferedReader;
import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class WaveUtil {
  public JSONObject getJsonObjectFromRequest(HttpServletRequest req) {
    try {
      StringBuilder jsonStr = new StringBuilder();
      BufferedReader reader = req.getReader();
      String line;
      while ((line = reader.readLine()) != null) {
        jsonStr.append(line);
      }
      return new JSONObject(new JSONTokener(jsonStr.toString()));
    } catch (JSONException e) {
      throw new RuntimeException("JSON parsing error", e);
    } catch (IOException e) {
      throw new RuntimeException("I/O error during reading JSON object (should not happen)", e);
    }
  }
}
