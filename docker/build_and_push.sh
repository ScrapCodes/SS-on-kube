#!/usr/bin/env bash

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
set -ex

sbt package
docker build -t scrapcodes/spark:v3.0.0-preview-sb-1 -f docker/Dockerfile .
docker push scrapcodes/spark:v3.0.0-preview-sb-1
