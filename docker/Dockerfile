# Copyright IBM, 2019.
#
# This file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


FROM scrapcodes/spark:v3.0.0-preview

ARG spark_uid=185

# RUN set -ex && \
#     mkdir -p /opt/jars/

COPY demo/target/scala-2.12/demo_2.12-0.1.0-SNAPSHOT.jar /opt/
COPY docker/*properties /opt/

ENV SPARK_HOME /opt/spark

WORKDIR /opt/spark/work-dir

ENTRYPOINT [ "/opt/entrypoint.sh" ]

USER ${spark_uid}

