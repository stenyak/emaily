package com.google.wave.extensions.emaily.robot;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.wave.api.Annotation;
import com.google.wave.api.Blip;
import com.google.wave.api.Gadget;
import com.google.wave.api.TextView;
import com.google.wave.api.Wavelet;

/**
 * Utility functions for debugging Wave and Robot interactions
 * @author dlux
 */
@Singleton
public class DebugHelper {
	
	// TODO(dlux): Add more information from the wavelet.
	public void DumpWaveletState(Wavelet wavelet) {
		Blip blip = wavelet.getRootBlip();
		TextView view = blip.getDocument();
		StringBuilder waveletState = new StringBuilder();
		waveletState.append("Blip content:");
		waveletState.append(view.getText());
		waveletState.append("\nAnnotations:\n");
		for (Annotation a: view.getAnnotations()) {
			waveletState.append("- ");
			waveletState.append(a.toString());
			waveletState.append("\n");
		}
		waveletState.append("Gadgets:\n");
		for (Gadget g: view.getGadgetView().getGadgets()) {
			waveletState.append("- ");
			waveletState.append(g.toString());
			waveletState.append("\n");
		}
		// TODO(dlux): level.INFO is not visible in appengine logs, find out why and use that level.
		logger.log(Level.WARNING, waveletState.toString());
	}

	// Injected dependencies
	private Logger logger;
	
	@Inject
	public void setLogger(Logger logger) {
		this.logger = logger;
	}
}
