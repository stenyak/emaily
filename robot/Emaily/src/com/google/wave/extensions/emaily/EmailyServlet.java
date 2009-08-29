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
			view.insertElement(0, new FormElement(ElementType.BUTTON, "Button", "button1", "button1"));
			view.insertElement(0, new FormElement(ElementType.CHECK, "Check", "check1", "check1"));
			view.insertElement(0, new FormElement(ElementType.INPUT, "Input", "input1", "input1"));
			view.insertElement(0, new FormElement(ElementType.LABEL, "Label", "label1", "label1"));
		}

		for (Event e : bundle.getEvents()) {
			if (e.getType() == EventType.WAVELET_PARTICIPANTS_CHANGED) {
				Blip blip = wavelet.appendBlip();
				TextView textView = blip.getDocument();
				textView.append("Hi, everybody!");
			}
		}
	}
}
