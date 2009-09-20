package com.google.wave.extensions.emaily.web;

import com.google.inject.servlet.ServletModule;
import com.google.wave.extensions.emaily.robot.EmailyProfileServlet;
import com.google.wave.extensions.emaily.robot.EmailyRobotServlet;

public class EmailyServletModule extends ServletModule {
	@Override
	protected void configureServlets() {
		super.configureServlets();
		serve("/_wave/robot/jsonrpc").with(EmailyRobotServlet.class);
		serve("/_wave/robot/profile").with(EmailyProfileServlet.class);
	}
}
