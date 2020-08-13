import React from "react"
import {Box, Container} from "@material-ui/core";
import ReactMarkdown from "react-markdown/with-html";
import CodeBlock from "../../../components/CodeBlock"
import LinkRenderer from "../../../components/LinkRenderer";

const markdown = `
GloballyDynamic Android Library
---
[![Maven Central](https://img.shields.io/maven-central/v/com.jeppeman.globallydynamic.android/all.svg?label=maven%20central&color=green)](https://search.maven.org/search?q=g:%22com.jeppeman.globallydynamic.android%22)

An Android library that adds an abstraction layer on top of Dynamic Delivery client API:s, e.g. 
[Play Core](https://developer.android.com/guide/playcore), 
[Dynamic Ability](https://developer.huawei.com/consumer/en/doc/development/AppGallery-connect-Guides/agc-featuredelivery-initsdk)
in order to provide a unified API regardless of underlying distribution platform. It also provides a
Dynamic Delivery client implementation of it's own that interacts with [GloballyDynamic Servers](/docs/user/server).

Currently supported App Stores / Distribution platforms include:
* Google Play Store ([Dynamic Delivery](https://developer.android.com/guide/app-bundle/dynamic-delivery))
* Huawei App Gallery ([Dynamic Ability](https://developer.huawei.com/consumer/en/doc/development/AppGallery-connect-Guides/agc-featuredelivery-introduction))
* Most other App Stores in combination with [GloballyDynamic Server](./server) (e.g. Amazon App Store, Samsung Galaxy Store)

The library comes with a different artifact for each underlying app store, the way you configure which one to use
is through build variants. 
All of these artifacts expose the same API to consumers, it pretty much mirrors the API of [Play Core Library](https://developer.android.com/guide/playcore)
and delegates to the underlying app store's client API:s, e.g. \`com.jeppeman.globallydynamic.android:gplay\` delegates
to Play Core, \`com.jeppeman.globallydynamic.android:huawei\` delegates to Dynamic Ability, 
\`com.jeppeman.globallydynamic.android:selfhosted\` delegates to the implementation that interacts with a GloballyDynamic
Server, etc.

Code
---
[https://github.com/jeppeman/GloballyDynamic/tree/master/globallydynamic-android-lib](https://github.com/jeppeman/GloballyDynamic/tree/master/globallydynamic-android-lib)

Usage
---
### Gradle
\`\`\`groovy
repositories {
    mavenCentral()
}

dependencies {
    // Use for Google Play variants
    gplayImplementation 'com.jeppeman.globallydynamic.android:gplay:1.0'
    
    // Use for Huawei App Gallery variants
    huaweiImplementation 'com.jeppeman.globallydynamic.android:huawei:1.0'
    
    // Use for other variants, e.g. during development or distribution through some other App Store
    selfHostedImplementation 'com.jeppeman.globallydynamic.android:selfhosted:1.0'
}
\`\`\`
**Snapshots**
\`\`\`groovy
repositories {
    maven {
        url 'https://oss.sonatype.org/content/repositories/snapshots/'    
    }
}
\`\`\`
See the [Gradle Plugin](./gradle) for more configuration details.

### Enable GlobalSplitCompat 
In order for your app to access code and resources from a downloaded module you need to enable GlobalSplitCompat as 
illustrated below.
\`\`\`kotlin
import com.jeppeman.globallydynamic.globalsplitcompat.GlobalSplitCompatApplication

class MyApplication : GlobalSplitCompatApplication()

// OR

import com.jeppeman.globallydynamic.globalsplitcompat.GlobalSplitCompat

class MyApplication : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        GlobalSplitCompat.install(this)
    }
}

// FOR INSTALLED ACTIVITIES
class MyActivity : AppCompatActivity() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        GlobalSplitCompat.installActivity(this)
    }
}
\`\`\`

### Make a request to download on demand modules (dynamic features, languages)
\`\`\`kotlin
val globalSplitInstallManager: GlobalSplitInstallManager = GlobalSplitInstallManagerFactory.create(context)

val request = GlobalSplitInstallRequest.newBuilder()
    .addModule("myDynamicFeature")
    .addModule("myDynamicFeature2")
    .addLanguage(Locale.forLanguageTag("DE"))
    .build()

globalSplitInstallManager.startInstall(request)
    // When a request is accepted, a session id is generated
    // that you can use to monitor your request
    .addOnSuccessListener { sessionId -> ...  }
    // Use this listener to handle errors any errors encountered
    // during the request
    .addOnFailureListener { exception -> ... }
\`\`\`

### Deferred module installation
\`\`\`kotlin
// Features
globalSplitInstallManager.deferredInstall("myDynamicFeature", "myDynamicFeature2")

// Languages
globalSplitInstallManager.deferredLanguageInstall(listOf(Locale.forLanguageTag("DE")))
\`\`\`

### Monitor the state of an ongoing installation
\`\`\`kotlin
var mySessionId = 0

val listener = GlobalSplitInstallUpdatedListener { state ->
    if (state.sessionId() == mySessionId) {
        when (state.status()) {
            GlobalSplitInstallSessionStatus.DOWNLOADING -> ...
            GlobalSplitInstallSessionStatus.INSTALLING -> ...
            GlobalSplitInstallSessionStatus.INSTALLED -> ...
            ...
        }
    }
}

globalSplitInstallManager.registerListener(listener)
    // When a request is accepted a session id is generated
    // that you can use to monitor your request
    .addOnSuccessListener { sessionId -> mySessionId = sessionId }
    // Use this listener to handle errors any errors encountered
    // during the request
    .addOnFailureListener { exception -> ... }
\`\`\`


### Get a user confirmation
In certain cases a user confirmation needs to be obtain to proceed with and installation, an example of this is when
a request to download a very large module is executed. Handle it like so:
\`\`\`kotlin
val listener = GlobalSplitInstallUpdatedListener { state ->
    if (state.sessionId() == mySessionId) {
        when (state.status()) {
            GlobalSplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION -> {
                globalSplitInstallManager.startConfirmationDialogForResult(
                    state, 
                    activity, 
                    // Can be used in onActivityResult to determine whether
                    // the user accepted the installation or not
                    requestCode
                )
            }
        }
    }
}
\`\`\`

### Android 11 (R) with the selfhosted artifact
If your app is using the \`com.jeppeman.globallydynamic.android:selfhosted\` artifact while targeting Android R and [Scoped Storage](https://developer.android.com/preview/privacy/storage) is enabled,
the app requires a full restart after the user has granted permission to install from unknown sources.
When granting the permission the system will kill and then recreate the app and bring it back to the state in which
it was prior to dying. This can be handled gracefully in the following manner:
\`\`\`kotlin
fun installMyModule() {
    val request = GlobalSplitInstallRequest.newBuilder()
        .addModule("myModule")
        .build()
        
    globalSplitInstallManager.registerListener(GlobalSplitInstallUpdatedListener { state ->
        if (state.status() == GlobalSplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION) {
            globalSplitInstallManager.startConfirmationDialogForResult(state, myActivity, MY_REQUEST_CODE)
        }
    })
    
    globalSplitInstallManager.startInstall(request)
}

override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == MY_REQUEST_CODE && data?.hasExtra(GlobalSplitInstallConfirmResult.EXTRA_RESULT) == true) {
        val installConfirmResult = data.getIntExtra(
                GlobalSplitInstallConfirmResult.EXTRA_RESULT,
                GlobalSplitInstallConfirmResult.RESULT_DENIED
        )
        
        if (installConfirmResult == GlobalSplitInstallConfirmResult.RESULT_CONFIRMED) {
            // The app recovered from the force stop and has permission to install, run the install again
            installMyModule()
        } else {
            // The user did not grant install permissions, do something else
        }
    }
}
\`\`\`

**Note:** if the app does not automatically restart after the user has granted the permission, it is probably due to
Android Studio preventing it from doing so; when testing this flow you should sever the app's connection to Android Studio,
either by pressing the Stop button in Android Studio and the manually restarting the app, or installing the app through
\`./gradlew install\` and then manually starting it.

For more examples, see [user guide](/user-guide).

### Handle request errors
\`\`\`kotlin
globalSplitInstallManager.startInstall(request)
     .addOnFailureListener { exception -> 
         when ((exception as? GlobalSplitInstallException)?.getErrorCode()) {
             GlobalSplitInstallErrorCode.NETWORK_ERROR -> ...
             GlobalSplitInstallErrorCode.INSUFFICIENT_STORAGE -> ...
         }
     }
\`\`\`

### Cancel a request
\`\`\`kotlin
globalSplitInstallManager.cancelInstall(mySessionId)
\`\`\`

### Install native libraries from dynamic modules
\`\`\`kotlin
GlobalSplitInstallHelper.loadLibrary(context, "myLib")
\`\`\`

### Install missing split APK:s
This will install all missing split APK:s (if any), i.e. missing install time features and density/language APK:s;
this is useful if only the \`base-master\` APK is installed, this can be the case if you enable dynamic delivery for an app store
that does not support it natively, since you will only be allowed to upload one APK. See 
[Enable Dynamic Delivery for Amazon App Store](/user-guide/amazon-app-store) for more info.
\`\`\`kotlin
globalSplitInstallManager.installMissingSplits()
    .addOnSuccessListener { sessionId -> ...  }
    .addOnFailureListener { exception -> ... }
\`\`\`


`;

const Android = () => {
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

export default Android;
