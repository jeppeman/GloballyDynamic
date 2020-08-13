import React from "react";
import {Box, Container} from "@material-ui/core";
import ReactMarkdown from "react-markdown/with-html";
import CodeBlock from "../../components/CodeBlock";
import LinkRenderer from "../../components/LinkRenderer";
import ImageRenderer from "../../components/ImageRenderer";

const markdown = `
Firebase App Distribution
---
App bundles are not supported on Firebase App Distribution either, however, enabling Dynamic Delivery for it is fairly 
straightforward:

1. Run a dedicated GloballyDynamic server that is reachable from your app.
2. Configure the [Firebase App Distribution Gradle plugin](https://firebase.google.com/docs/app-distribution/android/distribute-gradle) 
to use an APK that has been stripped of on-demand modules.
3. Build and upload the APK.

### 1. Run a dedicated server
Follow the guide [here](./server).

### 2. Configure the gradle plugin
\`\`\`groovy
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:4.0.1'
        classpath 'com.jeppeman.globallydynamic.gradle:plugin:1.0'
        classpath 'com.google.firebase:firebase-appdistribution-gradle:2.0.0'
    }
}

apply plugin: 'com.android.application'
apply plugin: 'com.jeppeman.globallydynamic'
apply plugin: 'com.google.firebase.appdistribution'

android {
    flavorDimensions 'distributionPlatform'
    
    productFlavors {
        firebase {
            dimension 'distributionPlatform'
            firebaseAppDistribution {
                // The output path of the universal apk
                apkPath "\${buildDir}/outputs/universal_apk/firebaseRelease/universal.apk"
            }
        }
    }
    
    globallyDynamicServers {
        selfHosted {
            serverUrl 'https://my.globallydynamic.server'
            username 'my-username'
            password 'my-secret-password'
            applyToBuildVariants 'firebaseRelease'
        }
    }
}

configurations {
    firebaseReleaseImplementation {}
}

dependencies {
    firebaseReleaseImplementation 'com.jeppeman.globallydynamic.android:selfhosted:1.0'
}
\`\`\`

### 3. Build and upload the APK

**Option 1: Build a universal APK (\`./gradlew buildUniversalApkForFirebaseRelease\`):** this APK will include *all* code and 
resources from the application, including dynamic feature modules, both install time and on-demand. There is a way
to exclude on-demand modules from the produced APK however; by disabling *fusing* for a dynamic feature module,
it will be excluded from the universal APK, like so:
\`\`\`xml
<manifest xmlns:dist="http://schemas.android.com/apk/distribution"
    package="com.example.myapp.feature">

    <dist:module
        dist:instant="false"
        dist:title="@string/title_my_feature">
        <dist:delivery>
            <dist:on-demand />
        </dist:delivery>
        
        <!-- Fusing disabled -->
        <dist:fusing dist:include="false" /> 
    </dist:module>
</manifest>
\`\`\`
The appealing thing about the universal APK in comparison to the others is that you do not have to call
\`globalSplitInstallManager.installMissingSplits()\`, install time features are immediately available, while 
on-demand modules are still downloaded dynamically.

**Option 2: Build a base APK (\`./gradlew buildBaseApkForFirebaseRelease\`)**: this APK will also only contain code and resources
from the main application module, the difference compared to the standard APK is that this APK is also stripped of 
non-default languages and assets - so somewhat more naked, if you will. With this APK it is strongly recommended
that you use \`globalSplitInstallManager.installMissingSplits()\` since non-default assets / languages will be missing from
the APK.<br/><br/>
**Option 3: Build a bundle and a standard APK (\`./gradlew bundleFirebaseRelease assembleFirebaseRelease\`)**: this APK will *only* contain code and resources from the 
main application module, i.e. no dynamic feature modules, neither install time nor on-demand modules. Therefore install
time modules also become on-demand modules, i.e. they will be downloaded at runtime as they are needed. The android
library comes with a way of installing all *missing* splits, meaning missing install time modules and density/language 
modules, this can be called immediately after application startup if you want install time modules to readily 
available as soon as possible:
\`\`\`kotlin
globalSplitInstallManager.installMissingSplits()
    .addOnSuccessListener { startMyActivityFromInstallTimeFeature() }
\`\`\`

**Publish the APK**<br/>
Finally publish your produced APK to Firebase App Distribution like so:

\`./gradlew appDistributionUploadFirebaseRelease\`

### Live Example
My example project [Android Jetpack Playground](https://github.com/jeppeman/android-jetpack-playground) is 
published on Firebase App Distribution, refer to it for a complete setup.
`;

const FirebaseAppDistribution = () => {
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

export default FirebaseAppDistribution;

