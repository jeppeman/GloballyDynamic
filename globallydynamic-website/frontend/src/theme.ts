import {red} from '@material-ui/core/colors';
import {createMuiTheme} from '@material-ui/core/styles';

const theme = createMuiTheme({
    palette: {
        primary: {
            main: '#436b78',
            dark: '#4b7786',
        },
        secondary: {
            main: '#debd74',
        },
        error: {
            main: red.A400,
        },
        text: {
            primary: '#555555',
            secondary: '#ddd',
            disabled: 'yellow',
            hint: '#debd74'
        },
        background: {
            default: '#fff',
            paper: '#436b78'
        },
        common: {}
        // action: {
        //     selected:
        // }
    }
});

export default theme;
