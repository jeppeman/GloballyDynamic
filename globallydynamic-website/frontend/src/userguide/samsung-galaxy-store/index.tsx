import React from "react";
import {Box, Container} from "@material-ui/core";
import ReactMarkdown from "react-markdown/with-html";
import CodeBlock from "../../components/CodeBlock";
import LinkRenderer from "../../components/LinkRenderer";
import ImageRenderer from "../../components/ImageRenderer";
import GalaxyGif from "../../assets/galaxy.gif"

const markdown = `
Enable Dynamic Delivery for Samsung Galaxy Store
---
The story with [Samsung Galaxy Store](https://www.samsung.com/us/apps/galaxy-store/) is the same as Amazon App Store:
it does not natively support Dynamic Delivery. However, the process of enabling it is a little bit simpler as 
Self Signing is enabled by default. Therefore it is merely a two-step process:

1. Run a dedicated GloballyDynamic server that is reachable from your app.
2. Build an APK that is stripped of on-demand modules and upload to Samsung Galaxy Seller Portal.

### 1. Run a dedicated server
Follow the guide [here](./server).

### 2. Build an APK that is stripped of on-demand modules and publish it
App bundles (.aab:s) can not be uploaded to Samsung Galaxy Store neither, therefore a suitable APK stripped of 
on-demand modules has to built and published there as well.

Configure your app to use the dedicated server:
\`\`\`groovy
android {
    flavorDimensions 'distributionPlatform'
    
    productFlavors {
        galaxy {
            dimension 'distributionPlatform'
        }
    }
    
    globallyDynamicServers {
        selfHosted {
            serverUrl 'https://my.globallydynamic.server'
            username 'my-username'
            password 'my-secret-password'
            applyToBuildVariants 'galaxyRelease'
        }
    }
}

configurations {
    galaxyReleaseImplementation {}
}

dependencies {
    galaxyReleaseImplementation 'com.jeppeman.globallydynamic.android:selfhosted:0.1'
}
\`\`\`
To build the actual APK, the process is the same as for Amazon App Store, except that you do not have to build an
unsigned APK - your options are the following:

**Option 1: Build a standard APK (\`./gradlew assembleGalaxyRelease\`)**: this APK will *only* contain code and resources from the 
main application module, i.e. no dynamic feature modules, neither install time nor on-demand modules. Therefore install
time modules also become on-demand modules, i.e. they will be downloaded at runtime as they are needed. The android
library comes with a way of installing all *missing* splits, meaning missing install time modules and density/language 
modules, this can be called immediately after application startup if you want install time modules to readily 
available as soon as possible:
\`\`\`kotlin
globalSplitInstallManager.installMissingSplits()
    .addOnSuccessListener { startMyActivityFromInstallTimeFeature() }
\`\`\`
**Option 2: Build a base APK (\`./gradlew buildBaseApkForGalaxyRelease\`)**: this APK will also only contain code and resources
from the main application module, the difference compared to the standard APK is that this APK is also stripped of 
non-default languages and assets - so somewhat more naked, if you will. With this APK it is strongly recommended
that you use \`globalSplitInstallManager.installMissingSplits()\` since non-default assets / languages will be missing from
the APK.<br/><br/>
**Option 3: Build a universal APK (\`./gradlew buildUniversalApkForGalaxyRelease\`):** this APK will include *all* code and 
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

Finally publish your produced APK and get it approved and you're up and running.

### Live Example
My example project [Android Jetpack Playground](https://github.com/jeppeman/android-jetpack-playground) is 
published on Samsung Galaxy Store, refer to it for a complete setup.

![Galaxy](${GalaxyGif})
`

const SamsungGalaxyStore = () => {
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

export default SamsungGalaxyStore;
