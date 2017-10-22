# nearby-api-pub-sub-kotlin

This sample used as a base https://github.com/googlesamples/android-nearby/tree/master/messages/NearbyDevices

Generating Nearby API Key
-------------------------

This sample uses the Gradle build system. To build this project, use the
"gradlew build" command. Or, use "Import Project" in Android Studio.

To use this sample, follow the following steps:

1. Create a project on
[Google Developer Console](https://console.developers.google.com/). Or, use an
existing project.

2. Click on `APIs & auth -> APIs`, and enable `Nearby Messages API`.

3. Click on `Credentials`, then click on `Create new key`, and pick
`Android key`. Then register your Android app's SHA1 certificate
fingerprint and package name for your app. Use
`com.google.android.gms.nearby.messages.samples.nearbydevices`
for the package name.

4. Copy the API key generated, and replace/paste it in `AndroidManifest.xml`.
