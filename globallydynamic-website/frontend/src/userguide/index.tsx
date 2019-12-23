import React, {useEffect} from 'react';
import DrawerPage from "../components/DrawerPage"
import GettingStarted from "./getting-started"
import Server from "./server"
import AmazonAppStore from "./amazon-app-store";
import SamsungGalaxyStore from "./samsung-galaxy-store";
import HuaweiAppGallery from "./huawei-app-gallery";
import {useHistory, useLocation, useRouteMatch} from "react-router";
import Troubleshooting from "./troubleshooting";
import Development from "./getting-started/development";
import Complete from "./getting-started/complete";
import FirebaseAppDistribution from "./firebase-app-distribution";

export const userGuideDestinations = (pathPrefix: string) => ([
    {
        text: 'Getting started',
        url: 'getting-started',
        component: GettingStarted,
        exact: true,
        childDestinations: [
            {
                text: 'Development Setup',
                url: 'getting-started/development',
                component: Development
            },
            {
                text: 'Complete Setup',
                url: 'getting-started/complete',
                component: Complete
            }
        ]
    },
    {
        text: 'Running a dedicated GloballyDynamic server',
        url: 'server',
        component: Server
    },
    {
        text: 'Dynamic Delivery for Amazon App Store',
        url: 'amazon-app-store',
        component: AmazonAppStore
    },
    {
        text: 'Dynamic Delivery for Samsung Galaxy Store',
        url: 'samsung-galaxy-store',
        component: SamsungGalaxyStore
    },
    {
        text: 'Dynamic Delivery for Huawei App Gallery',
        url: 'huawei-app-gallery',
        component: HuaweiAppGallery
    },
    {
        text: 'Dynamic Delivery for Firebase App Distribution',
        url: 'firebase-app-distribution',
        component: FirebaseAppDistribution
    },
    {
        text: 'Troubleshooting',
        url: 'troubleshooting',
        component: Troubleshooting
    }
].map(({childDestinations, ...destination}) => ({
    pathPrefix,
    childDestinations: childDestinations?.map(childDestination => ({
        pathPrefix,
        ...childDestination
    })),
    ...destination
})));

const UserGuide = () => {
    const match = useRouteMatch();
    const history = useHistory();
    const location = useLocation();

    useEffect(() => {
        if (location.pathname === "/user-guide") {
            history.replace("/user-guide/getting-started/development")
        }
    }, [location]);

    useEffect(() => {
        document.documentElement.scrollTop = 0;
    });

    return (
        <DrawerPage destinations={userGuideDestinations(match.path)}/>
    );
}

export default UserGuide;
