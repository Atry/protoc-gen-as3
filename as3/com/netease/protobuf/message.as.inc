// vim: tabstop=4 shiftwidth=4

// Copyright (c) 2010 , NetEase.com,Inc. All rights reserved.
//
// Author: Yang Bo (pop.atry@gmail.com)
//
// Use, modification and distribution are subject to the "New BSD License"
// as listed at <url: http://www.opensource.org/licenses/bsd-license.php >.

public function toString():String {
	return messageToString(this)
}
/**
 *  @private
 */
public function writeToBuffer(output:WritingBuffer):void {
	import flash.errors.IllegalOperationError
	throw new IllegalOperationError("Not implemented.")
}
import flash.utils.IDataOutput
public final function writeExternal(output:IDataOutput):void {
	var wb:WritingBuffer = output as WritingBuffer
	if (wb == null) {
		wb = new WritingBuffer
		writeToBuffer(wb)
		wb.toNormal(output)
	} else {
		writeToBuffer(wb)
	}
}

