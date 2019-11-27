FROM openjdk:8

ADD . /usr/app/
WORKDIR /usr/app
ADD connector.jar connector.jar
CMD ["/usr/local/openjdk-8/bin/java", "-classpath", "/usr/app/connector.jar", "io.iohk.connector.ConnectorApp"]
