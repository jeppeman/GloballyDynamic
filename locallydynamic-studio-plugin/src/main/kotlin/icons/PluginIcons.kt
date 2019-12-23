package icons

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object PluginIcons {
    private fun icon(path: String): Icon = IconLoader.getIcon(path, PluginIcons::class.java)

    @JvmField
    val logo1616: Icon = icon("/icons/plugin_logo_16_16.png")
    @JvmField
    val logo8080: Icon = icon("/icons/plugin_logo_80_80.png")
    @JvmField
    val pluginIcon80: Icon = icon("/icons/pluginIcon.svg")
    @JvmField
    val startServer: Icon = icon("/icons/start_server.png")
    @JvmField
    val stopServer: Icon = icon("/icons/stop_server.png")
}