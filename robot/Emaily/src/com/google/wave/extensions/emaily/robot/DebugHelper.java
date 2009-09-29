package com.google.wave.extensions.emaily.robot;

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
import com.google.wave.extensions.emaily.util.StringPrinter;

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
   * @param wavelet The wavelet to log.
   */
  public void DumpWaveletState(Wavelet wavelet) {
    StringPrinter sp = new StringPrinter();
    PrintWaveletInfo(sp, wavelet);
    PrintBlipInfo(sp, wavelet.getRootBlip());
    logger.log(Level.INFO, sp.toString());
  }

  /**
   * Appends details of a RobotMessageBundle to the given StringPrinter.
   * 
   * @param sp
   *          The StringPrinter to append info to.
   * @param bundle
   *          The RobotMessageBundle to dump.
   */
  public static void PrintRobotMessageBundleInfo(StringPrinter sp,
      RobotMessageBundle bundle) {
    sp.println("RobotMessageBundle info:");
    sp.catln("RobotAddress: ", bundle.getRobotAddress());
    sp.catln("Is new wave? ", bundle.isNewWave());
    sp.catln("Was self added? ", bundle.wasSelfAdded());
    sp.catln("Was self removed? ", bundle.wasSelfRemoved());
    List<EventType> types = new ArrayList<EventType>();
    for (Event event : bundle.getEvents())
      types.add(event.getType());
    sp.print("Events: ").joinln(types, ", ");
  }

  /**
   * Appends details of a Blip to the given StringPrinter.
   * 
   * @param sp
   *          The StringPrinter to append info to.
   * @param blip
   *          The Blip MessageBundle to dump.
   */
  public static void PrintBlipInfo(StringPrinter sp, Blip blip) {
    sp.catln("Blip info for blip ID: ", blip.getBlipId());
    sp.catln("Parent blip ID: ", blip.getParentBlipId());
    sp.catln("Creator: ", blip.getCreator());
    sp.catln("Last modified time: ", blip.getLastModifiedTime());
    sp.print("Contributors: ").joinln(blip.getContributors(), ", ");
    sp.catln("Document text:", blip.getDocument().getText());
    sp.print("Annotations: ").joinln(blip.getDocument().getAnnotations(), ", ");
    sp.print("Gadgets: ").joinln(
        blip.getDocument().getGadgetView().getGadgets(), ", ");
    sp.print("Children blip IDs: ").joinln(blip.getChildBlipIds(), ", ");
    for (Blip child : blip.getChildren())
      PrintBlipInfo(sp, child);
    sp.catln("End of blip info for blip ID: ", blip.getBlipId());
  }

  /**
   * Appends details of a Wavelet to the given StringPrinter.
   * 
   * @param sp
   *          The StringPrinter to append info to.
   * @param wavelet
   *          The Wavelet to dump.
   */
  public static void PrintWaveletInfo(StringPrinter sp, Wavelet wavelet) {
    sp.println("Wavelet info:");
    sp.catln("title: ", wavelet.getTitle());
    sp.catln("ID: ", wavelet.getWaveletId());
    sp.catln("Wave ID: ", wavelet.getWaveId());
    sp.catln("Creator: ", wavelet.getCreator());
    sp.catln("Creation time: ", wavelet.getCreationTime());
    sp.catln("Last modified time: ", wavelet.getLastModifiedTime());
    sp.catln("Version: ", wavelet.getVersion());
    sp.print("Participants: ").joinln(wavelet.getParticipants(), ", ");
    for (Map.Entry<String, String> entry : wavelet.getDataDocuments()
        .entrySet())
      sp.catln("Data document: ", entry.getKey(), " : ", entry.getValue());
    sp.catln("Root blip ID: ", wavelet.getRootBlipId());
  }

  // Injected dependencies
  private Logger logger;

  @Inject
  public void setLogger(Logger logger) {
    this.logger = logger;
  }
}
