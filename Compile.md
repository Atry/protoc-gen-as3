# 从源码编译 #
## Unix ##
  1. 保证已经安装了以下东西，而且其可执行文件都在 PATH 中。
    * Make
    * Maven
    * JDK
  1. 下载 protobuf 2.3 源代码并解压。
  1. 修改 protoc-gen-as3/config.mk ，把 PROTOBUF\_DIR 设为 protobuf 2.3 源代码所在的目录。
  1. 进入下载的 protoc-gen-as3 目录，调用 make .