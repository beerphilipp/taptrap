# Dockerfile for the APK Merger component (TapTrap)
# Installs Python 3, required dependencies, and runs the merging script via entrypoint.sh
# Author: Philipp Beer

FROM eclipse-temurin:17-jdk
RUN apt-get update && apt-get install -y python3 python3-pip parallel
WORKDIR /app
RUN apt-get update && apt-get install -y parallel
COPY requirements.txt requirements.txt
RUN pip install --no-cache-dir --break-system-packages -r requirements.txt
COPY . .
RUN chmod +x entrypoint.sh
RUN ln -s /usr/bin/python3 /usr/bin/python
ENTRYPOINT ["./entrypoint.sh"]