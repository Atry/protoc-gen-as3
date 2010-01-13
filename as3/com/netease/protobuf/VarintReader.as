// vim: tabstop=4 shiftwidth=4

// Copyright (c) 2010 , 杨博 (Yang Bo) All rights reserved.
//
//         pop.atry@gmail.com
//
// Use, modification and distribution are subject to the "New BSD License"
// as listed at <url: http://www.opensource.org/licenses/bsd-license.php >.

package com.netease.protobuf {
	import flash.utils.*
	public final class VarintReader {
		private var input:IDataInput
		public function VarintReader(input:IDataInput) {
			this.input = input
		}
		private var bitsLeft:uint = 0
		private var number:uint = 0x80
		public function read(bits:uint):uint {
			if (bits <= bitsLeft) {
				bitsLeft -= bits
				return (number >>> (7 - bits)) & (uint(1 << bits) - 1)
			} else {
				var result:uint = (number >>> (7 - bitsLeft)) &
						(uint(1 << bitsLeft) - 1)
				bits -= bitsLeft
				var i:uint = bitsLeft
				for (;;) {
					if (number < 0x80) {
						bitsLeft = 0
						break
					}
					number = input.readUnsignedByte()
					if (bits - i <= 7) {
						result |= (number & (uint(1 << (bits - i)) - 1)) << i
						bitsLeft = 7 + i - bits
						break
					} else {
						result |= (number & 0x7F) << i
						i += 7
					}
				}
				return result
			}
		}
	}
}
