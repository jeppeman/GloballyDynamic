import React, {useEffect, useRef, useState} from "react";
import {
    AppBar,
    Box,
    Drawer,
    IconButton,
    List,
    ListItem,
    ListItemIcon,
    ListItemText,
    Toolbar,
    Typography,
    useTheme
} from "@material-ui/core";
import {ReactComponent as MenuOpened} from "../../assets/menu_opened.svg";
import {ReactComponent as MenuClosed} from "../../assets/menu_closed.svg";
import {Link, useRouteMatch} from "react-router-dom";
import NestedNavListItem from "../NestedNavListItem";
import {makeStyles} from "@material-ui/core/styles";
import {HeaderLocation} from "./index";
import {useLocation} from "react-router";
import {ExpandLess, ExpandMore} from "@material-ui/icons";

const useStyles = makeStyles(theme => ({
    menuButton: {
        marginRight: theme.spacing(2),
    },
    title: {
        color: theme.palette.text.secondary,
        flexGrow: 1,
        fontSize: '1.4285714285714284rem',
        fontWeight: 500,
        lineHeight: 1.6,
        letterSpacing: '0.0075rem'
    },
    icon: {
        fill: theme.palette.text.secondary,
        width: "1em",
        height: "1em",
        display: "inline-block",
        fontSize: "1.5rem",
        transition: "fill 200ms cubic-bezier(0.4, 0, 0.2, 1) 0ms",
        flexShrink: 0,
        userSelect: "none"
    },
    appBar: {
        zIndex: theme.zIndex.drawer + 1,
    },
    drawer: {
        transition: "max-height 250ms linear",
        width: '100%',
        flexShrink: 0,
        overflow: "hidden",
        padding: 0
    },
    drawerPaper: {
        width: '100%',
    },
    toolbar: theme.mixins.toolbar,
    listItem: {
        color: theme.palette.text.secondary,
        '&.selected': {
            color: theme.palette.text.hint
        }
    },
    listIcon: {
        fill: theme.palette.text.secondary,
        '&.selected': {
            fill: theme.palette.text.hint
        }
    },
    expand: {
        alignSelf: 'flex-end',
        color: theme.palette.text.secondary,
        marginRight: theme.spacing(4)
    }
}));

export type MobileProps = {
    selectedLocation?: HeaderLocation;
    locations: HeaderLocation[];
}

const MOBILE_ROOT_TITLE = 'GloballyDynamic'

type DrawerListItemProps = {
    location: HeaderLocation;
    setTitle: (title: string) => void;
    setDrawerOpen: (open: boolean) => void;
}

const DrawerListItem = ({setDrawerOpen, setTitle, location: {text, title, icon, url, destinations}}: DrawerListItemProps) => {
    const location = useLocation();
    const theme = useTheme();
    const classes = useStyles();
    const match = useRouteMatch();
    const [subListHeight, setSubListHeight] = useState(0);
    const selected = (url === '/' && url === location.pathname)
        || (url !== '/' && location.pathname.indexOf(url) !== -1);
    const nestedPathSelected = (!(!!url) && match.path === location.pathname)
        || (!!url && location.pathname.indexOf(url) !== -1);

    const [open, setOpen] = useState(selected || nestedPathSelected);
    const subListRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        const newHeight = subListRef.current?.clientHeight || 0;
        if (newHeight > subListHeight) {
            setSubListHeight(newHeight)
        }
    });

    useEffect(() => {
        setDrawerOpen(false);
        setOpen(selected || nestedPathSelected);
    }, [location.pathname]);

    return (
        <ListItem button
                  key={text}
                  selected={selected}
                  component={!destinations ? Link : "div"}
                  to={{pathname: url, state: {from: location.pathname}}}
                  style={{
                      paddingLeft: 0,
                      paddingRight: 0,
                      display: 'flex',
                      flexDirection: 'column',
                      alignItems: 'flex-start'
                  }}
                  replace={selected}
                  onClick={() => {
                      if (!destinations) {
                          setTitle(url === '/' ? MOBILE_ROOT_TITLE : text);
                          setDrawerOpen(false)
                      } else {
                          setOpen(!open);
                      }
                  }}>
            <Box style={{display: 'flex', flexDirection: 'row', width: '100%'}}>
                <ListItemIcon
                    style={{marginLeft: theme.spacing(2)}}>{React.cloneElement(icon, {className: `${classes.listIcon}${selected && !destinations ? ' selected' : ''}`})}</ListItemIcon>
                <ListItemText primary={title}
                              className={`${classes.listItem}${selected && !destinations ? ' selected' : ''}`}/>
                {
                    destinations ?
                        open
                            ? <ExpandLess className={classes.expand}/>
                            : <ExpandMore className={classes.expand}/>
                        : <></>
                }
            </Box>
            {
                destinations
                    ? <List component="nav"
                            ref={subListRef}
                            style={{
                                maxHeight: open && subListHeight > 0
                                    ? subListHeight
                                    : subListHeight <= 0
                                        ? 'auto'
                                        : 0
                            }}
                            className={classes.drawer}>
                        {destinations.map(destination =>
                            <NestedNavListItem key={destination.url}
                                               indent={theme.spacing(6)}
                                               navigateOnParentMenuClick={!(!!destination.childDestinations)}
                                               destination={destination}/>)}
                    </List>
                    : <></>
            }
        </ListItem>
    );
};

const Mobile = ({selectedLocation, locations}: MobileProps) => {
    const [title, setTitle] = useState("");
    const [open, setOpen] = useState(false);
    const classes = useStyles();
    const locationText = selectedLocation
        ? selectedLocation.url === '/'
            ? MOBILE_ROOT_TITLE
            : selectedLocation.text
        : "";

    if (locationText && locationText !== title) {
        setTitle(locationText)
    }

    return (
        <>
            <AppBar className={classes.appBar} position="fixed">
                <Toolbar>
                    <IconButton edge="start" className={classes.menuButton} color="inherit" aria-label="menu"
                                onClick={() => setOpen(!open)}>
                        {
                            open
                                ? <MenuOpened className={classes.icon}/>
                                : <MenuClosed className={classes.icon}/>
                        }
                    </IconButton>
                    <Typography variant={selectedLocation?.url === '/' ? "h1" : "h6"} className={classes.title}>
                        {title}
                    </Typography>
                </Toolbar>
            </AppBar>
            <Drawer
                className={classes.drawer}
                variant={"persistent"}
                classes={{paper: classes.drawerPaper}}
                open={open}>
                <div className={classes.toolbar}/>
                <List>
                    {locations.map(location => <DrawerListItem key={location.url}
                                                               setTitle={setTitle}
                                                               setDrawerOpen={setOpen}
                                                               location={location}/>)}
                </List>
            </Drawer>
        </>
    );
};

export default Mobile;
