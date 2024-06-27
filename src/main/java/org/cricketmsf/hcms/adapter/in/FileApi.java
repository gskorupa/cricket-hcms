package org.cricketmsf.hcms.adapter.in;

import java.io.ByteArrayInputStream;

import org.cricketmsf.hcms.application.in.DocumentPort;
import org.cricketmsf.hcms.domain.Document;
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
import jakarta.ws.rs.core.Response;

@Path("/files")
/**
 * FileApi is a REST API adapter for the DocumentPort. It provides a set of
 * RESTful endpoint to get files stored as documents in the database.
 * The API can be secured with a token if configured in the application.properties file.
 */
public class FileApi {

    @Inject
    DocumentPort documentPort;
    @Inject
    Logger logger;

    @ConfigProperty(name = "app.token")
    String appToken;
    @ConfigProperty(name = "get.document.authorization.required")
    boolean getDocumentAuthorizationRequired;

 

    @GET
    @Path("{path}")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(responseCode = "200", description = "File content as binary/octet-stream MIME type.")
    @Operation(summary = "Get file", description = "Get a file with the specified path. The file will be returned as a download.")
    public Response getBinary(
            @Parameter(description = "Token to authorize the request.", required = false, example = "app-token", schema = @Schema(type = SchemaType.STRING)) @HeaderParam("X-app-token") String token,
            @Parameter(description = "Path to the document.", required = true, example = "docs/doc1", schema = @Schema(type = SchemaType.STRING)) @PathParam("path") String path) {
        if (getDocumentAuthorizationRequired && (token == null || !token.equals(appToken))) {
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

}
