package com.jeppeman.globallydynamic.website

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping

private const val INDEX = "forward:/index.html"

@Controller
class SiteController {
    @RequestMapping(value = ["/docs"])
    fun docs() = INDEX

    @RequestMapping(value = ["/docs/user"])
    fun userDocs() = INDEX

    @RequestMapping(value = ["/docs/user/android"])
    fun userAndroidDocs() = INDEX

    @RequestMapping(value = ["/docs/user/server"])
    fun userServerDocs() = INDEX

    @RequestMapping(value = ["/docs/user/gradle"])
    fun userGradleDocs() = INDEX

    @RequestMapping(value = ["/docs/user/studio"])
    fun userStudioDocs() = INDEX

    @RequestMapping(value = ["/docs/javadoc/android"])
    fun javadocAndroidDocs() = INDEX

    @RequestMapping(value = ["/docs/javadoc/server"])
    fun javadocServerDocs() = INDEX

    @RequestMapping(value = ["/user-guide"])
    fun userGuide() = INDEX

    @RequestMapping(value = ["/user-guide/getting-started"])
    fun userGuideGettingStarted() = INDEX

    @RequestMapping(value = ["/user-guide/getting-started/development"])
    fun userGuideGettingStartedDev() = INDEX

    @RequestMapping(value = ["/user-guide/getting-started/complete"])
    fun userGuideGettingStartedComplete() = INDEX

    @RequestMapping(value = ["/user-guide/server"])
    fun userGuideServer() = INDEX

    @RequestMapping(value = ["/user-guide/amazon-app-store"])
    fun userGuideAmazon() = INDEX

    @RequestMapping(value = ["/user-guide/samsung-galaxy-store"])
    fun userGuideGalaxy() = INDEX

    @RequestMapping(value = ["/user-guide/huawei-app-gallery"])
    fun userGuideHuawei() = INDEX

    @RequestMapping(value = ["/user-guide/firebase-app-distribution"])
    fun userGuideFirebase() = INDEX

    @RequestMapping(value = ["/user-guide/troubleshooting"])
    fun userGuideTroubleshooting() = INDEX

    @RequestMapping(value = ["/release-notes"])
    fun releaseNotes() = INDEX
}