// vim: tabstop=4 shiftwidth=4

// Copyright (c) 2010 , NetEase.com,Inc. All rights reserved.
// Copyright (c) 2012 , Yang Bo. All rights reserved.
//
// Author: Yang Bo (pop.atry@gmail.com)
//
// Use, modification and distribution are subject to the New BSD License
// as listed at <url: http://www.opensource.org/licenses/bsd-license.php >.

package com.netease.protobuf {
	import google.protobuf.MethodOptions
	import google.protobuf.ServiceOptions
	import flash.errors.*
	import flash.utils.*

	public final class CustomOption {
		public static function getMethodOptions(methodFullName:String):MethodOptions {
			const m:Array = methodFullName.match(/^(.+)\.[^\.]+$/)
			if (m == null) {
				return null
			}
			const serviceClass:Class = Class(getDefinitionByName(m[1]))
			var optionsBytes:ByteArray
			try {
				optionsBytes =
					serviceClass.OPTIONS_BYTES_BY_METHOD_NAME[methodFullName]
			} catch (e:ReferenceError) {
				return null
			}
			if (optionsBytes) {
				const result:MethodOptions = new MethodOptions
				result.mergeFrom(optionsBytes)
				return result
			} else {
				return null
			}
		}

		public static function getServiceOptions(serviceClass:Class):ServiceOptions {
			var optionsBytes:ByteArray
			try {
				optionsBytes = serviceClass.OPTIONS_BYTES
			} catch (e:ReferenceError) {
				return null
			}
			const result:ServiceOptions = new ServiceOptions
			result.mergeFrom(optionsBytes)
			return result
		}
	}
}
