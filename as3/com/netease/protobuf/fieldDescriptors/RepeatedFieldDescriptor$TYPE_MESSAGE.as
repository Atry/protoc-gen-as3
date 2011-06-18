// vim: tabstop=4 shiftwidth=4

// Copyright (c) 2011 , Yang Bo All rights reserved.
//
// Author: Yang Bo (pop.atry@gmail.com)
//
// Use, modification and distribution are subject to the "New BSD License"
// as listed at <url: http://www.opensource.org/licenses/bsd-license.php >.

package com.netease.protobuf.fieldDescriptors {
	import com.netease.protobuf.*
	import flash.utils.*
	public final class RepeatedFieldDescriptor$TYPE_MESSAGE extends
			RepeatedFieldDescriptor {
		public var messageType:Class
		public function RepeatedFieldDescriptor$TYPE_MESSAGE(
				fullName:String, name:String, tag:uint, messageType:Class) {
			this.fullName = fullName
			this.name = name
			this.tag = tag
			this.messageType = messageType
		}
		override public function get nonPackedWireType():int {
			return WireType.LENGTH_DELIMITED
		}
		override public function get type():Class {
			return Array
		}
		override public function get elementType():Class {
			return messageType
		}
		override public function readSingleField(input:IDataInput):* {
			return ReadUtils.read$TYPE_MESSAGE(input, new messageType)
		}
		override public function writeSingleField(output:WritingBuffer,
				value:*):void {
			WriteUtils.write$TYPE_MESSAGE(output, value)
		}
	}
}
