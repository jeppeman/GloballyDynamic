#!/usr/bin/env bash

set -e

# Create a bucket in Google Cloud Storage where bundles will be stored
bucket_id=globallydynamic
gsutil mb -l eu "gs://${bucket_id}"

port=8080
zone=europe-west1-b
instance_name=globallydynamic

# Environment variables for the VM instance, GloballyDynamic Server configuration and java location
echo "
export GLOBALLY_DYNAMIC_PORT=${port}
export GLOBALLY_DYNAMIC_STORAGE_BACKEND=gcp
export GLOBALLY_DYNAMIC_GCP_BUCKET_ID=${bucket_id}
export GLOBALLY_DYNAMIC_HTTPS_REDIRECT=false
export GLOBALLY_DYNAMIC_OVERRIDE_EXISTING_BUNDLES=false
export GLOBALLY_DYNAMIC_VALIDATE_SIGNATURE_ON_DOWNLOAD=true
export JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk-amd64
" > vm_environment

# Define startup script (called when VM starts), this start the GloballyDynamic Server
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
gcloud compute instances create ${instance_name} \
    --tags=http-server,https-server \
    --scopes=cloud-platform \
    --zone=${zone} \
    --metadata-from-file=environment=vm_environment,startup-script=startup_script

# Build an executable server jar in the VM
gcloud compute ssh --zone ${zone} ${instance_name} -- "sudo apt-get install -y git openjdk-8-jdk \\
    && cd / \\
    && sudo git clone https://github.com/jeppeman/GloballyDynamic.git \\
    && cd GloballyDynamic/globallydynamic-server-lib \\
    && sudo ./gradlew executableJar -PoutputDir=/ -ParchiveName=globallydynamic-server.jar
"

# Allow incoming traffic on port 8080
gcloud compute firewall-rules create allow-http-${port} \
    --allow tcp:${port} \
    --source-ranges 0.0.0.0/0 \
    --target-tags http-server \
    --description "Allow port ${port} access to http-server"

# Restart instance
gcloud compute instances stop ${instance_name}
gcloud compute instances start ${instance_name}

# Cleanup
rm startup_script
rm vm_environment

# Get the ip of the instance
server_ip=$(gcloud compute instances describe ${instance_name} --format='get(networkInterfaces[0].accessConfigs[0].natIP)')
server_url="http://${server_ip}:${port}"

echo "Waiting for the server to start, this may take a few minutes.."

iterations=0
while [[ -z $(curl -s --max-time 2 ${server_url}) ]];
do
if [[ ${iterations} -gt 60 ]];
then
   echo "Server failed to start"
   exit 1
fi
iterations=$((${iterations} + 1))
sleep 2
done

echo "Done! Address to server: ${server_url}"
