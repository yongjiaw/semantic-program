# manually build Soar for linux now, package it with the docker later
docker run --rm \
  --platform linux/amd64 \
  -v ../Soar:/soar \
  ubuntu:22.04 \
  bash -c "
    apt-get update -q && \
    apt-get install -y --no-install-recommends \
      build-essential swig python3 python3-pip openjdk-11-jdk-headless && \
    pip3 install --no-cache-dir scons && \
    cd /soar && \
    python3 scons/scons.py --opt --out=/soar/out-linux sml_java
  "