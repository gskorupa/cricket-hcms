package org.cricketmsf.hcms.adapter.in;

import java.util.List;

import org.cricketmsf.hcms.application.in.DocumentPort;
import org.cricketmsf.hcms.domain.Document;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

@Path("/api")
public class DocumentApi {

    @Inject
    DocumentPort documentPort;

    @GET
    @Path("/docs/")
    public Response getDocs(@QueryParam("path") String path) {
        if(path==null) path = "";
        List<Document> list = documentPort.getDocs(path);
        return Response.ok(list).build();
    }

    @GET
    @Path("/reload")
    public Response reload() {
        documentPort.reload();
        return Response.ok().build();
    }

}
