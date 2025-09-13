package pl.experiot.hcms.adapters.driving;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponseSchema;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import pl.experiot.hcms.adapters.driven.auth.SignomixAuthClient;
import pl.experiot.hcms.app.logic.DocumentAccessLogic;
import pl.experiot.hcms.app.logic.TokenCache;
import pl.experiot.hcms.app.logic.dto.Document;
import pl.experiot.hcms.app.logic.dto.User;
import pl.experiot.hcms.app.ports.driving.ForDocumentsIface;

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

    @Inject
    TokenCache tokenCache;
    @Inject
    DocumentAccessLogic documentAccessLogic;

    @ConfigProperty(name = "auth.token")
    String authToken;
    @ConfigProperty(name = "get.document.authorization.required")
    boolean getDocumentAuthorizationRequired;
    @ConfigProperty(name = "document.folders.indexes")
    String indexes;
    @ConfigProperty(name = "document.folders.restricted")
    String restrictedFolders;

    @RestClient
    SignomixAuthClient authClient;

    @GET
    @Path("/docs/")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponseSchema(value = List.class, responseDescription = "List of document objects.", responseCode = "200")
    @Operation(summary = "List documents", description = "List documents with the specified path. If the result is a single document and it is a binary file, the file will be returned as a download.")
    public Response getDocs(
            @Parameter(description = "Token to authorize the request.", required = false, example = "app-token", schema = @Schema(type = SchemaType.STRING)) @HeaderParam("Authorization") String token,
            @Parameter(description = "If true, documents will be listed with their content.", required = false, example = "true", schema = @Schema(type = SchemaType.BOOLEAN)) @QueryParam("content") boolean withContent,
            @Parameter(description = "Path to the document or directory. If not provided, the root directory will be listed.", required = false, example = "/documentation/", schema = @Schema(type = SchemaType.STRING)) @QueryParam("path") String path) {

        if (getDocumentAuthorizationRequired) {
            User user = getUser(token);
            if (user == null) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }
        }
        String searchPath = path;
        if (searchPath == null) {
            searchPath = "";
        }

        List<Document> list = new ArrayList<>();
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
            @Parameter(description = "Token to authorize the request.", required = false, example = "app-token", schema = @Schema(type = SchemaType.STRING)) @HeaderParam("Authorization") String token,
            @Parameter(description = "Site name.", required = true, example = "site1", schema = @Schema(type = SchemaType.STRING)) @QueryParam("site") String site) {
        if (getDocumentAuthorizationRequired) {
            User user = getUser(token);
            if (user == null) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }
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
            @Parameter(description = "Token to authorize the request.", required = false, example = "app-token", schema = @Schema(type = SchemaType.STRING)) @HeaderParam("Authorization") String token) {
        if (getDocumentAuthorizationRequired) {
            User user = getUser(token);
            if (user == null) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }
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
            @Parameter(description = "Token to authorize the request.", required = false, example = "app-token", schema = @Schema(type = SchemaType.STRING)) @HeaderParam("Authorization") String token,
            @Parameter(description = "Path to the document or directory. Default \"/\"", required = false, example = "/documentation/", schema = @Schema(type = SchemaType.STRING)) @QueryParam("path") String path,
            @Parameter(description = "Tag (name:value}.", required = true, example = "type:article", schema = @Schema(type = SchemaType.STRING)) @QueryParam("tag") String tag,
            @Parameter(description = "Tag name to sort by.", required = false, example = "date", schema = @Schema(type = SchemaType.STRING)) @QueryParam("sort") String sort,
            @Parameter(description = "Sort direction.", required = false, example = "asc", schema = @Schema(type = SchemaType.STRING)) @QueryParam("direction") String direction,
            @Parameter(description = "If true, documents will be listed with their content.", required = false, example = "true", schema = @Schema(type = SchemaType.BOOLEAN)) @QueryParam("content") boolean withContent) {
        if (getDocumentAuthorizationRequired) {
            User user = getUser(token);
            if (user == null) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }
        }
        // properties param should be like "type:article,author:John Doe"
        String[] pair = tag.split(":");
        if (pair.length != 2) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Invalid tag definition (expecting name:value)")
                    .build();
        }
        if (path == null || path.equals("")) {
            path = "/";
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
            @Parameter(description = "Token to authorize the request.", required = false, example = "app-token", schema = @Schema(type = SchemaType.STRING)) @HeaderParam("Authorization") String token,
            @Parameter(description = "Path to the document or directory. Default \"/\"", required = false, example = "/documentation/", schema = @Schema(type = SchemaType.STRING)) @QueryParam("path") String path,
            @Parameter(description = "Tag (name:value}.", required = true, example = "type:article", schema = @Schema(type = SchemaType.STRING)) @QueryParam("tag") String tag,
            @Parameter(description = "Tag name to sort by.", required = false, example = "date", schema = @Schema(type = SchemaType.STRING)) @QueryParam("sort") String sort,
            @Parameter(description = "Sort direction.", required = false, example = "asc", schema = @Schema(type = SchemaType.STRING)) @QueryParam("direction") String direction) {
        if (getDocumentAuthorizationRequired) {
            User user = getUser(token);
            if (user == null) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }
        }
        // properties param should be like "type:article,author:John Doe"
        String[] pair = tag.split(":");
        if (pair.length != 2) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Invalid tag definition (expecting name:value)")
                    .build();
        }
        if (path == null || path.equals("")) {
            path = "/";
        }
        Document doc = documentPort.findFirstDocument(path, pair[0], pair[1], sort, direction);
        if (doc == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Not found").build();
        }
        return Response.ok(doc).build();
    }

    @GET
    @Path("/document/")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponseSchema(value = Document.class, responseDescription = "Document object.", responseCode = "200")
    @Operation(summary = "Get document", description = "Get a document (JSON object) with the specified path. If the document is a binary file, the file will be returned as a download.")
    //@CacheResult(cacheName = "document-cache")
    public Response getDoc(
            @Parameter(description = "Token to authorize the request.", required = false, example = "app-token", schema = @Schema(type = SchemaType.STRING)) @HeaderParam("Authentication") String token,
            @Parameter(description = "Document name.", required = true, example = "/docs/doc1.md", schema = @Schema(type = SchemaType.STRING)) @QueryParam("name") String name) {
        User user = null;
        logger.info("token: " + token);
        if (isRestrictedFolder(name)) {
            if (token != null && !token.isEmpty()) {
                if (tokenCache.containsToken(token)) {
                    user = tokenCache.getUser(token);
                } else {
                    user = getUser(token);
                    tokenCache.addToken(token, user);
                }
                if (user == null) {
                    return Response.status(Response.Status.UNAUTHORIZED).build();
                }
            }
        }
        name = documentAccessLogic.getOrganizationDocName(name, user);
        logger.info("requesting document: " + name);
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

    @GET
    @Path("/search")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponseSchema(value = List.class, responseDescription = "List of document names.", responseCode = "200")
    @Operation(summary = "Full text search", description = "Search documents with the specified text.")
    public Response searchDocs(
            @Parameter(description = "Token to authorize the request.", required = false, example = "app-token", schema = @Schema(type = SchemaType.STRING)) @HeaderParam("Authorization") String token,
            @Parameter(description = "Text to search.", required = true, example = "keyword", schema = @Schema(type = SchemaType.STRING)) @QueryParam("text") String text,
            @Parameter(description = "Document language code or * for all supported languages", required = false, example = "en", schema = @Schema(type = SchemaType.STRING)) @QueryParam("lang") String lang) {
        if (getDocumentAuthorizationRequired) {
            User user = getUser(token);
            if (user == null) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }
        }
        List<String> list = documentPort.searchDocuments(text, lang);
        return Response.ok(list).build();
    }

    @POST
    @Path("/docs/")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(responseCode = "501", description = "Not implemented")
    @APIResponseSchema(value = String.class, responseDescription = "Document saved.", responseCode = "200")
    @Operation(summary = "Save document to the storage", description = "Save a document. The document must be provided in the request body.")
    public Response saveDoc(
            @Parameter(description = "Token to authorize the request.", required = true, example = "app-token", schema = @Schema(type = SchemaType.STRING)) @HeaderParam("Authorization") String token,
            @RequestBody(description = "Document to save.", required = true) Document doc) {
        if (getDocumentAuthorizationRequired) {
            User user = getUser(token);
            if (user == null) {
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }
        }
        return Response.status(Response.Status.NOT_IMPLEMENTED).entity("Not implemented").build();
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

    private User getUser(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        try {
            Response response = authClient.getUser(token);
            if (response.getStatus() == 200) {
                return response.readEntity(User.class);
            } else {
                logger.warn("Error getting user. Code: " + response.getStatus());
            }
        } catch (Exception e) {
            logger.warn("Error getting user: " + e.getMessage());
        }
        return null;
    }

    private boolean isRestrictedFolder(String documentName) {
        if(restrictedFolders==null || restrictedFolders.isEmpty()){
            return false;
        }
        String[] folders = restrictedFolders.split(";");
        for (String folder : folders) {
            // check if the document name starts with the restricted folder name
            // document name starts with "/" so we need to add it to the folder name
            if (documentName.startsWith("/"+folder)) {
                return true;
            }
        }
        return false;
    }

}
