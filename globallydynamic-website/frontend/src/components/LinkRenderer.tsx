import React, {ReactNode} from "react";
import {Link} from "react-router-dom";
import YouTube from "react-youtube";
import {DrawerPageContext} from "./DrawerPage";
import {makeStyles} from "@material-ui/core/styles";
import ResizeDetector from 'react-resize-detector';
import ReactResizeDetector from "react-resize-detector";

const useStyles = makeStyles(theme => ({
    youtube: {
        width: '100%'
    }
}));

const LinkRenderer = ({children, href}: { children: ReactNode, href: string; }) => {
    const classes = useStyles();
    const isYoutube = !!href.match("youtube.com")
    const isLocal = !(!!href.match("https?://.+"))
    return (
        <DrawerPageContext.Consumer>
            {updatePageHeight =>
                isYoutube ?
                    <ResizeDetector handleHeight={true}>
                        {() => {
                            updatePageHeight();
                            return <YouTube className={classes.youtube} videoId={href.split("?v=")[1]}/>
                        }}
                    </ResizeDetector>
                    : isLocal ? <Link to={href}>{children}</Link> :
                    <a rel="noopener noreferrer" target="_blank" href={href}>{children}</a>
            }
        </DrawerPageContext.Consumer>
    );
}

export default LinkRenderer;
