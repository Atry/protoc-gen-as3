// vim: tabstop=4 shiftwidth=4

// Copyright (c) 2010 , NetEase.com,Inc. All rights reserved.
//
// Author: Yang Bo (pop.atry@gmail.com)
//
// Use, modification and distribution are subject to the "New BSD License"
// as listed at <url: http://www.opensource.org/licenses/bsd-license.php >.

package com.netease.protobuf {
	import flash.utils.describeType
	import flash.utils.getDefinitionByName;
	import flash.utils.IDataInput
	import flash.utils.IDataOutput
	import flash.utils.ByteArray
	public final class TextFormat {
		private static function printMessage(output:IDataOutput,
				message:Message,
				newLine:uint,
				indentChars:String = "",
				currentIndent:String = ""):void {
			output.writeUTFBytes("{")
			output.writeByte(newLine)
			const fieldIndent:String = indentChars + currentIndent
			const description:XML = describeType(message)
			for each (var protobufField:XML in description.accessor.metadata.
					(@name == "ProtocolBuffersField")) {
				const lowerCamelCaseName:String = protobufField.parent().@name;
				const originName:String = protobufField.arg.
						(@key == "name").@value
				const value:* = message[lowerCamelCaseName]
				if (value is Array) {
					// repeated
					for (var i:int = 0; i < value.length; i++) {
						output.writeUTFBytes(fieldIndent)
						output.writeUTFBytes(originName)
						output.writeUTFBytes(":")
						printValue(output, protobufField, value[i], newLine,
								indentChars, fieldIndent)
						output.writeByte(newLine)
					}
				} else {
					const m:Array = lowerCamelCaseName.match(/^(__)?(.)(.*)$/)
					m[0] = "has"
					m[1] = m[1].toUpperCase()
					const hasField:String = m.join("")
					const hasFieldAccessor:XML = description.accessor.
							(@name == hasField)
					if (hasFieldAccessor && !message[hasField]) {
						// optional field without assigned value
						continue;
					}
					output.writeUTFBytes(fieldIndent)
					output.writeUTFBytes(originName)
					output.writeUTFBytes(": ")
					printValue(output, protobufField, value, newLine,
							indentChars, fieldIndent)
					output.writeByte(newLine)
				}
			}
			for (var key:String in message) {
				if (key.search(/^[0-9]$/) == 0) {
					// unknown field
					// TODO
				} else {
					
					getDefinitionByName(key)
					//output.writeUTFBytes("[")
					//output
				}
			}
			// TODO: extension
			// TODO: unknown fields
			
			trace(description)
		}

		
		private static function printValue(output:IDataOutput,
				protobufField:XML,
				value:*,
				newLine:uint,
				indentChars:String = "",
				currentIndent:String = ""):void {
			
		}	
		
		public static function printToUTFBytes(output:IDataOutput,
				message:Message,
				singleLineMode:Boolean = true):void {
			printMessage(output, message,
					(singleLineMode ? ' ' : '\n').charCodeAt(),
					singleLineMode ? "" : "  ")
		}
		
		public static function printToString(message:Message,
				singleLineMode:Boolean = true):String {
			const ba:ByteArray = new ByteArray
			printToUTFBytes(ba, message, singleLineMode)
			ba.position = 0
			return ba.readUTFBytes(ba.length)
		}

		public static function readFromUTFBytes(input:IDataInput):Message {
			return null
		}
		
		public static function readFromString(text:String):Message {
			return null
		}
	}
}