import React, {useEffect, useRef, useState} from "react"
import {findTop} from "../utils";

export type JavadocProps = { src: string; }

const Javadoc = ({src}: JavadocProps) => {
    const ref = useRef<HTMLIFrameElement>(null);
    const [height, setHeight] = useState(0);

    useEffect(() => {
        const top = findTop(ref.current);
        const newHeight = window.innerHeight - top;
        if (newHeight !== height) {
            setHeight(newHeight);
        }
    });

    return <iframe frameBorder={0}
                   ref={ref}
                   style={{border: 0}}
                   title="GloballyDynamic Android Javadoc"
                   width="100%"
                   height={height}
                   src={src}/>
}

export default Javadoc;
