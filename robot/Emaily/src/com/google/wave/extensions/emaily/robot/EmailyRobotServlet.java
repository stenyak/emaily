package com.google.wave.extensions.emaily.robot;

import com.google.inject.Singleton;
import com.google.wave.api.AbstractRobotServlet;
import com.google.wave.api.Blip;
import com.google.wave.api.ElementType;
import com.google.wave.api.FormElement;
import com.google.wave.api.RobotMessageBundle;
import com.google.wave.api.TextView;
import com.google.wave.api.Wavelet;

@Singleton
public class EmailyRobotServlet extends AbstractRobotServlet {

	private static final long serialVersionUID = 8878209094937861353L;

	@Override
	public void processEvents(RobotMessageBundle bundle) {
		Wavelet wavelet = bundle.getWavelet();

		if (bundle.wasSelfAdded()) {
			Blip blip = wavelet.getRootBlip();
			TextView view = blip.getDocument();
			view.replace("Testing");
			for (int i = 0; i < 3; ++i) {
				view.append("\nTo: test" + Integer.toString(i) + "@example.com");
				view.appendElement(new FormElement(ElementType.CHECK, "remove" + Integer.toString(i), "Remove", "CHECKED"));
			}
			view.append("\n\nHello,\n\n\n");
			view.appendElement(new FormElement(ElementType.BUTTON, "emailsend", "Send Email"));
			view.append("   ");
			view.appendElement(new FormElement(ElementType.BUTTON, "not_email", "Not email"));
			view.append("   ");
			view.appendElement(new FormElement(ElementType.BUTTON, "add_bcc", "Add Bcc"));
		}
	}

}
