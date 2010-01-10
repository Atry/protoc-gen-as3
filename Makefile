include config.mk

all: com/netease/protocGenAs3/Main.class

com/netease/protocGenAs3/Main.class: \
	com/netease/protocGenAs3/Main.java \
	google/protobuf/compiler/Plugin.java \
	$(PROTOBUF_DIR)/java/target/protobuf-java-2.3.0.jar
	javac -Xlint:unchecked \
	-classpath $(PROTOBUF_DIR)/java/target/protobuf-java-2.3.0.jar:. \
	com/netease/protocGenAs3/Main.java

google/protobuf/compiler/Plugin.java: $(PROTOBUF_DIR)/src/$(PROTOC_EXE)
	$(PROTOBUF_DIR)/src/protoc --proto_path=$(PROTOBUF_DIR)/src \
	--java_out=. $(PROTOBUF_DIR)/src/google/protobuf/compiler/plugin.proto

$(PROTOBUF_DIR)/src/$(PROTOC_EXE): $(PROTOBUF_DIR)/Makefile
	cd $(PROTOBUF_DIR) && make

$(PROTOBUF_DIR)/Makefile: $(PROTOBUF_DIR)/configure
	cd $(PROTOBUF_DIR) && ./configure

$(PROTOBUF_DIR)/java/target/protobuf-java-2.3.0.jar: $(PROTOBUF_DIR)/src
	cd $(PROTOBUF_DIR)/java && mvn package

plugin: all
	java -classpath $(PROTOBUF_DIR)/java/target/protobuf-java-2.3.0.jar:. \
	com.netease.protocGenAs3.Main

.PHONY: plugin all
