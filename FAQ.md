# 有辦法自動反序列化成對應類別嗎？像amf的`registerClassAlias`，在讀的時候自動對應物件？ #

官方推荐的做法是使用Union Type： http://code.google.com/apis/protocolbuffers/docs/techniques.html#union

如果类型很多，实现Union Type时可以使用[Extension](AdvancedUsage#Extension.md)功能。

# 如何遍历字段？ #
对于 extension 字段，请使用 `for ... in` 语法，例如：
```
var test1:Test1 = new Test1
for (var number:* in test1) {
  trace("tag number:", number);
  trace("field value:", test1[number]);
  if (number == Test2.t) {
    var test2:Test2 = test1[number];
  }
}
```

`for ... in`只包含 extension 字段而不包含静态字段，这一点和C++的[ListFields](http://code.google.com/apis/protocolbuffers/docs/reference/cpp/google.protobuf.message.html#Reflection.ListFields.details)不同。

如果需要遍历静态字段，请使用 `flash.utils.describeType` 取得字段名，然后再根据字段名遍历对象。
# 如何处理 64 位整数？ #
我定义了 UInt64 和 Int64 两个类来存放 proto 文件中定义的 64 位整数。虽然你可以从中存取这个数的高 32 位和低 32 位，但是这两个类并没有包含算术运算功能。如果你需要进行算术运算，请自己编写算术运算的代码，或使用第三方库 Big Integer 。
# 如何生成 Bindable 的类 #
import "options.proto"，然后指定 (as3\_bindable) 属性即可。
```
import "options.proto"
message MyBindable {
  option (as3_bindable) = true;
}
```
注意，options.proto 中引用了 google/protobuf/descriptor.proto ，这个文件可以在 protobuf 2.3 源代码包中找到。
# 如何使用 RPC？ #
若需使用，首先在 proto 文件中定义 Service：
```
message BarRequest {
}
message BarResponse {
}
service FooService {
	rpc Bar (BarRequest) returns (BarResponse);
}
```

然后这样使用：
```

// 初始化。
var fooService:FooService = new FooService();
var simpleWebRPC:SimpleWebRPC = new SimpleWebRPC("http://mydomain.com/myservice/");
fooService.sendFunction = simpleWebRPC.send;

// 发起一次 RPC 请求。
fooService.bar(new BarRequest(), function(result:*):void
{
	// 收到 RPC 回应。
	var response:BarResponse = result as BarResponse;
	if (response)
	{
		trace("RPC call succeeded:", response);
	}
	else
	{
		trace("RPC call failed:", ErrorEvent(result).text);
	}
});
```

SimpleWebRPC 是内置的 RPC 实现示例，你需要将它换成你自己的 RPC 实现。

# 这个库有没有实现类似c++的反射机制？ 即如果数据包：包长度+消息名称+消息数据，这个库能根据包里的消息名称创建相应的消息类型并反序列化吗？ #

protoc-gen-as3本身不包含反射机制，但你可以用`flash.utils.getDefinitionByName()`来根据名称创建消息。

# How can I deserialize from a text file? #

Use `TextFormat.printToUTFBytes`, `TextFormat.printToUTFBytes`, `TextFormat.mergeFromUTFBytes`, or `TextFormat.mergeFromString`.

See http://code.google.com/p/protoc-gen-as3/source/browse/as3/com/netease/protobuf/TextFormat.as

# Why do I see `dynamic` keyword in generated .as files? #

v0.9.x will use `dynamic class` for the messages which has extensions. v1.0.x will always use `dynamic class` to hold unknown fields and extensions.

# 我把一个消息写入`ByteArray`，然后再读这个`ByteArray`，没有得到任何结果。为什么？ #

读`ByteArray`之前要加一行代码 ` myByteArray.position = 0 `

# 在Flash Builder中编译生成的文件时提示找不到`FieldDescriptor$TYPE_STRING`类型 #

这是Flash Builder的bug。用Flex SDK编译是正常的。

# I see warning "the import fieldDescriptors could not be found" in Flash Builder, why? #

It's a bug of Flash Builder. You will not see the warnings if you compile the generated code by Flex SDK command line tools.