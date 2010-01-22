// vim: tabstop=4 shiftwidth=4

// Copyright (c) 2010 , 杨博 (Yang Bo) All rights reserved.
//
//         pop.atry@gmail.com
//
// Use, modification and distribution are subject to the "New BSD License"
// as listed at <url: http://www.opensource.org/licenses/bsd-license.php >.

package com.netease.protobuf {
	import flash.utils.*
	public final class Extension {
		public static function repeatedReadFunction(f:Function):Function {
			return function (input:IDataInput,
					object:Array, fieldNumber:uint):void {
				var a:Array = object[fieldNumber]
				if (a == null) {
					a = []
					object[fieldNumber] = a
				}
				a.push(f(input))
			}
		}
		public static function readFunction(f:Function):Function {
			return function (input:IDataInput,
					object:Array, fieldNumber:uint):void {
				object[fieldNumber] = f(input)
			}
		}
		public static function packedRepeatedReadFunction(f:Function):Function {
			return function (input:IDataInput,
					object:Array, fieldNumber:uint):void {
				var a:Array = object[fieldNumber]
				if (a == null) {
					a = []
					object[fieldNumber] = a
				}
				ReadUtils.readPackedRepeated(input, f, a)
			}
		}
		public static function repeatedMessageReadFunction(c:Class):Function {
			return function (input:IDataInput,
					object:Array, fieldNumber:uint):void {
				var a:Array = object[fieldNumber]
				if (a == null) {
					a = []
					object[fieldNumber] = a
				}
				const m:IExternalizable = new c
				ReadUtils.read_TYPE_MESSAGE(input, m)
				a.push(m)
			}
		}
		public static function messageReadFunction(c:Class):Function {
			return function (input:IDataInput,
					object:Array, fieldNumber:uint):void {
				const m:IExternalizable = new c
				ReadUtils.read_TYPE_MESSAGE(input, m)
				object[fieldNumber] = m
			}
		}
		public static function writeFunction(wireType:uint, f:Function):Function {
			return function (output:IDataOutput,
					object:Array, fieldNumber:uint):void {
				WriteUtils.writeTag(output, wireType, fieldNumber)
				f(output, object[fieldNumber])
			}
		}
		public static function packedRepeatedWriteFunction(f:Function):Function {
			return function (output:IDataOutput,
					object:Array, fieldNumber:uint):void {
				WriteUtils.writeTag(output, WireType.LENGTH_DELIMITED, fieldNumber)
				WriteUtils.writePackedRepeated(output, f, object[fieldNumber])
			}
		}
		public static function repeatedWriteFunction(wireType:uint, f:Function):Function {
			return function (output:IDataOutput,
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
