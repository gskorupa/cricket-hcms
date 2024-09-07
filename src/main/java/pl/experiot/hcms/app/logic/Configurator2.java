package pl.experiot.hcms.app.logic;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import jakarta.enterprise.context.ApplicationScoped;
import pl.experiot.hcms.adapters.driven.loader.fs.FromFilesystemLoader;
import pl.experiot.hcms.adapters.driven.loader.test.TestDocLoader;
import pl.experiot.hcms.adapters.driven.repo.DocumentRepository;
import pl.experiot.hcms.adapters.driven.repo.DocumentRepositoryH2;
import pl.experiot.hcms.adapters.driven.translator.DeeplTranslator;
import pl.experiot.hcms.adapters.driven.translator.DummyRepoModel;
import pl.experiot.hcms.adapters.driven.translator.DummyTranslator;
import pl.experiot.hcms.adapters.driven.translator.PathBasedRepoModel;
import pl.experiot.hcms.app.ports.driven.ForDocumentRepositoryIface;
import pl.experiot.hcms.app.ports.driven.ForDocumentsLoaderIface;
import pl.experiot.hcms.app.ports.driven.ForMultilanguageRepoModelIface;
import pl.experiot.hcms.app.ports.driven.ForTranslatorIface;

@ApplicationScoped
public class Configurator2 {

    @ConfigProperty(name = "hcms.database.type")
    String databaseType;
    @ConfigProperty(name = "hcms.loader.type")
    String loaderType;
    @ConfigProperty(name = "document.folders.sites")
    String sites;
    @ConfigProperty(name = "document.folders.excluded")
    String excludes;
    @ConfigProperty(name = "document.syntax")
    String syntax; /* "obsidian", "github" */
    @ConfigProperty(name = "document.extension.markdown")
    String markdownFileExtension;
    @ConfigProperty(name = "document.extension.html")
    String htmlFileExtension;
    @ConfigProperty(name = "document.folders.root")
    String root;
    @ConfigProperty(name = "document.folders.assets")
    String assets;
    @ConfigProperty(name = "hcms.sevice.url")
    String hcmsServiceUrl;

    @ConfigProperty(name = "hcms.translator.type")
    String translatorType;
    @ConfigProperty(name = "hcms.localization.model")
    String localizationModel;
    @ConfigProperty(name = "hcms.repository.language.main")
    String mainLanguage;
    @ConfigProperty(name = "hcms.repository.languages")
    String[] languages;

    public ForDocumentRepositoryIface getRepositoryPort() {
        switch (databaseType) {
            case "h2":
                return new DocumentRepositoryH2();
            case "map":
                return new DocumentRepository();
            default:
                return new DocumentRepositoryH2();
        }
    }

    public ForDocumentsLoaderIface getLoaderPort() {
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
        //loader.setRepositoryPort(getRepositoryPort());
        loader.setSites(sites);
        loader.setMarkdownFileExtension(markdownFileExtension);
        loader.setHtmlFileExtension(htmlFileExtension);
        loader.setSyntax(syntax);
        return loader;
    }

    public ForTranslatorIface getTranslatorPort() {
        ForTranslatorIface translator = null;
        switch (translatorType) {
            case "dummy":
                translator = new DummyTranslator();
                break;
            case "deepl":
                translator = new DeeplTranslator();
                break;                
            default:
                translator = new DummyTranslator();
                break;
        }
        return translator;

    }

    public ForMultilanguageRepoModelIface getRepoModelPort() {
        ForMultilanguageRepoModelIface repoModel;
        switch (localizationModel) {
            case "dummy":
                repoModel = new DummyRepoModel();
                break;
            case "path_prefix":
                repoModel = new PathBasedRepoModel();
                break;
            default:
                repoModel = new DummyRepoModel();
                break;
        }
        repoModel.setMainLanguage(mainLanguage);
        repoModel.setRepoLanguages(languages);
        return repoModel;
    }

}
