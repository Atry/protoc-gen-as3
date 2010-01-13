// vim: tabstop=4 shiftwidth=4

// Copyright (c) 2010 , 杨博 (Yang Bo) All rights reserved.
//
//         pop.atry@gmail.com
//
// Use, modification and distribution are subject to the "New BSD License"
// as listed at <url: http://www.opensource.org/licenses/bsd-license.php >.

package com.netease.protobuf {
	import flash.utils.*;
	public final class VarintWriter extends ByteArray {
		private var bitsLeft:uint = 0
		public function end():void {
			if (length == 1) {
				this[0] &= 0x7F
			} else {
				for (var i:uint = length; i > 0;) {
					const l:uint = i - 1
					const byte:uint = this[l]
					if (byte != 0x80) {
						this[l] = byte & 0x7F
						length = i
						return
					}
					i = l
				}
				this[0] = 0
				length = 1
			}
		}
		public function write(number:uint, bits:uint):void {
			if (bits <= bitsLeft) {
				this[length - 1] |= number << (7 - bitsLeft)
				bitsLeft -= bits
			} else {
				if (bitsLeft != 0) {
					this[length - 1] |= number << (7 - bitsLeft)
					bits -= bitsLeft
					number >>>= bitsLeft
				}
				while (bits >= 7) {
					writeByte(0x80 | number)
					number >>>= 7
					bits -= 7
				}
				writeByte(0x80 | number)
				bitsLeft = 7 - bits
			}
		}
	}
}
