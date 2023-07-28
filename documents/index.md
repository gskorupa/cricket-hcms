# Cricket HCMS documentation

## Introduction

Cricket HCMS is a headless content management system for managing content for websites and mobile applications. It is built on top of [Quarkus Microservices Framework](https://quarkus.io/).

In order to present how to use this site, I have prepared a related cricket-website-template project that allows you to view documents served by Cricket HCMS. The template is a SvelteKit application that can be built as a dynamic Node website or as a Docker image. The template is available at [https://githuc.com/gskorupa/cricket-website-template](https://githuc.com/gskorupa/cricket-website-template).

Both services can be run wit Docker Compose using docker-compose.yml file also available in the Cricket HCMS repository.

## Features

- [x] Git based content repository - code files (`*.md, *.html`) or Wiki pages
- [x] Markdown support
- [x] HTML support
- [x] REST API for accessing content
- [x] content versioning (provided by Git repository)
- [ ] multilanguage support
- [ ] reloading content database upon changes detected on disk
- [ ] full text search
- [ ] filtering content by tags
- [ ] service authorization
- [ ] content tags 

## Documentation

- [Building](building.md)
- [Running](running.md)
