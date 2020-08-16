import React from "react"
import {Box, Container} from "@material-ui/core";
import ReactMarkdown from "react-markdown/with-html";
import CodeBlock from "../../../components/CodeBlock"
import LinkRenderer from "../../../components/LinkRenderer";
import ImageRenderer from "../../../components/ImageRenderer";
import StudioInPlugin from "../../../assets/plugin_in_studio.png"
import ToolWindow from "../../../assets/tool_window.png"
import TopMenu from "../../../assets/top_menu.png"

const markdown = `
GloballyDynamic Studio Plugin
---
[![Jetbrains Plugin](https://img.shields.io/jetbrains/plugin/v/com.jeppeman.globallydynamic?color=green)](https://plugins.jetbrains.com/plugin/14658-globallydynamic)

An Android Studio plugin that embeds a [GloballyDynamic server](./server) that apps can download
split APK:s from during development. 
This allows for a smooth developer experience when testing dynamic delivery locally, no need to emulate downloads -
splits will be downloaded from the embedded server, you can test things such poor network conditions and cancellation
of downloads through system notifications etc with ease. <br/>

In order to use the embedded server it has to be configured through the [Gradle Plugin](./gradle), refer to it's documentation
for more details. 

Code
---
[https://github.com/jeppeman/GloballyDynamic/tree/master/globallydynamic-studio-plugin](https://github.com/jeppeman/GloballyDynamic/tree/master/globallydynamic-studio-plugin)

Usage
---
Download it from [here](https://plugins.jetbrains.com/plugin/14658-globallydynamic), in Android Studio search for
"GloballyDynamic" in plugins and you should see:

![Plugin](${StudioInPlugin})

Features
---
### Server Log
The plugin includes a tool window that displays logs from the GloballyDynamic server, for example requests and server errors.
It looks like this:

![Plugin](${ToolWindow})

This is useful to make sure that the interaction between an app and the server is working as expected.

### Server menu items
A GloballyDynamic group is added to the top menu, this has items for manually starting or stopping the GloballyDynamic server.
It looks like this:

![Plugin](${TopMenu})

Normally you do not need to use this since the server is started automatically (see next section); however, it can be useful 
if you want to test behavior in your app when connection can not be established with the server.

### Automatic server starting before running an app with GloballyDynamic enabled
If GloballyDynamic is enabled for a project and build type, the plugin will automatically start the server before
running the app.

### Example
The video below shows how the gradle plugin and android library interacts with the GloballyDynamic server in 
Android Studio while uploading bundles and downloading splits:

[YouTube](https://www.youtube.com/watch?v=K6CPYHlsJt4)
`;

const Studio = () => {
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

export default Studio;
