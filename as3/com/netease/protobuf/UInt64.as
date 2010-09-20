// vim: tabstop=4 shiftwidth=4

// Copyright (c) 2010 , NetEase.com,Inc. All rights reserved.
//
// Author: Yang Bo (pop.atry@gmail.com)
//
// Use, modification and distribution are subject to the "New BSD License"
// as listed at <url: http://www.opensource.org/licenses/bsd-license.php >.

package com.netease.protobuf {
	public final class UInt64 {
		public var low:uint;
		public var high:uint;
		public function UInt64(low:uint = 0, high:uint = 0) {
			this.low = low
			this.high = high
		}
	}
}
