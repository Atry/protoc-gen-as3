// vim: tabstop=4 shiftwidth=4

// Copyright (c) 2010 , 杨博 (Yang Bo) All rights reserved.
//
//         pop.atry@gmail.com
//
// Use, modification and distribution are subject to the "New BSD License"
// as listed at <url: http://www.opensource.org/licenses/bsd-license.php >.

package {
	import protobuf_unittest.*
	import flash.display.*
	import flash.utils.*
	import flash.system.*
	public final class Test extends Sprite {
		private static function assert(b:Boolean,
				errorMessage:String = "Assertion failed.",
				errorId:int = 0):void {
			if (!b) {
				throw new Error(errorMessage, errorId)
			}
		}
		private static function assertSame(l:*, r:*):void {
			if (typeof(l) == "object") {
				assert(getQualifiedClassName(l) ==
						getQualifiedClassName(r))
				var k:*
				for(k in l) {
					assertSame(l[k], r[k])
				}
				for(k in r) {
					assertSame(l[k], r[k])
				}
				const description:XML = describeType(l)
				var getter:XML
				for each(getter in description..accessor.(@access != "writeonly")) {
					assertSame(l[getter.@name], r[getter.@name])
				}
				for each(getter in description..variable) {
					assertSame(l[getter.@name], r[getter.@name])
				}
			} else {
				assert(l === r)
			}
		}
		private static function test(input:IExternalizable):void {
			const ba:ByteArray = new ByteArray
			input.writeExternal(ba)
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

			trace("All tests pass.")
			fscommand("quit")
		}
	}
}
