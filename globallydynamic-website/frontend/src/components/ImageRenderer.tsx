import React from "react";
import {DrawerPageContext} from "./DrawerPage";

const ImageRenderer = ({alt, src}: any) => {
    return (
        <DrawerPageContext.Consumer>
            {updatePageHeight =>
                <img onLoad={() => updatePageHeight()} alt={alt} src={src}
                     style={{maxWidth: '100%', maxHeight: '100%'}}/>}
        </DrawerPageContext.Consumer>
    )
}

export default ImageRenderer;
