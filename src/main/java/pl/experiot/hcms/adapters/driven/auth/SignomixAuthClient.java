package pl.experiot.hcms.adapters.driven.auth;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path("/api/auth")
@RegisterRestClient
public interface SignomixAuthClient {

    @GET
    @Path("/user")
    Response getUser(@HeaderParam("Authentication") String token);

}
