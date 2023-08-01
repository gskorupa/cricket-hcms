# Building Cricket HCMS

## Prerequisites

- Java 17
- Maven
- Docker 

## Building

To build the distribution package run the following command:
```shell
mvn clean package
```

If you want to build the Docker image you need to set Docker repository related properties in the `application.properties` file.
Then use commands:

```shell
./mvnw versions:set -DnewVersion=1.0.0
./mvn -Dquarkus.container-image.tag=1.0.0 -Dquarkus.container-image.push=true clean package
```