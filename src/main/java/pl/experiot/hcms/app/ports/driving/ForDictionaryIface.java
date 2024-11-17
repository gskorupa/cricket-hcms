package pl.experiot.hcms.app.ports.driving;

import pl.experiot.hcms.app.logic.dto.Dictionary;

public interface ForDictionaryIface {
    Dictionary getDictionary(String xmlData);
}
