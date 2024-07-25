#!/bin/bash

# Script to build and publish Cricket HCMS image

################################
## CONFIGURATION
##

# versions
versionHcms=1.0.0

# names
imageNameHcms=cricket-hcms

# repository
dockerHubType=true
dockerRegistry=
dockerGroup=


# the above variables can be overridden by local configuration
cfg_location="$1"
echo "cfg_location=$1"
if [ -z "$cfg_location" ]
then
    # default configuration
    cfg_location=./dev.cfg
fi
if [ -f "$cfg_location" ]
then
    echo "Building the image using configuration from "$cfg_location":"
    . "$cfg_location"
else
    echo "Building the image using default config:"
fi

# print config
echo
echo "imageNameHcms=$imageNameHcms"
echo "versionHcms=$versionHcms"
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
    -Dnative \
    -Dquarkus.container-image.name=$imageNameHcms \
    -Dquarkus.container-image.tag=$versionHcms \
    -Dquarkus.container-image.additional-tags=latest \
    -Dquarkus.container-image.build=true \
    clean package
else
    if [ $dockerHubType = "true" ]
    then
    ./mvnw \
    -Dnative \
    -Dquarkus.container-image.group=$dockerGroup \
    -Dquarkus.container-image.name=$imageNameHcms \
    -Dquarkus.container-image.tag=$versionHcms \
    -Dquarkus.container-image.additional-tags=latest \
    -Dquarkus.container-image.push=true \
    clean package
    else
    ./mvnw \
    -Dnative \
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


