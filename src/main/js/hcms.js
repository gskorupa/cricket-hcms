/**
 * Cricket HCMS API client library
 */
export const hcms = {
    /**
     * Get documents from Cricket HCMS
     * @param {boolean} devMode - true if development mode
     * @param {string} serviceUrl - HCMS service URL
     * @param {object} pathObject - path object. See: https://kit.svelte.dev/docs/routing#page-page-js
     * @param {string} rootFolder - root folder (for future use - actualy should be empty string)
     * @param {string} indexFileName - index file name
     * @returns {object} - documents
     */
    getDocuments: async function (devMode, serviceUrl, pathObject, rootFolder, indexFileName) {
        //console.log("hcms.getDocuments: devMode=" + devMode +" serviceUrl=" + serviceUrl)
        if (devMode) {
            let docs = [
                { content: "<b>test</b> doc", path: "/doc1" },
            ]
            return { paths: this.getPaths(pathObject.params.file), documents: docs }
        }
        let method = 'GET'
        let url = serviceUrl + "?content=true&path="
        //console.log("hcms.getDocuments: path", pathObject)
        if (pathObject.params.file.length == 0 && indexFileName != undefined) {
            url = url + rootFolder + '/' + indexFileName
        } else {
            url = url + rootFolder + '/' + pathObject.params.file
        }
        console.log("hcms.getDocuments: url=" + url)
        const headers = new Headers()
        headers.set('Accept', 'application/json');
        const response = await fetch(url, { method: method, mode: 'cors', headers: headers })
            .then(response => {
                if (!response.ok) {
                    throw new Error(response.statusText + " " + response.status)
                }
                let documents = response.json()
                return {
                    paths: this.getPaths(pathObject.params.file),
                    documents: documents
                }
            })
            .catch(error => {
                console.error('There was an error!', error)
                return {
                    paths: this.getPaths(pathObject.params.file),
                    documents: []
                }
            })
        return response
    },
    /**
     * Get paths
     * @param {string} docpath - document path
     * @returns {object} - paths
    */
    getPaths: function (docpath) {
        let paths = docpath.split("/")
        //console.log("hcms.getPaths: paths=" + paths)
        // insert "home" path if not present
        if (paths.length > 0 && paths[0].length > 0) {
            paths.splice(0, 0, "")
        }
        let result = []
        for (let i = 0; i < paths.length; i++) {
            result.push({
                name: paths[i],
                path: "/"
            })
        }
        for (let i = 1; i < paths.length; i++) {
            let tmp = result[i - 1].path + '/' + paths[i]
            if (tmp.startsWith("//")) {
                tmp = tmp.substring(1)
            }
            result[i].path = tmp
        }
        //console.log(result)
        return result
    },
    getElementPath: function (index, paths) {
        let path = ""
        for (let i = 0; i < index; i++) {
            path += paths[i]
        }
        return path
    },
    /**
     * Get document
     * @param {boolean} devMode - true if development mode
     * @param {string} serviceUrl - HCMS service URL
     * @param {string} path - document path
     * @param {string} indexFile - index file name
     * @param {string} token - optional authentication token (for future use)
     * @param {string} type - document type (use "navigation" to get example navigation.json in development mode
     * @returns {object} - document
     * @throws {Error} - error
     * */
    getDocument: function (devMode, serviceUrl, path, indexFile, token, type) {
        try {
            return Promise.resolve(getHcmsDocument(devMode, serviceUrl, path, indexFile, token, type)).then((result) => result);
        } catch (e) {
            throw new Error(e);
        }
    }

}

const getHcmsDocument = async function (devMode, serviceUrl, path, indexFile, token, type) {
    //console.log("hcms.getDocument: devMode=" + devMode +" serviceUrl=" + serviceUrl)
    if (devMode) {
        if (type != undefined && type == "navigation") {
            return {
                "title": "EXPERIOT",
                "logo": "logo.svg",
                "elements": [
                    { url: { pl: "pl", en: "en" }, label: { en: "Home", pl: "Start" }, target: "" }
                ]
            }
        } else {
            return {
                document:
                {
                    content: "<b>test</b> doc<br>" + path
                }
            }
        }
    }
    try {
        const headers = new Headers()
        //headers.set('Accept', 'application/json');
        if (token != undefined && token != null) {
            headers.set('Authentication', token);
        }
        let endpoint;
        if (typeof path === 'string' || path instanceof String) {
            //console.log("path is a string")
            endpoint = serviceUrl + "/api/document?path=" + path
        } else {
            //console.log("path is an object")
            endpoint = serviceUrl + "/api/document?path=/" + path.params.file
        }
        if (!(endpoint.endsWith(".md") || endpoint.endsWith(".html") || endpoint.endsWith(".json"))) {
            endpoint = endpoint + "/" + indexFile
        }
        console.log("endpoint:", endpoint)
        const res = await fetch(endpoint, {
            mode: 'cors',
            method: 'GET',
            headers: headers
        });
        let data;
        //console.log("res:", res.type)
        //console.log("res:", res.status)
        if (res.status == 200) {
            //if(type.toLowerCase() !== "file") {
                return await res.json();
            //}else{
            //    data = { content: "binary content" }
            //    return data
            //}
        } else {
            data = { content: "No content" }
            return data
        }
    } catch (e) {
        console.log('HCMS ERROR', e)
        throw new Error(e);
    }
}