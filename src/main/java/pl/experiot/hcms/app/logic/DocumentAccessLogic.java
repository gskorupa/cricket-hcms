package pl.experiot.hcms.app.logic;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;
import pl.experiot.hcms.app.logic.dto.User;

@ApplicationScoped
public class DocumentAccessLogic {

    @ConfigProperty(name = "organization.default.id")
    Long defaultOrganizationId;

    public String getOrganizationDocName(String documentName, User user){
        if(defaultOrganizationId==null || defaultOrganizationId<0){
            // organization access logic is not enabled
            return documentName;
        }
        // when no user is provided or user has no organization or tenant
        // then return the document name as is
        if(user==null){
            return documentName;
        }
        if(user.organization==null && user.tenant==null){
            return documentName;
        }
        if(defaultOrganizationId!=null && (user.organization==null || user.organization==defaultOrganizationId)){
            return documentName;
        }
        // for all other cases, return the document name with organization and tenant prefix
        String prefix="";
        if(user.organization!=null){
            prefix="/"+user.organization;
        }
        if(user.tenant!=null && user.tenant>0){
            prefix+="/"+user.tenant;
        }
        if(prefix.length()>0){
            int secondSlash = documentName.indexOf("/",1);
            if(secondSlash>0){
                return documentName.substring(0,secondSlash)+prefix+documentName.substring(secondSlash);
            }else{
                return documentName+prefix;
            }
        }
        return documentName;
    }

}
