import React, {useEffect} from 'react';
import './App.scss';
import {Route, Switch, useLocation} from "react-router";
import {Box, useMediaQuery} from "@material-ui/core";
import {CSSTransition, TransitionGroup} from "react-transition-group";
import Home from "./home"
import Documentation from "./documentation"
import UserGuide from "./userguide"
import Header from "./components/header"
import NotFound from "./components/NotFound"
import ReactGA from "react-ga";
import {makeStyles} from "@material-ui/core/styles";
import {pageNames} from "./constants";
import ReleaseNotes from "./release-notes";

const useStyles = makeStyles(theme => {
    const headerStyles = ['h1', 'h2', 'h3', 'h4', 'h5', 'h6'].reduce((acc, curr) => ({
        ...acc,
        [`& ${curr}`]: {
            marginBottom: theme.spacing(4),
            marginTop: theme.spacing(4),
            ...(curr === 'h1' ? {fontSize: '1.7em'} : {})
        }
    }), {})
    return {
        root: {
            '&.mobile': {
                marginTop: 64,
            },
            '& a': {
                color: '#0000ff'
            },
            '& div.transition-group': {
                position: 'relative'
            },
            '& section.route-section': {
                position: 'absolute',
                width: '100%',
                top: 0,
                left: 0
            },
            ...headerStyles
        }
    }
});

const App = () => {
    const classes = useStyles();
    const location = useLocation();
    const minWidth600 = useMediaQuery('(min-width:600px)');
    const pathSegments = location.pathname.split("/");

    useEffect(() => {
        const pageName = pageNames[location.pathname];
        ReactGA.pageview(location.pathname, undefined, pageName ? pageName.join(" > ") : undefined);
    }, [location.pathname]);

    return (
        <Box height="100%">
            <Header/>
            <Box width="100%"
                 className={`${classes.root}${!minWidth600 ? ' mobile' : ''}`}>
                <TransitionGroup className="transition-group">
                    <CSSTransition key={pathSegments[1]} classNames={"fade"} timeout={250}>
                        <section className="route-section">
                            <Switch location={location}>
                                <Route exact path='/' component={Home}/>
                                <Route path='/docs' component={Documentation}/>
                                <Route path='/user-guide' component={UserGuide}/>
                                <Route path='/release-notes' component={ReleaseNotes}/>
                                <Route component={NotFound}/>
                            </Switch>
                        </section>
                    </CSSTransition>
                </TransitionGroup>
            </Box>
        </Box>
    )
};

export default App;
