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
package com.google.wave.extensions.emaily.util;

import static com.google.wave.extensions.emaily.util.StrUtil.join;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.google.inject.Singleton;
import com.google.wave.api.Blip;
import com.google.wave.api.Event;
import com.google.wave.api.EventType;
import com.google.wave.api.RobotMessageBundle;
import com.google.wave.api.Wavelet;
import com.google.wave.extensions.emaily.data.BlipVersionView;
import com.google.wave.extensions.emaily.data.WaveletView;

/**
 * Utility functions for debugging Wave and Robot interactions and datastore entities.
 * 
 * @author dlux
 */
@Singleton
public class DebugHelper {

  /**
   * Appends details of a RobotMessageBundle to the given StringBuilder.
   * 
   * @param sp The StringBuilder to append info to.
   * @param bundle The RobotMessageBundle to dump.
   */
  public void printRobotMessageBundleInfo(StringBuilder sp, RobotMessageBundle bundle) {
    sp.append("RobotMessageBundle info:\n");
    sp.append("RobotAddress: ").append(bundle.getRobotAddress()).append("\n");
    sp.append("Is new wave? ").append(bundle.isNewWave()).append("\n");
    sp.append("Was self added? ").append(bundle.wasSelfAdded()).append("\n");
    sp.append("Was self removed? ").append(bundle.wasSelfRemoved()).append("\n");
    List<EventType> types = new ArrayList<EventType>();
    for (Event event : bundle.getEvents())
      types.add(event.getType());
    sp.append("Events: ").append(join(types, ", ")).append("\n");
  }

  /**
   * Appends details of a Blip to the given StringBuilder.
   * 
   * @param sp The StringBuilder to append info to.
   * @param blip The Blip MessageBundle to dump.
   */
  public void printBlipInfo(StringBuilder sp, Blip blip) {
    sp.append("Blip info for blip ID: ").append(blip.getBlipId()).append("\n");
    sp.append("Parent blip ID: ").append(blip.getParentBlipId()).append("\n");
    sp.append("Creator: ").append(blip.getCreator()).append("\n");
    sp.append("Last modified time: ").append(blip.getLastModifiedTime()).append("\n");
    sp.append("Contributors: ").append(join(blip.getContributors(), ", ")).append("\n");
    sp.append("Document text:").append(blip.getDocument().getText()).append("\n");
    sp.append("Annotations: ").append(join(blip.getDocument().getAnnotations(), ", ")).append("\n");
    sp.append("Gadgets: ").append(join(blip.getDocument().getGadgetView().getGadgets(), ", "))
        .append("\n");
    sp.append("Children blip IDs: ").append(join(blip.getChildBlipIds(), ", ")).append("\n");
    for (Blip child : blip.getChildren())
      printBlipInfo(sp, child);
    sp.append("End of blip info for blip ID: ").append(blip.getBlipId()).append("\n");
  }

  /**
   * Appends details of a Wavelet to the given StringBuilder.
   * 
   * @param sp The StringBuilder to append info to.
   * @param wavelet The Wavelet to dump.
   */
  public void printWaveletInfo(StringBuilder sp, Wavelet wavelet) {
    sp.append("Wavelet info:");
    sp.append("title: ").append(wavelet.getTitle()).append("\n");
    sp.append("ID: ").append(wavelet.getWaveletId()).append("\n");
    sp.append("Wave ID: ").append(wavelet.getWaveId()).append("\n");
    sp.append("Creator: ").append(wavelet.getCreator()).append("\n");
    sp.append("Creation time: ").append(wavelet.getCreationTime()).append("\n");
    sp.append("Last modified time: ").append(wavelet.getLastModifiedTime()).append("\n");
    sp.append("Version: ").append(wavelet.getVersion()).append("\n");
    sp.append("Participants: ").append(join(wavelet.getParticipants(), ", ")).append("\n");
    for (Map.Entry<String, String> entry : wavelet.getDataDocuments().entrySet())
      sp.append("Data document: ").append(entry.getKey()).append(" : ").append(entry.getValue())
          .append("\n");
    sp.append("Root blip ID: ").append(wavelet.getRootBlipId()).append("\n");
  }

  /**
   * Returns debug information about a WaveletView data object.
   * 
   * @param waveletView The wavelet view to dump.
   * @return The debug string.
   */
  public String printWaveletViewInfo(WaveletView waveletView) {
    StringBuilder sb = new StringBuilder();
    printWaveletViewInfo(sb, waveletView);
    return sb.toString();
  }

  /**
   * Returns debug information about a WaveletView data object.
   * 
   * @param sb The stringbuffer to append the information to.
   * @param waveletView The wavelet view to dump.
   */
  public void printWaveletViewInfo(StringBuilder sb, WaveletView waveletView) {
    sb.append("WaveletView info:\n");
    sb.append("Wavelet Id: ").append(waveletView.getWaveletId()).append('\n');
    sb.append("User email: ").append(waveletView.getEmail()).append('\n');
    sb.append("Email address token: ").append(waveletView.getEmailAddressToken()).append('\n');
    printTimestamp(sb, "Last email sent time: ", waveletView.getLastEmailSentTime());
    printTimestamp(sb, "Time for sending:     ", waveletView.getTimeForSending());
    sb.append("Sent blips: \n");
    for (BlipVersionView b : waveletView.getSentBlips()) {
      sb.append("- Blip Id: ").append(b.getBlipId()).append('\n');
      sb.append("  Blip version: ").append(b.getVersion()).append('\n');
      sb.append("  Participants:");
      StrUtil.join(sb, b.getParticipants(), ", ");
      sb.append('\n');
      printTimestamp(sb, "  First edited:     ", b.getFirstEditedTimestamp());
      printTimestamp(sb, "  Last changed:     ", b.getLastChangedTimestamp());
      printTimestamp(sb, "  Last submitted:   ", b.getLastSubmittedTimestamp());
      printTimestamp(sb, "  Becomes sendable: ", b.getTimeToBecomeSendable());
      // sb.append("  Content:").append(b.getContent()).append('\n');
    }
    sb.append("Unsent blips: \n");
    for (BlipVersionView b : waveletView.getUnsentBlips()) {
      sb.append("- Blip Id: ").append(b.getBlipId()).append('\n');
      sb.append("  Blip version: ").append(b.getVersion()).append('\n');
      sb.append("  Participants:");
      StrUtil.join(sb, b.getParticipants(), ", ");
      sb.append('\n');
      printTimestamp(sb, "  First edited:     ", b.getFirstEditedTimestamp());
      printTimestamp(sb, "  Last changed:     ", b.getLastChangedTimestamp());
      printTimestamp(sb, "  Last submitted:   ", b.getLastSubmittedTimestamp());
      printTimestamp(sb, "  Becomes sendable: ", b.getTimeToBecomeSendable());
      sb.append("  Content:").append(b.getContent()).append('\n');
    }
  }

  private DateFormat dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  /**
   * Prints a timestamp (long value in milliseconds) in a readable format into a stringbuffer.
   * 
   * @param sb The stringbuffer to print to.
   * @param prefix The prefix string to be printed.
   * @param time The time value.
   */
  private void printTimestamp(StringBuilder sb, String prefix, Long time) {
    sb.append(prefix);
    if (time == null) {
      sb.append("null");
    } else if (time == 0) {
      sb.append("0");
    } else if (time == Long.MAX_VALUE) {
      sb.append("In the future, far, far away...");
    } else {
      sb.append(dateTimeFormatter.format(new Date(time)));
    }
    sb.append('\n');
  }
}
