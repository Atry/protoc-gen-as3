# Copyright (c) 2010 , 杨博 (Yang Bo) All rights reserved.
#
#         pop.atry@gmail.com
#
# Use, modification and distribution are subject to the "New BSD License"
# as listed at <url: http://www.opensource.org/licenses/bsd-license.php >.
include config.mk

all: classes

classes: compiler/com/netease/protocGenAs3/Main.java proto \
	$(PROTOBUF_DIR)/java/target/protobuf-java-2.3.0.jar
	mkdir -p classes
	javac -encoding UTF-8 -Xlint:unchecked -d classes \
	-classpath "$(PROTOBUF_DIR)/java/target/protobuf-java-2.3.0.jar" \
	-sourcepath "proto$(PATH_SEPARATOR)compiler" \
	compiler/com/netease/protocGenAs3/Main.java

proto: $(PROTOBUF_DIR)/src/$(PROTOC_EXE)
	mkdir -p proto
	"$(PROTOBUF_DIR)/src/$(PROTOC_EXE)" \
	"--proto_path=$(PROTOBUF_DIR)/src" --java_out=proto \
	"$(PROTOBUF_DIR)/src/google/protobuf/compiler/plugin.proto"

$(PROTOBUF_DIR)/src/$(PROTOC_EXE): $(PROTOBUF_DIR)/Makefile
	cd $(PROTOBUF_DIR) && make

$(PROTOBUF_DIR)/Makefile: $(PROTOBUF_DIR)/configure
	cd $(PROTOBUF_DIR) && ./configure

$(PROTOBUF_DIR)/java/target/protobuf-java-2.3.0.jar: $(PROTOBUF_DIR)/src
	cd $(PROTOBUF_DIR)/java && mvn package

plugin: all
	java -ea \
	-classpath "$(PROTOBUF_DIR)/java/target/protobuf-java-2.3.0.jar$(PATH_SEPARATOR)classes" \
	com.netease.protocGenAs3.Main

clean:
	rm -fr classes
	rm -fr proto

.PHONY: plugin all clean
