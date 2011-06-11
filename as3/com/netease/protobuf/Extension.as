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
		public static function repeatedReadFunction(f:Function):Function {
			return function (input:IDataInput,
					object:Array, tag:uint):void {
				var a:Array = object[tag >>> 3]
				if (a == null) {
					a = []
					object[tag >>> 3] = a
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
		public static function readFunction(f:Function):Function {
			return function (input:IDataInput,
					object:Array, tag:uint):void {
				object[tag >>> 3] = f(input)
			}
		}
		public static function repeatedMessageReadFunction(c:Class):Function {
			return function (input:IDataInput,
					object:Array, tag:uint):void {
				var a:Array = object[tag >>> 3]
				if (a == null) {
					a = []
					object[tag >>> 3] = a
				}
				const m:IMessage = new c
				ReadUtils.read$TYPE_MESSAGE(input, m)
				a.push(m)
			}
		}
		public static function messageReadFunction(c:Class):Function {
			return function (input:IDataInput,
					object:Array, tag:uint):void {
				const m:IMessage = new c
				ReadUtils.read$TYPE_MESSAGE(input, m)
				object[tag >>> 3] = m
			}
		}
		public static function writeFunction(wireType:uint, f:Function):Function {
			return function (output:WritingBuffer,
					object:Array, fieldNumber:uint):void {
				WriteUtils.writeTag(output, wireType, fieldNumber)
				f(output, object[fieldNumber])
			}
		}
		public static function packedRepeatedWriteFunction(f:Function):Function {
			return function (output:WritingBuffer,
					object:Array, fieldNumber:uint):void {
				WriteUtils.writeTag(output, WireType.LENGTH_DELIMITED, fieldNumber)
				WriteUtils.writePackedRepeated(output, f, object[fieldNumber])
			}
		}
		public static function repeatedWriteFunction(wireType:uint, f:Function):Function {
			return function (output:WritingBuffer,
					object:Array, fieldNumber:uint):void {
				const field:Array = object[fieldNumber]
				for (var i:uint = 0; i < field.length; i++) {
					WriteUtils.writeTag(output, wireType, fieldNumber)
					f(output, field[i])
				}
			}
		}
	}
}
