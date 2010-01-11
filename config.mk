PROTOBUF_DIR=../protobuf-2.3.0
ifeq ($(OS), Windows_NT)
PROTOC_EXE=protoc.exe
PATH_SEPARATOR=;
else
PROTOC_EXE=protoc
PATH_SEPARATOR=:
endif
