// vim: tabstop=4 shiftwidth=4

// Copyright (c) 2010 , 杨博 (Yang Bo) All rights reserved.
//
//         pop.atry@gmail.com
//
// Use, modification and distribution are subject to the "New BSD License"
// as listed at <url: http://www.opensource.org/licenses/bsd-license.php >.

package com.netease.protobuf {
	import flash.utils.*
	import flash.errors.*
	public final class PostposeLengthBuffer extends ByteArray {
		//*
			// for flash player 9
			[ArrayElementType("uint")]
			private const _slices:Array = []
		/*/
			// for flash player 10
			private const _slices:Vector.<uint> = new Vector.<uint>
		//*/
		public function beginBlock():uint {
			if (beginSliceIndex % 2 != 0) {
				throw new IllegalOperationError
			}
			_slices.push(position)
			const beginSliceIndex:uint = _slices.length
			_slices.length += 2
			_slices.push(position)
			return beginSliceIndex
		}
		public function endBlock(beginSliceIndex:uint):void {
			if (_slices.length % 2 != 0) {
				throw new IllegalOperationError
			}
			_slices.push(position)
			const beginPosition:uint = _slices[beginSliceIndex + 2]
			_slices[beginSliceIndex] = position
			WriteUtils.write_TYPE_UINT32(this, position - beginPosition)
			_slices[beginSliceIndex + 1] = position
			_slices.push(position)
		}
		public function toNormal(output:IDataOutput):void {
			if (_slices.length % 2 != 0) {
				throw new IllegalOperationError
			}
			var i:uint = 0
			var begin:uint = 0
			while (i < _slices.length) {
				var end:uint = _slices[i]
				++i
				if (end > begin) {
					output.writeBytes(this, begin, end - begin)
				} else if (end < begin) {
					throw new IllegalOperationError
				}
				begin = _slices[i]
				++i
			}
			output.writeBytes(this, begin)
		}
	}
}
