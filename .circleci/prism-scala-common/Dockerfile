FROM openjdk:8u272

# Env variables
ENV SCALA_2_12_VERSION=2.12.10
ENV SCALA_2_13_VERSION=2.13.6
ENV SBT_VERSION=1.5.3
ENV TERRAFORM_VERSION=0.13.6
ENV NODE_VERSION=10.16.3

# Define working directory
WORKDIR /root

# Install Scala 2.13
## Piping curl directly in tar
RUN \
  curl -fsL https://downloads.typesafe.com/scala/$SCALA_2_13_VERSION/scala-$SCALA_2_13_VERSION.tgz | tar xfz - -C /root/ && \
  echo >> /root/.bashrc && \
  echo "export PATH=~/scala-$SCALA_2_13_VERSION/bin:$PATH" >> /root/.bashrc

# Install sbt
RUN \
  curl -L -o sbt-$SBT_VERSION.deb https://scala.jfrog.io/artifactory/debian/sbt-$SBT_VERSION.deb && \
  dpkg -i sbt-$SBT_VERSION.deb && \
  sbt --sbt-version $SBT_VERSION clean

# Prepare sbt for Scala 2.12 (warm cache)
RUN \
  sbt sbtVersion && \
  mkdir -p project && \
  echo "scalaVersion := \"${SCALA_2_12_VERSION}\"" > build.sbt && \
  echo "sbt.version=${SBT_VERSION}" > project/build.properties && \
  echo "case object Temp" > Temp.scala && \
  sbt compile && \
  rm -r project build.sbt Temp.scala target

# Prepare sbt for Scala 2.13 (warm cache)
RUN \
  sbt sbtVersion && \
  mkdir -p project && \
  echo "scalaVersion := \"${SCALA_2_13_VERSION}\"" > build.sbt && \
  echo "sbt.version=${SBT_VERSION}" > project/build.properties && \
  echo "case object Temp" > Temp.scala && \
  sbt compile && \
  rm -r project build.sbt Temp.scala target

# Install awscli
RUN \
  curl "https://s3.amazonaws.com/aws-cli/awscli-bundle.zip" -o "awscli-bundle.zip" && \
  unzip awscli-bundle.zip && \
  ./awscli-bundle/install -i /usr/local/aws -b /usr/local/bin/aws && \
  rm -rf awscli-bundle.zip awscli-bundle

# Install docker cli
RUN curl -fsSL https://get.docker.com -o get-docker.sh && sh get-docker.sh

# Install terraform
RUN export terraform="terraform_${TERRAFORM_VERSION}_linux_amd64.zip" \
  && curl "https://releases.hashicorp.com/terraform/$TERRAFORM_VERSION/$terraform" -o $terraform \
  && unzip $terraform \
  && mv terraform /usr/local/bin \
  && rm -f $terraform

# Install jq
RUN apt-get update && apt-get install -y jq

# Install node
ENV NVM_DIR=/root/.nvm
RUN \
   curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.35.1/install.sh | bash
RUN . "$NVM_DIR/nvm.sh" && nvm install ${NODE_VERSION} && nvm use v${NODE_VERSION} && nvm alias default v${NODE_VERSION}
ENV PATH="/root/.nvm/versions/node/v${NODE_VERSION}/bin/:${PATH}"
