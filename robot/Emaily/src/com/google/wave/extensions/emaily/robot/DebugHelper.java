package com.google.wave.extensions.emaily.robot;

import static com.google.wave.extensions.emaily.util.StrUtil.join;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.wave.api.Blip;
import com.google.wave.api.Event;
import com.google.wave.api.EventType;
import com.google.wave.api.RobotMessageBundle;
import com.google.wave.api.Wavelet;

/**
 * Utility functions for debugging Wave and Robot interactions
 * 
 * @author dlux
 */
@Singleton
public class DebugHelper {

  /**
   * Logs details of a wavelet to the INFO log.
   * 
   * @param wavelet
   *          The wavelet to log.
   */
  public void DumpWaveletState(Wavelet wavelet) {
    StringBuilder sp = new StringBuilder();
    PrintWaveletInfo(sp, wavelet);
    PrintBlipInfo(sp, wavelet.getRootBlip());
    logger.log(Level.INFO, sp.toString());
  }

  /**
   * Appends details of a RobotMessageBundle to the given StringBuilder.
   * 
   * @param sp
   *          The StringBuilder to append info to.
   * @param bundle
   *          The RobotMessageBundle to dump.
   */
  public static void PrintRobotMessageBundleInfo(StringBuilder sp,
      RobotMessageBundle bundle) {
    sp.append("RobotMessageBundle info:\n");
    sp.append("RobotAddress: ").append(bundle.getRobotAddress()).append("\n");
    sp.append("Is new wave? ").append(bundle.isNewWave()).append("\n");
    sp.append("Was self added? ").append(bundle.wasSelfAdded()).append("\n");
    sp.append("Was self removed? ").append(bundle.wasSelfRemoved())
        .append("\n");
    List<EventType> types = new ArrayList<EventType>();
    for (Event event : bundle.getEvents())
      types.add(event.getType());
    sp.append("Events: ").append(join(types, ", ")).append("\n");
  }

  /**
   * Appends details of a Blip to the given StringBuilder.
   * 
   * @param sp
   *          The StringBuilder to append info to.
   * @param blip
   *          The Blip MessageBundle to dump.
   */
  public static void PrintBlipInfo(StringBuilder sp, Blip blip) {
    sp.append("Blip info for blip ID: ").append(blip.getBlipId()).append("\n");
    sp.append("Parent blip ID: ").append(blip.getParentBlipId()).append("\n");
    sp.append("Creator: ").append(blip.getCreator()).append("\n");
    sp.append("Last modified time: ").append(blip.getLastModifiedTime())
        .append("\n");
    sp.append("Contributors: ").append(join(blip.getContributors(), ", "))
        .append("\n");
    sp.append("Document text:").append(blip.getDocument().getText()).append(
        "\n");
    sp.append("Annotations: ").append(
        join(blip.getDocument().getAnnotations(), ", ")).append("\n");
    sp.append("Gadgets: ").append(
        join(blip.getDocument().getGadgetView().getGadgets(), ", ")).append(
        "\n");
    sp.append("Children blip IDs: ").append(join(blip.getChildBlipIds(), ", "))
        .append("\n");
    for (Blip child : blip.getChildren())
      PrintBlipInfo(sp, child);
    sp.append("End of blip info for blip ID: ").append(blip.getBlipId())
        .append("\n");
  }

  /**
   * Appends details of a Wavelet to the given StringBuilder.
   * 
   * @param sp
   *          The StringBuilder to append info to.
   * @param wavelet
   *          The Wavelet to dump.
   */
  public static void PrintWaveletInfo(StringBuilder sp, Wavelet wavelet) {
    sp.append("Wavelet info:");
    sp.append("title: ").append(wavelet.getTitle()).append("\n");
    sp.append("ID: ").append(wavelet.getWaveletId()).append("\n");
    sp.append("Wave ID: ").append(wavelet.getWaveId()).append("\n");
    sp.append("Creator: ").append(wavelet.getCreator()).append("\n");
    sp.append("Creation time: ").append(wavelet.getCreationTime()).append("\n");
    sp.append("Last modified time: ").append(wavelet.getLastModifiedTime())
        .append("\n");
    sp.append("Version: ").append(wavelet.getVersion()).append("\n");
    sp.append("Participants: ").append(join(wavelet.getParticipants(), ", "))
        .append("\n");
    for (Map.Entry<String, String> entry : wavelet.getDataDocuments()
        .entrySet())
      sp.append("Data document: ").append(entry.getKey()).append(" : ").append(
          entry.getValue()).append("\n");
    sp.append("Root blip ID: ").append(wavelet.getRootBlipId()).append("\n");
  }

  // Injected dependencies
  private Logger logger;

  @Inject
  public void setLogger(Logger logger) {
    this.logger = logger;
  }
}
