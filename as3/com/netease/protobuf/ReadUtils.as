// vim: tabstop=4 shiftwidth=4

// Copyright (c) 2010 , 杨博 (Yang Bo) All rights reserved.
//
//         pop.atry@gmail.com
//
// Use, modification and distribution are subject to the "New BSD License"
// as listed at <url: http://www.opensource.org/licenses/bsd-license.php >.

package com.netease.protobuf {
	import flash.utils.*
	public final class ReadUtils {
		public static function skip(input:IDataInput, wireType:uint):void {
			switch (wireType) {
			case WireType.VARINT:
				while (input.readUnsignedByte() > 0x80) {}
				break
			case WireType.FIXED_64_BIT:
				input.readInt()
				input.readInt()
				break
			case WireType.LENGTH_DELIMITED:
				for (var i:uint = read_TYPE_UINT32(input); i != 0; i--) {
					input.readByte()
				}
				break
			case WireType.FIXED_32_BIT:
				input.readInt()
				break
			default:
				throw new ArgumentError
			}
		}
		public static function readTag(input:IDataInput):Tag {
			const tag:Tag = new Tag
			const reader:VarintReader = new VarintReader(input)
			tag.wireType = reader.read(3)
			tag.number = reader.read(32)
			return tag
		}
		public static function read_TYPE_DOUBLE(input:IDataInput):Number {
			return input.readDouble()
		}
		public static function read_TYPE_FLOAT(input:IDataInput):Number {
			return input.readFloat()
		}
		public static function read_TYPE_INT64(input:IDataInput):Int64 {
			const result:Int64 = new Int64
			const reader:VarintReader = new VarintReader(input)
			result.low = reader.read(32)
			result.high = reader.read(32)
			return result
		}
		public static function read_TYPE_UINT64(input:IDataInput):UInt64 {
			const result:UInt64 = new UInt64
			const reader:VarintReader = new VarintReader(input)
			result.low = reader.read(32)
			result.high = reader.read(32)
			return result
		}
		public static function read_TYPE_INT32(input:IDataInput):int {
			return new VarintReader(input).read(32)
		}
		public static function read_TYPE_FIXED64(input:IDataInput):Int64 {
			const result:Int64 = new Int64
			result.low = input.readUnsignedInt()
			result.high = input.readInt()
			return result
		}
		public static function read_TYPE_FIXED32(input:IDataInput):int {
			return input.readInt()
		}
		public static function read_TYPE_BOOL(input:IDataInput):Boolean {
			return read_TYPE_UINT32(input) != 0
		}
		public static function read_TYPE_STRING(input:IDataInput):String {
			const length:uint = read_TYPE_UINT32(input)
			return input.readUTFBytes(length)
		}
		public static function read_TYPE_BYTES(input:IDataInput):ByteArray {
			const result:ByteArray = new ByteArray
			const length:uint = read_TYPE_UINT32(input)
			if (length > 0) {
				input.readBytes(result, 0, length)
			}
			return result
		}
		public static function read_TYPE_UINT32(input:IDataInput):uint {
			return (new VarintReader(input)).read(32)
		}
		public static function read_TYPE_ENUM(input:IDataInput):int {
			return read_TYPE_INT32(input)
		}
		private static function sintToInt(n:int):int {
			return (n << 1) ^ (n >>> 31)
		}
		public static function read_TYPE_SFIXED32(input:IDataInput):int {
			return sintToInt(input.readInt())
		}
		public static function read_TYPE_SFIXED64(input:IDataInput):Int64 {
			const result:Int64 = read_TYPE_FIXED64(input)
			const low:uint = result.low
			const high:uint = result.high
			result.low = (high >>> 31) ^ (low << 1) 
			result.high = (low >>> 31) ^ (high << 1)
			return result
		}
		public static function read_TYPE_SINT32(input:IDataInput):int {
			return sintToInt(read_TYPE_INT32(input))
		}
		public static function read_TYPE_SINT64(input:IDataInput):Int64 {
			const result:Int64 = read_TYPE_INT64(input)
			const low:uint = result.low
			const high:uint = result.high
			result.low = (high >>> 31) ^ (low << 1) 
			result.high = (low >>> 31) ^ (high << 1)
			return result
		}
		public static function read_TYPE_MESSAGE(input:IDataInput,
				message:IExternalizable):IExternalizable {
			message.readExternal(read_TYPE_BYTES(input))
			return message
		}
		public static function readPackedRepeated(input:IDataInput,
				readFuntion:Function, value:Array):void {
			const ba:ByteArray = read_TYPE_BYTES(input)
			while (ba.bytesAvailable > 0) {
				value.push(readFuntion(ba))
			}
		}
	}
}
