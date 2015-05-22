# What is it? #
**protoc-gen-as3** is a [Protocol Buffers](http://code.google.com/p/protobuf/) plugin for ActionScript 3.

This project aims to support Protocol Buffers in ActionScript 3 with minimum API and best performance. protoc-gen-as3's serialize/deserialize performance is near native AMF's writeObject/readObject, although protoc-gen-as3 runs in AVM2 while writeObject/readObject runs natively.

# How to contribute? #

Your contribution is welcome. I highly recommend that you  [clone](https://code.google.com/p/protoc-gen-as3/source/createClone), hack, and [tell me what you've done](https://code.google.com/p/protoc-gen-as3/issues/entry?template=Review%20request).

# Features #

I have implemented almost all protobuf features (more than any other protobuf's AS3 implementation) in protoc-gen-as3:
  * All basic types
  * Nested messages
  * Enumerations
  * Packed and non-packed repeated fields
  * [Extensions](AdvancedUsage#Extension.md)
  * RPC services
    * Custom options for services and methods
  * ActionScript metadata tag generation:
    * [Bindable](AdvancedUsage#Bindable.md)
    * [RemoteClass](AdvancedUsage#AMF_Wrapper.md)
  * [Text format](http://code.google.com/p/protoc-gen-as3/source/browse/as3/com/netease/protobuf/TextFormat.as)
  * Unknown fields

## Unsupported features ##

These features are supported by Google's C++/Java/Python implementation, but not in protoc-gen-as3:
  * Groups (a deprecated feature)
  * Custom options for messages, enums and files

# How to use it? #
Unlike other protobuf's as3 compilers, protoc-gen-as3 does not require you to modify original protobuf's source code. You can just use protoc binary (version 2.3+) with this plugin to generate ActionScript 3 source code:

`protoc --plugin=protoc-gen-as3=`_`path/to/protoc-gen-as3[.bat]`_` --as3_out=`_`output-path your.proto`_

Then, you can use the generated files to serialize and deserialize in protobuf format by invoking `mergeFrom()` and `writeTo()` method.

Before compiling, don't forget to let your ActionScript 3 compiler include protobuf.swc and the generated source files.

See AdvancedUsage and [ASDoc](https://code.google.com/p/protoc-gen-as3/downloads/detail?name=protoc-gen-as3-1.0.0-rc6-asdoc.tar.gz) for more information.