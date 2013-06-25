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
	/**
	 * @private
	 */
	public final class FieldDescriptor_TYPE_MESSAGE extends
			FieldDescriptor {
		public var messageUnion:Object
		public function FieldDescriptor_TYPE_MESSAGE(
				fullName:String, name:String, tag:uint, messageUnion:Object) {
			this.fullName = fullName
			this._name = name
			this.tag = tag
			this.messageUnion = messageUnion
		}
		override public function get type():Class {
			return (messageUnion as Class) || Class(messageUnion = messageUnion())
		}
		override public function readSingleField(input:IDataInput):* {
			return ReadUtils.read_TYPE_MESSAGE(input, new type)
		}
		override public function writeSingleField(output:WritingBuffer,
				value:*):void {
			WriteUtils.write_TYPE_MESSAGE(output, value)
		}
	}
}
