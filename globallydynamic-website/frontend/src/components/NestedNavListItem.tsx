import React, {useEffect, useRef, useState} from "react";
import {Link, useHistory, useLocation} from "react-router-dom";
import {makeStyles} from "@material-ui/core/styles";
import {List, ListItem, ListItemIcon, ListItemText} from "@material-ui/core";
import {ExpandLess, ExpandMore} from "@material-ui/icons";
import {DrawerPageDestination} from "./DrawerPage";

const useStyles = makeStyles(theme => ({
    listItem: {
        '& .MuiTouchRipple-root': {
            color: theme.palette.text.secondary,
        }
    },
    listText: {
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
    subList: {
        transition: "height 250ms linear",
    },
    subListWrapper: {
        overflow: "hidden"
    }
}));

export type NestedNavListProps = {
    navigateOnParentMenuClick: boolean;
    destination: DrawerPageDestination;
    indent: number;
}

let animationTimeout;

export const NestedNavListItem = (
    {
        destination: {pathPrefix, text, icon, url, childDestinations},
        indent,
        navigateOnParentMenuClick
    }: NestedNavListProps
) => {
    const classes = useStyles();
    const location = useLocation();
    const subListRef = useRef<HTMLDivElement>(null);
    const history = useHistory();
    const [subListHeight, setSubListHeight] = useState(0);
    const fullUrl = `${pathPrefix}${url.length > 0 ? "/" : ""}${url}`
    const selected = (!(!!url) && pathPrefix === location.pathname)
        || (!!url && location.pathname === fullUrl);
    const nestedPathSelected = (!(!!url) && pathPrefix === location.pathname)
        || (!!url && location.pathname.indexOf(fullUrl) !== -1);
    const [open, setOpen] = useState(selected || nestedPathSelected);

    const listItemClassName = `${classes.listItem}${selected ? " selected" : ""}`
    const listTextClassName = `${classes.listText}${selected ? " selected" : ""}`
    const listIconClassName = `${classes.listIcon}${selected ? " selected" : ""}`
    const shouldRenderList = !navigateOnParentMenuClick || (childDestinations && nestedPathSelected)

    useEffect(() => {
        const newHeight = subListRef.current?.clientHeight || 0;
        if (newHeight > subListHeight) {
            setSubListHeight(newHeight)
        }
        if (selected && childDestinations) {
            history.replace(childDestinations[0].url || "")
        }
    });

    useEffect(() => {
        setOpen(selected || nestedPathSelected);
    }, [selected, nestedPathSelected])

    return (
        <>
            <ListItem button
                      style={{paddingLeft: indent}}
                      selected={selected}
                      className={listItemClassName}
                      to={{pathname: fullUrl, state: {from: location.pathname}}}
                      onClick={navigateOnParentMenuClick
                          ? () => {
                          }
                          : (e: React.SyntheticEvent) => {
                              setOpen(!open);
                              e.stopPropagation();
                              e.preventDefault();
                          }
                      }
                      component={navigateOnParentMenuClick ? Link : "div"}>
                {icon
                    ?
                    <ListItemIcon className={listIconClassName}>
                        {React.cloneElement(icon, {className: listIconClassName})}
                    </ListItemIcon>
                    : <></>
                }
                <ListItemText className={listTextClassName} primary={text}/>
                {childDestinations ?
                    open
                        ? <ExpandLess className={listIconClassName}/>
                        : <ExpandMore className={listIconClassName}/>
                    : <></>
                }
            </ListItem>
            <div className={classes.subListWrapper}>
                <List style={{
                    height: open && shouldRenderList && subListHeight > 0
                        ? subListHeight
                        : subListHeight <= 0
                            ? 'auto'
                            : 0
                }}
                      component="div"
                      disablePadding
                      className={classes.subList}
                      ref={subListRef}>
                    {
                        (childDestinations || []).map(({text, icon, url}) => {
                            const fullUrl = `${pathPrefix}${url.length > 0 ? "/" : ""}${url}`
                            const nestedPathSelected = (!(!!url) && pathPrefix === location.pathname)
                                || (!!url && location.pathname.indexOf(fullUrl) !== -1);

                            const nestedListItemClassName = `${classes.listItem}${nestedPathSelected ? " selected" : ""}`
                            const nestedListTextClassName = `${classes.listText}${nestedPathSelected ? " selected" : ""}`
                            const nestedListIconClassName = `${classes.listIcon}${nestedPathSelected ? " selected" : ""}`

                            return (
                                <ListItem button
                                          className={nestedListItemClassName}
                                          style={{paddingLeft: indent * 2}}
                                          key={url}
                                          selected={nestedPathSelected}
                                          to={{
                                              pathname: fullUrl,
                                              state: {from: location.pathname}
                                          }}
                                          component={Link}>
                                    {icon ? <ListItemIcon className={nestedListIconClassName}>
                                        {React.cloneElement(icon, {className: nestedListIconClassName})}
                                    </ListItemIcon> : <></>}
                                    <ListItemText
                                        className={nestedListTextClassName}
                                        primary={text}/>
                                </ListItem>
                            );
                        })
                    }
                </List>
            </div>
        </>
    )
}

export default NestedNavListItem;
