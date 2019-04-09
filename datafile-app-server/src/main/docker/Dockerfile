#
# ============LICENSE_START=======================================================
#  Copyright (C) 2019 Nordix Foundation.
# ================================================================================
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# SPDX-License-Identifier: Apache-2.0
# ============LICENSE_END=========================================================
#
FROM openjdk:8-jre-alpine

WORKDIR /opt/app/datafile
RUN mkdir -p /var/log/ONAP

ADD /target/datafile-app-server.jar /opt/app/datafile/

ADD /config/application.yaml /opt/app/datafile/config/
ADD /config/dfc.jks.b64 /opt/app/datafile/config/
ADD /config/datafile_endpoints.json /opt/app/datafile/config/
ADD /config/ftp.jks.b64 /opt/app/datafile/config/

EXPOSE 8100 8433

RUN addgroup -S onap && adduser -S datafile -G onap
RUN chown -R datafile:onap /opt/app/datafile
RUN chown -R datafile:onap /var/log/ONAP
RUN base64 -d /opt/app/datafile/config/dfc.jks.b64 > /opt/app/datafile/config/dfc.jks && base64 -d /opt/app/datafile/config/ftp.jks.b64 > /opt/app/datafile/config/ftp.jks 
RUN chown -R root:root /opt/app/datafile/config/*
RUN chmod -R 755 /opt/app/datafile/config/

USER datafile

ENTRYPOINT ["/usr/bin/java", "-jar", "/opt/app/datafile/datafile-app-server.jar"]