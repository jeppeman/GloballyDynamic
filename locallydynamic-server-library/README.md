# locallydynamic-server-library

A portable http server that receives bundles and uses the <a href="https://developer.android.com/studio/command-line/bundletool">bundletool</a> to generate and serve split APK:s to clients that are using the
<a href="../locallydynamic-android-lib">LocallyDynamic Android library</a>. <br/>
It can be embedded into an application, or run standalone on any system that has java installed. 

Usage
---
### Launching the server as a standalone application from the command line
First build an executable jar as follows:
```shell script
./gradlew executableJar
```
Then run it from the command line with `java -jar path/to/executable/jar.jar [options]`.<br/> 
The following command line arguments can be given:

| Flag | Values | 
| :------------- | :--------- | 
| <kbd>--port</kbd> - the port to start the server on <br/><br/>**Note**: when set to 0, the server will be assigned a random available port | **Acceptable Values:** <kbd>int</kbd> > 0 <br/><br/>**Default Value:** <kbd>0</kbd>|
| <kbd>--username</kbd> - the username required to make requests to the server <br/><br/>**Note**: used in combination with <kbd>password</kbd> for basic auth - if left empty, no username will be required for clients | **Acceptable Values:** <kbd>string</kbd> - e.g. `myusername`|
| <kbd>--password</kbd> - the password required to make requests to the server <br/><br/>**Note**: used in combination with <kbd>username</kbd> for basic auth - if left empty, no password will be required for clients | **Acceptable Values:** <kbd>string</kbd> - e.g. `mypassword`|
| <kbd>--https-redirect</kbd> - whether or not redirect http requests to https | **Acceptable Values:** <kbd>true</kbd> <kbd>false</kbd><br/><br/>**Default Value:** <kbd>false</kbd>|
| <kbd>--storage-backend</kbd> - the storage backend to used for storing uploaded bundles | **Acceptable Values:** <ul><li><kbd>local</kbd> - store bundles locally on the machine where the server is running.</li><li><kbd>gcp</kbd> - store bundles in Google Cloud Storage.</li></ul>**Default Value:** <kbd>local</kbd> |
| <kbd>--local-storage-path</kbd> - the path on the machine to store bundle at <br/><br/>**Note**: used in combination with <kbd>--storage-backend local</kbd> - has no effect otherwise. If a relative path is given, bundles will be stored relative to where the application was started from | **Acceptable Values:** <kbd>string</kbd> - e.g. `/home/me/locallydynamic`|
| <kbd>--gcp-bucket-id</kbd> - the id of the bucket ot use in Google Cloud Storage <br/><br/>**Note**: used in combination with <kbd>--storage-backend gcp</kbd> - has no effect otherwise. | **Acceptable Values:** <kbd>string</kbd> - e.g. `my-bucket-id`|

The options can also be picked up through the following environment variables:
```shell script
LOCALLY_DYNAMIC_PORT=<port>
LOCALLY_DYNAMIC_USERNAME=<username>
LOCALLY_DYNAMIC_PASSWORD=<password>
LOCALLY_DYNAMIC_HTTPS_REDIRECT=<https-redirect>
LOCALLY_DYNAMIC_STORAGE_BACKEND=<storage-backend>
LOCALLY_DYNAMIC_LOCAL_STORAGE_PATH=<local-storage-path>
LOCALLY_DYNAMIC_GCP_BUCKET_ID=<gcp-bucket-id>
```
The command line arguments take precedence over environment variables.<br/><br/>
**Example**
```shell script
# With a local storage backend
java -jar server/build/libs/locallydynamic-server-0.1-SNAPSHOT-standalone.jar
    --port 8080 \
    --username johndoe \
    --password my-secret-password \
    --https-redirect false \
    --storage-backend local \
    --local-storage-path /myapp/bundles
   
# With a gcp storage backend
java -jar server/build/libs/locallydynamic-server-0.1-SNAPSHOT-standalone.jar
    --port 8080 \
    --username johndoe \
    --password my-secret-password \
    --https-redirect false \
    --storage-backend gcp \
    --gcp-bucket-id my-bucket-id 
```

### Embedding a server into an application
You can also embed the server into your application (this is done in the <a href="../locallydynamic-studio-plugin">Android Studio plugin</a>),
to do so, add the following to your `build.gradle`:
```groovy
repositories {
    mavenCentral()
}

dependencies {
    implementation 'com.jeppeman.locallydynamic.server:server:0.3' 
}
```
**Or snapshot**
```groovy
repositories {
    maven {
        url "https://oss.sonatype.org/content/repositories/snapshots/"
    }
}

dependencies {
    implementation 'com.jeppeman.locallydynamic.server:server:0.4-SNAPSHOT' 
}
```

Then use the api as follows:
```kotlin
import com.jeppeman.locallydynamic.server.LocalStorageBackend
import com.jeppeman.locallydynamic.server.LocallyDynamicServer
import com.jeppeman.locallydynamic.server.Logger
import java.nio.file.Paths

// Can be any class implementing the com.jeppeman.locallydynamic.server.StorageBackend interface
val storageBackend = LocalStorageBackend.builder()
    .setBaseStoragePath(Paths.get("bundles"))
    .build()

// Can be any class implementing the com.jeppeman.locallydynamic.server.Logger interface
val logger = Logger()

val configuration = LocallyDynamicServer.Configuration.builder()
    .setPort(8080)
    .setHttpsRedirect(false)
    .setStorageBackend(storageBackend)
    .setUsername("username")
    .setPassword("password")
    .setLogger(logger)
    .build()

val server = LocallyDynamicServer(configuration)
server.start()
```

### Running the server in GCP
See <a href="app.yaml">app.yaml</a> for an example of how to run the server on Google App Engine - it could
be run similarly on any cloud provider, in this case the server configuration is provided through the previously mentioned environment
variables.

Endpoints
---
The endpoints that the server exposes are listed below, all are protected by the credentials provided in the configuration (if any).

### Uploading a bundle to the server (`/upload`)
App bundle uploading is done automatically by the <a href="../locallydynamic-gradle-plugin">gradle plugin</a> when building an app bundle - but in case you want to upload manually, POST to the `/upload` endpoint, see <a href="../locallydynamic-gradle-plugin/plugin/src/main/java/com/jeppeman/locallydynamic/gradle/UploadBundleTask.kt">UploadBundleTask.kt</a> for an example.
The request needs to have the header `Content-Type: multipart/form-data` set, and the body needs to contain the following parts:

| Name | Values | 
| :------------- | :--------- | 
| <kbd>bundle</kbd> - the bundle to upload | **Acceptable Values:** <kbd>application/octet-stream</kbd> - the byte stream of the bundle to upload |
| <kbd>application-id</kbd> - the application id of the bundle to upload | **Acceptable Values:** <kbd>text/plain</kbd> - e.g. `com.example.locallydynamic`|
| <kbd>version</kbd> - the version code of the bundle to upload | **Acceptable Values:** <kbd>text/plain</kbd> - e.g. `24` (versionCodeInt) |
| <kbd>variant</kbd> - the variant that was used to build the bundle to upload | **Acceptable Values:** <kbd>text/plain</kbd> - e.g. `debug`, `release` or `freeDebug` etc.. |
| <kbd>keystore</kbd> - the keystore to sign APK:s in the bundle with | **Acceptable Values:** <kbd>application/octet-stream</kbd> - the byte stream of the keystore to sign APK:s in the bundle with, e.g. the debug.keystore |
| <kbd>signing-config</kbd> - the signing config for the keystore to sign APK:s in the bundle with | **Acceptable Values:** <kbd>application/json</kbd> - needs to have the following structure: <br/><br/> `{ "storePassword": "my-secret-store-password", "keyAlias": "my-key-alias", "keyPassword": "my-secret-key-password"}`|

### Registering a device on the server (`/register`)
In order to download split APK:s, a client needs to register itself with a <a href="https://developer.android.com/studio/command-line/bundletool#manually_create_json">device specification</a>. <br/>
This is done automatically by the <a href="../locallydynamic-android-lib">android library</a> - but in case you need to do it manually, POST to the `/register` end point with an `application/json; charset=utf8` body containing 
the device specification of the device you want to register, e.g.
```json
{
  "supportedAbis": ["arm64-v8a", "armeabi-v7a"],
  "glExtensions": ["GL_IMAGE"],
  "deviceFeatures": ["android.hardware.camera"],
  "supportedLocales": ["en", "fr"],
  "screenDensity": 640,
  "sdkVersion": 27
}
```
The response will be a `text/plain` containing a UUID that needs to be attached when downloading split APK:s e.g. `123e4567-e89b-12d3-a456-426655440000`.
<br/><br/>
**Note**: a new registration request needs to be sent before every download request as device specifications are cleaned up
on the server immediately after downloading

### Downloading split APK:s (`/download`)
Downloading of split APK:s that have been previously uploaded to the server is done automatically by the <a href="../locallydynamic-android-lib">android library</a> - but in case you need to do it manually, GET from the 
`/download` endpoint, the request is specified with the following query parameters:

| Name | Required | Values | 
| :--------- | :--------- | :--------- | 
| <kbd>device-id</kbd> - the device id that was previously received from a call to `/register` | Yes | **Acceptable Values:** <kbd>string</kbd> - e.g. `123e4567-e89b-12d3-a456-426655440000` |
| <kbd>application-id</kbd> - the application id of the bundle to download split APK:s from | Yes | **Acceptable Values:** <kbd>string</kbd> - e.g. `com.example.locallydynamic`|
| <kbd>version</kbd> - the version code of the bundle to download split APK:s from | Yes | **Acceptable Values:** <kbd>number</kbd> - e.g. `24` (versionCodeInt) |
| <kbd>variant</kbd> - the variant that was used to build the bundle to download split APK:s from | Yes | **Acceptable Values:** <kbd>string</kbd> - e.g. `debug`, `release` or `freeDebug` etc.. |
| <kbd>throttle</kbd> - the amount of time in ms to throttle the downloading of split APK:s by | No | **Acceptable Values:** <kbd>number</kbd> - e.g. `5000` - will throttle for 5 seconds |
| <kbd>features</kbd> - the dynamic features from the bundle to download <br/><br/>**Note**: either this or <kbd>languages</kbd> need to be included in the request | No | **Acceptable Values:** <kbd>string</kbd> - e.g. `myfeature,myotherfeature` |
| <kbd>languages</kbd> - the languages from the bundle to download <br/><br/>**Note**: either this or <kbd>features</kbd> need to be included in the request | No | **Acceptable Values:** <kbd>string</kbd> - e.g. `de,it,fr` |

**Example**:<br/>
`/download?device-id=f94498a5-353e-4c9c-88e9-6bf86809698d&variant=debug&version=1&application-id=com.example.locallydynamic&throttle=1000&languages=ko,features=myfeature`