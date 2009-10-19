package com.google.wave.extensions.emaily.robot;

import com.google.apphosting.api.ApiProxy;
import com.google.inject.Singleton;
import com.google.wave.api.ProfileServlet;

/**
 * Servlet to return the profile information for the robot.
 * 
 * @author dlux
 */
@Singleton
public class EmailyProfileServlet extends ProfileServlet {
  private static final long serialVersionUID = 8460374464245972812L;

  // This is a workaround for the erroneous VersionID returned through the environment.
  private static final int VERSION = (int) Float.parseFloat(ApiProxy.getCurrentEnvironment()
      .getVersionId());
  private static final String APPSPOT_ID = ApiProxy.getCurrentEnvironment().getAppId();
  private static final String APPSPOT_FULL_ID = VERSION + ".latest." + APPSPOT_ID;

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
    return "http://" + APPSPOT_FULL_ID + ".appspot.com/_wave/robot/profile";
  }
}
