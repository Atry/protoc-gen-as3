// vim: tabstop=4 shiftwidth=4

// Copyright (c) 2010 , NetEase.com,Inc. All rights reserved.
//
// Author: Yang Bo (pop.atry@gmail.com)
//
// Use, modification and distribution are subject to the New BSD License
// as listed at <url: http://www.opensource.org/licenses/bsd-license.php >.

package com.netease.protobuf {
	import flash.errors.IllegalOperationError
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

	}
}
