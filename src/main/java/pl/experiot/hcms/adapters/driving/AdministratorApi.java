package pl.experiot.hcms.adapters.driving;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponseSchema;
import org.jboss.logging.Logger;

import jakarta.inject.Inject;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;
import pl.experiot.hcms.app.ports.driving.ForDocumentsIface;

@Path("/api")
/**
 * DocumentApi is a REST API adapter for the DocumentPort. It provides a set of
 * RESTful endpoints to manage documents
 * stored in the system. The API is secured with a token that must be provided
 * in the request header.
 */
public class AdministratorApi {

    @Inject
    ForDocumentsIface documentPort;
    @Inject
    Logger logger;

    @ConfigProperty(name = "app.token")
    String appToken;

    @POST
    @Path("/reload")
    @Produces("text/plain")
    @APIResponse(responseCode = "200", description = "Command executed successfully.")
    @APIResponse(responseCode = "401", description = "Unauthorized")
    @APIResponse(responseCode = "501", description = "Not implemented")
    @APIResponseSchema(value = String.class, responseDescription = "Command executed successfully.", responseCode = "200")
    @Operation(summary = "Command to reload the documents", description = "Forces the system service to reload the documents from the storage.")
    public Response reload(
            @Parameter(description = "Token to authorize the request.", required = true, example = "app-token", schema = @Schema(type = SchemaType.STRING)) @HeaderParam("X-app-token") String token) {
        if (token == null || !token.equals(appToken)) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
        return Response.status(Response.Status.NOT_IMPLEMENTED).entity("Not implemented").build();
        // documentPort.reload();
        // return Response.ok().build();
    }

}
