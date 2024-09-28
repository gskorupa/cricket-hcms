package pl.experiot.hcms.adapters.driving;

import java.io.ByteArrayInputStream;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jboss.logging.Logger;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import pl.experiot.hcms.app.logic.Document;
import pl.experiot.hcms.app.ports.driving.ForDocumentsIface;

@Path("/api/file")
/**
 * FileApi is a REST API adapter for the DocumentPort. It provides a set of
 * RESTful endpoint to get files stored as documents in the database.
 * The API can be secured with a token if configured in the
 * application.properties file.
 */
public class FileApi {

    /*
     * @Inject
     * DocumentPort documentPort;
     */
    @Inject
    Logger logger;
    @Inject
    ForDocumentsIface documentPort;

    @ConfigProperty(name = "auth.token")
    String authToken;
    @ConfigProperty(name = "get.document.authorization.required")
    boolean documentAuthorizationRequired;

    @GET
    @Path("{path}")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(responseCode = "200", description = "File content as binary/octet-stream MIME type.")
    @Operation(summary = "Get file", description = "Get a file with the specified path. The file will be returned as a download.")
    public Response getBinary(
            @Parameter(description = "Token to authorize the request.", required = false, example = "app-token", schema = @Schema(type = SchemaType.STRING)) @HeaderParam("X-app-token") String token,
            @Parameter(description = "Path to the document.", required = true, example = "docs/doc1", schema = @Schema(type = SchemaType.STRING)) @PathParam("path") String path) {
        if (documentAuthorizationRequired && (token == null || !token.equals(authToken))) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        logger.info("requesting file: " + path);
        Document doc = documentPort.getDocument(path);
        if (doc == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        if (doc.binaryFile == true) {
            try {
                ByteArrayInputStream bis = new ByteArrayInputStream(doc.binaryContent);
                return Response.ok(bis, doc.mediaType)
                        .header("content-disposition",
                                "attachment; filename = " + doc.getFileName())
                        .build();
            } catch (Exception e) {
                return Response.serverError().entity(e.getMessage()).build();
            }
        } else {
            return Response.ok(doc.content).build();
        }
    }

    @GET
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(responseCode = "200", description = "File content as binary/octet-stream MIME type.")
    @Operation(summary = "Get file", description = "Get a file with the specified path. The file will be returned as a download.")
    public Response getBinaryByName(
            @Parameter(description = "Token to authorize the request.", required = false, example = "app-token", schema = @Schema(type = SchemaType.STRING)) @HeaderParam("X-app-token") String token,
            @Parameter(description = "Document name.", required = true, example = "/docs/doc1.png", schema = @Schema(type = SchemaType.STRING)) @QueryParam("name") String name) {
        if (documentAuthorizationRequired && (token == null || !token.equals(authToken))) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        logger.info("requesting file: " + name);
        Document doc = documentPort.getDocument(name);
        if (doc == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        if (doc.binaryFile == true) {
            try {
                ByteArrayInputStream bis = new ByteArrayInputStream(doc.binaryContent);
                return Response.ok(bis, doc.mediaType)
                        .header("content-disposition",
                                "attachment; filename = " + doc.getFileName())
                        .build();
            } catch (Exception e) {
                return Response.serverError().entity(e.getMessage()).build();
            }
        } else {
            return Response.ok(doc.content).build();
        }
    }

}
