# Running

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

## Running local build with Docker

```shell
docker run --volume=/home/greg/tests/img/documents:/home/jboss/documents -p 8080:8080 gskorupa/crickethcms:latest
```

## Running with Docker Compose

```shell
docker compose up
```

## Using the service


To get the list of documents in the repository you can use the following command:

```shell
curl -i "http://localhost:8080/api/docs?path=/"
```