// vim: tabstop=4 shiftwidth=4

// Copyright (c) 2010 , 杨博 (Yang Bo) All rights reserved.
//
//         pop.atry@gmail.com
//
// Use, modification and distribution are subject to the "New BSD License"
// as listed at <url: http://www.opensource.org/licenses/bsd-license.php >.

package com.netease.protobuf {
	import flash.utils.*;
	public function messageToString(message:Object):String {
		var s:String = getQualifiedClassName(message) + "(\n"
		const descriptor:XML = describeType(message)
		for each (var getter:String in descriptor.accessor.(@access != "writeonly").@name) {
			if (getter.search(/^has(.)(.*)$/) != -1) {
				continue
			}
			s += fieldToString(message, descriptor, getter)
		}
		for each (var field:String in descriptor.variable.@name) {
			s += fieldToString(message, descriptor, field)
		}
		for (var k:* in message) {
			s += k + "=" + message[k] + ";\n"
		}
		s += ")"
		return s
	}
}

function fieldToString(message:Object, descriptor:XML, name:String):String {
	var hasField:String = "has" + name.charAt(0).toUpperCase() + name.substr(1)
	if (descriptor.accessor.(@name == hasField).length() != 0 && !message[hasField]) {
		return ""
	}
	var field:* = message[name]
	if (field != null && field.constructor == Array && field.length == 0) {
		return ""
	}
	return name + "=" + field + ";\n"
}
