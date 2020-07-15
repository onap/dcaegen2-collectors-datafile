#
# ============LICENSE_START=======================================================
#  Copyright (C) 2019 Nordix Foundation.
#  Copyright (C) 2020 Nokia.
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
FROM openjdk:11.0.6-jre-slim

WORKDIR /opt/app/datafile
RUN mkdir -p /var/log/ONAP
RUN mkdir -p /opt/app/datafile/etc/cert/

RUN groupadd -r onap && useradd -ms /bin/bash datafile -g onap
RUN chown -R datafile:onap /var/log/ONAP

ADD /config/application.yaml /opt/app/datafile/config/
RUN chmod -R 777 /opt/app/datafile/config/

USER datafile

RUN mkdir -p /home/datafile/.ssh
ENV KNOWN_HOSTS_FILE_PATH "/home/datafile/.ssh/known_hosts"

ADD /target/datafile-app-server.jar /opt/app/datafile/

EXPOSE 8100 8433
ENTRYPOINT ["/usr/local/openjdk-11/bin/java", "-jar", "/opt/app/datafile/datafile-app-server.jar"]
