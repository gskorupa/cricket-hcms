<!-- metadata
keyword:running
-->
# Running

THIS DOCUMENT IS A WORK IN PROGRESS

In order to present how to use this site, I have prepared a related cricket-website-template project that allows you to view documents served by Cricket HCMS. The template is a SvelteKit application that can be built as a dynamic Node website or as a Docker image. The template is available at [https://github.com/gskorupa/cricket-website-template](https://github.com/gskorupa/cricket-website-template).

Both services can be run wit Docker Compose using docker-compose.yml file also available in the Cricket HCMS repository.

## Running local build with Java

```shell
    java -jar target/quarkus-app/quarkus-run.jar
```
Content of the `documents` subfolder (default document root) will be loaded into the service repository.

The document root folder location can be changed by setting the `document.folders.root`
environment variable.
```
java -Ddocument.folders.root=doc -jar target/quarkus-app/quarkus-run.jar
```

Once launched, the API specification in JSON format can be downloaded from http://localhost:8080/q/openapi.
Viewing the specification in interactive mode is enabled by the Swagger UI http://localhost:8080/q/swagger-ui.

## Running the Docker image

The docker image pushed to the docker repository can then be run with a command like the following example image `gskorupa/cricket-hcms:latest`.

```shell
docker run --volume=./documents:/home/jboss/documents -p 8080:8080 gskorupa/cricket-hcms:latest
```

## Using the service

You can download the API specification in JSON format from http://localhost:8080/q/openapi.

Viewing the specification in interactive mode will be enabled by Swagger UI http://localhost:8080/q/swagger-ui
