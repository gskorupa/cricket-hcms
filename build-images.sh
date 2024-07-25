#!/bin/bash

# Script to build and publish Cricket HCMS image

################################
## CONFIGURATION
##

# versions
versionHcms=0.13.3
versionWebsite=0.1.0

# names
imageNameHcms=cricket-hcms
imageNameWebsite=cricket-website

# repository
dockerHubType=true
#dockerRegistry=docker.io
#dockerGroup=gskorupa
#dockerUser=gskorupa
dockerRegistry=
dockerGroup=
dockerUser=


# the above variables can be overridden by local configuration
cfg_location="$1"
echo "cfg_location=$1"
if [ -z "$cfg_location" ]
then
    # default configuration
    cfg_location=./.env
fi
if [ -f "$cfg_location" ]
then
    echo "Building images using configuration from "$cfg_location":"
    . "$cfg_location"
else
    echo "Building images using default config:"
fi

# print config
echo
echo "imageNameHcms=$imageNameHcms"
echo "imageNameWebsite=$imageNameWebsite"
echo "versionHcms=$versionHcms"
echo "versionWebsite=$versionWebsite"
echo "dockerHubType=$dockerHubType"
echo "dockerRegistry=$dockerRegistry"
echo "dockerGroup=$dockerGroup"

##
## end CONFIGURATION
##################################

read -p "Do you want to proceed? (yes/no) " yn
case $yn in 
	yes ) echo ok, building ...;;
	no ) echo exiting...;
		exit;;
	* ) echo invalid response;
        echo exiting...;
		exit 1;;
esac

if [ -z "$2" ] || [ "$2" = "hcms" ]; then
### hcms
./mvnw versions:set -DnewVersion=$versionHcms
retVal=$?
if [ $retVal -ne 0 ]; then
    exit $retval
fi
if [ -z "$dockerRegistry" ]
then
    echo
    ./mvnw \
    -Dquarkus.container-image.name=$imageNameHcms \
    -Dquarkus.container-image.tag=$versionHcms \
    -Dquarkus.container-image.additional-tags=latest \
    -Dquarkus.container-image.build=true \
    clean package
else
    if [ $dockerHubType = "true" ]
    then
    ./mvnw \
    -Dquarkus.container-image.group=$dockerGroup \
    -Dquarkus.container-image.name=$imageNameHcms \
    -Dquarkus.container-image.tag=$versionHcms \
    -Dquarkus.container-image.additional-tags=latest \
    -Dquarkus.container-image.push=true \
    clean package
    else
    ./mvnw \
    -Dquarkus.container-image.registry=$dockerRegistry \
    -Dquarkus.container-image.group=$dockerGroup \
    -Dquarkus.container-image.username=$dockerUser \
    -Dquarkus.container-image.password=$dockerPassword \
    -Dquarkus.container-image.name=$imageNameHcms \
    -Dquarkus.container-image.tag=$versionHcms \
    -Dquarkus.container-image.additional-tags=latest \
    -Dquarkus.container-image.push=true \
    clean package
    fi
fi
retVal=$?
if [ $retVal -ne 0 ]; then
    exit $retval
fi
echo
fi

if [ -z "$2" ] || [ "$2" = "website" ]; then
cd ../cricket-website
echo "PUBLIC_HCMS_URL = 'http://hcms:8080/api/docs'" > .env
echo "PUBLIC_HCMS_INDEX = 'index.md'" >> .env
echo "PUBLIC_HCMS_ROOT = ''" >> .env
if [ -z "$dockerRegistry" ]
then
    docker build -t $imageNameWebsite:$versionWebsite -t $imageNameWebsite:latest .
else
    if [ $dockerHubType = "true" ]
    then
    docker build -t $dockerUser/$imageNameWebsite:$versionWebsite -t $dockerUser/$imageNameWebsite:latest .
    docker push $dockerUser/$imageNameWebsite:$versionWebsite
    docker push $dockerUser/$imageNameWebsite:latest
    else
    docker build -t $dockerRegistry/$dockerGroup/$imageNameWebsite:$versionWebsite -t $dockerRegistry/$dockerGroup/$imageNameWebsite:latest .
    docker push $dockerRegistry/$dockerGroup/$imageNameWebsite:$versionWebsite
    docker push $dockerRegistry/$dockerGroup/$imageNameWebsite:latest
    fi
fi
retVal=$?
if [ $retVal -ne 0 ]; then
    exit $retval
fi
echo
fi


