package com.google.wave.extensions.emaily.robot;

import java.util.ArrayList;
import java.util.List;

import org.apache.james.mime4j.field.address.Address;
import org.apache.james.mime4j.field.address.Mailbox;
import org.apache.james.mime4j.message.Message;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.wave.api.AbstractRobotServlet;
import com.google.wave.api.Annotation;
import com.google.wave.api.Blip;
import com.google.wave.api.Gadget;
import com.google.wave.api.RobotMessageBundle;
import com.google.wave.api.StyleType;
import com.google.wave.api.StyledText;
import com.google.wave.api.TextView;
import com.google.wave.api.Wavelet;
import com.google.wave.extensions.emaily.util.MailUtil;

@Singleton
public class EmailyRobotServlet extends AbstractRobotServlet {
  private static final long serialVersionUID = 8878209094937861353L;

  /**
   * Handles all robot events.
   */
  @Override
  public void processEvents(RobotMessageBundle bundle) {
    if (bundle.wasSelfAdded()) {
      handleSelfAdded(bundle);
    }
  }

  /**
   * Handles case when the robot is added to the wave.
   * 
   * @param bundle All information from the robot event.
   */
  private void handleSelfAdded(RobotMessageBundle bundle) {
    Wavelet wavelet = bundle.getWavelet();
    Blip blip = wavelet.getRootBlip();
    TextView view = blip.getDocument();
    // Find position for the gadget: Right after the conversation title if any
    // TODO(dlux): If there is no subject, then keep some space for that.
    int gadget_position = 0;
    for (Annotation a : view.getAnnotations("conv/title")) {
      if (a.getRange().getEnd() > gadget_position) {
        gadget_position = a.getRange().getEnd();
      }
    }
    // TODO(dlux): Use bundle.getRobotAddress when it is working.
    view.insertElement(gadget_position, new Gadget(
        "http://2.latest.emaily-wave.appspot.com"
            + "/gadgets/target-selection-gadget.xml"));
    debugHelper.DumpWaveletState(wavelet);
  }

  /**
   * Processes the emails accumulated so far: creates one new wave per email.
   * 
   * @param wavelet Handle to create new wavelets from.
   */
  private void sendEmails(Wavelet wavelet) {
    for (Message message : emails) {
      List<String> participants = new ArrayList<String>();
      participants.add("kryzthov@appspot.com");
      if (message.getTo() != null) {
        for (Address to : message.getTo()) {
          if (to instanceof Mailbox) {
            String waveAddress = MailUtil.getWaveAddress((Mailbox) to);
            if (waveAddress != null) {
              if (waveAddress.equals("christophe.taton@wavesandbox.com"))
                participants.add(waveAddress);
            }
          }
        }
      } else {
        participants.add("christophe.taton@wavesandbox.com");
      }
      Wavelet newWavelet = wavelet.createWavelet(participants, null);
      newWavelet.setTitle(message.getSubject());
      Blip blip = newWavelet.getRootBlip();
      TextView textView = blip.getDocument();
      textView.setAuthor("emaily@appspot.com");

      if (message.getFrom() != null) {
        // Note: appendStyledText has a bug: the style does not apply to the
        // last character.
        textView.appendStyledText(new StyledText("From: ", StyleType.BOLD));
        StringBuilder sb = new StringBuilder();
        for (Mailbox from : message.getFrom()) {
          if (from == null)
            continue;
          if (sb.length() > 0)
            sb.append(", ");
          sb.append(from.toString());
        }
        textView.append(sb.toString());
        textView.appendMarkup("<br/>\n");
      }
      if (message.getTo() != null) {
        textView.appendStyledText(new StyledText("To: ", StyleType.BOLD));
        StringBuilder sb = new StringBuilder();
        for (Address recipient : message.getTo()) {
          if (recipient == null)
            continue;
          if (sb.length() > 0)
            sb.append(", ");
          sb.append(recipient.toString());
        }
        textView.append(sb.toString());
        textView.appendMarkup("<br/>\n");
      }
      if (message.getBody() != null) {
        // TODO(taton) The Wave robot Java API is buggy when processing
        // end-of-lines. This still does not work properly.
        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        MailUtil.mimeEntityToText(sb, message);
        sb.append("\n");
        textView.append(sb.toString());
      }
    }
    emails.clear();
  }

  // Injected dependencies
  private DebugHelper debugHelper;

  @Inject
  public void setDebugHelper(DebugHelper debugHelper) {
    this.debugHelper = debugHelper;
  }
}
