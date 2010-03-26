public function toString():String {
	return messageToString(this)
}
protected function writePostposeLength(output:PostposeLengthBuffer):void {
	import flash.errors.IllegalOperationError
	throw new IllegalOperationError("Not implemented.")
}
import flash.utils.IDataOutput
public final function writeExternal(output:IDataOutput):void {
	import com.netease.protobuf.PostposeLengthBuffer
	var plb:PostposeLengthBuffer = output as PostposeLengthBuffer
	if (plb == null) {
		plb = new PostposeLengthBuffer
		writePostposeLength(plb)
		plb.toNormal(output)
	} else {
		writePostposeLength(plb)
	}
}

