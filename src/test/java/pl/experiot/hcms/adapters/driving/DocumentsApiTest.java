package pl.experiot.hcms.adapters.driving;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import pl.experiot.hcms.app.logic.Document;

@QuarkusTest
class DocumentApiTest {

    @Test
    void testGetDocsEndpoint() {
        given()
            .header("X-app-token", "app-token")
            .when()
            .get("/api/docs/")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON);
    }

    @Test
    void testGetDocsEndpointUnauthorized() {
        given()
            .header("X-app-token", "invalid-token")
            .when()
            .get("/api/docs/")
            .then()
            .statusCode(401);
    }

    @Test
    void testGetDocsEndpointWithContent() {
        given()
            .header("X-app-token", "app-token")
            .queryParam("content", true)
            .when()
            .get("/api/docs/")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON);
    }

    @Test
    void testGetDocsEndpointWithPath() {
        given()
            .header("X-app-token", "app-token")
            .queryParam("path", "/documentation/")
            .when()
            .get("/api/docs/")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON);
    }

    @Test
    void testGetPathsEndpoint() {
        given()
                .header("X-app-token", "app-token")
                .queryParam("site", "site1")
                .when()
                .get("/api/paths/")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON);
    }

    @Test
    void testGetSiteNamesEndpoint() {
        given()
            .header("X-app-token", "app-token")
            .when()
            .get("/api/sites/")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON);
    }

    
    @Test
    void testFindDocsEndpoint() {
        given()
            .header("X-app-token", "app-token")
            .queryParam("path", "/documentation/")
            .queryParam("tag", "type:article")
            .queryParam("sort", "date")
            .queryParam("direction", "asc")
            .queryParam("content", true)
            .when()
            .get("/api/find/")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("size()", equalTo(0));
    }

    
    @Test
    void testFindFirstEndpoint() {
        given()
            .header("X-app-token", "app-token")
            .queryParam("path", "/documentation/")
            .queryParam("tag", "type:article")
            .queryParam("sort", "date")
            .queryParam("direction", "asc")
            .when()
            .get("/api/findfirst/")
            .then()
            .statusCode(404);

            //.body("fileName", equalTo("example.doc"))
            //.body("binaryFile", equalTo(true));
    }
    

     @Test
    void testGetDocEndpoint() {
        given()
            .header("X-app-token", "app-token")
            .queryParam("name", "/docs/doc1.md")
            .when()
            .get("/api/document/")
            .then()
            .statusCode(404);
            //.contentType(ContentType.JSON)
            //.body("fileName", equalTo("doc1.md"))
            //.body("binaryFile", equalTo(false));
    } 

    @Test
    void testSaveDocEndpoint() {
        Document doc = new Document();
        // Set the properties of the document object

        given()
            .header("X-app-token", "app-token")
            .contentType(ContentType.JSON)
            .body(doc)
            .when()
            .post("/api/docs/")
            .then()
            .statusCode(501) // Not implemented
            .contentType(ContentType.TEXT)
            .body(equalTo("Not implemented"));
    }
}