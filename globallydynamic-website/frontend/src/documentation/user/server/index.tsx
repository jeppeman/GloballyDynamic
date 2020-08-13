import React from "react"
import {Box, Container} from "@material-ui/core";
import ReactMarkdown from "react-markdown/with-html";
import CodeBlock from "../../../components/CodeBlock"
import LinkRenderer from "../../../components/LinkRenderer";

const markdown = `
GloballyDynamic Server Library
---
[![Maven Central](https://img.shields.io/maven-central/v/com.jeppeman.globallydynamic.server/server.svg?label=maven%20central&color=green)](https://search.maven.org/artifact/com.jeppeman.globallydynamic.server/server)

A portable http server that receives [app bundles](https://developer.android.com/platform/technology/app-bundle) and uses [bundletool](https://developer.android.com/studio/command-line/bundletool) to generate and serve split APK:s to clients that are using the
[GloballyDynamic Android library](./android). <br/>
It can be embedded into a java application, or run standalone on any system that has java installed. 

It comes with 3 different options for how to store bundles:
* Locally on the machine where the server is running
* On [Google Cloud Storage](https://cloud.google.com/storage)
* On [Amazon S3](https://aws.amazon.com/s3/)

Bundles on the server are identified by 3 variables: 

1. Application id (e.g. com.example.myapplication) 
2. Build variant (e.g. amazonRelease)
3. Version code (e.g. 23)

You can therefore have multiple versions of the same application + version code combination active at the same time,
i.e. one for each build variant.

Code
---
[https://github.com/jeppeman/GloballyDynamic/tree/master/globallydynamic-server-lib](https://github.com/jeppeman/GloballyDynamic/tree/master/globallydynamic-server-lib)

Usage
---
### Launching the server as a standalone application from the command line
First build an executable jar as follows:
\`\`\`shell
# Clone the repo
git clone https://github.com/jeppeman/GloballyDynamic && cd GloballyDynamic/globallydynamic-server-lib

# Build an executable jar
./gradlew executableJar -PoutputDir=$(pwd) -ParchiveName=globallydynamic-server.jar
\`\`\`
Then run it from the command line with \`java -jar globallydynamic-server.jar [options]\`.<br/> 
The following command line arguments can be given:

| Flag | Values | 
| :------------- | :--------- | 
| <kbd>--port</kbd> - the port to start the server on <br/><br/>**Note**: when set to 0, the server will be assigned a random available port | **Acceptable Values:** <kbd>int</kbd> > 0 <br/><br/>**Default Value:** <kbd>0</kbd>|
| <kbd>--username</kbd> - the username required to make requests to the server <br/><br/>**Note**: used in combination with <kbd>password</kbd> for basic auth - if left empty, no username will be required for clients | **Acceptable Values:** <kbd>string</kbd> - e.g. \`myusername\`|
| <kbd>--password</kbd> - the password required to make requests to the server <br/><br/>**Note**: used in combination with <kbd>username</kbd> for basic auth - if left empty, no password will be required for clients | **Acceptable Values:** <kbd>string</kbd> - e.g. \`mypassword\`|
| <kbd>--https-redirect</kbd> - whether or not redirect http requests to https | **Acceptable Values:** <kbd>true</kbd> <kbd>false</kbd><br/><br/>**Default Value:** <kbd>false</kbd>|
| <kbd>--override-existing-bundles</kbd> - whether or not to replace existing bundles when uploading, i.e. bundles with the same version as the one being uploaded. | **Acceptable Values:** <kbd>true</kbd> <kbd>false</kbd><br/><br/>**Default Value:** <kbd>true</kbd>|
| <kbd>--validate-signature-on-download</kbd> - whether or not to validate the signature of the application that wants to download splits | **Acceptable Values:** <kbd>true</kbd> <kbd>false</kbd><br/><br/>**Default Value:** <kbd>false</kbd>|
| <kbd>--storage-backend</kbd> - the storage backend to used for storing uploaded bundles | **Acceptable Values:** <ul><li><kbd>local</kbd> - store bundles locally on the machine where the server is running.</li><li><kbd>gcp</kbd> - store bundles in Google Cloud Storage.</li><li><kbd>s3</kbd> - store bundles in Amazon S3.</li></ul>**Default Value:** <kbd>local</kbd> |
| <kbd>--local-storage-path</kbd> - the path on the machine to store bundle at <br/><br/>**Note**: used in combination with <kbd>--storage-backend local</kbd> - has no effect otherwise. If a relative path is given, bundles will be stored relative to where the application was started from | **Acceptable Values:** <kbd>string</kbd> - e.g. \`/home/me/globallydynamic\`|
| <kbd>--gcp-bucket-id</kbd> - the id of the bucket ot use in Google Cloud Storage <br/><br/>**Note**: used in combination with <kbd>--storage-backend gcp</kbd> - has no effect otherwise.<br/>**Note**: if the server is not running on GCP, you will need to set the \`GOOGLE_APPLICATION_CREDENTIALS\` environment variable in order to authenticate with GCP, see more [here](https://cloud.google.com/docs/authentication/getting-started#setting_the_environment_variable). | **Acceptable Values:** <kbd>string</kbd> - e.g. \`my-bucket-id\`|
| <kbd>--s3-bucket-id</kbd> - the id of the bucket ot use in S3 <br/><br/>**Note**: used in combination with <kbd>--storage-backend s3</kbd> - has no effect otherwise.<br/>**Note**: if the server is not running on AWS, you will need to set the following environment variables in order to authenticate with AWS: <ul><li>\`AWS_ACCESS_KEY_ID\`</li><li>\`AWS_SECRET_KEY\`</li><li>\`AWS_REGION\`</li></ul> see more [here](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/setup-credentials.html). | **Acceptable Values:** <kbd>string</kbd> - e.g. \`my-bucket-id\`|

The options can also be picked up through the following environment variables:
\`\`\`shell script
GLOBALLY_DYNAMIC_PORT=<port>
GLOBALLY_DYNAMIC_USERNAME=<username>
GLOBALLY_DYNAMIC_PASSWORD=<password>
GLOBALLY_DYNAMIC_HTTPS_REDIRECT=<https-redirect>
GLOBALLY_DYNAMIC_OVERRIDE_EXISTING_BUNDLES=<override-existing>
GLOBALLY_DYNAMIC_VALIDATE_SIGNATURE_ON_DOWNLOAD=<validate-on-download>
GLOBALLY_DYNAMIC_STORAGE_BACKEND=<storage-backend>
GLOBALLY_DYNAMIC_LOCAL_STORAGE_PATH=<local-storage-path>
GLOBALLY_DYNAMIC_GCP_BUCKET_ID=<gcp-bucket-id>
GLOBALLY_DYNAMIC_S3_BUCKET_ID=<s3-bucket-id>
\`\`\`
The command line arguments take precedence over environment variables.<br/><br/>
**Example**
\`\`\`shell script
# With a local storage backend
java -jar globallydynamic-server.jar \\
    --port 8080 \\
    --username johndoe \\
    --password my-secret-password \\
    --https-redirect false \\
    --override-existing-bundles false \\
    --validate-signature-on-download true \\
    --storage-backend local \\
    --local-storage-path /myapp/bundles
   
# With a gcp storage backend
java -jar globallydynamic-server.jar \\
    --port 8080 \\
    --username johndoe \\
    --password my-secret-password \\
    --https-redirect false \\
    --override-existing-bundles false \\
    --validate-signature-on-download true \\
    --storage-backend gcp \\
    --gcp-bucket-id my-bucket-id 
    
# With an s3 storage backend
java -jar globallydynamic-server.jar \\
    --port 8080 \\
    --username johndoe \\
    --password my-secret-password \\
    --https-redirect false \\
    --override-existing-bundles false \\
    --validate-signature-on-download true \\
    --storage-backend s3 \\
    --s3-bucket-id my-bucket-id 
\`\`\`

### Embedding a server into a java application
You can also embed the server into your java application (this is done in the [Android Studio plugin](./studio)),
to do so, add the following to your \`build.gradle\`:
\`\`\`groovy
repositories {
    mavenCentral()
}

dependencies {
    implementation 'com.jeppeman.globallydynamic.server:server:1.0' 
}
\`\`\`
**Or snapshots**
\`\`\`groovy
repositories {
    maven {
        url "https://oss.sonatype.org/content/repositories/snapshots/"
    }
}
\`\`\`

Then use the api as follows:
\`\`\`kotlin
import com.jeppeman.globallydynamic.server.LocalStorageBackend
import com.jeppeman.globallydynamic.server.GloballyDynamicServer
import com.jeppeman.globallydynamic.server.Logger
import java.nio.file.Paths

// Can be any class implementing the com.jeppeman.globallydynamic.server.StorageBackend interface
val storageBackend = LocalStorageBackend.builder()
    .setBaseStoragePath(Paths.get("bundles"))
    .build()

// Can be any class implementing the com.jeppeman.globallydynamic.server.Logger interface
val logger = Logger()

val configuration = GloballyDynamicServer.Configuration.builder()
    .setPort(8080)
    .setHttpsRedirect(false)
    .setStorageBackend(storageBackend)
    .setOverrideExistingBundles(false)
    .setValidateSignatureOnDownload(true)
    .setUsername("username")
    .setPassword("password")
    .setLogger(logger)
    .build()

val server = GloballyDynamicServer(configuration)
server.start()
\`\`\`

Endpoints
---
The endpoints that the server exposes are listed below, all are protected by the credentials provided in the configuration (if any).

### Uploading a bundle to the server (\`/upload\`)
App bundle uploading is done automatically by the [gradle plugin](./gradle) when building an app bundle - but in case you want to upload manually, 
POST to the \`/upload\` endpoint, see [UploadBundleTask.kt](https://github.com/jeppeman/GloballyDynamic/blob/master/globallydynamic-gradle-plugin/plugin/src/main/java/com/jeppeman/globallydynamic/gradle/UploadBundleTask.kt) for an example.
The request needs to have the header \`Content-Type: multipart/form-data\` set, and the body needs to contain the following parts:

| Name | Values | 
| :------------- | :--------- | 
| <kbd>bundle</kbd> - the bundle to upload | **Acceptable Values:** <kbd>application/octet-stream</kbd> - the byte stream of the bundle to upload |
| <kbd>application-id</kbd> - the application id of the bundle to upload | **Acceptable Values:** <kbd>text/plain</kbd> - e.g. \`com.example.globallydynamic\`|
| <kbd>version</kbd> - the version code of the bundle to upload | **Acceptable Values:** <kbd>text/plain</kbd> - e.g. \`24\` (versionCodeInt) |
| <kbd>variant</kbd> - the variant that was used to build the bundle to upload | **Acceptable Values:** <kbd>text/plain</kbd> - e.g. \`debug\`, \`release\` or \`freeDebug\` etc.. |
| <kbd>keystore</kbd> - the keystore to sign APK:s in the bundle with | **Acceptable Values:** <kbd>application/octet-stream</kbd> - the byte stream of the keystore to sign APK:s in the bundle with, e.g. the debug.keystore |
| <kbd>signing-config</kbd> - the signing config for the keystore to sign APK:s in the bundle with | **Acceptable Values:** <kbd>application/json</kbd> - needs to have the following structure: <br/><br/> \`{ "storePassword": "my-secret-store-password", "keyAlias": "my-key-alias", "keyPassword": "my-secret-key-password"}\`|

### Downloading split APK:s (\`/download\`)
Downloading of split APK:s that have been previously uploaded to the server is done automatically by the [android library](./android) - but in case you need to do it manually, POST to the 
\`/download\` endpoint with an \`application/json; charset=utf8\` body containing the device specification of the device you want to download for, e.g.

\`\`\`json
{
  "supportedAbis": ["arm64-v8a", "armeabi-v7a"],
  "glExtensions": ["GL_IMAGE"],
  "deviceFeatures": ["android.hardware.camera"],
  "supportedLocales": ["en", "fr"],
  "screenDensity": 640,
  "sdkVersion": 27
}
\`\`\`
Certain query parameters listed below are also needed:

| Name | Required | Values | 
| :--------- | :--------- | :--------- | 
| <kbd>device-id</kbd> - the device id that was previously received from a call to \`/register\` | Yes | **Acceptable Values:** <kbd>string</kbd> - e.g. \`123e4567-e89b-12d3-a456-426655440000\` |
| <kbd>application-id</kbd> - the application id of the bundle to download split APK:s from | Yes | **Acceptable Values:** <kbd>string</kbd> - e.g. \`com.example.globallydynamic\`|
| <kbd>version</kbd> - the version code of the bundle to download split APK:s from | Yes | **Acceptable Values:** <kbd>number</kbd> - e.g. \`24\` (versionCodeInt) |
| <kbd>variant</kbd> - the variant that was used to build the bundle to download split APK:s from | Yes | **Acceptable Values:** <kbd>string</kbd> - e.g. \`debug\`, \`release\` or \`freeDebug\` etc.. |
| <kbd>signature</kbd> - the SHA-1 fingerprint of the keystore used to sign the application | Yes | **Acceptable Values:** <kbd>string</kbd> e.g. \`3A:B0:F2:86:6F:43:06:35:06:99:18:1D:2F:6A:9D:DB:15:31:44:99\` |
| <kbd>throttle</kbd> - the amount of time in ms to throttle the downloading of split APK:s by | No | **Acceptable Values:** <kbd>number</kbd> - e.g. \`5000\` - will throttle for 5 seconds |
| <kbd>features</kbd> - the dynamic features from the bundle to download <br/><br/>**Note**: either this or <kbd>languages</kbd> need to be included in the request | No | **Acceptable Values:** <kbd>string</kbd> - e.g. \`myfeature,myotherfeature\` |
| <kbd>languages</kbd> - the languages from the bundle to download <br/><br/>**Note**: either this or <kbd>features</kbd> need to be included in the request | No | **Acceptable Values:** <kbd>string</kbd> - e.g. \`de,it,fr\` |
| <kbd>include-missing</kbd> - whether or not to include missing splits in the download, i.e. missing install time features and density/language splits | No | **Acceptable Values:** <kbd>true</kbd> <kbd>false</kbd> |

**Example**:<br/>
\`/download?device-id=f94498a5-353e-4c9c-88e9-6bf86809698d&variant=debug&version=1&application-id=com.example.globallydynamic&throttle=1000&languages=ko,features=myfeature\`
`;

const Server = () => {
    return (
        <Box>
            <Container>
                <ReactMarkdown
                    escapeHtml={false}
                    source={markdown}
                    renderers={
                        {
                            code: CodeBlock,
                            link: LinkRenderer
                        }
                    }/>
            </Container>
        </Box>
    );
}

export default Server;
