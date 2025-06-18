FROM eclipse-temurin:17-jdk-jammy
# We need to force-use AMD64 as the Android SDK is
# not available for ARM and therefore fails on Mac with Apple Silicon

# Install required packages
RUN apt-get update && apt-get install -y --no-install-recommends \
    unzip wget git curl ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Set environment variables
ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV PATH=$PATH:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools

# Download Android SDK command-line tools
RUN mkdir -p ${ANDROID_SDK_ROOT}/cmdline-tools \
    && cd ${ANDROID_SDK_ROOT}/cmdline-tools \
    && wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O tools.zip \
    && unzip tools.zip \
    && mv cmdline-tools latest \
    && rm tools.zip

# Accept licenses and install SDK components
RUN yes | sdkmanager --licenses

# Install Android 15 platform (API 35) and required build tools
RUN sdkmanager \
    "platform-tools" \
    "platforms;android-35" \
    "build-tools;35.0.0"

WORKDIR /app

# install gradle 8.9
RUN wget https://services.gradle.org/distributions/gradle-8.9-bin.zip -O gradle.zip \
    && unzip gradle.zip -d /opt/ \
    && rm gradle.zip \
    && ln -s /opt/gradle-8.9/bin/gradle /usr/bin/gradle

COPY . /app

# Create the debug keystore used to sign the debug build of the app
RUN mkdir -p $HOME/.android && \
    keytool -genkey -v -keystore $HOME/.android/debug.keystore \
    -storepass android -keypass android \
    -alias androiddebugkey -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=Android Debug,O=Android,C=US"

RUN gradle assembleDebug --stacktrace

# create a local.properties file and set the sdk.dir to the android sdk path
#RUN echo "sdk.dir=${ANDROID_SDK_ROOT}" > /workspace/local.properties

CMD ["cp", "app/build/outputs/apk/debug/app-debug.apk", "/apk-output/click.taptrap.poc.apk"]