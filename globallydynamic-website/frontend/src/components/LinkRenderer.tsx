import React, {ReactNode} from "react";
import {Link} from "react-router-dom";
import YouTube from "react-youtube";
import {DrawerPageContext} from "./DrawerPage";
import {makeStyles} from "@material-ui/core/styles";

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
                    <YouTube className={classes.youtube} videoId={href.split("?v=")[1]} onReady={updatePageHeight}/>
                    : isLocal ? <Link to={href}>{children}</Link> :
                    <a rel="noopener noreferrer" target="_blank" href={href}>{children}</a>
            }
        </DrawerPageContext.Consumer>
    );
}

export default LinkRenderer;
