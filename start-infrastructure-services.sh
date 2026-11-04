#!/bin/bash -e

. ./set-env.sh

docker compose up -d --build $* mysql


