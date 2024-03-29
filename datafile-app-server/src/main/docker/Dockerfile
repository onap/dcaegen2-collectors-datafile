#
# ============LICENSE_START=======================================================
#  Copyright (C) 2019 Nordix Foundation.
#  Copyright (C) 2020 Nokia.
#  Copyright (C) 2021 Samsung Electronics.
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
FROM nexus3.onap.org:10001/onap/integration-java11:8.0.0

EXPOSE 8100 8433

ARG user=datafile
ARG group=datafile

USER root
WORKDIR /opt/app/datafile

ADD /config/application.yaml /opt/app/datafile/config/

RUN mkdir -p /var/log/ONAP /opt/app/datafile/etc/cert/ && \
    addgroup $group && adduser --system --disabled-password --no-create-home --ingroup $group $user && \
    chown -R $user:$group /var/log/ONAP /opt/app/datafile/config && \
    chmod -R u+rw /opt/app/datafile/config/

# required for CSIT tests
RUN apk add --no-cache openssh && \
    mkdir -p /home/$user/.ssh && \
    chown -R $user:$group /home/$user

USER $user

COPY --chown=$user:$group /target/datafile-app-server.jar /opt/app/datafile/
ENTRYPOINT ["java", "-jar", "/opt/app/datafile/datafile-app-server.jar"]
