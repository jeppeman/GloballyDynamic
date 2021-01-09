import React, {ReactElement, useEffect} from "react";
import {makeStyles} from "@material-ui/core/styles";
import {Description, Home, NewReleases, School} from "@material-ui/icons";
import {useHistory, useLocation} from "react-router";
import {CssBaseline, useMediaQuery} from "@material-ui/core";
import {documentationDestinations} from "../../documentation";
import {userGuideDestinations} from "../../userguide";
import {DrawerPageDestination} from "../DrawerPage";
import Desktop from "./Desktop";
import Mobile from "./Mobile";

const useStyles = makeStyles(theme => ({
    root: {
        display: "flex",
        flexGrow: 1,
        flexDirection: 'column'
    },
}));

export type HeaderLocation = {
    title: string;
    text: string;
    icon: ReactElement;
    url: string
    destinations?: DrawerPageDestination[];
};

const locations = [
    {
        title: 'Home',
        text: 'GloballyDynamic',
        icon: <Home/>,
        url: '/'
    },
    {
        title: 'Docs',
        text: 'Docs',
        icon: <Description/>,
        url: '/docs',
        destinations: documentationDestinations('/docs')
    },
    {
        title: 'User Guide',
        text: 'User Guide',
        icon: <School/>,
        url: '/user-guide',
        destinations: userGuideDestinations('/user-guide')
    },
    {
        title: 'Release notes',
        text: 'Release notes',
        icon: <NewReleases />,
        url: '/release-notes'
    }
];

const Header = () => {
    const minWidth600 = useMediaQuery('(min-width:600px)');
    const location = useLocation();
    const history = useHistory();
    const classes = useStyles();
    const selectedLocation = locations.find(loc =>
        (loc.url === '/' && loc.url === location.pathname)
        || (loc.url !== '/' && location.pathname.indexOf(loc.url) !== -1));

    useEffect(() => {
        if (location.pathname !== "/" && location.pathname.endsWith("/")) {
            history.replace(location.pathname.substr(0, location.pathname.length - 1))
        }
    });

    return (
        <div className={classes.root}>
            <CssBaseline/>
            {minWidth600
                ? <Desktop selectedLocation={selectedLocation} locations={locations}/>
                : <Mobile selectedLocation={selectedLocation} locations={locations}/>
            }
        </div>
    )
}

export default Header;
