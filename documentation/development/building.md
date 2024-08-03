# Building Cricket HCMS

## Prerequisites

- Java 17 or later
- Maven
- Docker 

 Cricket HCMS is built on top of [Quarkus Microservices Framework](https://quarkus.io/).

## Building

To build the distribution package run the following command:
```shell
mvn clean package
```

If you want to build the Docker image you need to set Docker repository related properties in the `application.properties` or set environment variables as in the example below.

Commands syntax:

```shell
./mvnw versions:set -DnewVersion=VERSION
./mvnw -Dquarkus.container-image.group=GROUP -Dquarkus.container-image.tag=TAG \
-Dquarkus.container-image.additional-tags=latest -Dquarkus.container-image.push=PUSH \
clean package
```

Command variable meaning:
- `VERSION` - version of the Docker image
- `GROUP` - Docker repository group
- `PUSH` - logical value indicating if the image should be pushed to the repository

Example:

```shell
./mvnw versions:set -DnewVersion=1.0.0
./mvnw -Dquarkus.container-image.group=gskorupa -Dquarkus.container-image.tag=1.0.0 \
-Dquarkus.container-image.additional-tags=latest -Dquarkus.container-image.push=true \
clean package
```
The image published to the Docker repository with the above command can be pulled with the following command:

```shell
docker pull gskorupa/cricket-hcms:1.0.0
```
or
```shell
docker pull gskorupa/cricket-hcms:latest
```

## Build script

Script `build-image.sh` can be used to build and publish the Cricket HCMS Docker image.

```shell
sh build-image.sh ./dev.cfg
```

