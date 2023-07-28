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

If you want to build the Docker you need to set Docker repository related properties in teh `application.properties` file.
Then you can build the Docker image with command:

```shell
mvn -Dquarkus.container-image.push=true clean package
```