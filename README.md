# Imaging service

[![Build Status](https://travis-ci.com/ArqiSoft/imaging-service.svg?branch=master)](https://travis-ci.com/ArqiSoft/imaging-service)

Service generates thumbnails for various file formats:

## Supported formats

* jpg, jpeg, png, bmp, gif, tif, tiff, svg, ico - ouptut formats: jpeg, png, bmp, gif, tif, ico

* pdf, doc, docx, xls, xlsx, ppt, pptx, ods, odt - output formats: jpg, jpeg, png, bmp, gif

* rxn, mol - utput formats: png, svg

* cif - output format: png

* czi, lif, ims, lsm, nd2 - output format: png

## System Requirements

Java 1.8, Maven 3.x
Optional: docker, docker-compose

## Local Build Setup

```terminal
# build
mvn clean package

# run as standalone application
mvn spring-boot:run
```

## Create and start docker image

1. Use `docker-compose build` command to build the docker image.
2. Use `docker-compose up -d` command to launch the docker image.
