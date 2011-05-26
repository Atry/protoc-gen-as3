# Copyright (c) 2010 , NetEase.com,Inc. All rights reserved.
#
# Author: Yang Bo (pop.atry@gmail.com)
#
# Use, modification and distribution are subject to the "New BSD License"
# as listed at <url: http://www.opensource.org/licenses/bsd-license.php >.
include config.mk

ifeq ($(OS), Windows_NT)
PROTOC_GEN_AS3=dist\\protoc-gen-as3$(BAT)
else
PROTOC_GEN_AS3=dist/protoc-gen-as3$(BAT)
endif

ALL=dist/protoc-gen-as3 dist/protoc-gen-as3.bat \
	dist/protobuf.swc dist/README dist/options.proto\
	dist/protoc-gen-as3.jar dist/protobuf-java-$(PROTOBUF_VERSION).jar

all: $(ALL)

classes/com/netease/protocGenAs3/Main.class: \
	plugin.proto.java/google/protobuf/compiler/Plugin.java \
	options.proto.java/com \
	compiler/com/netease/protocGenAs3/Main.java \
	$(PROTOBUF_DIR)/java/target/protobuf-java-$(PROTOBUF_VERSION).jar \
	| classes
	$(JAVAC) -encoding UTF-8 -Xlint:all -d classes \
	-classpath "$(PROTOBUF_DIR)/java/target/protobuf-java-$(PROTOBUF_VERSION).jar" \
	-sourcepath "plugin.proto.java$(PATH_SEPARATOR)compiler$(PATH_SEPARATOR)options.proto.java" \
	compiler/com/netease/protocGenAs3/Main.java

plugin.proto.java/google/protobuf/compiler/Plugin.java: \
	$(PROTOCDEPS) | plugin.proto.java
	$(PROTOC) \
	"--proto_path=$(PROTOBUF_DIR)/src" --java_out=plugin.proto.java \
	"$(PROTOBUF_DIR)/src/google/protobuf/compiler/plugin.proto"

dist.tar.gz: $(ALL)
	tar -acf dist.tar.gz -C dist .

dist/README: README | dist
	cp $< $@

dist/options.proto: options.proto | dist
	cp $< $@

dist/protoc-gen-as3: dist/protoc-gen-as3.jar dist/protobuf-java-$(PROTOBUF_VERSION).jar \
	| dist
	(echo '#!/bin/sh';\
	echo 'cd `dirname "$$0"` && java -jar protoc-gen-as3.jar') > $@
	chmod +x $@

dist/protoc-gen-as3.bat: dist/protoc-gen-as3.jar dist/protobuf-java-$(PROTOBUF_VERSION).jar \
	| dist
	(echo '@cd %~dp0';\
	echo '@java -jar protoc-gen-as3.jar') > $@
	chmod +x $@

dist/protobuf.swc: descriptor.proto.as3/google \
	$(wildcard as3/com/netease/protobuf/*.as) | dist
	$(COMPC) -include-sources+=as3,descriptor.proto.as3 -output=$@

MANIFEST.MF:
	echo Class-Path: protobuf-java-$(PROTOBUF_VERSION).jar > $@

dist/protoc-gen-as3.jar: classes/com/netease/protocGenAs3/Main.class \
	MANIFEST.MF | dist
	$(JAR) cemf com/netease/protocGenAs3/Main MANIFEST.MF $@ -C classes .

dist/protobuf-java-$(PROTOBUF_VERSION).jar: \
	$(PROTOBUF_DIR)/java/target/protobuf-java-$(PROTOBUF_VERSION).jar \
	| dist
	cp $< $@

options.proto.java descriptor.proto.as3 classes plugin.proto.java unittest.proto.as3 dist:
	mkdir $@

ifndef PROTOC
PROTOC=$(PROTOBUF_DIR)/src/protoc$(EXE)
PROTODEPS=$(PROTOC)
$(PROTOC): $(PROTOBUF_DIR)/Makefile 
	cd $(PROTOBUF_DIR) && $(MAKE)

$(PROTOBUF_DIR)/Makefile: $(PROTOBUF_DIR)/configure
	cd $(PROTOBUF_DIR) && ./configure

$(PROTOBUF_DIR)/configure:
	cd $(PROTOBUF_DIR) && ./autogen.sh

$(PROTOBUF_DIR)/java/target/protobuf-java-$(PROTOBUF_VERSION).jar: \
	$(PROTOBUF_DIR)/src \
	$(PROTOC)
	cd $(PROTOBUF_DIR)/java && $(MVN) package
endif

clean:
	$(RM) -r dist
	$(RM) -r dist.tar.gz
	$(RM) -r classes
	$(RM) -r unittest.proto.as3
	$(RM) -r descriptor.proto.as3
	$(RM) -r plugin.proto.java
	$(RM) -r test.swc
	$(RM) -r test.swf
	$(RM) -r options.proto.java

test: test.swf
	(sleep 1s; echo c; sleep 1s; echo c; sleep 1s) | $(FDB) $<

test.swf: test.swc test/Test.mxml dist/protobuf.swc
	$(MXMLC) -library-path+=test.swc,dist/protobuf.swc -output=$@ \
	-source-path+=test test/Test.mxml -debug \
	-static-link-runtime-shared-libraries=true

test.swc: unittest.proto.as3/protobuf_unittest dist/protobuf.swc
	$(COMPC) -include-sources+=unittest.proto.as3 \
	-external-library-path+=dist/protobuf.swc -output=$@

options.proto.java/com: \
	options.proto \
	$(PROTOCDEPS) \
	| options.proto.java 
	$(PROTOC) \
	--proto_path=. \
	"--proto_path=$(PROTOBUF_DIR)/src" \
	--java_out=options.proto.java $<
	touch $@

descriptor.proto.as3/google: \
	$(PROTOCDEPS) \
	dist/protoc-gen-as3$(BAT) \
	| descriptor.proto.as3
	$(PROTOC) \
	--plugin=protoc-gen-as3=$(PROTOC_GEN_AS3) \
	"--proto_path=$(PROTOBUF_DIR)/src" \
	--as3_out=descriptor.proto.as3 \
	"$(PROTOBUF_DIR)/src/google/protobuf/descriptor.proto"
	touch $@

unittest.proto.as3/protobuf_unittest: \
	$(PROTOCDEPS) \
	dist/protoc-gen-as3$(BAT) \
	$(wildcard test/*.proto) \
	| unittest.proto.as3
	$(PROTOC) \
	--plugin=protoc-gen-as3=$(PROTOC_GEN_AS3) \
	--proto_path=test --proto_path=. \
	"--proto_path=$(PROTOBUF_DIR)/src" \
	--as3_out=unittest.proto.as3 \
	$(PROTOBUF_DIR)/src/google/protobuf/unittest.proto \
	$(PROTOBUF_DIR)/src/google/protobuf/unittest_import.proto \
	test/issue12.proto \
	test/test.proto 
	touch $@

.PHONY: plugin all clean test
