import React from "react"
import {Box, Container} from "@material-ui/core";
import ReactMarkdown from "react-markdown/with-html";
import CodeBlock from "../../components/CodeBlock";
import LinkRenderer from "../../components/LinkRenderer";
import ImageRenderer from "../../components/ImageRenderer";
import {versions} from "../../constants";

const markdown = `
# Running a dedicated GloballyDynamic server

In order to enable dynamic delivery for environments without native support for it - for example 
Amazon App Store, Samsung Galaxy Store or internally distributed builds on Firebase App Distribution - one has to run a 
[GloballyDynamic server](/docs/user/server) that is hosted in a location which is reachable from the app you want to 
enable it for.<br/>
For instance, this website has one running at [https://globallydynamic.io/api](https://globallydynamic.io/api) which I
utilize for private projects.<br/>

GloballyDynamic Server is designed to be lightweight and environment agnostic, it can be run on any cloud provider on which
you can run Java - the server on this website is running on [Google Compute Engine](https://cloud.google.com/compute).<br/>
It comes with 3 different options for how to store bundles:
* Locally on the machine where the server is running
* On [Google Cloud Storage](https://cloud.google.com/storage)
* On [Amazon S3](https://aws.amazon.com/s3/)

An upcoming feature of this project is running the server as a service that developers can sign up for and use for free, 
this will eliminate the need for self-hosting a server. In the meantime, however, in order to leverage dynamic delivery
on platforms without native support for it you have to host your own server.

To get started with configuring and running your server, follow the steps below on an environment where git and java are
available:

### Build an executable server jar
\`\`\`shell
# Clone the repo
git clone https://github.com/jeppeman/GloballyDynamic 

# Go to the server lib directory
cd GloballyDynamic/globallydynamic-server-lib

# Build a standalone executable jar
./gradlew executableJar -PoutputDir=$(pwd) -ParchiveName=globallydynamic-server.jar
\`\`\`

### Run the server
Pick one of the 3 aforementioned storage options along with other configuration options:

**Option 1: bundles stored locally**
\`\`\`shell
java -jar globallydynamic-server.jar \\
    --port 8080 \\ # The port the server will listen on
    --username johndoe \\ # The user name for the server (used as part of basic auth for client requests)
    --password my-secret-password \\ # The password for the server (used as part of basic auth for client requests)
    --storage-backend local \\ # The storage backend in which to store uploaded bundles
    --local-storage-path path/to/bundles # The path on the local file system where bundles should be stored
\`\`\`

**Option 2: bundles stored in Google Cloud Storage**
\`\`\`shell
java -jar globallydynamic-server.jar \\
    --port 8080 \\ # The port the server will listen on
    --username johndoe \\ # The user name for the server (used as part of basic auth for client requests)
    --password my-secret-password \\ # The password for the server (used as part of basic auth for client requests)
    --storage-backend gcp \\ # The storage backend in which to store uploaded bundles
    --gcp-bucket-id my-gcp-bucket # The bucket id on google cloud storage in which to store bundles
\`\`\`
**Note**: if the server is *not* running on [GCP](https://cloud.google.com/), you will have to set the environment 
variable \`GOOGLE_APPLICATION_CREDENTIALS\` in order to authenticate with GCP, see more 
[here](https://cloud.google.com/docs/authentication/getting-started#setting_the_environment_variable).

**Option 3: bundles stored on Amazon S3**
\`\`\`shell
java -jar globallydynamic-server.jar \\
    --port 8080 \\ # The port the server will listen on
    --username johndoe \\ # The user name for the server (used as part of basic auth for client requests)
    --password my-secret-password \\ # The password for the server (used as part of basic auth for client requests)
    --storage-backend s3 \\ # The storage backend in which to store uploaded bundles
    --s3-bucket-id my-s3-bucket # The bucket id on google cloud storage in which to store bundles
\`\`\`
**Note**: if the server is *not* running on AWS, you will have to set the following environment variables in order to 
authenticate with AWS:
* \`AWS_ACCESS_KEY_ID\`
* \`AWS_SECRET_KEY\`
* \`AWS_REGION\`

See more [here](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/setup-credentials.html).

You can then use the server from your app in the following way:
\`\`\`groovy
android {
    globallyDynamicServers {
        selfHosted {
            serverUrl "http://<ip-to-server>:8080"
            username "johndoe"
            password "my-secret-password"
            applyToBuildVariants "release"
        }
    }
}

dependencies {
    releaseImplementation "com.jeppeman.globallydynamic.android:selfhosted:${versions.ANDROID}"
}
\`\`\`

### Example: running the server on Google Compute Engine

In order to get up and running quickly with a server on Google Compute Engine you can run the shell command
below after having installed and configured [Google Cloud SDK](https://cloud.google.com/sdk/install). 
\`\`\`shell
curl https://globallydynamic.io/scripts/compute_engine_launch.sh | /usr/bin/env bash
\`\`\`

The command will download and execute a script that does the following:

1. Create a bucket in Google Cloud Storage in which to store bundles
2. Create a VM instance that will start a GloballyDynamic server on machine startup, the server will be listening on port 8080
and have the credentials johndoe/my-secret-password 
3. Create a firewall rule that will allow incoming traffic on port 8080

The last line of output from script will look something like \`Done! Address to server: http://<ip-to-server>:8080\`,
use this address when configuring the app, like so:
\`\`\`groovy
globallyDynamicServers {
    selfHosted {
        serverUrl "http://<ip-to-server>:8080"
        username "johndoe"
        password "my-secret-password"
        applyToBuildVariants "release"
    }
}
\`\`\`

Here is the source of the downloaded script:
\`\`\`shell
#!/usr/bin/env bash

set -e

# Create a bucket in Google Cloud Storage where bundles will be stored
bucket_id="globallydynamic-$(uuidgen | tr '[:upper:]' '[:lower:]')"
gsutil mb -l eu "gs://\${bucket_id}"

port=8080
zone=europe-west1-b
instance_name=globallydynamic

# Environment variables for the VM instance, GloballyDynamic Server configuration and java location
echo "
export GLOBALLY_DYNAMIC_PORT=\${port}
export GLOBALLY_DYNAMIC_USERNAME=johndoe
export GLOBALLY_DYNAMIC_USERNAME=my-secret-password
export GLOBALLY_DYNAMIC_STORAGE_BACKEND=gcp
export GLOBALLY_DYNAMIC_GCP_BUCKET_ID=\${bucket_id}
export GLOBALLY_DYNAMIC_HTTPS_REDIRECT=false
export GLOBALLY_DYNAMIC_OVERRIDE_EXISTING_BUNDLES=false
export GLOBALLY_DYNAMIC_VALIDATE_SIGNATURE_ON_DOWNLOAD=true
export JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk-amd64
" > vm_environment

# Define startup script (called when VM starts), this will start the GloballyDynamic Server
echo "
#!/usr/bin/env bash

set -e
set -o xtrace

# Set up environment (load GloballyDynamic Server configuration)
curl -f http://metadata.google.internal/computeMetadata/v1/instance/attributes/environment -H \"Metadata-Flavor: Google\" > env_file

source env_file

# Run the server
java -jar globallydynamic-server.jar &
" > startup_script

# Create a VM instance
gcloud compute instances create \${instance_name} \\
    --image-project=debian-cloud \\
    --image-family=debian-9 \\
    --tags=http-server,https-server \\
    --scopes=cloud-platform \\
    --zone=\${zone} \\
    --metadata-from-file=environment=vm_environment,startup-script=startup_script
    
# Build an executable server jar in the VM
gcloud compute ssh --ssh-flag="-ttn" --zone \${zone} \${instance_name} -- "sudo apt-get install -y openjdk-8-jdk \\
    && cd / \\
    && sudo curl -L --output /globallydynamic-server.jar https://github.com/jeppeman/GloballyDynamic/releases/download/server-1.0.0/globallydynamic-server-1.0.0-standalone.jar
"

# Allow incoming traffic on port 8080
gcloud compute firewall-rules create allow-http-\${port} \\
    --allow tcp:\${port} \\
    --source-ranges 0.0.0.0/0 \\
    --target-tags http-server \\
    --description "Allow port \${port} access to http-server"
    
# Restart instance
gcloud compute instances stop \${instance_name} --zone \${zone}
gcloud compute instances start \${instance_name} --zone \${zone}

# Cleanup
rm startup_script
rm vm_environment

# Get the ip of the instance
server_ip=$(gcloud compute instances describe \${instance_name} --zone \${zone} --format='get(networkInterfaces[0].accessConfigs[0].natIP)')
server_url="http://\${server_ip}:\${port}"

echo "Waiting for the server to start, this may take a few minutes.."
iterations=0
while [[ -z $(curl -s --max-time 2 \${server_url}) ]];
do
if [[ \${iterations} -gt 60 ]];
then
   echo "Server failed to start"
   exit 1
fi
iterations=$((\${iterations} + 1))
sleep 2
done

echo "Done! Address to server: \${server_url}"

\`\`\`
Note: this script does not create a production worthy setup, in order to convert it to one, you should create a [managed instance group](https://cloud.google.com/compute/docs/instance-groups)
with a [load balancer](https://cloud.google.com/iap/docs/load-balancer-howto) in front. 

For more configuration options, see the [server documentation](/docs/user/server).
`;

const Server = () => {
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

export default Server;
