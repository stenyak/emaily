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
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.wave.extensions.emaily.config.EmailyConfig;
import com.google.wave.extensions.emaily.data.DataAccess;
import com.google.wave.extensions.emaily.data.EntityLockManager;
import com.google.wave.extensions.emaily.data.WaveletData;
import com.google.wave.extensions.emaily.email.EmailSender.EmailSendingException;

/**
 * @author dlux
 * 
 */
@Singleton
public class EmailSchedulerServlet extends HttpServlet {
  // Configuration properties (in seconds):
  // Maximum time we want the servlet to deliver emails. After that, it will return.
  private static final String MAX_SERVING_TIME = "scheduler.max_servlet_serving_time";
  // The lock timeout for a wavelet data object. No other email sending will happen for this
  // wavelet until this timeout expires. It is used to reduce concurrency. In seconds.
  private static final String WAVELET_DATA_LOCK_TIMEOUT = "scheduler.wavelet_data_lock_timeout";
  // Execpted maximum processing timeout of the wavelet data. In ms.
  private static final String WAVELET_DATA_PROCESSING_TIMEOUT = "scheduler.wavelet_data_processing_timeout";
  private static final String[] requiredLongProperties = { MAX_SERVING_TIME,
      WAVELET_DATA_LOCK_TIMEOUT };

  // injected dependencies
  private final Provider<DataAccess> dataAccessProvider;
  private final EmailyConfig config;
  private final ScheduledEmailSender sender;
  private final Logger logger;
  private final EntityLockManager entityLockManager;

  @Inject
  public EmailSchedulerServlet(Provider<DataAccess> dataAccessProvider, EmailyConfig config,
      ScheduledEmailSender sender, Logger logger, EntityLockManager entityLockManager) {
    this.dataAccessProvider = dataAccessProvider;
    this.config = config;
    this.sender = sender;
    this.logger = logger;
    this.entityLockManager = entityLockManager;
    config.checkRequiredLongProperties(requiredLongProperties);
  }

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
      IOException {
    logger.info("New cron request");
    sendScheduledEmails();
  }

  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
      IOException {
    doGet(req, resp);
  }

  /**
   * Send emails which are scheduled for sending.
   */
  private void sendScheduledEmails() {
    try {
      // Time-limiting the sending.
      logger.info("Querying sendable emails");
      long endTime = Calendar.getInstance().getTimeInMillis() + config.getLong(MAX_SERVING_TIME)
          * 1000;
      List<String> ids = dataAccessProvider.get().getWaveletIdsToSend();
      for (final String id : ids) {
        if (Calendar.getInstance().getTimeInMillis() >= endTime) {
          break;
        }
        logger.info("WaveletData to process: " + id);

        // Run the email processing in a wavelet lock.
        entityLockManager.executeInLock(WaveletData.class, id, config
            .getLong(WAVELET_DATA_LOCK_TIMEOUT), config.getLong(WAVELET_DATA_PROCESSING_TIMEOUT),
            false, new Runnable() {
              @Override
              public void run() {
                try {
                  WaveletData waveletData = dataAccessProvider.get().getWaveletData(id);
                  sender.sendScheduledEmails(waveletData);
                  dataAccessProvider.get().commit();
                } catch (EmailSendingException e) {
                  // If it cannot send email, we dumps the error to the log and continue with the
                  // next email. The next cron even will again pick up the same email, but the
                  // others in the queue are also processed.
                  e.printStackTrace();
                  dataAccessProvider.get().close();
                }
              }
            });

      }
    } finally {
      dataAccessProvider.get().close();
    }
  }
}
