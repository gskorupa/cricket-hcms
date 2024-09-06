package pl.experiot.hcms.app.logic;

import pl.experiot.hcms.adapters.driven.loader.fs.FromFilesystemLoader;
import pl.experiot.hcms.adapters.driven.loader.test.TestDocLoader;
import pl.experiot.hcms.adapters.driven.repo.DocumentRepository;
import pl.experiot.hcms.adapters.driven.repo.DocumentRepositoryH2;
import pl.experiot.hcms.adapters.driven.translator.DummyRepoModel;
import pl.experiot.hcms.adapters.driven.translator.DummyTranslator;
import pl.experiot.hcms.app.ports.driven.ForDocumentRepositoryIface;
import pl.experiot.hcms.app.ports.driven.ForDocumentsLoaderIface;
import pl.experiot.hcms.app.ports.driven.ForMultilanguageRepoModelIface;
import pl.experiot.hcms.app.ports.driven.ForTranslatorIface;

public class Configurator {

    public static ForDocumentRepositoryIface getRepositoryPort(String databaseType) {
        switch (databaseType) {
            case "h2":
                return new DocumentRepositoryH2();
            case "map":
                return new DocumentRepository();
            default:
                return new DocumentRepositoryH2();
        }
    }

    public static ForDocumentsLoaderIface getLoaderPort(String loaderType, String root, String assets, String excludes,
            String hcmsServiceUrl) {
        ForDocumentsLoaderIface loader;
        switch (loaderType.toLowerCase()) {
            case "filesystem":
                loader = new FromFilesystemLoader();
                loader.setAssets(assets);
                loader.setExcludes(excludes);
                loader.setHcmsServiceUrl(hcmsServiceUrl);
                loader.setRoot(root);
                break;
            default:
                loader = new TestDocLoader();
        }
        return loader;
    }

    public static ForTranslatorIface getTranslatorPort(String translatorType) {
        ForTranslatorIface translator = null;
        switch (translatorType) {
            case "dummy":
                translator = new DummyTranslator();
                break;
            default:
                translator = new DummyTranslator();
                break;
        }
        return translator;  
    }

    public static ForMultilanguageRepoModelIface getRepoModelPort(String localizationModel) {
        ForMultilanguageRepoModelIface repoModel = null;
        switch (localizationModel) {
            case "dummy":
                repoModel = new DummyRepoModel();
                break;
            default:
                repoModel = new DummyRepoModel();
                break;
        }
        return repoModel;
    }

}
