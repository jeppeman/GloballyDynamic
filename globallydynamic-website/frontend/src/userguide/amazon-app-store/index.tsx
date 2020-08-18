import React from "react";
import {Box, Container} from "@material-ui/core";
import ReactMarkdown from "react-markdown/with-html";
import CodeBlock from "../../components/CodeBlock";
import LinkRenderer from "../../components/LinkRenderer";
import ImageRenderer from "../../components/ImageRenderer";
import AmazonGif from "../../assets/amazon.gif"
import {versions} from "../../constants";

const markdown = `
# Enable Dynamic Delivery for Amazon App Store

Currently, [Amazon App Store](https://developer.amazon.com/apps-and-games) does not natively support dynamic delivery, meaning that 
you can not upload app bundles, but it can be enabled through the following steps:

1. Run a dedicated GloballyDynamic server that is reachable from your app.
2. Make a request for enabling self signing of your app in Amazon Developer Console.
3. Build an *unsigned* APK that is stripped of on-demand modules and upload it to Amazon Developer Console.

Below follows further instructions for each of the steps:

### 1. Run a dedicated server
Follow the guide [here](./server).

### 2. Make a request for enabling self signing in Amazon Developer Console
By default when you publish an app to Amazon App Store the APK will be stripped of the original developer signature and re-signed 
by Amazon. Unfortunately this breaks dynamic delivery as the split APK:s will be downloaded from a GloballyDynamic server, 
rather than from Amazon App Store, and in order to install split APK:s they need to be signed with the same certificate 
as the base APK.

You can keep the original signature by [making a request](https://developer.amazon.com/support/contact-us) in Amazon 
Developer Console in which you state that you want to enable "Self Signing" for you application.
Once approved, you can start uploading APK:s and have the original signature preserved.

### 3. Build an *unsigned* APK that is stripped of on-demand modules and publish it
Since app bundles (.aab:s) can not be uploaded to Amazon App Store, a suitable APK that has been stripped of on-demand
modules has to be built and uploaded. 

First of all, the app has to be configured to use the dedicated server, like so:
\`\`\`groovy
android {
    flavorDimensions 'distributionPlatform'
    
    productFlavors {
        amazon {
            dimension 'distributionPlatform'
        }
    }
    
    globallyDynamicServers {
        selfHosted {
            serverUrl 'https://my.globallydynamic.server'
            username 'my-username'
            password 'my-secret-password'
            applyToBuildVariants 'amazonRelease'
        }
    }
}

configurations {
    amazonReleaseImplementation {}
}

dependencies {
    amazonReleaseImplementation 'com.jeppeman.globallydynamic.android:selfhosted:${versions.ANDROID}'
}
\`\`\`
When publishing with Self Signing, Amazon requires you to first upload an *unsigned* APK that they will wrap with
security features that verifies binary authenticity, you then download this wrapped APK from Amazon Developer Console
and sign it with your certificate and upload the signed version.<br/><br/> 
There are a few different ways in which you can produce this unsigned APK that is stripped of on-demand modules:

**Option 1 (recommended), build a universal APK (\`./gradlew buildUnsignedUniversalApkForAmazonRelease\`):** 

This APK will include *all* code and resources from the application, including dynamic feature modules, 
both install time and on-demand. There is a way to exclude on-demand modules from the produced APK however; 
by disabling *fusing* for a dynamic feature module, it will be excluded from the universal APK, like so:
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

**Option 2, build a base APK (\`./gradlew buildUnsignedBaseApkForAmazonRelease\`):**
this APK will *only* contain code and resources
from the main application module, i.e. no dynamic feature modules, neither install time nor on-demand modules. Therefore install
time modules also become on-demand modules, i.e. they will be downloaded at runtime as they are needed. The android
library comes with a way of installing all *missing* splits, meaning missing install time modules and density/language 
modules, this can be called immediately after application startup if you want install time modules to be readily 
available as soon as possible:
\`\`\`kotlin
globalSplitInstallManager.installMissingSplits()
    .addOnSuccessListener { startMyActivityFromInstallTimeFeature() }
\`\`\`
With this APK it is strongly recommended that you call this on app start since non-default assets / languages will
be missing from the APK.


**Option 3, build a bundle and a standard APK (\`./gradlew bundleAmazonRelease assembleAmazonRelease\`):**
this APK will also only contain code and resources from the 
main application module, the difference compared to the base APK is that this APK is not stripped of 
non-default languages and assets - so somewhat less naked, if you will. With this APK it is you can also use
\`globalSplitInstallManager.installMissingSplits()\` if you want install time feature to be available as soon
as possible.

**Publish the APK**<br/>
When you've managed to upload the signed version of the APK and gotten an approval, dynamic delivery for you
app on Amazon App Store should be up and running.

### Live Example
My example project [Android Jetpack Playground](https://github.com/jeppeman/android-jetpack-playground) is 
published on Amazon App Store, refer to it for a complete setup.

![Amazon](${AmazonGif})
`

const AmazonAppStore = () => {
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

export default AmazonAppStore;
