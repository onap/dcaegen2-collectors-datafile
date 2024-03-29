# DFC (DataFile Collector)

Datafile Collector is responsible for collecting PM counter files from PNF (Physical Network Function) and
then publish these files to Dmaap DataRouter.

## Introduction

DFC is delivered as one **Docker container** which hosts application server and can be started by `docker-compose`.

## Compiling DFC

Whole project (top level of DFC directory) and each module (sub module directory) can be compiled using
`mvn clean install` command.

## Build image 
```
mvn install docker:build
```

## Main API Endpoints

Running with dev-mode of DFC

- **Heartbeat**: http://<container_address>:8100/**heartbeat** or https://<container_address>:8443/**heartbeat**

- **Start DFC**: http://<container_address>:8100/**start** or https://<container_address>:8433/**start**

- **Stop DFC**: http://<container_address>:8100/**stopDatafile** or https://<container_address>:8433/**stopDatafile**

## Maven GroupId:

org.onap.dcaegen2.collectors

### Maven Parent ArtifactId:

dcae-services

### Maven Children Artifacts:
1. datafile-app-server: Datafile Collector (DFC) server
2. datafile-commons: Common code for whole dfc modules
3. datafile-dmaap-client: http client used to connect to dmaap message router/data router

## License

Copyright (C) 2018-2019 NOKIA Intellectual Property, 2018-2019 Nordix Foundation. All rights reserved.
[License](http://www.apache.org/licenses/LICENSE-2.0)
