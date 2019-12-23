import React, {ReactElement, useEffect, useRef, useState} from "react";
import {makeStyles, useTheme} from "@material-ui/core/styles";
import {Route, useLocation, useRouteMatch} from "react-router-dom";
import {Box, List, useMediaQuery} from "@material-ui/core";
import {CSSTransition, TransitionGroup} from "react-transition-group";
import {Switch} from "react-router";
import {findTop} from "../utils";
import NotFound from "./NotFound";
import NestedNavListItem from "./NestedNavListItem";

const drawerWidth = 240;

const useStyles = makeStyles(theme => ({
    drawerPageContainer: {
        display: "flex",
    },
    drawer: {
        width: drawerWidth,
        flexShrink: 0,
        background: theme.palette.primary.dark
    },
}));

export type DrawerPageDestination = {
    text: string;
    url: string;
    pathPrefix: string;
    exact?: boolean;
    icon?: ReactElement;
    component: React.ComponentType;
    childDestinations?: DrawerPageDestination[];
}

export type DrawerPageProps = {
    destinations: DrawerPageDestination[];
}

export const DrawerPageContext = React.createContext(() => {
})

const DrawerPage = ({destinations}: DrawerPageProps) => {
    const classes = useStyles();
    const location = useLocation();
    const match = useRouteMatch();
    const nav = useRef(null);
    const theme = useTheme();
    const sectionRef = useRef<HTMLElement>(null);
    const [navTop, setNavTop] = useState(0);
    const [pageHeight, setPageHeight] = useState(0);
    const minWidth600 = useMediaQuery('(min-width:600px)');

    useEffect(() => {
        const newNavTop = findTop(nav.current)
        if (newNavTop !== navTop) {
            setNavTop(newNavTop);
        }

        updatePageHeight();
    });

    const updatePageHeight = () => {
        const newPageHeight = Math.max(sectionRef.current?.scrollHeight || pageHeight, window.innerHeight - navTop)
        console.log(newPageHeight, sectionRef.current)
        if (newPageHeight !== pageHeight) {
            setPageHeight(newPageHeight);
        }
    }

    return (
        <Box className={classes.drawerPageContainer} width="100%">
            {minWidth600
                ? <List {...{ref: nav}}
                        component="nav"
                        className={classes.drawer}
                        style={{minHeight: pageHeight}}>
                    {destinations.map(destination =>
                        <NestedNavListItem key={destination.url}
                                           indent={theme.spacing(2)}
                                           navigateOnParentMenuClick={true}
                                           destination={destination}/>)}
                </List>
                : <></>
            }
            <DrawerPageContext.Provider value={updatePageHeight}>
                <Box width="100%">
                    <TransitionGroup className="transition-group">
                        <CSSTransition key={location.key} classNames="fade" timeout={250}>
                            <section className="route-section" ref={sectionRef}>
                                <Switch location={location}>
                                    {
                                        destinations.flatMap(destination =>
                                            [destination].concat(destination.childDestinations || []))
                                            .map(({url, exact, component: DestinationComponent}) => {
                                                const fullUrl = `${match.path}${url.length > 0 ? "/" : ""}${url}`
                                                return <Route key={fullUrl} exact={exact || !url.length} path={fullUrl}
                                                              component={DestinationComponent}/>
                                            })
                                    }
                                    <Route component={NotFound}/>
                                </Switch>
                            </section>
                        </CSSTransition>
                    </TransitionGroup>
                </Box>
            </DrawerPageContext.Provider>
        </Box>
    )
};

export default DrawerPage;
