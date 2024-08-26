package org.cricketmsf.hcms.adapter.driving;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import org.cricketmsf.hcms.app.driving_ports.ForDocumentsIface;
import org.cricketmsf.hcms.app.logic.Document;
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
    ForDocumentsIface documentPort;
    @Inject
    Logger logger;

    @ConfigProperty(name = "app.token")
    String appToken;
    @ConfigProperty(name = "get.document.authorization.required")
    boolean getDocumentAuthorizationRequired;
    @ConfigProperty(name = "document.folders.indexes")
    String indexes;

    @GET
    @Path("/docs/")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponseSchema(value = List.class, responseDescription = "List of document objects.", responseCode = "200")
    @Operation(summary = "List documents", description = "List documents with the specified path. If the result is a single document and it is a binary file, the file will be returned as a download.")
    public Response getDocs(
            @Parameter(description = "Token to authorize the request.", required = false, example = "app-token", schema = @Schema(type = SchemaType.STRING)) @HeaderParam("X-app-token") String token,
            @Parameter(description = "If true, documents will be listed with their content.", required = false, example = "true", schema = @Schema(type = SchemaType.BOOLEAN)) @QueryParam("content") boolean withContent,
            @Parameter(description = "Path to the document or directory. If not provided, the root directory will be listed.", required = false, example = "/documentation/", schema = @Schema(type = SchemaType.STRING)) @QueryParam("path") String path) {

        if (getDocumentAuthorizationRequired && (token == null || !token.equals(appToken))) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        String searchPath = path;
        if (searchPath == null) {
            searchPath = "";
        }

        List<Document> list=new ArrayList<>();
        logger.debug("checking: " + searchPath);
        Document doc = documentPort.getDocument(searchPath);
        if (doc != null) {
            list.add(doc);
        } else {
            logger.debug("requesting: " + searchPath);
            if (withContent) {
                list = documentPort.getDocuments(searchPath, true);
            } else {
                list = documentPort.getDocuments(searchPath, false);
            }
        }
        list = sort(list);
        logger.debug("found: " + list.size() + " documents");
        for (Document d : list) {
            logger.debug(d.fileName);
        }
        try {
            if (list.size() == 1 && list.get(0).binaryFile == true) {
                return Response.ok(list).build();
            } else {
                return Response.ok(list).build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    @GET
    @Path("/paths/")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponseSchema(value = List.class, responseDescription = "List of document paths.", responseCode = "200")
    @Operation(summary = "List paths", description = "List paths of documents in the specified site.")
    public Response getPaths(
            @Parameter(description = "Token to authorize the request.", required = false, example = "app-token", schema = @Schema(type = SchemaType.STRING)) @HeaderParam("X-app-token") String token,
            @Parameter(description = "Site name.", required = true, example = "site1", schema = @Schema(type = SchemaType.STRING)) @QueryParam("site") String site) {
        if (getDocumentAuthorizationRequired && (token == null || !token.equals(appToken))) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        List<String> list = documentPort.getPaths(site);
        return Response.ok(list).build();
    }

    @GET
    @Path("/sites/")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponseSchema(value = List.class, responseDescription = "List of site names.", responseCode = "200")
    @Operation(summary = "List site names", description = "List site names provided by the document service.")
    public Response getSiteNames(
            @Parameter(description = "Token to authorize the request.", required = false, example = "app-token", schema = @Schema(type = SchemaType.STRING)) @HeaderParam("X-app-token") String token) {
        if (getDocumentAuthorizationRequired && (token == null || !token.equals(appToken))) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        List<String> list = documentPort.getSiteNames();
        return Response.ok(list).build();
    }

    @GET
    @Path("/find/")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponseSchema(value = List.class, responseDescription = "List of document objects.", responseCode = "200")
    @Operation(summary = "Find documents", description = "Find documents with the specified path and tag (name:value). Path is optional.")
    public Response findDocs(
            @Parameter(description = "Token to authorize the request.", required = false, example = "app-token", schema = @Schema(type = SchemaType.STRING)) @HeaderParam("X-app-token") String token,
            @Parameter(description = "Path to the document or directory. Default \"/\"", required = false, example = "/documentation/", schema = @Schema(type = SchemaType.STRING)) @QueryParam("path") String path,
            @Parameter(description = "Tag (name:value}.", required = true, example = "type:article", schema = @Schema(type = SchemaType.STRING)) @QueryParam("tag") String tag,
            @Parameter(description = "Tag name to sort by.", required = false, example = "date", schema = @Schema(type = SchemaType.STRING)) @QueryParam("sort") String sort,
            @Parameter(description = "Sort direction.", required = false, example = "asc", schema = @Schema(type = SchemaType.STRING)) @QueryParam("direction") String direction,
            @Parameter(description = "If true, documents will be listed with their content.", required = false, example = "true", schema = @Schema(type = SchemaType.BOOLEAN)) @QueryParam("content") boolean withContent) {
        if (getDocumentAuthorizationRequired && (token == null || !token.equals(appToken))) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        //properties param should be like "type:article,author:John Doe"
        String[] pair = tag.split(":");
        if (pair.length != 2) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Invalid tag definition (expecting name:value)").build();
        }
        if(path==null || path.equals("")){
            path="/";
        }
        List<Document> list = documentPort.findDocuments(path, pair[0], pair[1], sort, direction, withContent);
        return Response.ok(list).build();
    }

    @GET
    @Path("/findfirst/")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponseSchema(value = Document.class, responseDescription = "First document matching rules", responseCode = "200")
    @Operation(summary = "Find first document", description = "Find documents with the specified path and tag (name:value). Path is optional.")
    public Response findFirst(
            @Parameter(description = "Token to authorize the request.", required = false, example = "app-token", schema = @Schema(type = SchemaType.STRING)) @HeaderParam("X-app-token") String token,
            @Parameter(description = "Path to the document or directory. Default \"/\"", required = false, example = "/documentation/", schema = @Schema(type = SchemaType.STRING)) @QueryParam("path") String path,
            @Parameter(description = "Tag (name:value}.", required = true, example = "type:article", schema = @Schema(type = SchemaType.STRING)) @QueryParam("tag") String tag,
            @Parameter(description = "Tag name to sort by.", required = false, example = "date", schema = @Schema(type = SchemaType.STRING)) @QueryParam("sort") String sort,
            @Parameter(description = "Sort direction.", required = false, example = "asc", schema = @Schema(type = SchemaType.STRING)) @QueryParam("direction") String direction) {
        if (getDocumentAuthorizationRequired && (token == null || !token.equals(appToken))) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        //properties param should be like "type:article,author:John Doe"
        String[] pair = tag.split(":");
        if (pair.length != 2) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Invalid tag definition (expecting name:value)").build();
        }
        if(path==null || path.equals("")){
            path="/";
        }
        Document doc = documentPort.findFirstDocument(path, pair[0], pair[1], sort, direction);
        return Response.ok(doc).build();
    }


    @GET
    @Path("/document/")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponseSchema(value = Document.class, responseDescription = "Document object.", responseCode = "200")
    @Operation(summary = "Get document", description = "Get a document (JSON object) with the specified path. If the document is a binary file, the file will be returned as a download.")
    public Response getDoc(
            @Parameter(description = "Token to authorize the request.", required = false, example = "app-token", schema = @Schema(type = SchemaType.STRING)) @HeaderParam("X-app-token") String token,
            @Parameter(description = "Document name.", required = true, example = "/docs/doc1.md", schema = @Schema(type = SchemaType.STRING)) @QueryParam("name") String name) {
        if (getDocumentAuthorizationRequired && (token == null || !token.equals(appToken))) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        logger.debug("requesting document: " + name);
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
            return Response.ok(doc).build();
        }
    }

    /*
     * @GET
     * 
     * @Path("/file/{path}")
     * 
     * @APIResponse(responseCode = "401", description = "Unauthorized")
     * 
     * @APIResponse(responseCode = "200", description =
     * "File content as binary/octet-stream MIME type.")
     * 
     * @Operation(summary = "Get file", description =
     * "Get a file with the specified path. The file will be returned as a download."
     * )
     * public Response getFile(
     * 
     * @Parameter(description = "Token to authorize the request.", required = false,
     * example = "app-token", schema = @Schema(type =
     * SchemaType.STRING)) @HeaderParam("X-app-token") String token,
     * 
     * @Parameter(description = "Path to the document.", required = true, example =
     * "docs/doc1", schema = @Schema(type = SchemaType.STRING)) @PathParam("path")
     * String path) {
     * if (getDocumentAuthorizationRequired && (token == null ||
     * !token.equals(appToken))) {
     * return Response.status(Response.Status.UNAUTHORIZED).build();
     * }
     * Document doc = documentPort.getDocument("/"+path);
     * if (doc == null) {
     * return Response.status(Response.Status.NOT_FOUND).build();
     * }
     * if (doc.binaryFile == true) {
     * try {
     * ByteArrayInputStream bis = new ByteArrayInputStream(doc.binaryContent);
     * return Response.ok(bis, doc.mediaType)
     * .header("content-disposition",
     * "attachment; filename = " + doc.getFileName())
     * .build();
     * } catch (Exception e) {
     * return Response.serverError().entity(e.getMessage()).build();
     * }
     * } else {
     * return Response.ok(doc.content).build();
     * }
     * }
     */

/*     @GET
    @Path("/file/")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(responseCode = "200", description = "File content as binary/octet-stream MIME type.")
    @Operation(summary = "Get file", description = "Get a file with the specified path. The file will be returned as a download.")
    public Response getBinary(
            @Parameter(description = "Token to authorize the request.", required = false, example = "app-token", schema = @Schema(type = SchemaType.STRING)) @HeaderParam("X-app-token") String token,
            @Parameter(description = "Document name.", required = true, example = "/docs/doc1.png", schema = @Schema(type = SchemaType.STRING)) @QueryParam("name") String name) {
        if (getDocumentAuthorizationRequired && (token == null || !token.equals(appToken))) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        logger.debug("requesting file: " + name);
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
 */
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
    @APIResponseSchema(value = String.class, responseDescription = "Document saved.", responseCode = "200")
    @Operation(summary = "Save document to the storage", description = "Save a document. The document must be provided in the request body.")
    public Response saveDoc(
            @Parameter(description = "Token to authorize the request.", required = true, example = "app-token", schema = @Schema(type = SchemaType.STRING)) @HeaderParam("X-app-token") String token,
            @RequestBody(description = "Document to save.", required = true) Document doc) {
        if (token == null || !token.equals(appToken)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
        // documentPort.addDocument(doc);
        // return Response.ok().build();
    }

    private List<Document> sort(List<Document> list) {
        ArrayList<Document> sorted = new ArrayList<>();
        String[] indexes = this.indexes.split(";");
        logger.debug("sorting documents - " + indexes[0]);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).fileName.equals(indexes[0])) {
                sorted.add(0, list.get(i));
            } else {
                sorted.add(list.get(i));
            }
        }
        return sorted;
    }

}
