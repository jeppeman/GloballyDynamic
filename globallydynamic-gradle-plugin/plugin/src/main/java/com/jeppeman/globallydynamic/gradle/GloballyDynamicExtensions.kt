package com.jeppeman.globallydynamic.gradle

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

open class GloballyDynamicServersExtension @Inject constructor(
    objectFactory: ObjectFactory
): NamedDomainObjectContainer<GloballyDynamicServer> by objectFactory.domainObjectContainer(GloballyDynamicServer::class.java) {
    companion object {
        const val NAME = "globallyDynamicServers"
    }
}
