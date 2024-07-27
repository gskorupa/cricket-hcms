# Documents publishing

## Introduction

THIS DOCUMENT IS A WORK IN PROGRESS

Assuming you are editing the documents on your local machine, you can publish it
to a remote server running the Cricket HCMS service.

It is highly recommended to use a version control system like Git to manage your
documents. This way you can easily track changes and revert them if necessary. You
can also use a version control system to publish your documents as described below.

When you are ready to publish your documents, you can do so in a number of ways.

- sending a local documents tree with `scp` command
- using a GitHub actions workflow triggered by a push to the repository

## Sending a documents tree

You can send a documents tree using `scp` command. The documents tree is a folder containing
the `documents` folder. The `documents` folder contains all the documents you want to publish.

```shell
scp -r documents/ user@host:/path/to/destination
```

Where:

- `documents/` is the documents tree root folder
- `user` is the user name on the remote host
- `host` is the remote host name or IP address
- `/path/to/destination` is the path to the documents folder on the remote host

## Using a Git repository

Where documents are stored in a Git repository, this repository can be cloned on a server running Cricket HCMS.

In this case, in order to publish a new version, log on to this server, navigate to the folder with the cloned repository and issue the `git pull` command. HCMS will detect the changes to the documents automatically and update its database.

## Using a GitHub actions workflow

If the source documents are in a Git repository, it is possible to automate their publishing.
A GitHub action workflow can be used for this purpose. The workflow is defined in the file `.github/workflows/publish.yml`. You can use it as a template for your own workflow.

See: https://github.com/marketplace/actions/scp-files