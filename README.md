# GloballyDynamic
[![CircleCI](https://circleci.com/gh/jeppeman/GloballyDynamic.svg?style=svg)](https://circleci.com/gh/jeppeman/GloballyDynamic)
<p align="center">
    <img src="assets/images/cover_image.png" >
</p>

**A set of tools geared towards making [Dynamic Delivery](https://developer.android.com/guide/app-bundle/dynamic-delivery) universally available, regardless of underlying 
App Store / distribution platform, while also providing a single unified Android client API and a streamlined developer experience.
<br/><br/>Currently supported platforms include:**
* Google Play Store ([Play Feature Delivery](https://developer.android.com/guide/app-bundle/dynamic-delivery))
* Huawei App Gallery ([Dynamic Ability](https://developer.huawei.com/consumer/en/doc/development/AppGallery-connect-Guides/agc-featuredelivery-introduction))
* Most other platforms in combination with [GloballyDynamic Server](globallydynamic-server-lib) (e.g. Amazon App Store, Samsung Galaxy Store, Firebase App Distribution or during local development)

Why?
---
Dynamic delivery is great, it can go a long way towards saving device storage space as well as network consumption for users. However, adopting it can pose a few problems:
* If you want to leverage capabilities such as on-demand Dynamic Feature Modules for multiple platforms, you have to provide a 
*separate client side integration for each app store you distribute through that supports dynamic delivery* - with current trends pointing towards an increasingly scattered landscape of app stores 
(e.g. Huawei and Amazon devices come without Play Store installed), the problem is likely to get magnified in the future.

* Many app stores / distribution platforms do not support app bundles (e.g. Amazon App Store, Samsung Galaxy Store, 
Firebase App Distribution), if you distribute through any of these you are unable to make use of dynamic delivery. 


* It can not be used if your device does not have an app store with native support for dynamic delivery installed, 
e.g. emulators w/o Google Play, custom devices or Amazon Fire devices.

GloballyDynamic provides the tools and infrastructure necessary to address these issues.

Key capabilities
---
* **Unified client API**: write once, run with any platform supporting dynamic delivery. The 
[Android Library](globallydynamic-android-lib) comes in different flavors that each expose an identical API, but delegate to a 
different underlying app store client API (e.g Play Core or Dynamic Ability)

* **Dynamic delivery for app stores without support for app bundles**: e.g. Samsung Galaxy Store, 
Amazon App Store. This is made available by interplay between [GloballyDynamic Server](globallydynamic-server-lib), 
the [Android Library](globallydynamic-android-lib) and the [Gradle Plugin](globallydynamic-gradle-plugin).

* **Dynamic delivery during development**: download and install split APK:s (Dynamic feature APK:s, configuration APK:s etc.) 
from a server running locally in Android Studio during development, without having to upload bundles to a distribution 
platform (e.g. Google Play or Huawei App Gallery) or manually build/install splits to your device. Made available by the
[Android Studio Plugin](globallydynamic-studio-plugin)

* **Dynamic delivery with any device**: utilize dynamic delivery for devices that do not have an app store with dynamic delivery support installed (e.g. emulators w/o Google Play or custom devices).

* **Dynamic delivery for internal builds**: utilize dynamic delivery for internally distributed builds, e.g. builds distributed through [Firebase App Distribution](https://firebase.google.com/docs/app-distribution), before they get promoted to other distribution platforms.

* **Ease of testing various install scenarios during development**: quickly iterate and test install scenarios such as:
    * Slow download speeds: configure dev server to throttle downloads of split APK:s
    * Broken server connection: toggle on/off the dev server to test failing connections
    * Cancellation of installs: cancel ongoing installations through system notifications 
    * Multiple install sessions: run multiple install sessions during development

Getting started
---
* Follow the [User Guide](https://globallydynamic.io/user-guide)

Documentation
---
* [User docs](https://globallydynamic.io/docs/user)
* [Javadoc](https://globallydynamic.io/docs/javadoc)

Release notes
---
* [Release notes](https://globallydynamic.io/release-notes)

License
---
```
Copyright 2020 Jesper Åman

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
