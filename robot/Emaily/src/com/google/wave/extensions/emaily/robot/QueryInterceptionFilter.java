package com.google.wave.extensions.emaily.robot;

import java.io.BufferedReader;
import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.google.inject.Singleton;

@Singleton
public class QueryInterceptionFilter implements Filter {
  public static final String JSON_OBJECT_ATTRIBUTE = "emaily.json_object";

  public void doFilter(ServletRequest req, ServletResponse resp, FilterChain filterChain)
      throws IOException, ServletException {
    StringBuilder jsonStr = new StringBuilder();
    BufferedReader reader = req.getReader();
    String line;
    while ((line = reader.readLine()) != null) {
      jsonStr.append(line);
    }
    try {
      JSONObject json = new JSONObject(new JSONTokener(jsonStr.toString()));
      req.setAttribute(JSON_OBJECT_ATTRIBUTE, json);
    } catch (JSONException e) {
      throw new RuntimeException("JSON parsing error", e);
    }
    filterChain.doFilter(req, resp);
  }

  public void init(FilterConfig filterConfig) throws ServletException {
    // Do nothing.
  }

  public void destroy() {
    // Do nothing.
  }
}
