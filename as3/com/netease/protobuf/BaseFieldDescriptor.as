// vim: tabstop=4 shiftwidth=4

// Copyright (c) 2010 , NetEase.com,Inc. All rights reserved.
//
// Author: Yang Bo (pop.atry@gmail.com)
//
// Use, modification and distribution are subject to the "New BSD License"
// as listed at <url: http://www.opensource.org/licenses/bsd-license.php >.
package com.netease.protobuf {
	import flash.errors.IllegalOperationError
	import flash.utils.getDefinitionByName;
	import flash.utils.IDataInput
	public class BaseFieldDescriptor {
		public var name:String
		public var tag:uint
		public function get type():Class {
			throw new IllegalOperationError("Not Implemented!")
		}
		public function readSingleField(input:IDataInput):* {
			throw new IllegalOperationError("Not Implemented!")
		}
		public function writeSingleField(output:WritingBuffer, value:*):void {
			throw new IllegalOperationError("Not Implemented!")
		}
		public function read(source:IDataInput, destination:Message, tag:uint):void {
			throw new IllegalOperationError("Not Implemented!")
		}
		public function write(destination:WritingBuffer, source:Message):void {
			throw new IllegalOperationError("Not Implemented!")
		}
		private static const ACTIONSCRIPT_KEYWORDS:Object = {
			"as" : true,		"break" : true,		"case" : true,
			"catch" : true,		"class" : true,		"const" : true,
			"continue" : true,	"default" : true,	"delete" : true,
			"do" : true,		"else" : true,		"extends" : true,
			"false" : true,		"finally" : true,	"for" : true,
			"function" : true,	"if" : true,		"implements" : true,
			"import" : true,	"in" : true,		"instanceof" : true,
			"interface" : true,	"internal" : true,	"is" : true,
			"native" : true,	"new" : true,		"null" : true,
			"package" : true,	"private" : true,	"protected" : true,
			"public" : true,	"return" : true,	"super" : true,
			"switch" : true,	"this" : true,		"throw" : true,
			"to" : true,		"true" : true,		"try" : true,
			"typeof" : true,	"use" : true,		"var" : true,
			"void" : true,		"while" : true,		"with" : true
		}

		public static function toLowerCamelCase(origin:String):String {
			if (origin in ACTIONSCRIPT_KEYWORDS) {
				return "__" + origin
			}
			return origin.replace(/_[a-z]/g, regexToUpperCase)
		}
		
		public function get fieldName():String {
			return toLowerCamelCase(name.substr(name.lastIndexOf('.') + 1))
		}
		
		public function get scope():String {
			return name.substr(0, name.lastIndexOf('.'))
		}
		
		public static function fromString(name:String):BaseFieldDescriptor {
			try {
				return BaseFieldDescriptor(getDefinitionByName(name))
			} catch (e:ReferenceError) {
			}
			const lastDotPosition:int = name.lastIndexOf('.')
			return getDefinitionByName(name.substr(0, lastDotPosition))[
					name.substr(lastDotPosition + 1)]
		}
		
		private var toStringCache:String

		public function toString():String {
			return toStringCache || (toStringCache = scope + '.' + fieldName)
		}
		
	}

}
function regexToUpperCase(matched:String, index:int, whole:String):String {
	return matched.charAt(1).toUpperCase()
}