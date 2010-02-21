// vim: tabstop=4 shiftwidth=4

// Copyright (c) 2010 , 杨博 (Yang Bo) All rights reserved.
//
//         pop.atry@gmail.com
//
// Use, modification and distribution are subject to the "New BSD License"
// as listed at <url: http://www.opensource.org/licenses/bsd-license.php >.

package com.netease.protobuf {
	import flash.utils.*
	public final class WriteUtils {
		public static function writeTag(output:IDataOutput,
				wireType:uint, number:uint):void {
			const varint:VarintWriter = new VarintWriter;
			varint.write(wireType, 3);
			varint.write(number, 32);
			varint.end()
			output.writeBytes(varint)
		}
		public static function write_TYPE_DOUBLE(output:IDataOutput, value:Number):void {
			output.endian = Endian.LITTLE_ENDIAN
			output.writeDouble(value)
		}
		public static function write_TYPE_FLOAT(output:IDataOutput, value:Number):void {
			output.endian = Endian.LITTLE_ENDIAN
			output.writeFloat(value)
		}
		public static function write_TYPE_INT64(output:IDataOutput, value:Int64):void {
			const varint:VarintWriter = new VarintWriter;
			varint.write(value.low, 32);
			varint.write(value.high, 32);
			varint.end()
			output.writeBytes(varint)
		}
		public static function write_TYPE_UINT64(output:IDataOutput, value:UInt64):void {
			const varint:VarintWriter = new VarintWriter;
			varint.write(value.low, 32);
			varint.write(value.high, 32);
			varint.end()
			output.writeBytes(varint)
		}
		public static function write_TYPE_INT32(output:IDataOutput, value:int):void {
			const varint:VarintWriter = new VarintWriter;
			varint.write(value, 32);
			if (value < 0) {
				varint.write(0xffffffff, 32);
			}
			varint.end()
			output.writeBytes(varint)
		}
		public static function write_TYPE_FIXED64(output:IDataOutput, value:Int64):void {
			output.endian = Endian.LITTLE_ENDIAN
			output.writeUnsignedInt(value.low)
			output.writeInt(value.high)
		}
		public static function write_TYPE_FIXED32(output:IDataOutput, value:int):void {
			output.endian = Endian.LITTLE_ENDIAN
			output.writeInt(value)
		}
		public static function write_TYPE_BOOL(output:IDataOutput, value:Boolean):void {
			output.writeByte(value ? 1 : 0)
		}
		public static function write_TYPE_STRING(output:IDataOutput, value:String):void {
			var plb:PostposeLengthBuffer = output as PostposeLengthBuffer
			if (plb == null) {
				plb = new PostposeLengthBuffer
			}
			const i:uint = plb.beginBlock()
			plb.writeUTFBytes(value)
			plb.endBlock(i)
			if (plb != output) {
				plb.toNormal(output)
			}
		}
		public static function write_TYPE_BYTES(output:IDataOutput, value:ByteArray):void {
			write_TYPE_UINT32(output, value.length)
			output.writeBytes(value)
		}
		public static function write_TYPE_UINT32(output:IDataOutput, value:uint):void {
			const varint:VarintWriter = new VarintWriter;
			varint.write(value, 32);
			varint.end()
			output.writeBytes(varint)
		}
		public static function write_TYPE_ENUM(output:IDataOutput, value:int):void {
			write_TYPE_INT32(output, value)
		}
		public static function write_TYPE_SFIXED32(output:IDataOutput, value:int):void {
			write_TYPE_FIXED32(output, ZigZag.encode32(value))
		}
		public static function write_TYPE_SFIXED64(output:IDataOutput, value:Int64):void {
			output.endian = Endian.LITTLE_ENDIAN
			output.writeUnsignedInt(ZigZag.encode64low(value.low, value.high))
			output.writeUnsignedInt(ZigZag.encode64high(value.low, value.high))
		}
		public static function write_TYPE_SINT32(output:IDataOutput, value:int):void {
			write_TYPE_UINT32(output, ZigZag.encode32(value))
		}
		public static function write_TYPE_SINT64(output:IDataOutput, value:Int64):void {
			const varint:VarintWriter = new VarintWriter;
			varint.write(ZigZag.encode64low(value.low, value.high), 32)
			varint.write(ZigZag.encode64high(value.low, value.high), 32)
			varint.end()
			output.writeBytes(varint)
		}
		public static function write_TYPE_MESSAGE(output:IDataOutput, value:IExternalizable):void {
			var plb:PostposeLengthBuffer = output as PostposeLengthBuffer
			if (plb == null) {
				plb = new PostposeLengthBuffer
			}
			const i:uint = plb.beginBlock()
			value.writeExternal(plb)
			plb.endBlock(i)
			if (plb != output) {
				plb.toNormal(output)
			}
		}
		public static function writePackedRepeated(output:IDataOutput,
				writeFunction:Function, value:Array):void {
			var plb:PostposeLengthBuffer = output as PostposeLengthBuffer
			if (plb == null) {
				plb = new PostposeLengthBuffer
			}
			const i:uint = plb.beginBlock()
			for (var j:uint = 0; j < value.length; j++) {
				writeFunction(plb, value[j])
			}
			plb.endBlock(i)
			if (plb != output) {
				plb.toNormal(output)
			}
		}
	}
}
