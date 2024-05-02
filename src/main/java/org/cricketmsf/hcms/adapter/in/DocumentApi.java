package org.cricketmsf.hcms.adapter.in;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.cricketmsf.hcms.application.in.DocumentPort;
import org.cricketmsf.hcms.domain.Document;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

@Path("/api")
public class DocumentApi {

    @Inject
    DocumentPort documentPort;
    @Inject
    Logger logger;

    @ConfigProperty(name = "app.token")
    String appToken;

    @GET
    @Path("/docs/")
    public Response getDocs(@QueryParam("path") String path) {
        String searchPath = path;
        if (searchPath == null) {
            searchPath = "";
        }
        logger.info("requesting: " + searchPath);
        List<Document> list = documentPort.getDocs(searchPath);
        if (list.size() == 1 && list.get(0).binaryFile == true) {
            try {
                ByteArrayInputStream bis = null; // https://www.knowledgefactory.net/2021/10/quarkus-export-data-to-pdf-example.html
                Document doc = list.get(0);
                bis = new ByteArrayInputStream(doc.binaryContent);
                return Response.ok(bis, doc.mediaType)
                        .header("content-disposition",
                                "attachment; filename = " + doc.getFileName())
                        .build();
            } catch (Exception e) {
                return Response.serverError().entity(e.getMessage()).build();
            }
        } else {
            return Response.ok(list).build();
        }
    }

    @GET
    @Path("/reload")
    public Response reload(@HeaderParam("X-app-token") String token) {
        if (token == null || !token.equals(appToken)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        documentPort.reload();
        return Response.ok().build();
    }

    /*
     * @POST
     * 
     * @Path("/pull")
     * public Response pullDocuments(@HeaderParam("X-app-token") String token) {
     * if (token == null || !token.equals(appToken)) {
     * return Response.status(Response.Status.UNAUTHORIZED).build();
     * }
     * documentPort.reload();
     * return Response.ok().build();
     * }
     */

    @POST
    @Path("/docs/")
    public Response saveDoc(@HeaderParam("X-app-token") String token, Document doc) {
        if (token == null || !token.equals(appToken)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        documentPort.addDocument(doc);
        return Response.ok().build();
    }

}
