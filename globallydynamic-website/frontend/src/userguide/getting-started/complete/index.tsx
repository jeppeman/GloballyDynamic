import React from "react"
import {Box, Container} from "@material-ui/core";
import ReactMarkdown from "react-markdown/with-html";
import CodeBlock from "../../../components/CodeBlock";
import LinkRenderer from "../../../components/LinkRenderer";
import ImageRenderer from "../../../components/ImageRenderer";
import {versions} from "../../../constants";

const markdown = `
Complete setup
---
This page will walk you through how to configure your project for a fully fledged production 
setup compatible with any app store you intend to use dynamic delivery with, such as Google Play Store, 
Huawei App Gallery, Amazon App Store, Samsung Galaxy Store, etc.

#### 1. Configure the \`build.gradle\` of your base application module
The Android library comes with a different artifact for each underlying app store, the way you configure which one to use
is through build variants. 
All of these artifacts expose an identical API to consumers, you therefore don't have to modify your code as you switch
between build variants. The API pretty much mirrors that of the [Play Core Library](https://developer.android.com/guide/playcore)

In this example, configurations for Google Play Store, Huawei App Gallery, Amazon App Store and Samsung Galaxy Store are 
added, if you do not intend to distribute through some of these channels you can just remove their respective 
configurations.
\`\`\`groovy
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:4.0.1'
        classpath 'com.jeppeman.globallydynamic.gradle:plugin:${versions.GRADLE}'
    }
}

apply plugin: 'com.android.application'
apply plugin: 'com.jeppeman.globallydynamic'

android {
    flavorDimensions 'distributionPlatform'
    
    // Note: these product flavors have to be present for
    // dynamic feature modules as well since they are 
    // declaring a dependency on the app module
    productFlavors {
         // Google Play Store
         gplay {
             dimension 'distributionPlatform'
         }
         
         // Huawei App Gallery
         huawei {
             dimension 'distributionPlatform'
         }
         
         // Amazon App Store
         amazon {
             dimension 'distributionPlatform'
         }
         
         // Samsung Galaxy Store
         galaxy {
             dimension 'distributionPlatform'
         }
    }
    
    // DSL from the gradle plugin in which you configure your GloballyDynamic servers
    globallyDynamicServers {
        // Dev server running inside Android Studio (requires the Android Studio Plugin), applied to debug variants.
        // Note that 'serverUrl' is left intentionally empty here, when this is the case
        // the Android Studio integrated server will be used
        studioIntegrated {
            // Optional: throttles the download speed of split APK:s by the amount of milliseconds given
            throttleDownloadBy 1000 
            
            // The build variants that this server should be applied to,
            // here we apply it to all debug variants. If you for instance 
            // rather would like to use internal app sharing for your Google Play
            // variant you can just remove 'gplayDebug' from below and use
            // the 'com.jeppeman.globallydynamic.android:gplay' artifact
            // for all gplay variants.
            applyToBuildVariants 'gplayDebug', 'huaweiDebug', 'amazonDebug', 'galaxyDebug' 
        }
        
        // A dedicated server running elsewhere, can be used to enable dynamic delivery 
        // where otherwise unavailable, e.g. Amazon App Store or Samsung Galaxy Store.
        // If you do not intend to distribute to platforms without native support for
        // dynamic delivery you can leave this out.
        // Refer to the "Running a dedicated GloballyDynamic server" page to learn
        // how to run one.
        selfHosted {
            // The address to your GloballyDynamic server
            serverUrl 'https://my.globallydynamic.server' 
            
            // The username to the server
            username 'my-username' 
            
            // The password to the server
            password 'my-secret-password' 
            
            // The build variants to apply this server to, in this
            // case we want to use it to enable dynamic delivery
            // on Amazon App Store and Samsung Galaxy Store
            applyToBuildVariants 'amazonRelease', 'galaxyRelease'
        }
    }
    
    dynamicFeatures = [':my_dynamic_feature']
}

repositories { 
    google()
    mavenCentral()
    // Needed if you are using the Huawei artifact
    maven { url 'http://developer.huawei.com/repo' } 
}

// Placeholder configurations, this is needed if you are declaring a dependency 
// that is specific to *both* a build type and a build flavor.
// See more at https://developer.android.com/studio/build/dependencies#dependency_configurations.
configurations {
    gplayReleaseImplementation {}
    huaweiReleaseImplementation {}
    amazonReleaseImplementation {}
    galaxyReleaseImplementation {}
}

dependencies {
    // With this artifact requests will be delegated to a self hosted GloballyDynamic server,
    // in this case and Android Studio integrated server
    debugImplementation 'com.jeppeman.globallydynamic.android:selfhosted:${versions.ANDROID}'
    
    // With this artifact requests will be delegated to Play Store
    gplayReleaseImplementation 'com.jeppeman.globallydynamic.android:gplay:${versions.ANDROID}'
    
    // With this artifact requests will be delegated to Huawei App Gallery
    huaweiReleaseImplementation 'com.jeppeman.globallydynamic.android:huawei:${versions.ANDROID}'
    
    // Self hosted dynamic delivery for Amazon App Store
    amazonReleaseImplementation 'com.jeppeman.globallydynamic.android:selfhosted:${versions.ANDROID}'
    
    // Self hosted dynamic delivery for Samsung Galaxy Store
    galaxyReleaseImplementation 'com.jeppeman.globallydynamic.android:selfhosted:${versions.ANDROID}'
}
\`\`\`

#### 2. Integrate Android library 
The exposed API from the Android library is the identical regardless of variant, hence you do not have to make a separate
integration for each app store you distribute through. <br/>The artifact applied to the variant you use will determine
where to route install requests however, so for instance, \`com.jeppeman.globallydynamic.android:gplay\` will route to
Play Store, \`com.jeppeman.globallydynamic.android:huawei\` will route to Huawei App Gallery, and so on.

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

#### 3. Build your bundle
Bundles can then be built for the various variants as follows:
* **Amazon**: \`./gradlew bundleAmazonDebug\`, \`./gradlew bundleAmazonRelease\`
* **Galaxy**: \`./gradlew bundleGalaxyDebug\`, \`./gradlew bundleGalaxyRelease\`
* **Google Play**: \`./gradlew bundleGplayDebug\`, \`./gradlew bundleGplayRelease\`
* **Huawei**: \`./gradlew bundleHuaweiDebug\`, \`./gradlew bundleHuaweiRelease\`

Releasing with dynamic delivery on Amazon App Store and Samsung Galaxy Store requires a little bit of a different workflow since they do not
support app bundles, refer to their documentation pages [here](/user-guide/amazon-app-store) and 
[here](/user-guide/samsung-galaxy-store) for more information on the subject.

# Live example
For an example application, refer to [Android Jetpack Playground](https://github.com/jeppeman/android-jetpack-playground);
it is published on Google Play Store, Huawei App Gallery, Amazon App Store and Samsung Galaxy Store and leverages
dynamic delivery on all of the platforms.<br/>
You can also have a look at the [android sample project](https://github.com/jeppeman/GloballyDynamic/tree/master/globallydynamic-android-lib/sample) as another example.
`

const Complete = () => {
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

export default Complete;
