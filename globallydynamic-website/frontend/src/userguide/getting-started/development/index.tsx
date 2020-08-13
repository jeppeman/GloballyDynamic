import React from "react"
import {Box, Container} from "@material-ui/core";
import ReactMarkdown from "react-markdown/with-html";
import CodeBlock from "../../../components/CodeBlock";
import LinkRenderer from "../../../components/LinkRenderer";
import ApkFromBundle from "../../../assets/apk_from_bundle.png"
import UploadLog from "../../../assets/upload_log.jpg"
import FlowDiagram from "../../../assets/flow_diagram.png"
import ImageRenderer from "../../../components/ImageRenderer";

const markdown = `
Development setup
---
This page will walk you through how to configure your project for a development setup, this setup will allow you to utilise 
Dynamic Delivery through a local server running in Android Studio before uploading your bundle to any App Store. It works in the following way:<br/><br/>
When a bundle is built by the [Android Gradle Plugin](https://developer.android.com/studio/releases/gradle-plugin)
(for example through \`./gradlew bundle\`), it is intercepted by the [GloballyDynamic Gradle plugin](/docs/user/gradle) and uploaded to 
a [GloballyDynamic Server](/docs/user/server) running inside Android Studio, your app then downloads split APK:s from 
this server via the [Android library](/docs/user/android). This process is illustrated in the picture below:

<span></span><div style="text-align: center;">![Flow Diagram](${FlowDiagram})</div>

#### 1. Install the GloballyDynamic [Android Studio Plugin](/docs/user/studio)
This is needed for Android Studio integrated development server that enables local dynamic delivery. This allows for a smooth
developer experience where you can test things such as slow download speeds, failing network conditions and cancellation
of downloads.

#### 2. Configure the \`build.gradle\` of your base application module
In order for your app to use the Android Studio integrated server it has to be configured as follows:
\`\`\`groovy
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:4.0.1'
        classpath 'com.jeppeman.globallydynamic.gradle:plugin:1.0'
    }
}

apply plugin: 'com.android.application'
apply plugin: 'com.jeppeman.globallydynamic'

android {
    // DSL from the gradle plugin in which you configure your GloballyDynamic servers
    globallyDynamicServers {
        // Dev server running inside Android Studio, applied to debug builds.
        // Note that 'serverUrl' is left intentionally empty here, when this is the case
        // the Android Studio integrated server will be used
        studioIntegrated {
            // Optional: throttles the download speed of split APK:s by the amount of milliseconds given.
            // Useful for testing download dialogs and slow network conditions etc.
            throttleDownloadBy 2000 
            
            // The build variants that this server should be applied to.
            // For the dev setup we only apply it to debug.
            applyToBuildVariants 'debug'
        }
    }
    
    dynamicFeatures = [':my_dynamic_feature']
}

dependencies {
    // With this artifact split install requests will be delegated to a self hosted 
    // GloballyDynamic server, in this case the Android Studio integrated one
    debugImplementation 'com.jeppeman.globallydynamic.android:selfhosted:1.0'
}
\`\`\`

If you are using kotlin script (build.gradle.kts):
\`\`\`kotlin
import com.jeppeman.globallydynamic.gradle.extensions.globallyDynamicServers

android {   
    globallyDynamicServers {
        create("studioIntegrated") {
            ...
        }
    }
}
\`\`\`

#### 3. Integrate Android library 
The Android library is a wrapper around underlying app store client API:s, as well as an implementation of it's own that 
interacts with [GloballyDynamic Servers](/docs/user/server). The API it exposes very much mirrors that of the 
[Play Core library](https://developer.android.com/guide/playcore).

**Enable GlobalSplitCompat**<br/><br/>
In order for your app to access code and resources from a downloaded module you need to enable GlobalSplitCompat as 
illustrated below:
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
**Installing dynamic modules**
\`\`\`kotlin
import com.jeppeman.globallydynamic.globalsplitinstall.*

val globalSplitInstallManager: GlobalSplitInstallManager = GlobalSplitInstallManagerFactory.create(context)

val request = GlobalSplitInstallRequest.newBuilder()
    .addModule("my_dynamic_feature")
    .addLanguage(Locale.forLanguageTag("de"))
    .build()

val installListener = GlobalSplitInstallUpdatedListener { state ->
   when (state.status()) {
       GlobalSplitInstallSessionStatus.DOWNLOADING -> // do things
       ...
   }
}

globalSplitInstallManager.registerListener(installListener)
globalSplitInstallManager.startInstall(request)
    .addOnSuccessListener { sessionId -> ...  }
    .addOnFailureListener { exception -> ... }
\`\`\`
For more information on the client API, refer to the [Android library documentation](/docs/user/android).

#### 4. Run your application
You can now install the base APK from an app bundle that has been produced by the Android Gradle Plugin, for example:
* \`./gradlew installDebug\`
<br/><br/>or
* Run the app from Android Studio with the deploy option "APK from app bundle" selected: <div style="display:inline-block;height:150px;width:150px;">![APK from bundle](${ApkFromBundle})</div>

After a successful build, a bundle should have been uploaded to the server running inside Android Studio, if it was, 
you should see something like the following in the GloballyDynamic Log tool window in Android Studio:

![Upload log](${UploadLog})

You can then start downloading split APK:s from your android application.

The video below shows the process in action:

[YouTube](https://www.youtube.com/watch?v=K6CPYHlsJt4)

#### Example
Refer to the [minimal sample of the Android lib source code](https://github.com/jeppeman/GloballyDynamic/tree/master/globallydynamic-android-lib/minimal-sample) for a full example.

For a complete production setup compatible with any app store you intend to use dynamic delivery with, such as 
Google Play Store, Huawei App Gallery, Amazon App Store, Samsung Galaxy Store, etc. Refer to the 
[complete setup page](/user-guide/getting-started/complete).
`

const Development = () => {
    return (
        <Box>
            <Container>
                <ReactMarkdown
                    escapeHtml={false}
                    source={markdown}
                    renderers={
                        {
                            code: CodeBlock,
                            link: LinkRenderer,
                            image: ImageRenderer
                        }
                    }/>
            </Container>
        </Box>
    );
}

export default Development;
