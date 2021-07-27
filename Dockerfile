FROM maven:3.6-jdk-11 AS BUILD

COPY ./ /connector/
WORKDIR /connector
RUN mvn -f goobi-viewer-connector/pom.xml clean package


# Build actual application container
FROM tomcat:9-jre11 as ASSEMBLE

ENV VIEWER_URL http://viewer/viewer
ENV SOLR_URL http://solr:8983/solr/collection1

RUN apt-get update && \ 
	apt-get -y install gettext-base \
	  wget && \
	apt-get -y clean && \
	rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/* && \
	rm -rf ${CATALINA_HOME}/webapps/*

RUN mkdir -p /opt/digiverso/viewer/oai && mkdir -p /usr/local/tomcat/conf/Catalina/localhost/ && mkdir -p /usr/local/tomcat/webapps/M2M

# redirect /
RUN mkdir ${CATALINA_HOME}/webapps/ROOT && \
    echo '<% response.sendRedirect("/M2M/"); %>' > ${CATALINA_HOME}/webapps/ROOT/index.jsp

COPY --from=BUILD  /connector/goobi-viewer-connector/target/M2M.war /

RUN unzip /M2M.war -d /usr/local/tomcat/webapps/M2M && rm /M2M.war
RUN wget -q -O /opt/digiverso/viewer/oai/MARC21slimUtils.xsl https://raw.githubusercontent.com/intranda/goobi-viewer-connector/master/goobi-viewer-connector/src/main/resources/MARC21slimUtils.xsl && wget -q -O /opt/digiverso/viewer/oai/MODS2MARC21slim.xsl https://raw.githubusercontent.com/intranda/goobi-viewer-connector/master/goobi-viewer-connector/src/main/resources/MODS2MARC21slim.xsl

COPY docker/* /
COPY docker/config_oai.xml.template /
CMD ["/run.sh"]
