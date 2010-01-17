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
		[ArrayElementType("uint")]
		private const slices:Array = []
		public function beginBlock():uint {
			const beginSliceIndex:uint = slices.length
			if (beginSliceIndex % 2 != 0) {
				throw new IllegalOperationError;
			}
			slices.length += 2
			slices.push(position)
			return beginSliceIndex
		}
		public function endBlock(beginSliceIndex:uint):void {
			if (slices.length % 2 != 1) {
				throw new IllegalOperationError;
			}
			const beginPosition:uint = slices[beginSliceIndex + 2]
			slices.push(position)
			slices[beginSliceIndex] = position
			WriteUtils.write_TYPE_UINT32(this, position - beginPosition)
			slices[beginSliceIndex + 1] = position
		}
		public function toNormal(output:IDataOutput):void {
			if (slices.length % 2 != 0) {
				throw new IllegalOperationError;
			}
			for (var i:uint = 0; i < slices.length; i += 2) {
				var begin:uint = slices[i]
				var length:uint = slices[i + 1] - begin
				if (length > 0) {
					output.writeBytes(this, begin, length)
				}
			}
		}
	}
}
