#!/bin/sh

podman run --entrypoint='' -v .:/home:Z -v ~/.m2:/root/.m2:Z -i -it maven:3-jdk-8 /bin/sh -c 'cd /home; mvn clean install'
