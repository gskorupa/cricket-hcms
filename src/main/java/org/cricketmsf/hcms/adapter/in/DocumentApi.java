package org.cricketmsf.hcms.adapter.in;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.cricketmsf.hcms.application.in.DocumentPort;
import org.cricketmsf.hcms.domain.Document;
import org.jboss.logging.Logger;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api")
public class DocumentApi {

    @Inject
    DocumentPort documentPort;
    @Inject
    Logger logger;

    @GET
    @Path("/docs/")
    public Response getDocs(@QueryParam("path") String path) {
        String searchPath = path;
        if (searchPath == null) {
            searchPath = "";
        }
        logger.info("requesting: " + searchPath);
        List<Document> list = documentPort.getDocs(searchPath);
        if (list.size() == 0 && list.get(0).binaryFile == true) {
            ByteArrayInputStream bis = null; //https://www.knowledgefactory.net/2021/10/quarkus-export-data-to-pdf-example.html
            // TODO: returns binary file
            return Response.ok(bis, MediaType.APPLICATION_OCTET_STREAM)
                    .header("content-disposition",
                            "attachment; filename = " + list.get(0).name)
                    .build();
        } else {
            return Response.ok(list).build();
        }
    }

    /* @GET
    @Path("/reload")
    public Response reload() {
        documentPort.reload();
        return Response.ok().build();
    } */

}
