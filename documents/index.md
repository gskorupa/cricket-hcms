# Cricket HCMS documentation

<p align="center">
    <img  width="30%" src="/api/file?path=examples/cricket-logo.svg">
</p>

## Introduction

Cricket HCMS is a headless content management system that can be used to provide content for presentation services 
(e.g. websites or mobile apps).

Starting up, the service reads files from the configured disk folder and its subfolders and places them in its database. The database is continuously updated when changes are detected in the monitored file system.

The documents are made available via a REST API, with Markdown-formatted content auomatically translated into HTML.

## About this demo

This demo web application is a simple example of a website that uses Cricket HCMS as a content provider. The website is build using the Svelte framework and is served by a Node.js server. The source code for the website can be found in Git repository [cricket-website](https://github.com/gskorupa/cricket-website).

## HCMS features

- Support for document sources from a Git repository or Wiki (e.g. GitHub wiki, Obsidian and others)
- Markdown support
- HTML support
- Reading document metadata from document content (e.g. title, description, keywords)
- Automatic translation of Markdown content into HTML
- REST API for accessing content
- Reloading content database upon changes detected on disk or on user request
- Serving binary files
- Content versioning

## Developer documentation

- [Building](building.md)
- [Running](running.md)
- [Running with Docker Compose](running-with-docker-compose.md)
- [Content publishing](publishing.md)
- [Accessing binary files](binary-files.md)
- [Multi-language support](multi-language.md)

