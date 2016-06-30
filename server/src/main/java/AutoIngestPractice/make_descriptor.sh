#!/usr/bin/env bash

# Author: Malisa Smith

# Download all the proto schema files into the schemas_proto folder
# First clean-up old files from previous runs
rm -rf schemas_proto/*
# Obtain the raw github url's if not raw already:
#raw_schema_urls=$(python url_converter.py --getrawfromfile schema_urls)
raw_schema_urls="https://raw.githubusercontent.com/bmeg/bmeg-schemas/master/bmeg/gaea/schema/sample.proto"
for raw_url in ${raw_schema_urls};
do
    wget --timestamping --directory-prefix ./schemas_proto ${raw_url};
done

# Replace user-defined package imports with no path. This allows proto files to find each other.
#For example,     import "ga4gh/common.proto";       becomes       import "common.proto";
for proto_file in schemas_proto/*; do
    sed -i -r '/^import "[A-Za-z0-9/_]+.proto";/ {/^import "google\/protobuf\/[A-Za-z0-9_]+.proto";/!  {s/^import "[A-Za-z0-9/_]+\/([A_Za-z0-9_]+).proto";/import "\1.proto";/}}' $proto_file
done

# Remove any temporary files in the schemas_proto directory which have have been created as a result of editing, etc:
#rm -rf schemas_proto/*~

# Generate descriptor class files for java version with protoc:
protoc descriptor.proto --java_out=.

# convert .proto files into a serialized FileDescriptorSet
cd schemas_proto
protoc --include_source_info -o MyFileDescriptorSet.pb *
cd ../
