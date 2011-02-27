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
	public final class WritingBuffer extends ByteArray {
		public function WritingBuffer() {
			endian = Endian.LITTLE_ENDIAN
		}
		private const slices:ByteArray = new ByteArray
		public function beginBlock():uint {
			slices.writeUnsignedInt(position)
			const beginSliceIndex:uint = slices.length
			if (beginSliceIndex % 8 != 4) {
				throw new IllegalOperationError
			}
			slices.writeDouble(0)
			slices.writeUnsignedInt(position)
			return beginSliceIndex
		}
		public function endBlock(beginSliceIndex:uint):void {
			if (slices.length % 8 != 0) {
				throw new IllegalOperationError
			}
			slices.writeUnsignedInt(position)
			slices.position = beginSliceIndex + 8
			const beginPosition:uint = slices.readUnsignedInt()
			slices.position = beginSliceIndex
			slices.writeUnsignedInt(position)
			WriteUtils.write$TYPE_UINT32(this, position - beginPosition)
			slices.writeUnsignedInt(position)
			slices.position = slices.length
			slices.writeUnsignedInt(position)
		}
		public function toNormal(output:IDataOutput):void {
			if (slices.length % 8 != 0) {
				throw new IllegalOperationError
			}
			slices.position = 0
			var begin:uint = 0
			while (slices.bytesAvailable > 0) {
				var end:uint = slices.readUnsignedInt()
				if (end > begin) {
					output.writeBytes(this, begin, end - begin)
				} else if (end < begin) {
					throw new IllegalOperationError
				}
				begin = slices.readUnsignedInt()
			}
			if (begin < length) {
				output.writeBytes(this, begin)
			}
		}
	}
}
