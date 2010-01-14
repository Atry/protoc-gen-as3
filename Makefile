# Copyright (c) 2010 , 杨博 (Yang Bo) All rights reserved.
#
#         pop.atry@gmail.com
#
# Use, modification and distribution are subject to the "New BSD License"
# as listed at <url: http://www.opensource.org/licenses/bsd-license.php >.
include config.mk

all: classes/com/netease/protocGenAs3/Main.class 

classes/com/netease/protocGenAs3/Main.class: \
	proto/google/protobuf/compiler/Plugin.java \
	compiler/com/netease/protocGenAs3/Main.java \
	$(PROTOBUF_DIR)/java/target/protobuf-java-2.3.0.jar \
	| classes
	javac -encoding UTF-8 -Xlint:all -d classes \
	-classpath "$(PROTOBUF_DIR)/java/target/protobuf-java-2.3.0.jar" \
	-sourcepath "proto$(PATH_SEPARATOR)compiler" \
	compiler/com/netease/protocGenAs3/Main.java

proto/google/protobuf/compiler/Plugin.java: \
	$(PROTOBUF_DIR)/src/$(PROTOC_EXE) | proto
	"$(PROTOBUF_DIR)/src/$(PROTOC_EXE)" \
	"--proto_path=$(PROTOBUF_DIR)/src" --java_out=proto \
	"$(PROTOBUF_DIR)/src/google/protobuf/compiler/plugin.proto"

classes proto test_proto:
	mkdir $@

$(PROTOBUF_DIR)/src/$(PROTOC_EXE): $(PROTOBUF_DIR)/Makefile
	cd $(PROTOBUF_DIR) && make

$(PROTOBUF_DIR)/Makefile: $(PROTOBUF_DIR)/configure
	cd $(PROTOBUF_DIR) && ./configure

$(PROTOBUF_DIR)/configure:
	cd $(PROTOBUF_DIR) && ./autogen.sh

$(PROTOBUF_DIR)/java/target/protobuf-java-2.3.0.jar: $(PROTOBUF_DIR)/src
	cd $(PROTOBUF_DIR)/java && mvn package

plugin: all
	java -ea \
	-classpath "$(PROTOBUF_DIR)/java/target/protobuf-java-2.3.0.jar$(PATH_SEPARATOR)classes" \
	com.netease.protocGenAs3.Main

clean:
	rm -fr classes
	rm -fr proto

test_proto/protobuf_unittest/: \
	$(PROTOBUF_DIR)/src/protoc$(PROTOC_EXE) \
	classes/com/netease/protocGenAs3/Main.class \
	| test_proto
	PATH=protoc-gen-as3/bin:$$PATH \
	"$(PROTOBUF_DIR)/src/protoc$(PROTOC_EXE)" \
	"--proto_path=$(PROTOBUF_DIR)/src" \
	--as3_out=test_proto \
	$(PROTOBUF_DIR)/src/google/protobuf/unittest.proto
	touch $@

.PHONY: plugin all clean
