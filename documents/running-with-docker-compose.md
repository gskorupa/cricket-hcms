# Running demo website with Docker Compose

This document describes how to run the demo website using Docker Compose. The demo website is a simple website that displays the documents stored in the Cricket HCMS service. Additionally, HAProxy is used to provide a reverse proxy for both the Cricket HCMS service and the demo website.

The example can be used as a starting point for building a more complex website using the Cricket HCMS service.

THIS DOCUMENT IS A WORK IN PROGRESS

## Prerequisites

- [Docker](https://docs.docker.com/get-docker/)
- [Docker Compose](https://docs.docker.com/compose/install/)

## Configuration

Docker compose configuration is stored in the `docker-compose.yml` file in the root of the repository. The configuration includes the following services:

- `hcms` - the Cricket HCMS service
- `website` - the demo website
- `gateway` - the HAProxy service working as an API gateway

```yaml
name: cricket-hcms-demo
services:
  hcms:
    image: gskorupa/cricket-hcms:latest
    volumes:
      - ./documents:/home/jboss/documents
    environment:
      FILE_TO_WATCH: version.txt
    ports:
      - 8080:8080

  website:
    image: gskorupa/cricket-demo-website:latest
    ports:
      - 8081:8080

  gateway:
    image: haproxy:2.4
    volumes:
      - ./haproxy.cfg:/usr/local/etc/haproxy/haproxy.cfg:ro
    ports:
      - 8080:8080
```

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

