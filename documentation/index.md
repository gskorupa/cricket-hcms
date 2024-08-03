# Cricket HCMS documentation

<p align="center">
    <img src="./assets/cricket-logo.svg" width="20%">
</p>

## Introduction

Cricket HCMS is a headless content management system designed for developers looking for an easy and quick to implement HCMS solution. It is a lightweight, file-based CMS that provides a REST API for content retrieval. 

Cricket HCMS is a headless content management system that can be used to provide content for presentation services (e.g. websites or mobile applications).
At startup, the service reads files from the configured disk folder and its subfolders and stores them in its database. The database is continuously updated as changes are detected in the monitored file system.
Documents are made available via a REST API, with Markdown formatted content automatically translated into HTML.

## About this demo

This demo is an example of a website using Cricket HCMS. It is built using the Svelte framework and is served by a Node.js server. The website code is kept simple to focus on the integration with the Cricket HCMS service.
The [source code](https://github.com/gskorupa/cricket-website) can be used as a starting point for building a more complex website using the Cricket HCMS service.

The demo hcms service is configured to read documents from the `documents` folder in the root of the repository. See [Running with Docker Compose](running-with-docker-compose.md) for more information on how the demo is configured.

## HCMS features

- Serves content from a Git repository or wiki files (e.g. GitHub wiki, Obsidian and others)
- Markdown support
- HTML support
- Read document metadata from document content (e.g. title, description, keywords)
- Automatic translation of Markdown content into HTML
- REST API to access content
- Reload content database when changes are detected on disk or on user request
- Serving binary files (e.g. images, PDFs, etc.)
- Multi-site support

Release history is available in the [history](history.md) document.

Planned features are listed on [the project roadmap](https://github.com/users/gskorupa/projects/5).

## Developer documentation

- [Building](development/building.md)
- [Running](development/running.md)
- [Running with Docker Compose](development/running-with-docker-compose.md)
- [Content publishing](development/publishing.md)
- [Accessing binary files](development/binary-files.md)
- [Multi-language support](development/multi-language.md)
- [HCMS API documentation](development/rest-api.md)

