First run `sh make_descriptor.sh` to generate MyFileDescriptorSet.pb in the schemas_proto folder.

Next convert the schema .proto file into its .java format and compile it. Replace paths and file names with your local version.
```
$SRC_DIR='/home/malisa/git_repos/gaea/server/src/main/scala/gaea/convoy/AutoIngestPractice/schemas_proto'
$DST_DIR='/home/malisa/git_repos/gaea/server/src/main/scala/gaea/convoy/AutoIngestPractice'
protoc -I=${SRC_DIR} --java_out=$DST_DIR $SRC_DIR/sample.proto
javac Sample.java
```

Next compile all the top-level .java files into .class files. Replace the `PROTOBUF_JAVA_JAR` path with your local version.
```
PROTOBUF_JAVA_JAR='/home/malisa/.m2/repository/com/google/protobuf/protobuf-java/3.0.0-beta-2/protobuf-java-3.0.0-beta-2.jar'
javac -cp ${PROTOBUF_JAVA_JAR}:. FieldInfo.java MapInfo.java MessageAsNode.java ParsedDescriptor.java ParsedDescriptorTest.java`
```

Finally, run ParsedDescriptorTest. Replace `FILEDESCRIPTORSET_PATH` with your local version.
```
FILEDESCRIPTORSET_PATH='/home/malisa/git_repos/gaea/server/src/main/scala/gaea/convoy/AutoIngestPractice/schemas_proto/MyFileDescriptorSet.pb'
java -cp ${PROTOBUF_JAVA_JAR}:. ParsedDescriptorTest ${FILEDESCRIPTORSET_PATH}
```