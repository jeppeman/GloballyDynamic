import React from "react";
import {Box, Container} from "@material-ui/core";
import ReactMarkdown from "react-markdown/with-html";
import CodeBlock from "../components/CodeBlock";
import LinkRenderer from "../components/LinkRenderer";
import ImageRenderer from "../components/ImageRenderer";

const markdown = `
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
