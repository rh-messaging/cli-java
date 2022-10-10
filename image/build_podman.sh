#!/usr/bin/env bash

#
# Copyright (c) 2022 Red Hat, Inc.
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements. See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License. You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

set -Eeuo pipefail
set -x

ARCH="amd64 arm64 ppc64le s390x"
VERSION="${1:-latest}"
REGISTRY="${2:-quay.io}"
NAMESPACE="${3:-messaging}"

IMAGE_NAME="${REGISTRY}/${NAMESPACE}/cli-java:${VERSION}"

# https://docs.docker.com/build/buildx/multiplatform-images/
sudo podman run --privileged --rm docker.io/tonistiigi/binfmt --install all
podman manifest rm ${IMAGE_NAME} || true

# https://gitlab.cee.redhat.com/keycloak/rhsso-openshift-intermediate-docker-image/-/blob/main/build.sh
echo "Creating a new manifest: ${IMAGE_NAME}"
podman manifest create ${IMAGE_NAME}

echo "Building a new docker image: ${IMAGE_NAME}, arch: ${ARCH}"
for i in $ARCH
do
  podman build --arch=$i -t ${IMAGE_NAME}.${i} --build-arg ARCH=${i} --build-arg VERSION=${VERSION} .
  podman push ${IMAGE_NAME}.${i}
  podman manifest add ${IMAGE_NAME} ${IMAGE_NAME}.${i}
done

echo "Pushing a new manifest: ${IMAGE_NAME}"
podman manifest push ${IMAGE_NAME} docker://${IMAGE_NAME}
