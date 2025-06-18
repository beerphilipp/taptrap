# Dockerfile for running the Google Play Crawler (npm run crawl)
# Expects CLI args via: docker run ... -- <args>
# Author: Philipp Beer

FROM node:18-alpine
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
ENTRYPOINT ["npm", "run", "crawl", "--"]