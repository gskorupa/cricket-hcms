package pl.experiot.hcms.adapters.driving;

import io.quarkus.test.junit.QuarkusTest;
import static io.restassured.RestAssured.given;
import io.restassured.http.ContentType;
import static org.hamcrest.CoreMatchers.is;
import org.junit.jupiter.api.Test;

@QuarkusTest
class AdministratorApiTest {

    @Test
    void testReloadEndpoint() {
        given()
            .header("X-app-token", "app-token")
            .when()
            .post("/api/reload")
            .then()
            .statusCode(200)
            .contentType(ContentType.TEXT)
            .body(is("OK"));
    }

    @Test
    void testReloadEndpointUnauthorized() {
        given()
            .header("X-app-token", "invalid-token")
            .when()
            .post("/api/reload")
            .then()
            .statusCode(401);
    }

    @Test
    void testReloadEndpointNotImplemented() {
        given()
            .header("X-app-token", "app-token")
            .when()
            .post("/api/reload")
            .then()
            .statusCode(200);
    }
}
