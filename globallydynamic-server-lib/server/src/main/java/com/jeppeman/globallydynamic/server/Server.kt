package com.jeppeman.globallydynamic.server

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.jeppeman.globallydynamic.server.server.BuildConfig
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.client.HttpClients
import org.eclipse.jetty.server.Handler
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.net.URI

interface GloballyDynamicServer {
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
        val overrideExistingBundles: Boolean,
        val validateSignatureOnDownload: Boolean,
        val hostAddress: String?,
        val logger: Logger,
        val pathHandlers: List<PathHandler>
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
            var overrideExistingBundles: Boolean = false

            @set:JvmSynthetic
            var validateSignatureOnDownload: Boolean = false

            @set:JvmSynthetic
            var storageBackend: StorageBackend = StorageBackend.LOCAL_DEFAULT

            @set:JvmSynthetic
            var pathHandlers = mutableListOf<PathHandler>()

            @set:JvmSynthetic
            var hostAddress: String? = null

            internal constructor(configuration: Configuration) : this() {
                port = configuration.port
                storageBackend = configuration.storageBackend
                username = configuration.username
                password = configuration.password
                httpsRedirect = configuration.httpsRedirect
                hostAddress = configuration.hostAddress
                overrideExistingBundles = configuration.overrideExistingBundles
                validateSignatureOnDownload = configuration.validateSignatureOnDownload
                logger = configuration.logger
            }

            fun setPort(port: Int) = apply { this.port = port }
            fun setStorageBackend(storageBackend: StorageBackend) = apply { this.storageBackend = storageBackend }
            fun setUsername(username: String) = apply { this.username = username }
            fun setPassword(password: String) = apply { this.password = password }
            fun setHttpsRedirect(httpsRedirect: Boolean) = apply { this.httpsRedirect = httpsRedirect }
            fun setHostAddress(hostAddress: String?) = apply { this.hostAddress = hostAddress }
            fun setOverrideExistingBundles(overrideExistingBundles: Boolean) =
                apply { this.overrideExistingBundles = overrideExistingBundles }

            fun setValidateSignatureOnDownload(validateSignatureOnDownload: Boolean) =
                apply { this.validateSignatureOnDownload = validateSignatureOnDownload }

            fun setLogger(logger: Logger) = apply { this.logger = logger }
            fun addPathHandlers(vararg pathHandlers: PathHandler) = apply { this.pathHandlers.addAll(pathHandlers) }

            fun build(): Configuration = Configuration(
                port = port,
                username = username,
                logger = logger,
                password = password,
                httpsRedirect = httpsRedirect,
                hostAddress = hostAddress,
                overrideExistingBundles = overrideExistingBundles,
                validateSignatureOnDownload = validateSignatureOnDownload,
                storageBackend = storageBackend,
                pathHandlers = pathHandlers
            )
        }

        companion object {
            fun builder(): Builder = Builder()
        }
    }

    companion object : (Configuration) -> GloballyDynamicServer {
        override fun invoke(configuration: Configuration): GloballyDynamicServer = GloballyDynamicServerImpl(
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

internal class GloballyDynamicServerImpl(
    private val configuration: GloballyDynamicServer.Configuration,
    private val gson: Gson = GsonBuilder()
        .disableHtmlEscaping()
        .create(),
    private val bundleManager: BundleManager = BundleManager(
        configuration.storageBackend,
        configuration.logger,
        gson,
        configuration.overrideExistingBundles
    ),
    private val server: Server = JettyServerWrapper(configuration.port),
    private val lazyPathHandlers: () -> List<PathHandler> = {
        listOf(
            DownloadSplitsPathHandler(
                bundleManager = bundleManager,
                validateSignature = configuration.validateSignatureOnDownload,
                logger = configuration.logger,
                gson = gson
            ),
            UploadBundlePathHandler(
                bundleManager = bundleManager,
                logger = configuration.logger
            ),
            LivenessPathHandler()
        ) + configuration.pathHandlers
    }
) : GloballyDynamicServer {
    override val address: String by lazy {
        configuration.hostAddress ?: try {
            val httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(
                    RequestConfig.custom()
                        .setConnectTimeout(1000)
                        .setConnectionRequestTimeout(1000)
                        .build()
                )
                .build()

            NetworkInterface.getNetworkInterfaces()
                .toList()
                .flatMap { it.interfaceAddresses }
                .asSequence()
                .map { it.address }
                .filterIsInstance<Inet4Address>()
                .filter { !it.isLoopbackAddress }
                .mapNotNull { server.uri.withHost(it.hostAddress) }
                .find { uri ->
                    try {
                        val response = httpClient.execute(HttpGet("$uri/${LivenessPathHandler().path}"))
                        response.statusLine.statusCode == 200
                    } catch (exception: Exception) {
                        false
                    }
                }?.toString() ?: server.uri.toString()
        } catch (exception: Exception) {
            server.uri.toString()
        }
    }

    private fun URI.withHost(host: String): URI? = try {
        URI(scheme, userInfo, host, port, path, query, fragment)
    } catch (exception: Exception) {
        null
    }

    override val isRunning: Boolean get() = server.isRunning

    override fun start(): String {
        if (!server.isStarted) {
            val pathHandlers = lazyPathHandlers()
            server.handler = RequestHandler(
                configuration = configuration,
                pathHandlers = pathHandlers
            )
            server.start()
            configuration.logger.i("GloballyDynamic Server v${BuildConfig.VERSION} started at $address")
            if (configuration.username.isNotBlank()) {
                configuration.logger.i("Username: ${configuration.username}")
            }
            if (configuration.password.isNotBlank()) {
                configuration.logger.i("Password: ${configuration.password}")
            }
            configuration.logger.i("Storage backend initialized: ${configuration.storageBackend}")
            configuration.logger.i("Https redirects enabled: ${configuration.httpsRedirect}")
            configuration.logger.i("Override existing bundles: ${configuration.overrideExistingBundles}")
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