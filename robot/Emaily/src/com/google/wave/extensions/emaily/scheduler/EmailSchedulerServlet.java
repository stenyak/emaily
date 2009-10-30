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

package com.google.wave.extensions.emaily.scheduler;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.wave.extensions.emaily.config.EmailyConfig;
import com.google.wave.extensions.emaily.data.DataAccess;
import com.google.wave.extensions.emaily.data.WaveletView;

/**
 * @author dlux
 * 
 */
@Singleton
public class EmailSchedulerServlet extends HttpServlet {
  // configuration properties (in seconds)
  private static final String MAX_SERVING_TIME = "scheduler.max_servlet_serving_time";
  private static final String[] requiredLongProperties = { MAX_SERVING_TIME };

  // injected dependencies
  private final Provider<DataAccess> dataAccessProvider;
  private final EmailyConfig emailyConfig;
  private final ScheduledEmailSender sender;
  
  public EmailSchedulerServlet(Provider<DataAccess> dataAccessProvider, EmailyConfig emailyConfig, ScheduledEmailSender sender) {
    this.dataAccessProvider = dataAccessProvider;
    this.emailyConfig = emailyConfig;
    this.sender = sender;
    emailyConfig.checkRequiredLongProperties(requiredLongProperties);
  }
  
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
      IOException {
    sendScheduledEmails();
    super.doGet(req, resp);
  }

  private void sendScheduledEmails() {
    try {
      long endTime = Calendar.getInstance().getTimeInMillis()
          + emailyConfig.getLong(MAX_SERVING_TIME) * 1000;
      List<?> ids = dataAccessProvider.get().getWaveletIdsToSend();
      for (Object idObj : ids) {
        if (Calendar.getInstance().getTimeInMillis() >= endTime) {
          break;
        }
        WaveletView waveletView = dataAccessProvider.get().getWaveletView((String) idObj);
        sender.SendScheduledEmail(waveletView);
        dataAccessProvider.get().commit();
      }
    } finally {
      dataAccessProvider.get().rollback();
    }
  }
}
