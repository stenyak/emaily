package com.google.wave.extensions.emaily.robot;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.wave.api.AbstractRobotServlet;
import com.google.wave.api.Annotation;
import com.google.wave.api.Blip;
import com.google.wave.api.Gadget;
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
			handleSelfAdded(wavelet);
		}
	}

	private void handleSelfAdded(Wavelet wavelet) {
		Blip blip = wavelet.getRootBlip();
		TextView view = blip.getDocument();
		// Find position for the gadget: Right after the conversation title if any
		int gadget_position = 0;
		for (Annotation a: view.getAnnotations("conv/title")) {
			if (a.getRange().getEnd() > gadget_position) {
				gadget_position = a.getRange().getEnd();
			}
		}
		view.insertElement(gadget_position, new Gadget("http://2.latest.emaily-wave.appspot.com/gadgets/target-selection-gadget.xml"));
		StringBuilder blipState = new StringBuilder();
		blipState.append("Blip content:");
		blipState.append(view.getText());
		blipState.append("\nAnnotations:\n");
		for (Annotation a: view.getAnnotations()) {
			blipState.append("- ");
			blipState.append(a.toString());
			blipState.append("\n");
		}
		logger.log(Level.WARNING, blipState.toString());
	}

	// Injected dependencies
	private Logger logger;
	
	@Inject
	public void setLogger(Logger logger) {
		this.logger = logger;
	}

}
