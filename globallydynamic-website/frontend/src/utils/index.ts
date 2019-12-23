export function findTop(element: HTMLElement | null) {
    let y = 0;
    while (element) {
        y += element.offsetTop;
        element = element.offsetParent as HTMLElement;
    }
    return y;
}
