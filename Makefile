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
	$(PROTOBUF_DIR)/src/$(PROTOC) | proto
	"$(PROTOBUF_DIR)/src/$(PROTOC)" \
	"--proto_path=$(PROTOBUF_DIR)/src" --java_out=proto \
	"$(PROTOBUF_DIR)/src/google/protobuf/compiler/plugin.proto"

dist.tar.gz: dist/protoc-gen-as3 dist/protoc-gen-as3.bat \
	dist/protobuf.swc dist/README\
	dist/protoc-gen-as3.jar dist/protobuf-java-2.3.0.jar
	tar -zcf dist.tar.gz dist

dist/README: README | dist
	cp README dist/README

dist/protoc-gen-as3: | dist
	echo -n -e '#!/bin/sh\ncd `dirname "$$0"`\njava -cp protobuf-java-2.3.0.jar -jar protoc-gen-as3.jar' > $@
	chmod +x $@

dist/protoc-gen-as3.bat: | dist
	echo -n -e '@echo off\r\ncd %~dp0\r\njava -cp protobuf-java-2.3.0.jar -jar protoc-gen-as3.jar' > $@
	chmod +x $@

dist/protobuf.swc: as3 | dist
	$(COMPC) -include-sources+=as3 -output=$@

dist/protoc-gen-as3.jar: classes/com/netease/protocGenAs3/Main.class | dist
	jar ecf com/netease/protocGenAs3/Main $@ classes

dist/protobuf-java-2.3.0.jar: \
	$(PROTOBUF_DIR)/java/target/protobuf-java-2.3.0.jar \
	| dist
	cp $< $@

classes proto test_proto dist:
	mkdir $@

$(PROTOBUF_DIR)/src/$(PROTOC): $(PROTOBUF_DIR)/Makefile
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
	rm -fr dist
	rm -fr dist.tar.gz
	rm -fr classes
	rm -fr proto
	rm -fr test_proto
	rm -fr test.swc

test.swc: test_proto/protobuf_unittest
	$(COMPC) -include-sources+=test_proto,as3 -output=test.swc

test_proto/protobuf_unittest: \
	$(PROTOBUF_DIR)/src/$(PROTOC) \
	classes/com/netease/protocGenAs3/Main.class \
	| test_proto
	PATH=bin:$$PATH \
	"$(PROTOBUF_DIR)/src/$(PROTOC)" \
	"--proto_path=$(PROTOBUF_DIR)/src" \
	--as3_out=test_proto \
	$(PROTOBUF_DIR)/src/google/protobuf/unittest.proto \
	$(PROTOBUF_DIR)/src/google/protobuf/unittest_import.proto
	touch $@

.PHONY: plugin all clean
