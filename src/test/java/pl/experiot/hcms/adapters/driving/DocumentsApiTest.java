package pl.experiot.hcms.adapters.driving;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.BeforeParameterizedClassInvocation;
import pl.experiot.hcms.app.logic.dto.Document;

@QuarkusTest
class DocumentApiTest {

    @BeforeAll
    static void setup() {
        // Set the base URI for RestAssured
        io.restassured.RestAssured.baseURI = "http://localhost:8080";
    }

    // @Test
    // void testGetDocsEndpoint() {
    //     given()
    //         .header("X-app-token", "app-token")
    //         .when()
    //         .get("/api/docs/")
    //         .then()
    //         .statusCode(200)
    //         .contentType(ContentType.JSON);
    // }

    // @Test
    // void testGetDocsEndpointWithContent() {
    //     given()
    //         .header("X-app-token", "app-token")
    //         .queryParam("content", true)
    //         .when()
    //         .get("/api/docs/")
    //         .then()
    //         .statusCode(200)
    //         .contentType(ContentType.JSON);
    // }

    // @Test
    // void testGetDocsEndpointWithPath() {
    //     given()
    //         .header("X-app-token", "app-token")
    //         .queryParam("path", "/documentation/")
    //         .when()
    //         .get("/api/docs/")
    //         .then()
    //         .statusCode(200)
    //         .contentType(ContentType.JSON);
    // }

    // @Test
    // void testGetPathsEndpoint() {
    //     given()
    //         .header("X-app-token", "app-token")
    //         .queryParam("site", "site1")
    //         .when()
    //         .get("/api/paths/")
    //         .then()
    //         .statusCode(200)
    //         .contentType(ContentType.JSON);
    // }

    // @Test
    // void testGetSiteNamesEndpoint() {
    //     given()
    //         .header("X-app-token", "app-token")
    //         .when()
    //         .get("/api/sites/")
    //         .then()
    //         .statusCode(200)
    //         .contentType(ContentType.JSON);
    // }

    // @Test
    // void testFindDocsEndpoint() {
    //     given()
    //         .header("X-app-token", "app-token")
    //         .queryParam("path", "/documentation/")
    //         .queryParam("tag", "type:article")
    //         .queryParam("sort", "date")
    //         .queryParam("direction", "asc")
    //         .queryParam("content", true)
    //         .when()
    //         .get("/api/find/")
    //         .then()
    //         .statusCode(200)
    //         .contentType(ContentType.JSON)
    //         .body("size()", equalTo(0));
    // }

    // @Test
    // void testFindFirstEndpoint() {
    //     given()
    //         .header("X-app-token", "app-token")
    //         .queryParam("path", "/documentation/")
    //         .queryParam("tag", "type:article")
    //         .queryParam("sort", "date")
    //         .queryParam("direction", "asc")
    //         .when()
    //         .get("/api/findfirst/")
    //         .then()
    //         .statusCode(404);
    // }

    // @Test
    // void testGetDocEndpoint() {
    //     given()
    //         .header("X-app-token", "app-token")
    //         .queryParam("name", "/docs/doc1.md")
    //         .when()
    //         .get("/api/document/")
    //         .then()
    //         .statusCode(404);

    // }

    // @Test
    // void testSaveDocEndpoint() {
    //     Document doc = new Document();
    //     // Set the properties of the document object

    //     given()
    //         .header("X-app-token", "app-token")
    //         .contentType(ContentType.JSON)
    //         .body(doc)
    //         .when()
    //         .post("/api/docs/")
    //         .then()
    //         .statusCode(501) // Not implemented
    //         .contentType(ContentType.TEXT)
    //         .body(equalTo("Not implemented"));
    // }

    // @Test
    // void testSearchDocsEndpoint() {
    //     given()
    //         .header("X-app-token", "app-token")
    //         .queryParam("text", "keyword")
    //         .queryParam("lang", "en")
    //         .queryParam("content", true)
    //         .when()
    //         .get("/api/search")
    //         .then()
    //         .statusCode(200)
    //         .contentType(ContentType.JSON);
    // }
}
