// vim: tabstop=4 shiftwidth=4

// Copyright (c) 2010 , 杨博 (Yang Bo) All rights reserved.
//
//         pop.atry@gmail.com
//
// Use, modification and distribution are subject to the "New BSD License"
// as listed at <url: http://www.opensource.org/licenses/bsd-license.php >.

package {
	import protobuf_unittest.*
	import protobuf_unittest.TestAllTypes.*
	import flash.display.*
	import flash.utils.*
	import flash.system.*
	include "../unittest.proto.as3/initializer.as.inc"
	public final class Test extends Sprite {
		private static function assert(b:Boolean,
				errorMessage:String = "Assertion failed.",
				errorId:int = 0):void {
			if (!b) {
				throw new Error(errorMessage, errorId)
			}
		}
		private static function assertSame(l:*, r:*, name:String = ""):void {
			if (typeof(l) == "object") {
				assert(getQualifiedClassName(l) ==
						getQualifiedClassName(r))
				var k:*
				for(k in l) {
					assertSame(l[k], r[k], name + "." + k)
				}
				for(k in r) {
					assertSame(l[k], r[k], name + "." + k)
				}
				const description:XML = describeType(l)
				for each(var getter:XML in description..accessor.(@access != "writeonly")) {
					assertSame(l[getter.@name], r[getter.@name], name + "." + getter.@name)
				}
				for each(var field:XML in description..variable) {
					assertSame(l[field.@name], r[field.@name], name + "." + field.@name)
				}
			} else {
				assert(l === r || (isNaN(l) && isNaN(r)), name + " not equal.")
			}
		}
		private static const HEX_CHARS:String = "0123456789ABCDEF"
		private static function test(input:IExternalizable):void {
			const ba:ByteArray = new ByteArray
			input.writeExternal(ba)
			var s:String = ""
			for (var i:uint = 0; i < ba.length; i++) {
				s += HEX_CHARS.charAt(ba[i] / 16)
				s += HEX_CHARS.charAt(ba[i] % 16)
				s += " "
			}
			trace(s)
			ba.position = 0
			const output:IExternalizable =
					new (getDefinitionByName(getQualifiedClassName(input)))
			output.readExternal(ba)
			assertSame(input, output)
		}
		public function Test() {
			const t1:TestPackedTypes = new TestPackedTypes
			t1.packedDouble = [1.23424353, 2.12]
			t1.packedEnum = [ForeignEnum.FOREIGN_BAZ, ForeignEnum.FOREIGN_FOO,
					ForeignEnum.FOREIGN_FOO, ForeignEnum.FOREIGN_BAR]
			test(t1)

			const t2:TestPackedExtensions = new TestPackedExtensions
			t2[packedDoubleExtension] = [324.234, 1.23424353, 2.12]
			t2[packedEnumExtension] =[ForeignEnum.FOREIGN_BAZ,
					ForeignEnum.FOREIGN_FOO, ForeignEnum.FOREIGN_FOO] 
			test(t2)

			const t3:TestAllTypes = new TestAllTypes
			t3.optionalString = "111foo"
			t3.defaultNestedEnum = NestedEnum.FOO
			t3.repeatedNestedMessage.push(new NestedMessage)
			t3.repeatedNestedMessage.push(new NestedMessage)
			t3.repeatedNestedMessage[1].bb = 123
			t3.optionalInt32 = -23412413
			t3.repeatedNestedEnum.push(NestedEnum.FOO)
			t3.repeatedNestedEnum.push(NestedEnum.BAR)
			t3.optionalNestedMessage = new NestedMessage
			t3.optionalNestedMessage.bb = 234
			test(t3)

			trace("All tests pass.")
			fscommand("quit")
		}
	}
}
