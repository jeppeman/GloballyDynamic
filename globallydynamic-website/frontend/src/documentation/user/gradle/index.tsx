import React from "react"
import {Box, Container} from "@material-ui/core";
import ReactMarkdown from "react-markdown/with-html";
import CodeBlock from "../../../components/CodeBlock"
import LinkRenderer from "../../../components/LinkRenderer";
import {versions} from "../../../constants";

const markdown = `
GloballyDynamic Gradle Plugin
---
<!-- [![Maven Central](https://img.shields.io/maven-central/v/com.jeppeman.globallydynamic.gradle/plugin.svg?label=maven%20central&color=green)](https://search.maven.org/artifact/com.jeppeman.globallydynamic.gradle/plugin) -->
[![Maven Central](https://img.shields.io/badge/maven--central-v${versions.GRADLE}-green)](https://search.maven.org/artifact/com.jeppeman.globallydynamic.gradle/plugin)

A gradle plugin that hooks into the bundle build process of the 
[Android Gradle Plugin](https://developer.android.com/studio/releases/gradle-plugin) and 
uploads produced bundles as well as their accompanying signing configurations (used for signing produced split APK:s) to a configured [GloballyDynamic server](./server).<br/>
Compatible with AGP 4.0 and above.<br/>

The plugin also generates meta data that gets packaged in the application that is being built for, this meta data is 
then made available to the [Android Library](./android) which uses it to communicate with the 
GloballyDynamic server.

Code
---
[https://github.com/jeppeman/GloballyDynamic/tree/master/globallydynamic-gradle-plugin](https://github.com/jeppeman/GloballyDynamic/tree/master/globallydynamic-gradle-plugin)

Usage
---
\`\`\`groovy
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath "com.jeppeman.globallydynamic.gradle:plugin:${versions.GRADLE}"
    }
}

apply plugin: 'com.jeppeman.globallydynamic'
\`\`\`
### Snapshots
\`\`\`groovy
buildscript {
    repositories {
        maven {
            url "https://oss.sonatype.org/content/repositories/snapshots/"
        }
    }
}
\`\`\`

Tasks
---
The plugin introduces the following variant aware tasks, normally you do not need to call any of these manually; 
however, if you want to enable dynamic delivery for Amazon App Store, Samsung Galaxy Store, or other platforms without
native support for it, the APK builder tasks are useful for creating APK:s suitable for distribution to these platforms.

| **Task** | **Description** |
| -------- | --------------- |
| \`writeGloballyDynamicConfigurationSourceFilesFor<Flavor><BuildType>\` | Writes configuration source files that will be included in the APK, the files include metadata that the android library will use to communicate with the GloballyDynamic server, such as url, username and password. |
| \`verifyGloballyDynamicDependenciesFor<Flavor><BuildType>\`            | Verifies that the setup is dependency setup is correct. |
| \`uploadGloballyDynamicBundle<Flavor><BuildType>\`                     | Uploads a bundle to a GloballyDynamic server. Any task in AGP that produces a bundle will be finalized by this if GloballyDynamic is enabled for the variant that the bundle is being built for. |
| \`buildUniversalApkFor<Flavor><BuildType>\`                            | Builds a universal APK from an app bundle, this APK will contain all code and resources from the application, including install time features and on-demand features, on-demand features can be excluded however. |
| \`buildUnsignedUniversalApkFor<Flavor><BuildType>\`                    | Builds an unsigned universal APK from an app bundle, this APK will contain all code and resources from the application, including install time features and on-demand features, on-demand features can be excluded however. |
| \`buildBaseApkFor<Flavor><BuildType>\`                                 | Builds a base apk from an app bundle, this APK will only contain code and base resources from the main application module, the rest is stripped |
| \`buildUnsignedBaseApkFor<Flavor><BuildType>\`                         | Builds an unsigned base apk from an app bundle, this APK will only contain code and base resources from the main application module, the rest is stripped |

Configuration
---
The plugin exposes a configuration DSL that can be added to the \`android\` in which you can configure your servers,
this comes in the form of a \`NamedDomainObjectContainer<GloballyDynamicServer>\` for example: 
\`\`\`groovy
android {
    globallyDynamicServers {
        myServer {
            throttleDownloadBy 2000
            serverUrl 'https://my.server.url'
            username 'username'
            password 'password'
            applyToBuildVariants 'freeDebug', 'paidDebug'
        }
    }
}
\`\`\`
Or if you are using kotlin script (\`build.gradle.kts\`):
\`\`\`kotlin
import com.jeppeman.globallydynamic.gradle.extensions.globallyDynamicServers

android {
    globallyDynamicServers {
        create("myServer") {
            throttleDownloadBy = 2000
            serverUrl = "https://my.server.url"
            username = "username"
            password = "password"
            applyToBuildVariants("freeDebug", "paidDebug")
        }
    }
}
\`\`\`

The \`GloballyDynamicServer\` configuration object has the following attributes:

| Attributes | Values | 
| :------------- | :--------- | 
| <kbd>throttleDownloadBy</kbd> - the amount of milliseconds to throttle the downloading of splits by | **Acceptable Values:** <kbd>long</kbd> > 0 - e.g. 5000 (will throttle for 5s)|
| <kbd>serverUrl</kbd> - the URL of the GloballyDynamic server that the application will get its split APK:s from <br/><br/>**Note**: when left empty, the plugin will try to use the server that is running in Android Studio, if it is running. | **Acceptable Values:** <kbd>string</kbd> - e.g. \`'http://192.168.0.58:8080'\`|
| <kbd>username</kbd> - the username for the GloballyDynamic server, leave empty if there are no credentials | **Acceptable Values:** <kbd>string</kbd> - e.g. \`'myusername'\`|
| <kbd>password</kbd> - the password for the GloballyDynamic server, leave empty if there are no credentials | **Acceptable Values:** <kbd>string</kbd> - e.g. \`'mypassword'\`|
| <kbd>applyToBuildVariants</kbd> - the build variants that this server should be applied to | **Acceptable Values:** <kbd>string...</kbd> - e.g. \`'freeDebug', 'paidRelease'\`|
| <kbd>uploadAutomatically</kbd> - whether or not to upload bundles to the server automatically after they have been produced | **Acceptable Values:** <kbd>true</kbd> <kbd>false</kbd><br/><br/>**Default Value:** <kbd>true</kbd>|

These can also be specified through properties with the same name, prefixed by \`globallyDynamicServer.\` e.g in. \`gradle.properties\` or via command line as follows:
\`\`\`shell script
./gradlew bundleDebug \\
    -PgloballyDynamicServer.serverUrl=http://my.url \\
    -PgloballyDynamicServer.throttleDownloadBy=5000 \\
    -PgloballyDynamicServer.username=username \\
    -PgloballyDynamicServer.password=password
\`\`\`

The plugin will also look for server configuration (url, username, password) under \`build/globallydynamic/server_info.json\` -
this is because the [Android Studio plugin](./studio) writes information about it's embedded 
server to this location.
<br/>
The order of precedence for these different ways of specifying the server information goes as follows:<br/>
\`Properties > DSL > server_info.json file\`

Examples
---
The following example configures an Android Studio integrated server for all debug variants and a production server
for a release variant:
\`\`\`groovy
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath "com.android.tools.build:gradle:4.0.1" 
        classpath "com.jeppeman.globallydynamic.gradle:plugin:${versions.GRADLE}"
    }
}

apply plugin: 'com.android.application'
apply plugin: 'com.jeppeman.globallydynamic'

android {   
    flavorDimensions 'packaging'
    
    productFlavors {
        selfHosted { dimension 'packaging' }
        gplay { dimension 'packaging' }
        huawei { dimension 'packaging' }
    }
    
    globallyDynamicServers {
        // serverUrl is left empty, the plugin will then look for server configuration
        // in the server_info.json file - this configuration should be used when running
        // the server inside Android Studio
        studioIntegrated {
            throttleDownloadBy 5000 // Usually nice to have while developing
            applyToBuildVariants 'selfHostedDebug', 'gplayDebug', 'huaweiDebug'
        }

        // Self hosted GloballyDynamic server used for selfHostedRelease variant
        production {
            serverUrl = "http://my.globallydynamic.server"
            username = "\${MY_SECRET_USERNAME}"
            password = "\${MY_SECRET_PASSWORD}"
            applyToBuildVariants 'selfHostedRelease'
        }
    }
}

repositories {
    mavenCentral()
}

// Placeholder configuration, this is needed if you are declaring a dependency that is specific to *both* a build type
// and a build flavor. See more <a href="https://developer.android.com/studio/build/dependencies#dependency_configurations">here</a>.
configurations { 
    selfHostedReleaseImplementation {}
}

dependencies {
    debugImplementation 'com.jeppeman.globallydynamic.android:selfhosted:${versions.ANDROID}'     
    selfHostedReleaseImplementation 'com.jeppeman.globallydynamic.android:selfhosted:${versions.ANDROID}'
}
\`\`\`
`;

const Gradle = () => {
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

export default Gradle;
