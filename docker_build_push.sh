#!/usr/bin/env bash
set -e

if ! command -v docker > /dev/null; then
	echo "docker needs to be installed"
	exit 1
fi

[[ -z ${IMAGE_PREFIX} ]] && IMAGE_PREFIX="registry.cn-hangzhou.aliyuncs.com/sspirits/blog"
# tag is release version
: "${TAG:?"Need to set tag version, for example 1.6.2 for releasing or test_build for test"}"
IMAGE=${IMAGE_PREFIX}:${TAG}

./gradlew clean installShadowDist
docker build -t "${IMAGE}" .
docker push "${IMAGE}"
