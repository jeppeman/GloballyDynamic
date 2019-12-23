import React from "react";
import {Box, Container, Typography} from "@material-ui/core";
import {makeStyles} from "@material-ui/core/styles";

const useStyles = makeStyles(theme => ({
    box: {
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center'
    },
    text: {
        textAlign: "center"
    }
}));

const NotFound = () => {
    const classes = useStyles();
    return (
        <Box className={classes.box} width={window.innerHeight} height={window.innerHeight}>
            <Container>
                <Typography variant={"h3"} className={classes.text}>
                    Oops..
                </Typography>
                <Typography variant={"h6"} className={classes.text}>
                    Looks like the page you are looking for does not exist
                </Typography>
            </Container>
        </Box>
    )
};

export default NotFound;
