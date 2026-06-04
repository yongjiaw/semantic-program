FROM almondsh/almond:latest

# Install OpenJDK 11 and set it as default
USER root
RUN apt-get update && \
    apt-get install -y --no-install-recommends openjdk-11-jdk-headless && \
    rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
RUN update-alternatives --set java  $JAVA_HOME/bin/java && \
    update-alternatives --set javac $JAVA_HOME/bin/javac

# Back to the notebook user
USER ${NB_UID}