package com.jeppeman.locallydynamic.server

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.eclipse.jetty.server.Handler
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URI

interface LocallyDynamicServer {
    val address: String?
    val isRunning: Boolean
    fun start(): String
    fun join()
    fun stop()

    class Configuration private constructor(
        val port: Int,
        val storageBackend: StorageBackend,
        val username: String,
        val password: String,
        val httpsRedirect: Boolean,
        val logger: Logger
    ) {
        fun newBuilder(): Builder = Builder(this)

        class Builder internal constructor() {
            @set:JvmSynthetic
            var port: Int = 0
            @set:JvmSynthetic
            var username: String = ""
            @set:JvmSynthetic
            var password: String = ""
            @set:JvmSynthetic
            var logger: Logger = Logger()
            @set:JvmSynthetic
            var httpsRedirect: Boolean = false
            @set:JvmSynthetic
            var storageBackend: StorageBackend = StorageBackend.LOCAL_DEFAULT

            internal constructor(configuration: Configuration) : this() {
                port = configuration.port
                storageBackend = configuration.storageBackend
                username = configuration.username
                password = configuration.password
                httpsRedirect = configuration.httpsRedirect
                logger = configuration.logger
            }

            fun setPort(port: Int) = apply { this.port = port }
            fun setStorageBackend(storageBackend: StorageBackend) = apply { this.storageBackend = storageBackend }
            fun setUsername(username: String) = apply { this.username = username }
            fun setPassword(password: String) = apply { this.password = password }
            fun setHttpsRedirect(httpsRedirect: Boolean) = apply { this.httpsRedirect = httpsRedirect }
            fun setLogger(logger: Logger) = apply { this.logger = logger }

            fun build(): Configuration = Configuration(
                port = port,
                username = username,
                logger = logger,
                password = password,
                httpsRedirect = httpsRedirect,
                storageBackend = storageBackend
            )
        }

        companion object {
            fun builder(): Builder = Builder()
        }
    }

    companion object : (Configuration) -> LocallyDynamicServer {
        override fun invoke(configuration: Configuration): LocallyDynamicServer = LocallyDynamicServerImpl(
            configuration = configuration
        )
    }
}

internal interface Server {
    val uri: URI
    val isRunning: Boolean
    val isStarted: Boolean
    var handler: Handler
    fun start()
    fun stop()
    fun join()
    fun destroy()
}

typealias JettyServer = org.eclipse.jetty.server.Server

internal class JettyServerWrapper(
    private val port: Int,
    private val jettyServer: JettyServer = JettyServer(port).apply {
        stopAtShutdown = true
    }
) : Server {
    override val uri: URI get() = jettyServer.uri
    override val isRunning: Boolean get() = jettyServer.isRunning
    override val isStarted: Boolean get() = jettyServer.isStarted
    override var handler: Handler
        get() = jettyServer.handler
        set(value) {
            jettyServer.handler = value
        }

    override fun start() = jettyServer.start()
    override fun stop() = jettyServer.stop()
    override fun join() = jettyServer.join()
    override fun destroy() = jettyServer.destroy()
}

internal class LocallyDynamicServerImpl(
    private val configuration: LocallyDynamicServer.Configuration,
    private val gson: Gson = GsonBuilder()
        .disableHtmlEscaping()
        .create(),
    private val deviceManager: DeviceManager = DeviceManager(configuration.storageBackend, gson),
    private val bundleManager: BundleManager = BundleManager(configuration.storageBackend, configuration.logger, gson),
    private val server: Server = JettyServerWrapper(configuration.port),
    private val lazyPathHandlers: () -> List<PathHandler> = {
        listOf(
            DownloadSplitsPathHandler(
                bundleManager = bundleManager,
                deviceManager = deviceManager,
                logger = configuration.logger
            ),
            RegisterDevicePathHandler(
                deviceManager = deviceManager,
                gson = gson,
                logger = configuration.logger
            ),
            UploadBundlePathHandler(
                bundleManager = bundleManager,
                logger = configuration.logger
            ),
            LivenessPathHandler()
        )
    }
) : LocallyDynamicServer {
    override val address: String
        get() = try {
            Socket().run {
                connect(InetSocketAddress("google.com", 80))
                URI(server.uri.scheme, server.uri.userInfo, localAddress.hostAddress,
                    server.uri.port, server.uri.path, server.uri.query, server.uri.fragment).toString()
            }
        } catch (exception: Exception) {
            "${server.uri}"
        }

    override val isRunning: Boolean get() = server.isRunning

    override fun start(): String {
        if (!server.isStarted) {
            val pathHandlers = lazyPathHandlers()
            server.handler = RequestHandler(
                username = configuration.username,
                password = configuration.password,
                httpsRedirect = configuration.httpsRedirect,
                pathHandlers = pathHandlers,
                logger = configuration.logger
            )
            server.start()
            configuration.logger.i("Server started at $address")
            if (configuration.username.isNotBlank()) {
                configuration.logger.i("Username: ${configuration.username}")
            }
            if (configuration.password.isNotBlank()) {
                configuration.logger.i("Password: ${configuration.password}")
            }
            configuration.logger.i("Storage backend initialized: ${configuration.storageBackend}")
            configuration.logger.i("Https redirects enabled: ${configuration.httpsRedirect}")
            pathHandlers.forEach { pathHandler ->
                configuration.logger.i("${pathHandler::class.java.simpleName} registered for path /${pathHandler.path}")
            }
        }

        return address
    }

    override fun join() {
        server.join()
    }

    override fun stop() {
        server.stop()
        server.destroy()
        configuration.logger.i("Server stopped")
    }
}