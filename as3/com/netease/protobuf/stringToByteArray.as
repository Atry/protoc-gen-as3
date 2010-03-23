// vim: tabstop=4 shiftwidth=4

// Copyright (c) 2010 , 杨博 (Yang Bo) All rights reserved.
//
//         pop.atry@gmail.com
//
// Use, modification and distribution are subject to the "New BSD License"
// as listed at <url: http://www.opensource.org/licenses/bsd-license.php >.

package com.netease.protobuf {
	import flash.utils.ByteArray
	public function stringToByteArray(s:String):ByteArray {
		const ba:ByteArray = new ByteArray
		for (var i:uint = 0; i < s.length; ++i) {
			ba.writeByte(s.charCodeAt(i))
		}
		return ba
	}
}
