from waveapi import events
from waveapi import model
from waveapi import robot

def onRobotAdded(properties, context):
  root_wavelet = context.GetRootWavelet()
  root_wavelet.CreateBlip().GetDocument().SetText("Hello, Emaily is here!")

if __name__ == '__main__':
  emailyRobot = robot.Robot('Emaily Robot',
      # Temporary email icon is taken from:
      # http://divasbluetinker.blogspot.com/2009/06/today-is-big-day.html
      image_url = 'http://1.bp.blogspot.com/_yNL0M57xbfg/Skr1tUorzkI/AAAAAAAAAIU/Texw5ki8Qw8/s320/EMAIL_ICON.jpg',
      version = '1',
      profile_url = 'http://emaily-wave.appspot.com')
  emailyRobot.RegisterHandler(events.WAVELET_SELF_ADDED, onRobotAdded)
  emailyRobot.Run()
