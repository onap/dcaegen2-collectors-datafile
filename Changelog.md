# Change Log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## [1.6.2] - 2022/02/11
### Changed
- SDK version bump from 1.8.7 to 1.8.8
- Spring version bump from 5.3.6 to 5.3.14

## [1.6.1] - 2021/08/24
### Changed
- SDK version bump from 1.5.0 to 1.8.7 in order to enable config file support

## [1.6.0] - 2021/07/21
### Changed
- docker base image to integration (from debian based to alpine based)
- java version bump from 11.0.8 to 11.0.9
- remove exec file permission for config file
- tomcat-embed-core version bump from 9.0.39 to 9.0.50
- spring version bump from 5.3.1 to 5.3.6
- spring-boot version bump from 2.4.0 to 2.4.5

## [1.5.5] - 2021/03/09
### Fixed
- make hostname verification configurable
- small sonar fixes related to affected code

## [1.5.4] - 2021/02/23
### Added
- JWT token support for HTTP/HTTPS
### Changed
- FileData / FileServerData prepared to store uri elements

## [1.5.3] - 2021/02/11
### Added
- HTTPS support for DFC
- test related to HTTPS support
- counters for http(s)
### Changed
- ftp clients and tests refactoring
- app config related to certificates unified for ftpes and https

## [1.5.1] - 2021/01/04
### Added
- HTTP support for DFC
- test related to HTTP support
### Changed
- interfaces and Scheme class moved from ftp package to common package
- ftp clients and tests refactoring taking this into account "common package"

## [1.5.0] - 2020/12/15
