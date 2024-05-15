package org.cricketmsf.hcms.adapter.in;

import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.info.License;

@OpenAPIDefinition(
/*     tags = {
            @Tag(name="hcms", description="HCMS operations."),
    }, */
    info = @Info(
        title="Cricket HCMS API",
        version = "1.0.0",
        contact = @Contact(
            name = "API Support",
            url = "http://experiot.pl/contact",
            email = "contact@experiot.pl"),
        license = @License(
            name = "The MIT License",
            url = "https://opensource.org/licenses/MIT"))
)
public class OpenApiApplication extends jakarta.ws.rs.core.Application{

}
