import React from "react";
import {Prism as SyntaxHighlighter} from "react-syntax-highlighter"

export type CodeBlockProps = { language: string; value: string; }

const CodeBlock = ({language, value}: CodeBlockProps) => {
    return (
        <SyntaxHighlighter language={language}>
            {value}
        </SyntaxHighlighter>
    );
};

export default CodeBlock;
