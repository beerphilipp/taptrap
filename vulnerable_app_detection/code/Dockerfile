FROM python:3.11-slim

WORKDIR /app

# Install GNU Parallel for task parallelization
RUN apt-get update \
 && apt-get install -y --no-install-recommends parallel \
 && rm -rf /var/lib/apt/lists/*

# Install Python dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy application code and install package
COPY . .
RUN pip install --no-cache-dir .

# Ensure entrypoint script is executable
RUN chmod +x docker_entrypoint.sh

ENTRYPOINT ["./docker_entrypoint.sh"]