// vim: tabstop=4 shiftwidth=4

// Copyright (c) 2011 , Yang Bo All rights reserved.
//
// Author: Yang Bo (pop.atry@gmail.com)
//
// Use, modification and distribution are subject to the "New BSD License"
// as listed at <url: http://www.opensource.org/licenses/bsd-license.php >.

package com.netease.protobuf {
	public class Binary64 {
		public var low:uint;
		/**
		 * @private
		 */
		internal var internalHigh:uint;
		public function Binary64(low:uint = 0, high:uint = 0) {
			this.low = low
			this.internalHigh = high
		}
		/**
		 * Division by n.
		 * @return The remainder after division.
		 */
		internal final function div(n:uint):uint {
			const modHigh:uint = internalHigh % n
			const mod:uint = (low % n + modHigh * 6) % n
			internalHigh /= n
			const newLow:Number = (modHigh * Number(0x100000000) + low) / n
			internalHigh += newLow / 0x100000000
			low = newLow
			return mod
		}
		internal final function mul(n:uint):void {
			const newLow:Number = Number(low) * n
			internalHigh = newLow / 0x100000000 + Number(internalHigh) * n
			low = newLow
		}
		internal final function add(n:uint):void {
			const newLow:Number = Number(low) + n
			internalHigh = newLow / 0x100000000 + internalHigh
			low = newLow
		}
		internal final function bitwiseNot():void {
			low = ~low
			internalHigh = ~internalHigh
		}
	}
}
