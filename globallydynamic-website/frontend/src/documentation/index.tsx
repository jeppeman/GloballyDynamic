import React, {useEffect} from 'react';
import UserDocs from "./user"
import Javadoc from "./javadoc"
import DrawerPage from "../components/DrawerPage";
import {ReactComponent as Java} from "../assets/java.svg";
import {Android as AndroidIcon, Cloud, Person} from "@material-ui/icons";
import {ReactComponent as GradleIcon} from "../assets/gradle.svg";
import {ReactComponent as StudioIcon} from "../assets/android_studio.svg";
import Android from "./user/android"
import Gradle from "./user/gradle"
import Server from "./user/server"
import Studio from "./user/studio";
import AndroidJavadoc from "./javadoc/android"
import ServerJavadoc from "./javadoc/server"
import {useHistory, useLocation, useRouteMatch} from "react-router";

export const documentationDestinations = (pathPrefix: string) => ([
    {
        text: 'User docs',
        icon: <Person/>,
        exact: true,
        url: 'user',
        component: UserDocs,
        childDestinations: [
            {
                text: 'Android Lib',
                icon: <AndroidIcon/>,
                url: 'user/android',
                component: Android
            },
            {
                text: 'Server Lib',
                icon: <Cloud/>,
                url: 'user/server',
                component: Server
            },
            {
                text: 'Gradle Plugin',
                icon: <GradleIcon/>,
                url: 'user/gradle',
                component: Gradle
            },
            {
                text: 'Studio Plugin',
                icon: <StudioIcon/>,
                url: 'user/studio',
                component: Studio
            }
        ]
    },
    {
        text: 'Javadoc',
        icon: <Java/>,
        url: 'javadoc',
        exact: true,
        component: Javadoc,
        childDestinations: [
            {
                text: 'Android',
                icon: <AndroidIcon/>,
                url: 'javadoc/android',
                component: AndroidJavadoc,
            },
            {
                text: 'Server',
                icon: <Cloud/>,
                url: 'javadoc/server',
                component: ServerJavadoc
            }
        ]
    },
].map(({childDestinations, ...destination}) => ({
    pathPrefix,
    childDestinations: childDestinations.map(childDestination => ({
        pathPrefix,
        ...childDestination
    })),
    ...destination
})));

const Documentation = () => {
    const history = useHistory();
    const location = useLocation();
    const match = useRouteMatch();
    useEffect(() => {
        if (location.pathname === "/docs") {
            history.replace("/docs/user/android")
        }
    }, [location]);

    useEffect(() => {
        document.documentElement.scrollTop = 0;
    });

    return <DrawerPage destinations={documentationDestinations(match.path)}/>;
}

export default Documentation;
