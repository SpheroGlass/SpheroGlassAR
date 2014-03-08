Drive Sphero with Google Glass using Augmented Reality
================

Sphero + Google Glass + AR mashup, drive a Sphero with Google Glass usign Augmented Reality.

Just point your Google Glass where you want Sphero to go!


Mode 1:

* Pair Glass and Sphero (see below)
* Install DriveSpheroGlass apk available at https://github.com/SpheroGlass/SpheroGlassAR/blob/master/target/SpheroGlassAR.apk?raw=true


Mode 2:

* Pair Glass and Sphero (see below)
* Clone this project: git@github.com:SpheroGlass/SpheroGlassAR.git
* Build and run the application in your Google Glass


Pairing your Sphero and Google Glass via bluetooth:
  
* Download settings apk from http://www.glassxe.com/2013/05/23/settings-apk-and-launcher2-apk-from-the-hacking-glass-session-at-google-io/
* Install settings apk: adb install Settings.apk
* Run settings: adb shell am start -n com.android.settings/com.android.settings.Settings (you can also run it via Launcher2.apk as explained in the link above)
