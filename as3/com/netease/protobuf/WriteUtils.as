// vim: tabstop=4 shiftwidth=4

// Copyright (c) 2010 , 杨博 (Yang Bo) All rights reserved.
//
//         pop.atry@gmail.com
//
// Use, modification and distribution are subject to the "New BSD License"
// as listed at <url: http://www.opensource.org/licenses/bsd-license.php >.

package com.netease.protobuf {
	import flash.utils.*;
	public final class WriteUtils {
		private static function writeVarint64(output:WritingBuffer, low:uint, high:uint):void {
			if (high == 0) {
				write_TYPE_UINT32(output, low)
			} else {
				for (var i:uint = 0; i < 4; ++i) {
					output.writeByte((low & 0x7F) | 0x80)
					low >>>= 7
				}
				if ((high & (0xFFFFFFF << 3)) == 0) {
					output.writeByte((high << 4) | low)
				} else {
					output.writeByte((((high << 4) | low) & 0x7F) | 0x80)
					write_TYPE_UINT32(output, high >>> 3)
				}
			}
		}
		public static function writeTag(output:WritingBuffer,
				wireType:uint, number:uint):void {
			write_TYPE_UINT32(output, (number << 3) | wireType)
		}
		public static function write_TYPE_DOUBLE(output:WritingBuffer, value:Number):void {
			output.endian = Endian.LITTLE_ENDIAN
			output.writeDouble(value)
		}
		public static function write_TYPE_FLOAT(output:WritingBuffer, value:Number):void {
			output.endian = Endian.LITTLE_ENDIAN
			output.writeFloat(value)
		}
		public static function write_TYPE_INT64(output:WritingBuffer, value:Int64):void {
			writeVarint64(output, value.low, value.high)
		}
		public static function write_TYPE_UINT64(output:WritingBuffer, value:UInt64):void {
			writeVarint64(output, value.low, value.high)
		}
		public static function write_TYPE_INT32(output:WritingBuffer, value:int):void {
			if (value < 0) {
				writeVarint64(output, value, 0xFFFFFFFF)
			} else {
				write_TYPE_UINT32(output, value)
			}
		}
		public static function write_TYPE_FIXED64(output:WritingBuffer, value:Int64):void {
			output.endian = Endian.LITTLE_ENDIAN
			output.writeUnsignedInt(value.low)
			output.writeInt(value.high)
		}
		public static function write_TYPE_FIXED32(output:WritingBuffer, value:int):void {
			output.endian = Endian.LITTLE_ENDIAN
			output.writeInt(value)
		}
		public static function write_TYPE_BOOL(output:WritingBuffer, value:Boolean):void {
			output.writeByte(value ? 1 : 0)
		}
		public static function write_TYPE_STRING(output:WritingBuffer, value:String):void {
			const i:uint = output.beginBlock()
			output.writeUTFBytes(value)
			output.endBlock(i)
		}
		public static function write_TYPE_BYTES(output:WritingBuffer, value:ByteArray):void {
			write_TYPE_UINT32(output, value.length)
			output.writeBytes(value)
		}
		public static function write_TYPE_UINT32(output:WritingBuffer, value:uint):void {
			for (;;) {
				if ((value & ~0x7F) == 0) {
					output.writeByte(value)
					return;
				} else {
					output.writeByte((value & 0x7F) | 0x80)
					value >>>= 7
				}
			}
		}
		public static function write_TYPE_ENUM(output:WritingBuffer, value:int):void {
			write_TYPE_INT32(output, value)
		}
		public static function write_TYPE_SFIXED32(output:WritingBuffer, value:int):void {
			write_TYPE_FIXED32(output, ZigZag.encode32(value))
		}
		public static function write_TYPE_SFIXED64(output:WritingBuffer, value:Int64):void {
			output.endian = Endian.LITTLE_ENDIAN
			output.writeUnsignedInt(ZigZag.encode64low(value.low, value.high))
			output.writeUnsignedInt(ZigZag.encode64high(value.low, value.high))
		}
		public static function write_TYPE_SINT32(output:WritingBuffer, value:int):void {
			write_TYPE_UINT32(output, ZigZag.encode32(value))
		}
		public static function write_TYPE_SINT64(output:WritingBuffer, value:Int64):void {
			writeVarint64(output,
					ZigZag.encode64low(value.low, value.high),
					ZigZag.encode64high(value.low, value.high))
		}
		public static function write_TYPE_MESSAGE(output:WritingBuffer, value:IMessage):void {
			const i:uint = output.beginBlock()
			value.writeToBuffer(output)
			output.endBlock(i)
		}
		public static function writePackedRepeated(output:WritingBuffer,
				writeFunction:Function, value:Array):void {
			const i:uint = output.beginBlock()
			for (var j:uint = 0; j < value.length; j++) {
				writeFunction(output, value[j])
			}
			output.endBlock(i)
		}
	}
}
