// vim: tabstop=4 shiftwidth=4

// Copyright (c) 2011 , Yang Bo All rights reserved.
//
// Author: Yang Bo (pop.atry@gmail.com)
//
// Use, modification and distribution are subject to the "New BSD License"
// as listed at <url: http://www.opensource.org/licenses/bsd-license.php >.

package com.netease.protobuf {
	public class Binary64 {
		/**
		 * @private
		 */
		internal static const CHAR_CODE_0:uint = '0'.charCodeAt();
		/**
		 * @private
		 */
		internal static const CHAR_CODE_9:uint = '9'.charCodeAt();
		/**
		 * @private
		 */
		internal static const CHAR_CODE_A:uint = 'a'.charCodeAt();
		/**
		 * @private
		 */
		internal static const CHAR_CODE_Z:uint = 'z'.charCodeAt();
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
		 * @private
		 */
		internal final function div(n:uint):uint {
			const modHigh:uint = internalHigh % n
			const mod:uint = uint((modHigh * 4294967296.0 + low) % n)
			internalHigh /= n
			const newLow:Number = (modHigh * 4294967296.0 + low) / n
			internalHigh += uint(newLow / 4294967296.0)
			low = newLow
			return mod
		}
		/**
		 * @private
		 */
		internal final function mul(n:uint):void {
			const newLow:Number = Number(low) * n
			internalHigh *= n
			internalHigh += uint(newLow / 4294967296.0)
			low *= n
		}
		/**
		 * @private
		 */
		internal final function add(n:uint):void {
			const newLow:Number = Number(low) + n
			internalHigh += uint(newLow / 4294967296.0)
			low = newLow
		}
		/**
		 * @private
		 */
		internal final function bitwiseNot():void {
			low = ~low
			internalHigh = ~internalHigh
		}
	}
}
