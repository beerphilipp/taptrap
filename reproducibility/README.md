# Artifact Evaluation Experiments

This folder contains utility scripts to run the experiments defined in the [artifact appendix](https://taptrap.click/artifact-appendix).

## Folder Structure

- `e1.sh` - Test script for experiment 1: Dataset preparation
- `e2.sh` - Test script for experiment 2: TapTrap functionality
- `e3.sh` - Test script for experiment 3: Malicious app detection
- `e4.sh` - Test script for experiment 4: Vulnerable app detection
- `start_emulator.sh` - Script to start an Android 15 Pixel 6a emulator

## APK Dataset

**Reviewers:** Please follow the instructions in the artifact appendix.

**Users:** You may request access to the APK dataset by contacting the authors. 

## Prepare and Run the Experiments

>[!NOTE]
> To produce the results in the paper, we used the following environment:
> - Dataset Preparation: Ubuntu 24.04 (x86) with 491 GB of RAM and 112 CPU cores
> - Malicious App Detection: 
>   - Extraction of animations: Ubuntu 24.04 (x86) with 491 GB of RAM and 112 CPU cores
>   - Analysis of animations: MacOS 15 (ARM) with 32 GB of RAM and 10 cores
> - Vulnerable App Detection: Ubuntu 24.04 (x86) with 491 GB of RAM and 112 CPU cores

### Install the Prerequisites

The following instructions assume a device running Ubuntu 24.04. For other operating systems, please follow [the official instructions](https://docs.docker.com/get-docker/).

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
- Install rootless Docker:
  ```sh
  dockerd-rootless-setuptool.sh install
  ```

#### Android Studio (recommended)

The following instructions assume a device running Ubuntu 24.04. For other operating systems, please follow [the official instructions](https://developer.android.com/studio/install).

We recommend installing Android Studio to inspect the source code of the apps. Follow these steps:

- Install dependencies (if you are runnging on a 64-bit machine):
  ```sh
  sudo dpkg --add-architecture i386 && \
  sudo apt-get update && \
  sudo apt-get install libc6:i386 libncurses6:i386 libstdc++6:i386 lib32z1 libbz2-1.0:i386
  ```
- Download Android Studio from https://developer.android.com/studio (`.tar.gz`)
- Unpack the file and replace `<file>` with the downloaded file:
  ```sh
  tar -zxvf <file> && sudo mv android-studio /usr/local
  ```
- Start Android Studio
  ```sh
  /usr/local//android-studio/bin/studio.sh
  ```
- Follow the Setup Wizard with default settings
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

### Run the Experiments

Please refer to the [artifact appendix](https://taptrap.click/artifact-appendix) (Section A.4) for instructions on how to run and reproduce the experiments, the expected outcomes of the experiments, and the approximate resources necessary.

## Troubleshooting

#### Missing system requirements while installing Docker rootless Docker

```sh
[ERROR] Missing system requirements. Run the following commands to
[ERROR] install the requirements and run this tool again.

########## BEGIN ##########
sudo sh -eux <<EOF
# Install newuidmap & newgidmap binaries
apt-get install -y uidmap
EOF
########## END #########
```

- Run the commands in between `BEGIN` and `END`

#### Permission denied: `./e1.sh` / `./e2.sh` / `./e3.sh` / `./e4.sh`

- Make sure the script is executable. Run the following command:
    ```sh
    chmod +x ./e1.sh
    ```
- Then run it again:
  ```sh
  ./e1.sh
  ```

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