PROTOBUF_DIR=../protobuf-2.3.0
ifeq ($(OS), Windows_NT)
EXE=.exe
PATH_SEPARATOR=;
else
PATH_SEPARATOR=:
endif
PROTOC=protoc$(EXE)
COMPC=compc$(EXE)
MXMLC=mxmlc$(EXE)
FDB=fdb$(EXE)
