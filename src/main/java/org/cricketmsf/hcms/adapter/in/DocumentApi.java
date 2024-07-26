package org.cricketmsf.hcms.adapter.in;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.List;

import org.cricketmsf.hcms.application.in.DocumentPort;
import org.cricketmsf.hcms.domain.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponseSchema;
import org.jboss.logging.Logger;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

@Path("/api")
/**
 * DocumentApi is a REST API adapter for the DocumentPort. It provides a set of
 * RESTful endpoints to manage documents
 * stored in the system. The API is secured with a token that must be provided
 * in the request header.
 */
public class DocumentApi {

    @Inject
    DocumentPort documentPort;
    @Inject
    Logger logger;

    @ConfigProperty(name = "app.token")
    String appToken;
    @ConfigProperty(name = "get.document.authorization.required")
    boolean getDocumentAuthorizationRequired;

    @GET
    @Path("/docs/")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponseSchema(value = List.class, responseDescription = "List of document objects.", responseCode = "200")
    @Operation(summary = "List documents", description = "List documents with the specified path. If the result is a single document and it is a binary file, the file will be returned as a download.")
    public Response getDocs(
            @Parameter(description = "Token to authorize the request.", required = false, example = "app-token", schema = @Schema(type = SchemaType.STRING)) @HeaderParam("X-app-token") String token,
            @Parameter(description = "If true, documents will be listed with their content.", required = false, example = "true", schema = @Schema(type = SchemaType.BOOLEAN)) @QueryParam("content") boolean withContent,
            @Parameter(description = "Path to the document or directory. If not provided, the root directory will be listed.", required = false, example = "docs", schema = @Schema(type = SchemaType.STRING)) @QueryParam("path") String path) {

        if (getDocumentAuthorizationRequired && (token == null || !token.equals(appToken))) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        String searchPath = path;
        if (searchPath == null) {
            searchPath = "";
        }
        logger.info("requesting: " + searchPath);
        List<Document> list;
        if (withContent) {
            list = documentPort.getDocs(searchPath, true);
        } else {
            list = documentPort.getDocs(searchPath, false);
        }
        try{
        if (list.size() == 1 && list.get(0).binaryFile == true) {
/*             try {
                ByteArrayInputStream bis = null; // https://www.knowledgefactory.net/2021/10/quarkus-export-data-to-pdf-example.html
                Document doc = list.get(0);
                bis = new ByteArrayInputStream(doc.binaryContent);
                return Response.ok(bis, doc.mediaType)
                        .header("content-disposition",
                                "attachment; filename = " + doc.getFileName())
                        .build();
            } catch (Exception e) {
                return Response.serverError().entity(e.getMessage()).build();
            } */
            //Document doc = list.get(0);
            //String binaryContent=Base64.getEncoder().encodeToString(doc.binaryContent);
            //doc.binaryContent=binaryContent.getBytes();
            //list.set(0, doc);
            return Response.ok(list).build();
        } else {
            return Response.ok(list).build();
        }
        }catch(Exception e){
            e.printStackTrace();
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/find/")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponseSchema(value = List.class, responseDescription = "List of document objects.", responseCode = "200")
    @Operation(summary = "Find documents", description = "Find documents with the specified path and list of {property name,poperty value} pairs. Path is optional.")
    public Response findDocs(
            @Parameter(description = "Token to authorize the request.", required = false, example = "app-token", schema = @Schema(type = SchemaType.STRING)) @HeaderParam("X-app-token") String token,
            @Parameter(description = "Path to the document or directory. If not provided, the root directory will be listed.", required = false, example = "docs", schema = @Schema(type = SchemaType.STRING)) @QueryParam("path") String path,
            @Parameter(description = "List of {property name,poperty value} pairs.", required = true, example = "type,article", schema = @Schema(type = SchemaType.STRING)) @QueryParam("properties") String properties) {
        if (getDocumentAuthorizationRequired && (token == null || !token.equals(appToken))) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        String[] pairs = properties.split(",");
        if (pairs.length % 2 != 0) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Invalid properties list").build();
        }
        List<Document> list = documentPort.findDocs(path, pairs);
        return Response.ok(list).build();
    }  


    @GET
    @Path("/document/")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponseSchema(value = Document.class, responseDescription = "Document object.", responseCode = "200")
    @Operation(summary = "Get document", description = "Get a document (JSON object) with the specified path. If the document is a binary file, the file will be returned as a download.")
    public Response getDoc(
            @Parameter(description = "Token to authorize the request.", required = false, example = "app-token", schema = @Schema(type = SchemaType.STRING)) @HeaderParam("X-app-token") String token,
            @Parameter(description = "Path to the document.", required = true, example = "docs/doc1", schema = @Schema(type = SchemaType.STRING)) @QueryParam("path") String path) {
        if (getDocumentAuthorizationRequired && (token == null || !token.equals(appToken))) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        Document doc = documentPort.getDocument(path);
        if (doc == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        if (doc.binaryFile == true){
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
            return Response.ok(doc).build();
        }
    }

/*     @GET
    @Path("/file/{path}")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(responseCode = "200", description = "File content as binary/octet-stream MIME type.")
    @Operation(summary = "Get file", description = "Get a file with the specified path. The file will be returned as a download.")
    public Response getFile(
            @Parameter(description = "Token to authorize the request.", required = false, example = "app-token", schema = @Schema(type = SchemaType.STRING)) @HeaderParam("X-app-token") String token,
            @Parameter(description = "Path to the document.", required = true, example = "docs/doc1", schema = @Schema(type = SchemaType.STRING)) @PathParam("path") String path) {
        if (getDocumentAuthorizationRequired && (token == null || !token.equals(appToken))) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        Document doc = documentPort.getDocument("/"+path);
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
    } */

    @GET
    @Path("/file/")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(responseCode = "200", description = "File content as binary/octet-stream MIME type.")
    @Operation(summary = "Get file", description = "Get a file with the specified path. The file will be returned as a download.")
    public Response getBinary(
            @Parameter(description = "Token to authorize the request.", required = false, example = "app-token", schema = @Schema(type = SchemaType.STRING)) @HeaderParam("X-app-token") String token,
            @Parameter(description = "Path to the document.", required = true, example = "docs/doc1", schema = @Schema(type = SchemaType.STRING)) @QueryParam("path") String path) {
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


    @POST
    @Path("/reload")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(responseCode = "501", description = "Not implemented")
    @APIResponseSchema(value = String.class, responseDescription = "Command executed successfully.", responseCode = "200")
    @Operation(summary = "Command to reload the documents", description = "Forces the system service to reload the documents from the storage.")
    public Response reload(
            @Parameter(description = "Token to authorize the request.", required = true, example = "app-token", schema = @Schema(type = SchemaType.STRING)) @HeaderParam("X-app-token") String token) {
        if (token == null || !token.equals(appToken)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
        // documentPort.reload();
        // return Response.ok().build();
    }

    @POST
    @Path("/docs/")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(responseCode = "501", description = "Not implemented")
    @APIResponseSchema(value = String.class, responseDescription = "Document saved." , responseCode = "200")
    @Operation(summary = "Save document to the storage", description = "Save a document. The document must be provided in the request body.")
    public Response saveDoc(
            @Parameter(description = "Token to authorize the request.", required = true, example = "app-token", schema = @Schema(type = SchemaType.STRING)) @HeaderParam("X-app-token") String token,
            @RequestBody(description = "Document to save.", required = true) Document doc) {
        if (token == null || !token.equals(appToken)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
        //documentPort.addDocument(doc);
        //return Response.ok().build();
    }

}
