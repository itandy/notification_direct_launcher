Notification Direct Launcher
===================================

I use Tasker to disable keyguard when I'm home. It works fine until when a notification comes, such as Whatsapp, and you click the notification to launch the app. Keyguard will be displayed and requires you to unlock the phone. I believe it's a security measure that the phone think you're still in the lockscreen and launching an app inside the lockscreen requires unlocking the phone which makes sense.

But that security measure completely defects the purpose of disabling keyguard in the first place. So I make this Xposed module to bypass the keyguard completely when you tap on notification messages and keyguard is disabled.

Again this module is based on AOSP source code but it also work on my Xperia phone so I expect it to work on some other phones too. Just try and see if it works on yours.

This is a Xposed module so your phone must be rooted to use it. The module is based on latest Xperia phone app. I tested it using my Sony Xperia ZR phone with 4.2.2 stock ROM.

Installation instructions:
1. Install the module
2. Run Xposed Installer and enable the module Avoid keyguard to display when launching notification activities if keyguard is disabled using 3rd party apps
3. Reboot the phone

