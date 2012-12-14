OpenWatch for Android
=================

A next generation [OpenWatch](http://openwatch.net) client for Android devices.

Downloading the Source
----------------------
When downloading the source make sure to clone the repository with:

    $ git clone git@github.com:OpenWatch/OpenWatch-Android.git
    
### Temporary Dependency
This project is currently configured to depend on a Library project [android-async-http](https://github.com/OnlyInAmerica/android-async-http). This is temporary as we contribute changes upstream to this project. To setup, clone the project, import "Existing Android Code" in Eclipse, right click on the OpenWatch project -> Android -> Libraries -> Add Class Folder -> Point to android-async-http root.


Running
----------------------

To run the software you'll need a device running Android 2.2+ with a camera and an ARMv7 processor with NEON support.

### Google Maps API v2

The new Google Maps API v2 requires you to piss yourself off registering each apk signing key you use (debug, production) with the [)Google API Console(https://code.google.com/apis/console]). 

On OSX:

1. cd ~/.android
2. keytool -list -v -keystore ./debug.keystore
3. when prompted for password, enter 'android'
4. copy the SHA1 fingerprint and append `:net.openwatch.reporter`
5. Follow [these steps](https://developers.google.com/maps/documentation/android/start#adding_the_api_key_to_your_application) to add the key to your Android application

TODO
----
+ Send available meta data during MediaCapture calls
+ Adjust bitrate of LQ stream per sendVideoChunk() network performance
+ 