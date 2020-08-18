import React from "react";
import {Box, Container} from "@material-ui/core";
import ReactMarkdown from "react-markdown/with-html";
import CodeBlock from "../../components/CodeBlock";
import LinkRenderer from "../../components/LinkRenderer";
import ImageRenderer from "../../components/ImageRenderer";
import ToolWindow from "../../assets/tool_window.png"

const markdown = `
# Troubleshooting

If things aren't working as expected, the cause can hopefully be found in either the server logs or LogCat; the Android Studio plugin includes
a tool window on the bottom named GloballyDynamic Log where all requests and server errors will be logged - it looks as follows:

![Tool Window](${ToolWindow})

Any operation carried out by the android library is logged under the tag \`I/GloballyDynamic\`, filter on it to find any errors produced.

If you have trouble reaching the android studio integrated server from your app, make sure that your machine is reachable on your network.
If you have a Samsung with Secure WiFi, http requests to the server may be blocked by it, if so, disable Secure WiFi.

### Android 11 (R) with selfhosted artifact
If your app is using the com.jeppeman.globallydynamic.android:selfhosted artifact while targeting Android R and [Scoped Storage](https://developer.android.com/preview/privacy/storage) is enabled,
the app requires a full restart after the user has granted permission to install from unknown sources.
When granting the permission the system will kill and then recreate the app and bring it back to the state in which
it was prior to dying. This can be handled gracefully in the following manner:
\`\`\`kotlin
fun installMyModule() {
    val request = GlobalSplitInstallRequest.newBuilder()
        .addModule("myModule")
        .build()
        
    globalSplitInstallManager.registerListener(GlobalSplitInstallUpdatedListener { state ->
        if (state.status() == GlobalSplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION) {
            globalSplitInstallManager.startConfirmationDialogForResult(state, myActivity, MY_REQUEST_CODE)
        }
    })
    
    globalSplitInstallManager.startInstall(request)
}

override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == MY_REQUEST_CODE && data?.hasExtra(GlobalSplitInstallConfirmResult.EXTRA_RESULT) == true) {
        val installConfirmResult = data.getIntExtra(
                GlobalSplitInstallConfirmResult.EXTRA_RESULT,
                GlobalSplitInstallConfirmResult.RESULT_DENIED
        )
        
        if (installConfirmResult == GlobalSplitInstallConfirmResult.RESULT_CONFIRMED) {
            // The app recovered from the force stop and has permission to install, run the install again
            installMyModule()
        } else {
            // The user did not grant install permissions, do something else
        }
    }
}
\`\`\`

**Note:** if the app does not automatically restart after the user has granted the permission, it is probably due to
Android Studio preventing it from doing so; when testing this flow you should sever the app's connection to Android Studio,
either by pressing the Stop button in Android Studio and the manually restarting the app, or installing the app through
\`./gradlew install\` and then manually starting it.
`;

const Troubleshooting = () => {
    return (
        <Box>
            <Container>
                <ReactMarkdown
                    escapeHtml={false}
                    source={markdown}
                    renderers={
                        {
                            code: CodeBlock,
                            link: LinkRenderer,
                            image: ImageRenderer
                        }
                    }/>
            </Container>
        </Box>
    );
}

export default Troubleshooting;

