#!/bin/bash

## This script is used to refresh Git repository
## Source: https://stackoverflow.com/questions/73298501/how-to-pull-an-online-github-repository-every-night-via-cron
## Thanks to: https://stackoverflow.com/users/23118/hlovdal

## Usage: ./repo-refresh.sh [--random-delay <minutes>]

if [ "$1" = "--random-delay" ]
then
        sleep $(awk "BEGIN { srand(); print rand()*$2*60 }")
fi

# change the path below to your repository path
cd /home/signomix/signomix-documentation
git checkout main
git fetch origin
git reset --hard origin/main