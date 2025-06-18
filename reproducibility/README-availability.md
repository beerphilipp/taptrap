# Artifact Evaluation Experiments

This directory is intended to contain utility scripts required to reproduce the experiments described in the paper, specifically for functionality and reproducibility evaluation.

> [!IMPORTANT]
> This directory will contain further utility script once the artifact's availability has been verified.

## Folder Structure

- `start_emulator.sh` - Script to start an Android 15 Pixel 6a emulator

## APK Dataset

To test the reproducibility of our experiments, you need to have access to the APKs analyzed in the paper. 
Artifact evaluators may send us their public SSH key to get access to our fileserver. 

## Prepare and Run the Experiments

### Install the Prerequisites

Instructions on installing the prerequisites assume a device running Ubuntu 24.04. For other operating systems, follow the official instructions.

#### Install Docker (necessary)

- Download the installation script
  ```sh
  curl -fsSL https://get.docker.com -o docker.sh
  ```
- Install Docker:
  ```sh
  sudo sh docker.sh
  ```

>[!NOTE]
> You may also follow the official instructions at https://docs.docker.com/get-docker/.


#### Android Studio (recommended)

We recommend installing Android Studio to inspect the source code of the apps. Follow these steps:

- Install dependencies (if you are runnging on a 64-bit machine):
  ```sh
  sudo apt-get install libc6:i386 libncurses5:i386 libstdc++6:i386 lib32z1 libbz2-1.0:i386
  ```
- Download Android Studio from https://developer.android.com/studio (`.tar.gz`)
- Unpack the file:
  ```sh
  tar -zxvf <file> && mv android-studio /usr/local
  ```
- Start Android Studio
  ```sh
  /usr/local//android-studio/bin/studio.sh
  ```
- Follow the Setup Wizard
- Ensure the `ANDROID_HOME` environment variable points to the SDK directory (typically `$HOME/Android/Sdk`):
  ```sh
  echo $ANDROID_HOME
  ```

>[!NOTE]
> You may also follow the official instructions at https://developer.android.com/studio/install.

#### Android Command Line Tools (necessary)

If you have Android Studio, the command-line tools are included. Otherwise, install them manually:

- Download the latest command-line tools:
  ```sh
  curl -O https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip
  ```
- Extract the downloaded file:
  ```sh
  unzip commandlinetools-linux-13114758_latest.zip
  ```
- Create the target directory:
  ```sh
  mkdir -p $HOME/Android/Sdk/cmdline-tools/latest
  ```
- Move the command-line tools into place:
  ```sh
  mv cmdline-tools/* $HOME/Android/Sdk/cmdline-tools/latest
  ```
- Set the `ANDROID_HOME` environment variable:
  - for Bash:
  ```sh
  echo 'export ANDROID_HOME="$HOME/Android/Sdk"' >> ~/.bashrc && \
  echo 'export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"' >> ~/.bashrc
  ```
  - for Zsh:
  ```sh
  echo 'export ANDROID_HOME="$HOME/Android/Sdk"' >> ~/.zshrc && \
  echo 'export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"' >> ~/.zshrc
  ```
  - apply the changes
  ```sh
  source ~/.bashrc   # or 
  source ~/.zshrc
  ```

>[!NOTE]
> You may also follow the official instructions at https://developer.android.com/tools/sdkmanager.

## Troubleshooting

#### `x86_64 emulation currently requires hardware acceleration!` when running `start_emulator.sh`

You are running as a user that lacks permission to access `/dev/kvm`. This prevents the emulator from using KVM-based hardware acceleration, which is required for x86_64 emulation.
To fix this issue, add your user to the `kvm` group:
```sh
sudo usermod -aG kvm <USER>
```
Log out and back in to apply the group membership and retry.

#### `This application failed to start because no Qt platform plugin could be initialized. Reinstalling the application may fix this problem.` when running `start_emulator.sh`

This error typically occurs in headless environments where the Qt application cannot find or load the required platform plugin (e.g., xcb) due to missing GUI libraries or display configuration.
Try running it without headless mode or set up a virtual display.