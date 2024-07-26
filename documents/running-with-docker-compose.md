# Running demo website with Docker Compose

This document describes how to run the demo website using Docker Compose. The demo website is a simple website that displays the documents stored in the Cricket HCMS service. Additionally, HAProxy is used to provide a reverse proxy for both the Cricket HCMS service and the demo website.

The example can be used as a starting point for building a more complex website using the Cricket HCMS service.

THIS DOCUMENT IS A WORK IN PROGRESS

## Prerequisites

Docker Compose must be installed on your system. You can download Docker Compose from the official Docker website: [https://docs.docker.com/get-docker/](https://docs.docker.com/get-docker/)

## Configuration

Docker compose configuration is stored in the `docker-compose.yml` file. It defines two services:

- `hcms` - the Cricket HCMS service
- `website` - the demo website

<pre  class="boredr shadow p-2  bg-secondary-subtle">
name: cricket-hcms-demo

services:
  hcms:
    image: cricket-hcms:latest
    environment:
      FOLDERS_ROOT: ./documents
      FILE_TO_WATCH: version.txt
      HCMS_SERVICE_URL: http://localhost:8081
    expose:
      - "8080"
    ports:
      - "8081:8080"
    volumes:
      - ./documents:/home/jboss/documents
    networks:
      - hcms
  
  website:
    image: cricket-website:latest
    depends_on:
      - hcms
    ports:
      - "8080:3000"
    networks:
      - hcms

networks:
  hcms:
    driver: bridge
</pre>

The service can be configured using environment variables. The following environment variables are available:

- `FOLDERS_ROOT` - the root folder where the documents are stored
- `FOLDERS_EXCLUDED` - a semi-colon separated list of folders to exclude from monitoring
- `MARKDOWN_EXTENSION` - the file extension for Markdown files (default: `.md`)
- `HTML_EXTENSION` - the file extension for HTML files (default: `.html`)
- `FILE_TO_WATCH` - the file to watch for changes (default: `version.txt`)


## Running the service

To run the service, execute the following command:

```shell
docker-compose up
```


## Stopping the service

To stop the service, press `Ctrl+C` in the terminal where the service is running.

## Accessing the service

Once the service is running, you can access the demo website at [http://localhost:8080](http://localhost:8080).

