import React from 'react';
import ReactDOM from 'react-dom';
import './index.css';
import App from './App';
import * as serviceWorker from './serviceWorker';
import {BrowserRouter} from "react-router-dom";
import {ThemeProvider} from '@material-ui/core/styles';
import theme from './theme';
import ReactGA from 'react-ga';

ReactGA.initialize("UA-151430677-1", {
    debug: process
        && process.env
        && (process.env.NODE_ENV !== "production")
});

ReactDOM.render(
    <React.StrictMode>
        <BrowserRouter>
            <ThemeProvider theme={theme}>
                <App/>
            </ThemeProvider>
        </BrowserRouter>
    </React.StrictMode>,
    document.getElementById('root')
);

serviceWorker.unregister();
