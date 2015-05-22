# Extension #

Assume you have a proto file:
```
message Test1 {
  extensions 10 to max;
}
message Test2 {
  extend Test1 {
    optional Test2 foo_bar = 10;
  }
}
```

Then you can use `[]` operator to set extension fields:
```
var test1:Test1 = new Test1();
test1[Test2.FOO_BAR] = new Test2();
test1.writeTo(output);
```

Note: Before you deserialize a message which has extension data, you must touch the class which defines the extension field, or the extension data will be treated as unknown fields. For example:
```
void(Test2.FOO_BAR); // Touch Test2 to static initialize it.
var test1:Test1 = new Test1();
test1.mergeFrom(input);
var test2:Test2 = test1[Test2.FOO_BAR];
```

Another way to initialize extension fields is including `initializer.as.inc`, which is in the output directory specified by `as3_out` of your `protoc` command.
```
include "initializer.as.inc"
```


# Bindable #
You can use `(as3_bindable)` option to generate classes with `[Bindable]` metadata tag. Just like this:
```
import "options.proto"
message MyBindable {
  option (as3_bindable) = true;
}
```

# AMF Wrapper #
The Protocol Buffer wire format is not self-delimiting, so you may use another wrapper packet format to store the message type and length. A simple way is wrapping Protocol Buffer messages into AMF stream. There are two options designed for wrapping Protobuf messages in AMF stream: `(as3_amf_auto_alias)` and `(as3_amf_alias)`.

```
import "options.proto"
message MyAMF {
  option (as3_amf_auto_alias) = true;
}
message MyAMF2 {
  option (as3_amf_alias) = "com.sample.MyAMFWithCustomAliasName";
}
```

Both the two options add `[RemoteClass]` metadata tag to the messages. Now you can invoke `IDataInput.readObject()` and `IDataOutput.writeObject()` with these messages in Flex applications.