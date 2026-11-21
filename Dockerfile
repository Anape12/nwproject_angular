FROM tomcat:9.0-jdk21-temurin@sha256:c341c4e0d21a718f8193611e30ac015540e3c148baa2bf9dc4ae5d7324072933

COPY server.xml /usr/local/tomcat/conf/server.xml
COPY target/app.war /usr/local/tomcat/webapps/ROOT.war
