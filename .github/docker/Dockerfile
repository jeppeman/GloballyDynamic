FROM openjdk:17-oracle

# Install packages
RUN apt-get update && apt-get install -y sudo file curl

# Install libonig2 (JQ dependency)
RUN curl -L --output libonig2.deb http://security.ubuntu.com/ubuntu/pool/universe/libo/libonig/libonig2_5.9.6-1ubuntu0.1_amd64.deb \
    && apt install ./libonig2.deb

# Install JQ
RUN curl -L --keepalive --output jq.deb http://security.ubuntu.com/ubuntu/pool/universe/j/jq/jq_1.5+dfsg-1ubuntu0.1_amd64.deb \
    && apt install ./jq.deb

# Install Android Studio
RUN curl -C - -L --progress-bar --output android-studio.tar.gz https://storage.googleapis.com/android-studio-distributions/android-studio-2022.2.1.20-linux.tar.gz \
    && tar -xvf android-studio.tar.gz -C opt \
    && rm android-studio.tar.gz

ENV ANDROID_STUDIO_PATH /opt/android-studio
