// vim: tabstop=4 shiftwidth=4

// Copyright (c) 2010 , NetEase.com,Inc. All rights reserved.
//
// Author: Yang Bo (pop.atry@gmail.com)
//
// Use, modification and distribution are subject to the New BSD License
// as listed at <url: http://www.opensource.org/licenses/bsd-license.php >.

package com.netease.protobuf {
	import flash.errors.IllegalOperationError
	import flash.errors.IOError
	import flash.utils.Dictionary;
	import flash.utils.getDefinitionByName;
	import flash.utils.IDataInput
	import flash.utils.IDataOutput

	public class Message {
		public final function mergeFrom(input:IDataInput):void {
			input.endian = flash.utils.Endian.LITTLE_ENDIAN
			readFromSlice(input, 0)
		}
		public final function mergeDelimitedFrom(input:IDataInput):void {
			input.endian = flash.utils.Endian.LITTLE_ENDIAN
			ReadUtils.read$TYPE_MESSAGE(input, this)
		}
		public final function writeTo(output:IDataOutput):void {
			const buffer:com.netease.protobuf.WritingBuffer = new com.netease.protobuf.WritingBuffer()
			writeToBuffer(buffer)
			buffer.toNormal(output)
		}
		public final function writeDelimitedTo(output:IDataOutput):void {
			const buffer:com.netease.protobuf.WritingBuffer = new com.netease.protobuf.WritingBuffer()
			WriteUtils.write$TYPE_MESSAGE(buffer, this)
			buffer.toNormal(output)
		}
		/**
		 *  @private
		 */
		public function readFromSlice(input:IDataInput, bytesAfterSlice:uint):void {
			throw new IllegalOperationError("Not implemented!")
		}
		/**
		 *  @private
		 */
		public function writeToBuffer(output:WritingBuffer):void {
			throw new IllegalOperationError("Not implemented!")
		}
		private function writeSingleUnknown(output:WritingBuffer, tag:uint,
				value:*):void {
			WriteUtils.write$TYPE_UINT32(output, tag)
			switch (tag & 7) {
			case WireType.VARINT:
				WriteUtils.write$TYPE_UINT64(output, value)
				break
			case WireType.FIXED_64_BIT:
				WriteUtils.write$TYPE_FIXED64(output, value)
				break
			case WireType.LENGTH_DELIMITED:
				WriteUtils.write$TYPE_BYTES(output, value)
				break
			case WireType.FIXED_32_BIT:
				WriteUtils.write$TYPE_FIXED32(output, value)
				break
			default:
				throw new IOError("Invalid wire type: " + (tag & 7))
			}
		}
		protected final function writeUnknown(output:WritingBuffer,
				fieldName:String):void {
			const tag:uint = uint(fieldName)
			if (tag == 0) {
				throw new ArgumentError(
						"Attemp to write an undefined string filed: " +
						fieldName)
			}
			var value:* = this[fieldName]
			const repeated:Array = value as Array
			if (repeated) {
				for each (var element:* in repeated) {
					writeSingleUnknown(output, tag, element)
				}
			} else {
				writeSingleUnknown(output, tag, value)
			}
		}
		protected final function writeExtensionOrUnknown(output:WritingBuffer,
				fieldName:String):void {
			var fieldDescriptor:BaseFieldDescriptor
			try {
				fieldDescriptor = BaseFieldDescriptor.fromString(fieldName)
			} catch (e:ReferenceError) {
				writeUnknown(output, fieldName)
				return
			}
			fieldDescriptor.write(output, this)
		}
		protected final function readUnknown(input:IDataInput, tag:uint):void {
			var value:*
			switch (tag & 7) {
			case WireType.VARINT:
				value = ReadUtils.read$TYPE_UINT64(input)
				break
			case WireType.FIXED_64_BIT:
				value = ReadUtils.read$TYPE_FIXED64(input)
				break
			case WireType.LENGTH_DELIMITED:
				value = ReadUtils.read$TYPE_BYTES(input)
				break
			case WireType.FIXED_32_BIT:
				value = ReadUtils.read$TYPE_FIXED32(input)
				break
			default:
				throw new IOError("Invalid wire type: " + (tag & 7))
			}
			const currentValue:* = this[tag]
			if (!currentValue) {
				this[tag] = value
			} else if (currentValue is Array) {
				currentValue.push(value)
			} else {
				this[tag] = [currentValue, value]
			}
		}
		protected final function readExtensionOrUnknown(extensions:Array,
				input:IDataInput, tag:uint):void {
			var fieldDescriptor:BaseFieldDescriptor = extensions[tag >>> 3];
			if (fieldDescriptor) {
				fieldDescriptor.read(input, this, tag);
			} else {
				readUnknown(input, tag)
			}
		}
	}
}
