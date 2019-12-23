# locallydynamic-gradle-plugin

A gradle plugin that hooks into the bundle build process of the <a href="https://developer.android.com/studio/releases/gradle-plugin">Android Gradle Plugin</a> and uploads produced bundles to a <a href="../locallydynamic-server-library">LocallyDynamic server</a>.<br/>
Compatible with AGP 3.5 and 3.6.

Usage
---
```groovy
buildscript {
    repositories {
        maven {
            url "https://plugins.gradle.org/m2"
        }
    }
    dependencies {
        classpath "com.jeppeman.locallydynamic.gradle:plugin:0.1"
    }
}

apply plugin: 'com.jeppeman.locallydynamic'

```
### Snapshot
```groovy
buildscript {
    repositories {
        maven {
            url "https://oss.sonatype.org/content/repositories/snapshots/"
        }
    }
    dependencies {
        classpath "com.jeppeman.locallydynamic.gradle:plugin:0.2-SNAPSHOT"
    }
}

apply plugin: 'com.jeppeman.locallydynamic'

```

Tasks
---
The plugin introduces the following variant aware tasks (none of which need to be called manually, they are run automatically after or before other tasks that are part of the bundle build process):

| **Task** | **Description** |
| -------- | --------------- |
| `writeLocallyDynamicConfigurationSourceFilesFor<Flavor><BuildType>` | Writes configuration source files that will be included in the APK, the files include metadata that the android library will use to communicate with the LocallyDynamic server, such as url, username and password. |
| `verifyLocallyDynamicDependenciesFor<Flavor><BuildType>`            | Verifies that the setup is dependency setup is correct. |
| `uploadLocallyDynamicBundle<Flavor><BuildType>`                     | Uploads a bundle to a LocallyDynamic server. Any task in AGP that produces a bundle will be finalized by this if LocallyDynamic is enabled for the variant that the bundle is being built for. |

Configuration
---
The plugin exposes a configuration DSL that can be added to each build type of your application, e.g.
```groovy
android {
    buildTypes {
        debug {
            locallyDynamic { ... }
        }
    }
}
```
It has the following attributes:

| Attributes | Values | 
| :------------- | :--------- | 
| <kbd>enabled</kbd> - whether or not LocallyDynamic should be enabled for the build type | **Acceptable Values:** <kbd>true</kbd> <kbd>false</kbd><br/><br/>**Default Value:** <kbd>false</kbd>|
| <kbd>throttleDownloadBy</kbd> - the amount of milliseconds to throttle the downloading of splits by | **Acceptable Values:** <kbd>long</kbd> > 0 - e.g. 5000 (will throttle for 5s)|
| <kbd>serverUrl</kbd> - the URL of the LocallyDynamic server that the application will get its split APK:s from <br/><br/>**Note**: when left empty, the plugin will try to use the server that is running in Android Studio, if it is running. | **Acceptable Values:** <kbd>string</kbd> - e.g. `'http://192.168.0.58:8080'`|
| <kbd>username</kbd> - the username for the LocallyDynamic server, leave empty if there are no credentials | **Acceptable Values:** <kbd>string</kbd> - e.g. `'myusername'`|
| <kbd>password</kbd> - the password for the LocallyDynamic server, leave empty if there are no credentials | **Acceptable Values:** <kbd>string</kbd> - e.g. `'mypassword'`|

These can also be specified through properties with the same name, prefixed by `locallyDynamic.` e.g in. `gradle.properties` or via command line as follows:
```shell script
./gradlew bundleDebug \
    -PlocallyDynamic.serverUrl=http://my.url \
    -PlocallyDynamic.throttleDownloadBy=5000 \
    -PlocallyDynamic.username=username \
    -PlocallyDynamic.password=password
```

The plugin will also look for server configuration (url, username, password) under `build/locallydynamic/server_info.json` -
this is because the <a href="../locallydynamic-studio-plugin">Android Studio plugin</a> writes information about its embedded 
server to this location.
<br/>
The order of precedence for these different ways of specifying the server information goes as follows:<br/>
`Properties > DSL > server_info.json file`

Examples
---
The following example has different configurations for different build types, and no configuration for the release build type - 
in which case the android library will simply delegate to the Play Core library.
```groovy
buildscript {
    repositories {
        maven {
            url "https://plugins.gradle.org/m2"
        }
    }
    dependencies {
        classpath "com.android.tools.build:gradle:3.5.3" // Compatible with 3.5 and above
        classpath "com.jeppeman.locallydynamic.gradle:plugin:0.1"
    }
}

apply plugin: 'com.android.application'
apply plugin: 'com.jeppeman.locallydynamic'

android {   
    buildTypes {
        // serverUrl is left empty, the plugin will then look for server configuration
        // in the server_info.json file - this configuration should be used when running
        // the server inside Android Studio
        debug {
            locallyDynamic {
                enabled = true
                throttleDownloadBy = 5000 // Usually nice to have while developing
            }
        }

        // Custom build type that might be used for internal distribution - 
        // Here we explicitly specify url, username and password since we want
        // to use a dedicated server. We remove throttling here 
        internalDistribution {
            locallyDynamic {
                enabled = true
                serverUrl = "http://my.locallydynamic.server"
                username = "${MY_SECRET_USERNAME}"
                password = "${MY_SECRET_PASSWORD}"
            }
        }
        
        // No locallyDynamic {} configuration specified for release, the android library will
        // delegate to Play Core
        release {
        
        }
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // The debug artifact will talk to the LocallyDynamic server
    debugImplementation 'com.jeppeman.locallydynamic:locallydynamic-debug:0.1'
    
    // The release artifact will only delegate to the Play Core library
    releaseImplementation 'com.jeppeman.locallydynamic:locallydynamic:0.1'     
}
```