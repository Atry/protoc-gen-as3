// vim: tabstop=4 shiftwidth=4

// Copyright (c) 2010 , 杨博 (Yang Bo) All rights reserved.
//
//         pop.atry@gmail.com
//
// Use, modification and distribution are subject to the "New BSD License"
// as listed at <url: http://www.opensource.org/licenses/bsd-license.php >.

package com.netease.protobuf {
	import flash.errors.*;
	import flash.utils.*;
	public final class WritingBuffer extends ByteArray {
		private const _slices:ByteArray = new ByteArray
		public function beginBlock():uint {
			_slices.writeUnsignedInt(position)
			const beginSliceIndex:uint = _slices.length
			if (beginSliceIndex % 8 != 4) {
				throw new IllegalOperationError
			}
			_slices.writeDouble(0)
			_slices.writeUnsignedInt(position)
			return beginSliceIndex
		}
		public function endBlock(beginSliceIndex:uint):void {
			if (_slices.length % 8 != 0) {
				throw new IllegalOperationError
			}
			_slices.writeUnsignedInt(position)
			_slices.position = beginSliceIndex + 8
			const beginPosition:uint = _slices.readUnsignedInt()
			_slices.position = beginSliceIndex
			_slices.writeUnsignedInt(position)
			WriteUtils.write_TYPE_UINT32(this, position - beginPosition)
			_slices.writeUnsignedInt(position)
			_slices.position = _slices.length
			_slices.writeUnsignedInt(position)
		}
		public function toNormal(output:IDataOutput):void {
			if (_slices.length % 8 != 0) {
				throw new IllegalOperationError
			}
			_slices.position = 0
			var begin:uint = 0
			while (_slices.bytesAvailable > 0) {
				var end:uint = _slices.readUnsignedInt()
				if (end > begin) {
					output.writeBytes(this, begin, end - begin)
				} else if (end < begin) {
					throw new IllegalOperationError
				}
				begin = _slices.readUnsignedInt()
			}
			if (begin < length) {
				output.writeBytes(this, begin)
			}
		}
	}
}
