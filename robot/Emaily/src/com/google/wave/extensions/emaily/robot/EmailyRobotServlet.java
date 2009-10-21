package com.google.wave.extensions.emaily.robot;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import javax.servlet.http.HttpServletRequest;

import org.apache.james.mime4j.field.address.Address;
import org.apache.james.mime4j.field.address.Mailbox;
import org.apache.james.mime4j.message.Message;
import org.apache.james.mime4j.parser.MimeEntityConfig;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.wave.api.AbstractRobotServlet;
import com.google.wave.api.Annotation;
import com.google.wave.api.Blip;
import com.google.wave.api.Event;
import com.google.wave.api.EventType;
import com.google.wave.api.Range;
import com.google.wave.api.RobotMessageBundle;
import com.google.wave.api.StyleType;
import com.google.wave.api.StyledText;
import com.google.wave.api.TextView;
import com.google.wave.api.Wavelet;
import com.google.wave.extensions.emaily.config.HostingProvider;
import com.google.wave.extensions.emaily.data.BlipVersionView;
import com.google.wave.extensions.emaily.data.DataAccess;
import com.google.wave.extensions.emaily.data.WaveletView;
import com.google.wave.extensions.emaily.email.EmailSender;
import com.google.wave.extensions.emaily.email.MailUtil;
import com.google.wave.extensions.emaily.email.PersistentEmail;
import com.google.wave.extensions.emaily.scheduler.EmailScheduler;
import com.google.wave.extensions.emaily.util.DebugHelper;

@SuppressWarnings("serial")
@Singleton
public class EmailyRobotServlet extends AbstractRobotServlet {
  // Injected dependencies
  private final EmailSender emailSender;
  private HostingProvider hostingProvider;
  private final Provider<HttpServletRequest> reqProvider;
  private final PersistenceManagerFactory pmFactory;
  private final Logger logger;
  private final EmailScheduler emailScheduler;
  private final DebugHelper debugHelper;
  private final Provider<DataAccess> dataAccessProvider;

  @Inject
  public EmailyRobotServlet(EmailSender emailSender, HostingProvider hostingProvider,
      Provider<HttpServletRequest> reqProvider, PersistenceManagerFactory pmFactory, Logger logger,
      EmailScheduler emailScheduler, DebugHelper debugHelper,
      Provider<DataAccess> dataAccessProvider) {
    this.emailSender = emailSender;
    this.hostingProvider = hostingProvider;
    this.reqProvider = reqProvider;
    this.pmFactory = pmFactory;
    this.logger = logger;
    this.emailScheduler = emailScheduler;
    this.debugHelper = debugHelper;
    this.dataAccessProvider = dataAccessProvider;
  }

  /**
   * Handles all robot events.
   */
  @Override
  public void processEvents(RobotMessageBundle bundle) {
    // Old behavior.
    // TODO(dlux): To be removed when the new is working.
    for (Event event : bundle.getEvents()) {
      if (event.getType() == EventType.BLIP_SUBMITTED) {
        sendEmailNow(bundle, event);
      }
    }
    // New behavior:
    String proxyingFor = getProxyingFor();
    if (proxyingFor != null) {
      String email = hostingProvider.getEmailAddressFromRobotProxyFor(proxyingFor);
      processWaveletViewModifications(bundle, email);
    }
  }

  /**
   * Process modifications for the wavelet changes and schedule sending.
   * 
   * @param bundle The robot message bundle.
   * @param email The email of the user.
   */
  private void processWaveletViewModifications(RobotMessageBundle bundle, String email) {
    try {
      String waveletId = bundle.getWavelet().getWaveletId();
      WaveletView waveletView = dataAccessProvider.get().getWaveletView(waveletId, email);
      if (waveletView == null) {
        waveletView = new WaveletView(waveletId, email);
        dataAccessProvider.get().persistWaveletView(waveletView);
      }
      for (Event e : bundle.getEvents()) {
        processBlipEvent(waveletView, e);
      }
      emailScheduler.calculateWaveletViewNextSendTime(waveletView);
      logger.info(debugHelper.printWaveletViewInfo(waveletView));
    } catch (RuntimeException e) {
      dataAccessProvider.get().rollback();
      throw e;
    } finally {
      dataAccessProvider.get().close();
    }
  }

  private void processBlipEvent(WaveletView waveletView, Event e) {
    // We are dealing only with blip events.
    Blip blip = e.getBlip();
    if (blip == null) {
      return;
    }
    // find the corresponding blip in the unsent ones
    BlipVersionView blipVersionView = null;
    for (BlipVersionView b : waveletView.getUnsentBlips()) {
      if (blip.getBlipId().equals(b.getBlipId())) {
        blipVersionView = b;
        break;
      }
    }
    // If it did not exist yet, create one:
    if (blipVersionView == null) {
      blipVersionView = new BlipVersionView();
      blipVersionView.setBlipId(blip.getBlipId());
      blipVersionView.setParentId(waveletView.getId());
    }
    // Update the blip version to the latest
    blipVersionView.setVersion(blip.getVersion());
    // Store the text
    // TODO(dlux): add "title" handling.
    blipVersionView.setContent(blip.getDocument().getText());
    boolean still_editing = false;
    switch (e.getType()) {
    case BLIP_DELETED:
      blipVersionView.setContent("");
      break;
    case BLIP_SUBMITTED:
      break;
    default:
      still_editing = true;
      break;
    }
    emailScheduler.SetBlipViewTimes(blipVersionView, still_editing);
  }

  /**
   * Handle when a blip is submitted. Currenlty it sends the blip in email
   * immediately.
   * 
   * @param bundle RobotMessageBundle received from the Wave Server.
   * @param event The BLIP_SUBMITTED event.
   */
  private void sendEmailNow(RobotMessageBundle bundle, Event event) {
    // Get subject
    String emailSubject = bundle.getWavelet().getTitle();

    // Get body
    String emailBody;
    TextView textView = event.getBlip().getDocument();
    boolean isRootBlip = event.getBlip().getBlipId().equals(bundle.getWavelet().getRootBlipId());
    if (isRootBlip) {
      List<Annotation> titleAnnotations = textView.getAnnotations("conv/title");
      if (titleAnnotations.size() > 0) {
        Range subjectRange = titleAnnotations.get(0).getRange();
        emailBody = textView.getText().substring(subjectRange.getEnd());
      } else {
        emailBody = textView.getText();
      }
    } else {
      emailBody = textView.getText();
    }

    // Get sender email addresses
    String senderEmail = hostingProvider.getEmailAddressForWaveParticipantIdInEmailyDomain(event
        .getBlip().getCreator());

    // Get recipient address
    String proxyingFor = getProxyingFor();
    String recipient = hostingProvider.getEmailAddressFromRobotProxyFor(proxyingFor);
    emailSender.simpleSendTextEmail(senderEmail, recipient, emailSubject, emailBody);
  }

  private String getProxyingFor() {
    JSONObject json = (JSONObject) reqProvider.get().getAttribute("jsonObject");
    try {
      return json.getString("proxyingFor");
    } catch (JSONException e) {
      throw new RuntimeException("JSON error", e);
    }
  }

  /**
   * Processes the incoming emails accumulated so far: creates one new wave per
   * email.
   * 
   * @param wavelet Handle to create new waves.
   */
  // TODO(taton): Eliminate these @SuppressWarnings
  @SuppressWarnings( { "unchecked", "unused" })
  private void processIncomingEmails(Wavelet wavelet) {
    PersistenceManager pm = pmFactory.getPersistenceManager();
    try {
      Query query = pm.newQuery(PersistentEmail.class);
      List<PersistentEmail> emails = (List<PersistentEmail>) query.execute();
      for (PersistentEmail email : emails) {
        try {
          MimeEntityConfig config = new MimeEntityConfig();
          Message message = new Message(email.getInputStream(), config);
          createWaveFromMessage(wavelet, message);
        } catch (Exception exn) {
          logger.warning(exn.toString());
        }
        pm.deletePersistent(email);
      } // For loop

    } finally {
      pm.close();
    }
  }

  /**
   * Creates the Wave representing an email.
   * 
   * @param wavelet Handle to create new Waves.
   * @param message The incoming email to create the wave of.
   */
  private void createWaveFromMessage(Wavelet wavelet, Message message) {
    List<String> participants = new ArrayList<String>();
    // TODO(taton): this has to be solved some other way anyway
    participants.add("emaily-wave@appspot.com");
    if (message.getTo() != null) {
      // TODO(taton) The recipient should be inferred from the HTTP url.
      for (Address to : message.getTo()) {
        if (to instanceof Mailbox) {
          Mailbox mailbox = (Mailbox) to;
          String waveAddress = hostingProvider
              .getEmailAddressForWaveParticipantIdInEmailyDomain(mailbox.getAddress());
          participants.add(waveAddress);
        }
        // TODO(taton) Handle groups of addresses.
      }
    }
    if (participants.size() == 1) {
      logger.warning("Incoming email has no valid wave destination.");
      // For debugging, redirect the message to us.
      // TODO(taton) Remove this before the release.
      participants.add("christophe.taton@wavesandbox.com");
    }
    Wavelet newWavelet = wavelet.createWavelet(participants, null);
    newWavelet.setTitle(message.getSubject());
    Blip blip = newWavelet.getRootBlip();
    TextView textView = blip.getDocument();
    // TODO(taton) Use the proper Emaily address.
    textView.setAuthor("emaily-wave@appspot.com");

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
}
