# Copyright (c) 2010 , 杨博 (Yang Bo) All rights reserved.
#
#         pop.atry@gmail.com
#
# Use, modification and distribution are subject to the "New BSD License"
# as listed at <url: http://www.opensource.org/licenses/bsd-license.php >.
include config.mk

PROTOC_GEN_AS3=dist/protoc-gen-as3$(BAT)

all: dist/protoc-gen-as3 dist/protoc-gen-as3.bat \
	dist/protobuf.swc dist/README dist/options.proto\
	dist/protoc-gen-as3.jar dist/protobuf-java-2.3.0.jar

classes/com/netease/protocGenAs3/Main.class: \
	plugin.proto.java/google/protobuf/compiler/Plugin.java \
	options.proto.java/com \
	compiler/com/netease/protocGenAs3/Main.java \
	$(PROTOBUF_DIR)/java/target/protobuf-java-2.3.0.jar \
	| classes
	javac -encoding UTF-8 -Xlint:all -d classes \
	-classpath "$(PROTOBUF_DIR)/java/target/protobuf-java-2.3.0.jar" \
	-sourcepath "plugin.proto.java$(PATH_SEPARATOR)compiler$(PATH_SEPARATOR)options.proto.java" \
	compiler/com/netease/protocGenAs3/Main.java

plugin.proto.java/google/protobuf/compiler/Plugin.java: \
	$(PROTOBUF_DIR)/src/$(PROTOC) | plugin.proto.java
	"$(PROTOBUF_DIR)/src/$(PROTOC)" \
	"--proto_path=$(PROTOBUF_DIR)/src" --java_out=plugin.proto.java \
	"$(PROTOBUF_DIR)/src/google/protobuf/compiler/plugin.proto"

dist.tar.gz: all
	tar -acf dist.tar.gz -C dist .

dist/README: README | dist
	cp $< $@

dist/options.proto: options.proto | dist
	cp $< $@

dist/protoc-gen-as3: dist/protoc-gen-as3.jar dist/protobuf-java-2.3.0.jar \
	| dist
	(echo '#!/bin/sh';\
	echo 'cd `dirname "$$0"` && java -jar protoc-gen-as3.jar') > $@
	chmod +x $@

dist/protoc-gen-as3.bat: dist/protoc-gen-as3.jar dist/protobuf-java-2.3.0.jar \
	| dist
	(echo '@cd %~dp0';\
	echo '@java -jar protoc-gen-as3.jar') > $@
	chmod +x $@

dist/protobuf.swc: descriptor.proto.as3/google \
	$(wildcard as3/com/netease/protobuf/*.as) | dist
	$(COMPC) -include-sources+=as3,descriptor.proto.as3 -output=$@

dist/protoc-gen-as3.jar: classes/com/netease/protocGenAs3/Main.class \
	MANIFEST.MF | dist
	jar mcf MANIFEST.MF $@ -C classes .

dist/protobuf-java-2.3.0.jar: \
	$(PROTOBUF_DIR)/java/target/protobuf-java-2.3.0.jar \
	| dist
	cp $< $@

options.proto.java descriptor.proto.as3 classes plugin.proto.java unittest.proto.as3 dist:
	mkdir $@

$(PROTOBUF_DIR)/src/$(PROTOC): $(PROTOBUF_DIR)/Makefile
	cd $(PROTOBUF_DIR) && make

$(PROTOBUF_DIR)/Makefile: $(PROTOBUF_DIR)/configure
	cd $(PROTOBUF_DIR) && ./configure

$(PROTOBUF_DIR)/configure:
	cd $(PROTOBUF_DIR) && ./autogen.sh

$(PROTOBUF_DIR)/java/target/protobuf-java-2.3.0.jar: $(PROTOBUF_DIR)/src
	cd $(PROTOBUF_DIR)/java && mvn package

clean:
	rm -fr dist
	rm -fr dist.tar.gz
	rm -fr classes
	rm -fr unittest.proto.as3
	rm -fr descriptor.proto.as3
	rm -fr plugin.proto.java
	rm -fr test.swc
	rm -fr test.swf
	rm -rf options.proto.java

test: test.swf
	echo c | $(FDB) $<

test.swf: test.swc test/Test.as dist/protobuf.swc
	$(MXMLC) -library-path+=test.swc,dist/protobuf.swc -output=$@ \
	-source-path+=test test/Test.as -debug

test.swc: unittest.proto.as3/protobuf_unittest dist/protobuf.swc
	$(COMPC) -include-sources+=unittest.proto.as3 \
	-external-library-path+=dist/protobuf.swc -output=$@

options.proto.java/com: \
	options.proto \
	$(PROTOBUF_DIR)/src/$(PROTOC) \
	| options.proto.java 
	"$(PROTOBUF_DIR)/src/$(PROTOC)" \
	--proto_path=. \
	"--proto_path=$(PROTOBUF_DIR)/src" \
	--java_out=options.proto.java $<
	touch $@

descriptor.proto.as3/google: \
	$(PROTOBUF_DIR)/src/$(PROTOC) \
	$(PROTOC_GEN_AS3) \
	| descriptor.proto.as3
	"$(PROTOBUF_DIR)/src/$(PROTOC)" \
	--plugin=protoc-gen-as3=$(PROTOC_GEN_AS3) \
	"--proto_path=$(PROTOBUF_DIR)/src" \
	--as3_out=descriptor.proto.as3 \
	"$(PROTOBUF_DIR)/src/google/protobuf/descriptor.proto"
	touch $@

unittest.proto.as3/protobuf_unittest: \
	$(PROTOBUF_DIR)/src/$(PROTOC) \
	$(PROTOC_GEN_AS3) \
	| unittest.proto.as3
	"$(PROTOBUF_DIR)/src/$(PROTOC)" \
	--plugin=protoc-gen-as3=$(PROTOC_GEN_AS3) \
	--proto_path=. --proto_path=test \
	"--proto_path=$(PROTOBUF_DIR)/src" \
	--as3_out=unittest.proto.as3 \
	test/test.proto \
	$(PROTOBUF_DIR)/src/google/protobuf/unittest.proto \
	$(PROTOBUF_DIR)/src/google/protobuf/unittest_import.proto
	touch $@

.PHONY: plugin all clean test
