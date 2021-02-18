# Change Log
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/)
and this project adheres to [Semantic Versioning](http://semver.org/).

## [1.5.5] - 25/02/2021 
### Changed
- docker base image to integration (from debian based to alpine based)
- java version bump from 11.0.8 to 11.0.9
- remove exec file permission for config file

## [1.5.4] - 23/02/2021 
### Added
- JWT token support for HTTP/HTTPS
### Changed
- FileData / FileServerData prepared to store uri elements

## [1.5.3] - 11/02/2021 
### Added
- HTTPS support for DFC
- test related to HTTPS support
- counters for http(s)
### Changed
- ftp clients and tests refactoring
- app config related to certificates unified for ftpes and https

## [1.5.1] - 04/01/2021 
### Added
- HTTP support for DFC
- test related to HTTP support
### Changed
- interfaces and Scheme class moved from ftp package to common package
- ftp clients and tests refactoring taking this into account "common package"
 
## [1.5.0] - 15/12/2020      
