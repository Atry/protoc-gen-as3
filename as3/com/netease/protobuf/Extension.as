// vim: tabstop=4 shiftwidth=4

// Copyright (c) 2010 , NetEase.com,Inc. All rights reserved.
//
// Author: Yang Bo (pop.atry@gmail.com)
//
// Use, modification and distribution are subject to the "New BSD License"
// as listed at <url: http://www.opensource.org/licenses/bsd-license.php >.

package com.netease.protobuf {
	import flash.utils.*
	public final class Extension {
		public static function repeatedReadFunction(fieldName:String,
				f:Function):Function {
			return function (input:IDataInput,
					message:IMessage, tag:uint):void {
				var a:Array = message[fieldName]
				if (a == null) {
					a = []
					message[fieldName] = a
				}
				if ((tag & 7) == WireType.LENGTH_DELIMITED) {
					switch (f)
					{
						case ReadUtils.read$TYPE_STRING:
						case ReadUtils.read$TYPE_BYTES:
						{
							break;
						}

						default:
						{
							ReadUtils.readPackedRepeated(input, f, a)
							return;
						}
					}
				}
				a.push(f(input))
			}
		}
		public static function readFunction(fieldName:String,
				f:Function):Function {
			return function (input:IDataInput, message:IMessage, tag:uint):void {
				message[fieldName] = f(input)
			}
		}
		public static function repeatedMessageReadFunction(fieldName:String,
				c:Class):Function {
			return function (input:IDataInput,
					message:IMessage, tag:uint):void {
				var a:Array = message[fieldName]
				if (a == null) {
					a = []
					message[fieldName] = a
				}
				const m:IMessage = new c
				ReadUtils.read$TYPE_MESSAGE(input, m)
				a.push(m)
			}
		}
		public static function messageReadFunction(fieldName:String,
				c:Class):Function {
			return function (input:IDataInput,
					message:IMessage, tag:uint):void {
				const m:IMessage = new c
				ReadUtils.read$TYPE_MESSAGE(input, m)
				message[fieldName] = m
			}
		}
		public static function writeFunction(tag:uint, f:Function):Function {
			return function (output:WritingBuffer,
					message:IMessage, fieldName:String):void {
				WriteUtils.write$TYPE_UINT32(output, tag)
				f(output, message[fieldName])
			}
		}
		public static function packedRepeatedWriteFunction(tag:uint, f:Function):Function {
			return function (output:WritingBuffer,
					message:IMessage, fieldName:String):void {
				WriteUtils.write$TYPE_UINT32(output, tag)
				WriteUtils.writePackedRepeated(output, f, message[fieldName])
			}
		}
		public static function repeatedWriteFunction(tag:uint, f:Function):Function {
			return function (output:WritingBuffer,
					message:IMessage, fieldName:String):void {
				const field:Array = message[fieldName]
				for (var i:uint = 0; i < field.length; i++) {
					WriteUtils.write$TYPE_UINT32(output, tag)
					f(output, field[i])
				}
			}
		}
	}
}
