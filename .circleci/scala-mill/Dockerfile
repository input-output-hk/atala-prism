FROM openjdk:8u181

# Env variables
ENV SCALA_VERSION=2.12.10
ENV MILL_VERSION=0.6.1
ENV TERRAFORM_VERSION=0.12.24
ENV NODE_VERSION=10.16.3

# Define working directory
WORKDIR /root

# Scala expects this file
RUN touch /usr/lib/jvm/java-8-openjdk-amd64/release

# Install Scala
## Piping curl directly in tar
RUN \
  curl -fsL https://downloads.typesafe.com/scala/$SCALA_VERSION/scala-$SCALA_VERSION.tgz | tar xfz - -C /root/ && \
  echo >> /root/.bashrc && \
  echo "export PATH=~/scala-$SCALA_VERSION/bin:$PATH" >> /root/.bashrc

# Install mill
RUN \
  curl -L -o /usr/local/bin/mill https://github.com/lihaoyi/mill/releases/download/$MILL_VERSION/$MILL_VERSION && \
  chmod +x /usr/local/bin/mill && \
  touch build.sc && \
  mill -i resolve _ && \
  rm build.sc

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
