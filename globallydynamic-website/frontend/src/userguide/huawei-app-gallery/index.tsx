import React from "react"
import {Box, Container} from "@material-ui/core";
import ReactMarkdown from "react-markdown/with-html";
import CodeBlock from "../../components/CodeBlock";
import LinkRenderer from "../../components/LinkRenderer";
import ImageRenderer from "../../components/ImageRenderer";
import HuaweiGif from "../../assets/huawei.gif"

const markdown = `
Enable Dynamic Delivery for Huawei App Gallery
---
Huawei App Gallery supports Dynamic Delivery natively through [Dynamic Ability](https://developer.huawei.com/consumer/en/doc/development/AppGallery-connect-Guides/agc-featuredelivery-introduction),
hence there is no need to run a GloballyDynamic Server in order to enable it.

However, in order to verify that your application works as expected on Huawei App Gallery, you should use their
[open testing service](https://developer.huawei.com/consumer/jp/doc/development/AppGallery-connect-Guides/agc-betatest-introduction).
This service works similar to [Google Play's internal test tracks](https://developer.android.com/distribute/best-practices/launch/test-tracks)
and allows you to distribute the app on Huawei App Gallery to a group of testers without having to go through the 
official review pipeline. <br/>
The service is still in a beta stage and therefore requires you to [apply for access to it](https://developer.huawei.com/consumer/jp/doc/development/AppGallery-connect-Guides/agc-betatest-apply),
access is usually granted very quickly.

# Configuring your app to install dynamic modules through Huawei App Gallery
Simply create a Huawei build flavor and use the huawei artifact from the android library as follows:
\`\`\`groovy
android {
    flavorDimensions 'distributionPlatform'
    
    productFlavors {
        huawei {
            dimension 'distributionPlatform'
        }
    }
}

configurations {
    huaweiReleaseImplementation {}
}

dependencies {
    huaweiReleaseImplementation 'com.jeppeman.globallydynamic.android:huawei:0.1'
}
\`\`\`

# Live example
My example project [Android Jetpack Playground](https://github.com/jeppeman/android-jetpack-playground) is 
published on Huawei App Gallery, refer to it for a complete setup.

![Galaxy](${HuaweiGif})
`

const HuaweiAppGallery = () => {
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

export default HuaweiAppGallery;
