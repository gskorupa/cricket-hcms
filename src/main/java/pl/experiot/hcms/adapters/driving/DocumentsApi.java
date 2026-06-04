package pl.experiot.hcms.adapters.driving;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import pl.experiot.hcms.app.logic.dto.Document;
import pl.experiot.hcms.app.ports.driving.ForDocumentsIface;

@ApplicationScoped
@Path("/api")
public class DocumentsApi {

    @Inject
    ForDocumentsIface documentPort;

    @Inject
    Logger logger;

    @GET
    @Path("/docs/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDocs(@HeaderParam("X-app-token") String token,
                            @QueryParam("path") String path,
                            @QueryParam("content") boolean content) {
        String p = (path == null || path.isEmpty()) ? "/" : path;
        List<Document> docs = documentPort.getDocuments(p, content);
        return Response.ok(docs).build();
    }

    @GET
    @Path("/paths/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPaths(@HeaderParam("X-app-token") String token,
                             @QueryParam("site") String site) {
        List<String> paths = documentPort.getPaths(site);
        return Response.ok(paths).build();
    }

    @GET
    @Path("/sites/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSites(@HeaderParam("X-app-token") String token) {
        List<String> sites = documentPort.getSiteNames();
        return Response.ok(sites).build();
    }

    @GET
    @Path("/find/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response findDocs(@HeaderParam("X-app-token") String token,
                             @QueryParam("path") String path,
                             @QueryParam("tag") String tag,
                             @QueryParam("sort") String sort,
                             @QueryParam("direction") String direction,
                             @QueryParam("content") boolean content) {
        String tagName = null;
        String tagValue = null;
        if (tag != null && tag.contains(":")) {
            String[] parts = tag.split(":", 2);
            tagName = parts[0];
            tagValue = parts[1];
        }
        List<Document> docs = documentPort.findDocuments(path, tagName, tagValue, sort, direction, content);
        return Response.ok(docs).build();
    }

    @GET
    @Path("/findfirst/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response findFirst(@HeaderParam("X-app-token") String token,
                              @QueryParam("path") String path,
                              @QueryParam("tag") String tag,
                              @QueryParam("sort") String sort,
                              @QueryParam("direction") String direction) {
        String tagName = null;
        String tagValue = null;
        if (tag != null && tag.contains(":")) {
            String[] parts = tag.split(":", 2);
            tagName = parts[0];
            tagValue = parts[1];
        }
        var doc = documentPort.findFirstDocument(path, tagName, tagValue, sort, direction);
        if (doc == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(doc).build();
    }

    @GET
    @Path("/document/")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDocument(@HeaderParam("X-app-token") String token,
                                @QueryParam("name") String name) {
        var doc = documentPort.getDocument(name);
        if (doc == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(doc).build();
    }

    @GET
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    public Response search(@HeaderParam("X-app-token") String token,
                           @QueryParam("text") String text,
                           @QueryParam("lang") String lang,
                           @QueryParam("content") boolean content) {
        List<String> names = documentPort.searchDocuments(text, lang);
        if (content) {
            List<Document> docs = new ArrayList<>();
            for (String n : names) {
                var d = documentPort.getDocument(n);
                if (d != null) docs.add(d);
            }
            return Response.ok(docs).build();
        }
        return Response.ok(names).build();
    }

    @POST
    @Path("/docs/")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response saveDoc(@HeaderParam("X-app-token") String token, Document doc) {
        return Response.status(Response.Status.NOT_IMPLEMENTED).entity("Not implemented").build();
    }
}
