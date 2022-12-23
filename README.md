# Open Radio #

### What is this ? ###

* **Open Radio** is the project which is use [Android Auto SDK](http://developer.android.com/auto/index.html), innovative tool to bring Android Apps into vehicle's **Human Machine Interface (HMI)**, to provide live streaming of the world wide Radio Stations into the vehicle over Android mobile. As of end of 2019, Open Radio supported on Android TV. This is new feature and functionality is very limited. Updates will come in 2020!
* This project is use [Community Radio Browser's API](http://www.radio-browser.info) - a service that provides a list of radio stations broadcasting their live stream on the Internet.
* Graphics are provided by [Free Iconset: Beautiful Flat Mono Color Icons by Elegantthemes](http://www.iconarchive.com/show/beautiful-flat-one-color-icons-by-elegantthemes.html)
* Playlist parser is provided by [William Seemann](https://github.com/wseemann/JavaPlaylistParser)
* Playback powered by [Exo Player](https://github.com/google/ExoPlayer)
* Offline countries boundaries are provided by [Tobias Zwick](https://github.com/westnordost/countryboundaries)
* Android requirements : Android 4.2 (API level 17) (new APIs for implementing audio playback that is compatible with Auto) or newer.

### Permissions used ###

* INTERNET - To access internet connection.
* ACCESS_NETWORK_STATE - To monitor Internet connection state, detect connect and reconnect states.
* WAKE_LOCK - To keep screen on while playing Radio Station.
* ACCESS_COARSE_LOCATION - On user's demand only - to select Country for user based on Location. This helps to navigate local Radio Stations.
* READ_EXTERNAL_STORAGE (Android 12 and older), READ_MEDIA_IMAGES (Android 13 and newer) - On user's demand only - to read image from phone's memory when set it as image for Local Radio Station.
* FOREGROUND_SERVICE - To keep service active while playing stream.
* BLUETOOTH, BLUETOOTH_ADMIN, BLUETOOTH_CONNECT - On user's demand only - to handle connection with a Bluetooth device.
* RECORD_AUDIO - On user's demand only - to use voice search engine on Android TV.

### Delivery files ###

* [Google Play](https://play.google.com/store/apps/details?id=com.yuriy.openradio) - this application is suitable now for the Android Media Browser simulator as well as for the Android Auto.
* [APK files](https://bitbucket.org/ChernyshovYuriy/openradio/src/master/app/legacy/) - use pure APK file if Google Play is not available on your device.

### How to install and run ###
* There is a possibility to run application as general Android one, just to be sure that everything works as expected.
* **But the main feature is that application is fully compatible with vehicle's system.**

In order to run application just like it does on vehicle it is necessary to install [Android Auto for Mobile](https://play.google.com/store/apps/details?id=com.google.android.projection.gearhead&hl=en). Once it is done - swipe from the left side and select **Open Radio** application from the list. 
**Enjoy!**