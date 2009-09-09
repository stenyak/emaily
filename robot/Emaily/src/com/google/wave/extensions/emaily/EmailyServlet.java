package com.google.wave.extensions.emaily;

import com.google.wave.api.*;

public class EmailyServlet extends AbstractRobotServlet {

	private static final long serialVersionUID = -9189126806024643150L;

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
