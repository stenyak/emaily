package com.google.wave.extensions.emaily.robot;

import com.google.inject.Singleton;
import com.google.wave.api.ProfileServlet;

/**
 * Servlet to return the profile information for the robot.
 * @author dlux
 */
@Singleton
public class EmailyProfileServlet extends ProfileServlet {
  private static final long serialVersionUID = 8460374464245972812L;

  @Override
  public String getRobotAvatarUrl() {
    // TODO(dlux): Put this string to some config file.
    // TODO(dlux): Find someone who can create an avatar for this project.
    return "http://1.bp.blogspot.com/_yNL0M57xbfg/Skr1tUorzkI/AAAAAAAAAIU/Texw5ki8Qw8/s320/EMAIL_ICON.jpg";
  }

  @Override
  public String getRobotName() {
    // TODO(dlux): Put this string to some config file.
    return "Emaily Robot (Dev)";
  }

  @Override
  public String getRobotProfilePageUrl() {
    // TODO(dlux): Build this file from configuration and servlet context. It
    // did not work when I tried it.
    return "http://emaily-wave.appspot.com/_wave/robot/profile";
  }
}
