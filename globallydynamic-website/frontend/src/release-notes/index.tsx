import React from "react";
import {Box, Container} from "@material-ui/core";
import ReactMarkdown from "react-markdown/with-html";
import CodeBlock from "../components/CodeBlock";
import LinkRenderer from "../components/LinkRenderer";
import ImageRenderer from "../components/ImageRenderer";

const markdown = `
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
