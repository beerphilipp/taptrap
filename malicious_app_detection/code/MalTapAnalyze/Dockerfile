FROM --platform=linux/amd64 openjdk:17-slim
# We need to use the amd64 architecture for the Android SDK, as it is not available for arm64 and it fails on mac

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
    "build-tools;34.0.0"

WORKDIR /app

# install gradle 8.9
RUN wget https://services.gradle.org/distributions/gradle-8.9-bin.zip -O gradle.zip \
    && unzip gradle.zip -d /opt/ \
    && rm gradle.zip \
    && ln -s /opt/gradle-8.9/bin/gradle /usr/bin/gradle

COPY . /app

RUN chmod +x entrypoint.sh
ENTRYPOINT ["./entrypoint.sh", "/app"]