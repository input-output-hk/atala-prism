FROM openjdk:8

ADD . /usr/app/
WORKDIR /usr/app
ADD node.jar node.jar
CMD ["/usr/local/openjdk-8/bin/java", "-classpath", "/usr/app/node.jar", "io.iohk.node.NodeApp"]
