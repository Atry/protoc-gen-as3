// vim: tabstop=4 shiftwidth=4

// Copyright (c) 2010 , NetEase.com,Inc. All rights reserved.
//
// Author: Yang Bo (pop.atry@gmail.com)
//
// Use, modification and distribution are subject to the "New BSD License"
// as listed at <url: http://www.opensource.org/licenses/bsd-license.php >.

package com.netease.protobuf {
	import flash.errors.*;
	import flash.utils.*;
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
				throw new IOError("Invalid wire type: " + wireType)
			}
		}
		public static function readTag(input:IDataInput):Tag {
			const tag:Tag = new Tag
			const tagNumber:uint = read_TYPE_UINT32(input)
			tag.wireType = tagNumber & 7
			tag.number = tagNumber >>> 3
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
			var b:uint
			var i:uint = 0
			for (;; i += 7) {
				b = input.readUnsignedByte()
				if (i == 28) {
					break
				} else {
					if (b >= 0x80) {
						result.low |= ((b & 0x7f) << i)
					} else {
						result.low |= (b << i)
						return result
					}
				}
			}
			if (b >= 0x80) {
				b &= 0x7f
				result.low |= (b << i)
				result.high = b >>> 4
			} else {
				result.low |= (b << i)
				result.high = b >>> 4
				return result
			}
			for (i = 3;; i += 7) {
				b = input.readUnsignedByte()
				if (i < 32) {
					if (b >= 0x80) {
						result.high |= ((b & 0x7f) << i)
					} else {
						result.high |= (b << i)
						break
					}
				}
			}
			return result
		}
		public static function read_TYPE_UINT64(input:IDataInput):UInt64 {
			const result:UInt64 = new UInt64
			var b:uint
			var i:uint = 0
			for (;; i += 7) {
				b = input.readUnsignedByte()
				if (i == 28) {
					break
				} else {
					if (b >= 0x80) {
						result.low |= ((b & 0x7f) << i)
					} else {
						result.low |= (b << i)
						return result
					}
				}
			}
			if (b >= 0x80) {
				b &= 0x7f
				result.low |= (b << i)
				result.high = b >>> 4
			} else {
				result.low |= (b << i)
				result.high = b >>> 4
				return result
			}
			for (i = 3;; i += 7) {
				b = input.readUnsignedByte()
				if (i < 32) {
					if (b >= 0x80) {
						result.high |= ((b & 0x7f) << i)
					} else {
						result.high |= (b << i)
						break
					}
				}
			}
			return result
		}
		public static function read_TYPE_INT32(input:IDataInput):int {
			return int(read_TYPE_UINT32(input))
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
		private static function readSlice(input:IDataInput):Slice {
			const length:uint = read_TYPE_UINT32(input)
			return new Slice(input, input.bytesAvailable - length)
		}
		public static function read_TYPE_BYTES(input:IDataInput):ByteArray {
			const result:ByteArray = new ByteArray
			result.endian = Endian.LITTLE_ENDIAN
			const length:uint = read_TYPE_UINT32(input)
			if (length > 0) {
				input.readBytes(result, 0, length)
			}
			return result
		}
		public static function read_TYPE_UINT32(input:IDataInput):uint {
			var result:uint = 0
			for (var i:uint = 0;; i += 7) {
				const b:uint = input.readUnsignedByte()
				if (i < 32) {
					if (b >= 0x80) {
						result |= ((b & 0x7f) << i)
					} else {
						result |= (b << i)
						break
					}
				} else {
					while (input.readUnsignedByte() >= 0x80) {}
					break
				}
			}
			return result
		}
		public static function read_TYPE_ENUM(input:IDataInput):int {
			return read_TYPE_INT32(input)
		}
		public static function read_TYPE_SFIXED32(input:IDataInput):int {
			return ZigZag.decode32(input.readInt())
		}
		public static function read_TYPE_SFIXED64(input:IDataInput):Int64 {
			const result:Int64 = read_TYPE_FIXED64(input)
			const low:uint = result.low
			const high:uint = result.high
			result.low = ZigZag.decode64low(low, high)
			result.high = ZigZag.decode64high(low, high)
			return result
		}
		public static function read_TYPE_SINT32(input:IDataInput):int {
			return ZigZag.decode32(read_TYPE_UINT32(input))
		}
		public static function read_TYPE_SINT64(input:IDataInput):Int64 {
			const result:Int64 = read_TYPE_INT64(input)
			const low:uint = result.low
			const high:uint = result.high
			result.low = ZigZag.decode64low(low, high)
			result.high = ZigZag.decode64high(low, high)
			return result
		}
		public static function read_TYPE_MESSAGE(input:IDataInput,
				message:IExternalizable):IExternalizable {
			message.readExternal(readSlice(input))
			return message
		}
		public static function readPackedRepeated(input:IDataInput,
				readFuntion:Function, value:Array):void {
			const ba:Slice = readSlice(input)
			while (ba.bytesAvailable > 0) {
				value.push(readFuntion(ba))
			}
		}
	}
}
import flash.utils.IDataInput
import flash.utils.ByteArray
final class Slice implements IDataInput {
	private var originInput:IDataInput

	private var bytesAfterSlice:uint

	public function Slice(originInput:IDataInput, bytesAfterSlice:uint) {
		const originSlice:Slice = originInput as Slice
		if (originSlice) {
			this.originInput = originSlice.originInput
			this.bytesAfterSlice = originSlice.bytesAfterSlice + bytesAfterSlice
		} else {
			this.originInput = originInput
			this.bytesAfterSlice = bytesAfterSlice
		}
	}

	public function get bytesAvailable():uint {
		return originInput.bytesAvailable - bytesAfterSlice
	}

	public function set endian(value:String):void {
		originInput.endian = value
	}

	public function get endian():String {
		return originInput.endian
	}

	public function set objectEncoding(value:uint):void {
		originInput.objectEncoding = value
	}

	public function get objectEncoding():uint {
		return originInput.objectEncoding
	}

	public function readBoolean():Boolean {
		return originInput.readBoolean()
	}

	public function readByte():int {
		return originInput.readByte()
	}

	public function readBytes(bytes:ByteArray, offset:uint= 0, length:uint= 0):void {
		return originInput.readBytes(bytes, offset, length)
	}

	public function readDouble():Number {
		return originInput.readDouble()
	}

	public function readFloat():Number {
		return originInput.readFloat()
	}

	public function readInt():int {
		return originInput.readInt()
	}

	public function readMultiByte(length:uint, charSet:String):String {
		return originInput.readMultiByte(length, charSet)
	}

	public function readObject():* {
		return originInput.readObject()
	}

	public function readShort():int {
		return originInput.readShort()
	}

	public function readUnsignedByte():uint {
		return originInput.readUnsignedByte()
	}

	public function readUnsignedInt():uint {
		return originInput.readUnsignedInt()
	}

	public function readUnsignedShort():uint {
		return originInput.readUnsignedShort()
	}

	public function readUTF():String {
		return originInput.readUTF()
	}

	public function readUTFBytes(length:uint):String {
		return originInput.readUTFBytes(length)
	}

}
