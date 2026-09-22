set shell := ["bash", "-uc"]

default:
    @just --list

build:
    ./gradlew build

test:
    ./gradlew test

lint:
    ./gradlew checkstyleMain checkstyleTest

# performance-test (./gradlew jmh) is added in implementation step 12,
# once the JMH plugin is wired in.

run:
    ./gradlew bootRun

cli *args:
    ./gradlew bootRun --args="--spring.profiles.active=cli {{args}}"

clean:
    ./gradlew clean

verify: lint test build
