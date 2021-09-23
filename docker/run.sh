#!/bin/bash
set -e

[ -z "$CONFIGSOURCE" ] && CONFIGSOURCE="default"

set -u

# echo "Applying from environment..."
envsubst '\$VIEWER_URL \$SOLR_URL' </config_oai.xml.template >/opt/digiverso/viewer/config/config_oai.xml
envsubst '\$VIEWER_DOMAIN' </usr/local/tomcat/conf/server.xml.template >/usr/local/tomcat/conf/server.xml

case $CONFIGSOURCE in
  folder)
    if [ -z "$CONFIG_FOLDER" ]
    then
      echo "CONFIG_FOLDER is required"
      exit 1
    fi

    if ! [ -d "$CONFIG_FOLDER" ]
    then
      echo "CONFIG_FOLDER: $CONFIG_FOLDER does not exists or is not a folder"
      exit 1
    fi

    echo "Copying configuration from local folder"
    [ -d "$CONFIG_FOLDER" ] && cp -arv "$CONFIG_FOLDER"/* /opt/digiverso/viewer/config/
    ;;

  *)
    echo "Keeping configuration"
    ;;
esac

echo "Starting application server..."
exec catalina.sh run
