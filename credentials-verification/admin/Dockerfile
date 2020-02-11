FROM openjdk:8

ADD . /usr/app/
WORKDIR /usr/app
ADD admin.jar admin.jar
CMD ["/usr/local/openjdk-8/bin/java", "-classpath", "/usr/app/admin.jar", "io.iohk.cvp.admin.AdminApp"]
