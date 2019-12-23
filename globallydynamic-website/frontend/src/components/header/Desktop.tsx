import React, {useEffect, useRef, useState} from "react";
import {HeaderLocation} from "./index";
import {AppBar, Box, Tab, Tabs, Typography} from "@material-ui/core";
import {Link} from "react-router-dom";
import {makeStyles} from "@material-ui/core/styles";
import {pageNames} from "../../constants";
import {ArrowForwardIos} from "@material-ui/icons";
import {useLocation} from "react-router";

const useStyles = makeStyles(theme => ({
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
        width: '100%',
        flexShrink: 0,
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
    arrow: {
        fill: theme.palette.text.secondary,
        marginLeft: theme.spacing(2),
        marginRight: theme.spacing(2),
    },
    tabPanel: {
        paddingTop: theme.spacing(5),
        paddingBottom: theme.spacing(5),
        background: theme.palette.primary.dark,
        color: theme.palette.text.secondary,
    },
}));

function a11yProps(index: number) {
    return {
        id: `simple-tab-${index}`,
        'aria-controls': `simple-tabpanel-${index}`,
    };
}

interface TabPanelProps {
    children?: React.ReactNode;
    className?: string;
    style: any;
    index: any;
    value: any;
}

function TabPanel(props: TabPanelProps) {
    const {children, value, index, ...other} = props;

    return (
        <div
            role="tabpanel"
            hidden={value !== index}
            id={`simple-tabpanel-${index}`}
            aria-labelledby={`simple-tab-${index}`}
            {...other}
        >
            {value === index && (
                <Box p={3}>
                    <Typography variant={"h4"}>{children}</Typography>
                </Box>
            )}
        </div>
    );
}

export type DesktopProps = {
    selectedLocation?: HeaderLocation;
    locations: HeaderLocation[];
};

const Desktop = ({selectedLocation, locations}: DesktopProps) => {
    const [marginTop, setMarginTop] = useState(0);
    const appBarRef = useRef<HTMLElement>(null)
    const location = useLocation();
    const locationText = selectedLocation ? selectedLocation.text : "";
    const tabIndex = locations.findIndex(item => item.text === locationText)
    const classes = useStyles();
    const panelTextFromPath = pageNames[location.pathname]
        ? <div>
            {
                pageNames[location.pathname].map((text, index) =>
                    <span key={index}>{`${text}`}{index < pageNames[location.pathname].length - 1 ?
                        <ArrowForwardIos className={classes.arrow}/> : <></>}</span>)
            }
        </div>
        : null;

    useEffect(() => {
        const newMarginTop = appBarRef.current?.clientHeight || 0;
        if (newMarginTop > marginTop) {
            setMarginTop(newMarginTop)
        }
    });

    return (
        <>
            <AppBar position="fixed" ref={appBarRef}>
                <Tabs value={tabIndex}>
                    {locations.map(({title, text, icon, url}, index) => {
                        return (
                            <Tab label={title}
                                 key={index}
                                 icon={icon}
                                 to={{pathname: url, state: {from: location.pathname}}}
                                 {...a11yProps(index)}
                                 component={tabIndex === index ? "div" : Link}/>
                        )
                    })}
                </Tabs>
            </AppBar>
            {locations.map(({text}, index) =>
                <TabPanel key={index}
                          className={classes.tabPanel}
                          value={tabIndex}
                          style={{marginTop: marginTop}}
                          index={index}>{panelTextFromPath || text}</TabPanel>)}
        </>
    );
};

export default Desktop;
