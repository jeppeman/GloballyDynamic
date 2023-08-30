import React from "react";
import {Box, Container} from "@material-ui/core";
import ReactMarkdown from "react-markdown/with-html";
import CodeBlock from "../components/CodeBlock";
import LinkRenderer from "../components/LinkRenderer";
import ImageRenderer from "../components/ImageRenderer";

const markdown = `
Gradle plugin 1.7.0 (August 2023)
---
* AGP 8.1.1 compatibility.

Android library 1.3.0 (August 2023)
---
* Support for uninstalling modules on demand for the self hosted variant, use as follows:
\`\`\`kotlin
var mySessionId = 0
globalSplitInstallManager.registerListener { state ->
    if (state.sessionId() == mySessionId) {
       GlobalSplitInstallSessionStatus.UNINSTALLED -> ...
       GlobalSplitInstallSessionStatus.UNINSTALLING -> ...
    }
}

globalSplitInstallManager.startUninstall(listOf("mymodule"))
    .addOnSuccessListener { mySessionId = it }
\`\`\`
Note: this is a no-op for the gplay and huawei-variants.

Studio plugin 1.9.0 (August 2023)
---
* Android Studio Giraffe (2022.3.1) compatibility.
* Bump GloballyDynamic Server to 1.5.0

Server 1.5.0 (July 2023)
---
* Make compatible with gradle 8.x

Studio plugin 1.8.0 (July 2023)
---
* Android Studio Flamingo (2022.2.1) compatibility.
* Bump GloballyDynamic Server to 1.3.1
* Reworked the way the plugin determines whether GloballyDynamic is enabled for a module or not by leveraging the Gradle tooling API. 

Server 1.3.1 (July 2023)
---
* Bump to Java 17 source compat
* Fixed the address of GloballyDynamic server; made it so that the default value returns the correct address on the network.

Gradle plugin 1.6.0 (February 2023)
---
* AGP 7.4.1 compatibility.

Studio plugin 1.7.0 (February 2023)
---
* Android Studio Electric Eel (2022.1.1) compatibility.

Studio plugin 1.6.0 (October 2022)
---
* Android Studio Dolphin (2021.3.1) compatibility.

Android library 1.2.0 (July 2022)
---
* Support for customizable HTTP timeouts (read and connect) for the selfhosted variant. 

Note: this requires version > 1.5.0 of the GloballyDynamic gradle plugin.

Gradle plugin 1.5.0 (July 2022)
---
* Support for customizable HTTP timeouts (read and connect). 

Note: this requires version > 1.2.0 of the GloballyDynamic Android plugin selfhosted variant.

Android library 1.1.1 (June 2022)
---
* Android 12 (API level 31 and 32) support.
* Bumped Play Core to 1.10.3.
* Bumped Dynamic Ability to 1.0.17.300.

Studio plugin 1.5.0 (June 2022)
---
* Android Studio Chipmunk compatibility.
* Bumped GloballyDynamic server to 1.2.0.

Gradle plugin 1.4.0 (June 2022)
---
* AGP 7.2.1 compatibility.

Note: This version breaks compatibility with AGP 7.0.0 and below.

Server 1.2.0 (June 2022)
---
* Bumped bundletool to 1.10.0.
* Fixed the address of GloballyDynamic server; made it configurable and made it so that the default value returns the correct address on the network.

Studio plugin 1.4.0 (February 2022)
---
* Android Studio Bumblebee compatibility.

Gradle plugin 1.3.0 (August 2021)
---
* AGP 7.0.0 compatibility

Studio plugin 1.3.0 (August 2021)
---
* Android Studio Arctic Fox compatibility.
* Bump GloballyDynamic Server to 1.1.0

Server 1.1.0 (August 2021)
---
* Bump dependencies

Gradle plugin 1.2.1 (May 2021)
---
* Fix broken APK producer tasks when using AGP 4.2.0.

Studio plugin 1.2.0 (January 2021)
---
* Android Studio 4.2 compatibility.

Gradle plugin 1.2.0 (January 2021)
---
* AGP 4.2 compatibility.

Note: This version breaks compatibility with AGP 4.1 and below.

Studio plugin 1.1.0 (October 2020)
---
* Android Studio 4.1 compatibility.

Gradle plugin 1.1.0 (October 2020)
---
* AGP 4.1 compatibility.
`;

const ReleaseNotes = () => {
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
    )
}

export default ReleaseNotes;
