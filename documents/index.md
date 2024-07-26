# Cricket HCMS documentation

<p align="center">
    <img src="assets/cricket-logo.svg" width="20%">
</p>

## Introduction

Cricket HCMS is a headless content management system that can be used to provide content for presentation services (e.g. websites or mobile apps).
Starting up, the service reads files from the configured disk folder and its subfolders and places them in its database. The database is continuously updated when changes are detected in the monitored file system.
The documents are made available via a REST API, with Markdown-formatted content automatically translated into HTML.

## About this demo

This demo is an example of a website that uses Cricket HCMS. The website is build using the Svelte framework and is served by a Node.js server. The website code is intentionally simple to focus on the integration with the Cricket HCMS service.
The source code in Git repository [cricket-website](https://github.com/gskorupa/cricket-website) can be used as a starting point for building a more complex website using the Cricket HCMS service.

The demo's hcms service is configured to read documents from the `documents` folder in the root of the repository. See [Running with Docker Compose](running-with-docker-compose.md) for more information on how the demo is configured.

## HCMS features

- Serving content from a Git repository or Wiki files (e.g. GitHub wiki, Obsidian and others)
- Markdown support
- HTML support
- Reading document metadata from document content (e.g. title, description, keywords)
- Automatic translation of Markdown content into HTML
- REST API for accessing content
- Reloading content database upon changes detected on disk or on user request
- Serving binary files (e.g. images, PDFs, etc.)

## Developer documentation

- [Building](building.md)
- [Running](running.md)
- [Running with Docker Compose](running-with-docker-compose.md)
- [Content publishing](publishing.md)
- [Accessing binary files](binary-files.md)
- [Multi-language support](multi-language.md)

