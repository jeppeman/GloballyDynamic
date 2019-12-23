# locallydynamic-android-lib
An Android library that downloads and installs split APK:s from a <a href="../locallydynamic-server-library">LocallyDynamic server</a> while
exposing the same API as the Play Core library.

Usage
---
```gradle
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
### Snapshot
```gradle
repositories {
    maven {
        url "https://oss.sonatype.org/content/repositories/snapshots/"    
    }
}

dependencies {
    // The debug artifact will talk to the LocallyDynamic server
    debugImplementation 'com.jeppeman.locallydynamic:locallydynamic-debug:0.2-SNAPSHOT'
    
    // The release artifact will only delegate to the Play Core library
    releaseImplementation 'com.jeppeman.locallydynamic:locallydynamic:0.2-SNAPSHOT'     
}
```

### Example
Add the following to the `build.gradle` of your base application module: 

```gradle
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
        debug {
            locallyDynamic {
                enabled = true
                
                // Optional: throttles the download speed of split APK:s by the amount of milliseconds given
                throttleDownloadBy = 1000 
                
                // If serverUrl is left empty, the LocallyDynamic server
                // embedded in Android Studio server will be used (if running)
                serverUrl = "http://192.168.0.22:8080"
            }
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

Then use as follows:

```kotlin
import com.jeppeman.locallydynamic.LocallyDynamicSplitInstallManagerFactory

val splitInstallManager: SplitInstallManager = LocallyDynamicSplitInstallManagerFactory.create(context)
/*
Note: in the release artifact LocallyDynamicSplitInstallManagerFactory.create(context) 
will just delegate to SplitInstallManagerFactory.create(context) - no need to modify the code
between debug / release builds
*/

// Use it as you would normally use a SplitInstallmanager 
val request = SplitInstallRequest.newBuilder()
    .addModule("mymodule")
    .addLanguage(Locale.forLanguageTag("de"))
    .build()
    
splitInstallManager.registerListener(...)
splitInstallManager.startInstall(request)
```