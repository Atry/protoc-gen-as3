// vim: tabstop=4 shiftwidth=4

// Copyright (c) 2010 , NetEase.com,Inc. All rights reserved.
//
// Author: Yang Bo (pop.atry@gmail.com)
//
// Use, modification and distribution are subject to the "New BSD License"
// as listed at <url: http://www.opensource.org/licenses/bsd-license.php >.

package com.netease.protobuf {
	import flash.errors.IllegalOperationError
	import flash.errors.IOError
	import flash.utils.IDataInput
	import flash.utils.ByteArray
	public class RepeatedFieldDescriptor extends BaseFieldDescriptor {
		public function get elementType():Class {
			throw new IllegalOperationError("Not Implemented!")
		}
		override public final function read(input:IDataInput,
				message:Message, tag:uint):void {
			const destination:Array = message[this] || (message[this] = [])
			if ((tag & 7) == WireType.LENGTH_DELIMITED) {
				switch (elementType) {
				case String:
				case ByteArray:
					destination.push(readSingleField(input))
					break;
				default:
					const length:uint = ReadUtils.read$TYPE_UINT32(input)
					if (input.bytesAvailable < length) {
						throw new IOError("Invalid message length: " + length)
					}
					const bytesAfterSlice:uint = input.bytesAvailable - length
					while (input.bytesAvailable > bytesAfterSlice) {
						destination.push(readSingleField(input))
					}
					if (input.bytesAvailable != bytesAfterSlice) {
						throw new IOError("Invalid packed destination data")
					}
					break;
				}
			} else {
				destination.push(readSingleField(input))
			}
		}
		override public final function write(output:WritingBuffer,
				message:Message):void {
			const source:Array = message[this]
			if ((tag & 7) == WireType.LENGTH_DELIMITED) {
				WriteUtils.write$TYPE_UINT32(output, tag)
				const i:uint = output.beginBlock()
				for (var j:uint = 0; j < source.length; j++) {
					writeSingleField(output, source[j])
				}
				output.endBlock(i)
			} else {
				for (var k:uint = 0; k < source.length; k++) {
					WriteUtils.write$TYPE_UINT32(output, tag)
					writeSingleField(output, source[k])
				}
			}
		}

	}
}
