// vim: tabstop=4 shiftwidth=4

// Copyright (c) 2010 , NetEase.com,Inc. All rights reserved.
//
// Author: Yang Bo (pop.atry@gmail.com)
//
// Use, modification and distribution are subject to the "New BSD License"
// as listed at <url: http://www.opensource.org/licenses/bsd-license.php >.

package com.netease.protobuf.fieldDescriptors {
	import com.netease.protobuf.*
	import flash.utils.*
	public final class FieldDescriptor$TYPE_MESSAGE extends
			FieldDescriptor {
		public var messageType:Class
		public function FieldDescriptor$TYPE_MESSAGE(
				name:String, tag:uint, messageType:Class) {
			this.name = name
			this.tag = tag
			this.messageType = messageType
		}
		override public function get type():Class {
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
