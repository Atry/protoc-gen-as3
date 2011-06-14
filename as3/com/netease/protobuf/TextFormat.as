// vim: tabstop=4 shiftwidth=4

// Copyright (c) 2010 , NetEase.com,Inc. All rights reserved.
//
// Author: Yang Bo (pop.atry@gmail.com)
//
// Use, modification and distribution are subject to the "New BSD License"
// as listed at <url: http://www.opensource.org/licenses/bsd-license.php >.

package com.netease.protobuf {
	import com.netease.protobuf.fieldDescriptors.*;
	import flash.errors.IOError;
	import flash.utils.describeType
	import flash.utils.getDefinitionByName;
	import flash.utils.IDataInput
	import flash.utils.IDataOutput
	import flash.utils.ByteArray
	public final class TextFormat {
		private static function printHex(output:IDataOutput, value:uint):void {
			const hexString:String = value.toString(16)
			output.writeUTFBytes("00000000".substring(0, hexString.length))
			output.writeUTFBytes(hexString)
		}
		private static function printEnum(output:IDataOutput,
				value:int, enumType:Class):void {
			const enumTypeDescription:XML = describeType(enumType)
			for each(var name:String in enumTypeDescription.constant.@name) {
				if (enumType[name] === value) {
					output.writeUTFBytes(name)
					return
				}
			}
			throw new IOError(value + " is invalid for " +
					enumTypeDescription.@name)
		}
		private static function printBytes(output:IDataOutput,
				value:ByteArray):void {
			output.writeUTFBytes("\"");
			value.position = 0
			while (value.bytesAvailable > 0) {
				const byte:int = value.readByte()
				switch (byte) {
				case 7: output.writeUTFBytes("\\a" ); break;
				case 8: output.writeUTFBytes("\\b" ); break;
				case 12: output.writeUTFBytes("\\f" ); break;
				case 10: output.writeUTFBytes("\\n" ); break;
				case 13: output.writeUTFBytes("\\r" ); break;
				case 9: output.writeUTFBytes("\\t" ); break;
				case 11: output.writeUTFBytes("\\v" ); break;
				case 92: output.writeUTFBytes("\\\\"); break;
				case 39: output.writeUTFBytes("\\\'"); break;
				case 34 : output.writeUTFBytes("\\\""); break;
				default:
					if (byte >= 0x20) {
						output.writeByte(byte);
					} else {
						output.writeUTFBytes('\\');
						output.writeByte('0'.charCodeAt() + ((byte >>> 6) & 3));
						output.writeByte('0'.charCodeAt() + ((byte >>> 3) & 7));
						output.writeByte('0'.charCodeAt() + (byte & 7));
					}
					break;
				}
			}
			output.writeUTFBytes("\"");
		}
		private static function printString(output:IDataOutput,
				value:String):void {
			const buffer:ByteArray = new ByteArray
			buffer.writeUTFBytes(value)
			printBytes(output, buffer)
		}
		private static function printUnknownField(output:IDataOutput, tag:uint,
				value:Object, newLine:uint, currentIndent:String):void {
			output.writeUTFBytes(currentIndent)
			output.writeUTFBytes(String(tag >>> 3))
			output.writeUTFBytes(": ")
			switch (tag & 7) {
			case WireType.VARINT:
				output.writeUTFBytes(UInt64(value).toString())
				break
			case WireType.FIXED_32_BIT:
				output.writeUTFBytes("0x")
				printHex(output, uint(value))
				break
			case WireType.FIXED_64_BIT:
				const u64:UInt64 = UInt64(value)
				output.writeUTFBytes("0x")
				printHex(output, u64.high)
				printHex(output, u64.low)
				break
			case WireType.LENGTH_DELIMITED:
				printBytes(output, ByteArray(value))
				break
			}
			output.writeByte(newLine)
		}
		private static function printMessageFields(output:IDataOutput,
				message:Message,
				newLine:uint,
				indentChars:String = "",
				currentIndent:String = ""):void {
						const type:Class = Object(message).constructor
			const description:XML = describeType(type)
			for each (var fieldDescriptorName:String in description.constant.
					(0 == String(@type).search(
					/^com.netease.protobuf.fieldDescriptors::(Repeated)?FieldDescriptor\$/)
					).@name) {
				const fieldDescriptor:BaseFieldDescriptor =
						type[fieldDescriptorName]
				if (fieldDescriptor.name.search(/\/\./) != -1) {
					// extend on other message
					continue
				}
		
				const shortName:String = fieldDescriptor.fullName.substring(
						fieldDescriptor.fullName.lastIndexOf('.') + 1)
				if (fieldDescriptor.type == Array) {
					const fieldValues:Array = message[fieldDescriptor.name]
					if (fieldValues) {
						for (var i:int = 0; i < fieldValues.length; i++) {
							output.writeUTFBytes(currentIndent)
							output.writeUTFBytes(shortName)
							printValue(output, fieldDescriptor, fieldValues[i],
									newLine, indentChars, currentIndent)
						}
					}
				} else {
					const m:Array = fieldDescriptor.name.match(/^(__)?(.)(.*)$/)
					m[0] = ""
					m[1] = "has"
					m[2] = m[2].toUpperCase()
					const hasField:String = m.join("")
					try {
						if (!message[hasField]) {
							continue
						}
					} catch (e:ReferenceError) {
						// required
					}
					output.writeUTFBytes(currentIndent)
					output.writeUTFBytes(shortName)
					printValue(output, fieldDescriptor,
							message[fieldDescriptor.name], newLine, indentChars,
							currentIndent)
				}
			}
			for (var key:String in message) {
				var extension:BaseFieldDescriptor
				try {
					extension = BaseFieldDescriptor.getExtensionByName(key)
				} catch (e:ReferenceError) {
					if (key.search(/^[0-9]+$/) == 0) {
						// unknown field
						printUnknownField(output, uint(key), message[key],
								newLine, currentIndent)
					} else {
						throw new IOError("Bad unknown field " + key)
					}
					continue
				}
				if (extension.type == Array) {
					const extensionFieldValues:Array = message[key]
					for (var j:int = 0; j < extensionFieldValues.length; j++) {
						output.writeUTFBytes(currentIndent)
						output.writeUTFBytes("[")
						output.writeUTFBytes(extension.fullName)
						output.writeUTFBytes("]")
						printValue(output, extension,
								extensionFieldValues[j], newLine, indentChars,
								currentIndent)
					}
				} else {
					output.writeUTFBytes(currentIndent)
					output.writeUTFBytes("[")
					output.writeUTFBytes(extension.fullName)
					output.writeUTFBytes("]")
					printValue(output, extension, message[key], newLine,
							indentChars, currentIndent)
				}
			}
		}
		
		private static function printValue(output:IDataOutput,
				fieldDescriptor:BaseFieldDescriptor,
				value:Object,
				newLine:uint,
				indentChars:String = "",
				currentIndent:String = ""):void {
			const message:Message = value as Message
			if (message) {
				output.writeUTFBytes(" {")
				output.writeByte(newLine)
				printMessageFields(output, message, newLine, indentChars,
						indentChars + currentIndent)
				output.writeUTFBytes(currentIndent)
				output.writeUTFBytes("}")
			} else {
				output.writeUTFBytes(": ")
				const stringValue:String = value as String
				if (stringValue) {
					printString(output, stringValue)
				} else {
					const enumFieldDescriptor:FieldDescriptor$TYPE_ENUM =
							fieldDescriptor as FieldDescriptor$TYPE_ENUM
					if (enumFieldDescriptor) {
						printEnum(output, int(value),
								enumFieldDescriptor.enumType)
					} else {
						const enumRepeatedFieldDescriptor:
								RepeatedFieldDescriptor$TYPE_ENUM =
								fieldDescriptor as
								RepeatedFieldDescriptor$TYPE_ENUM
						if (enumRepeatedFieldDescriptor) {
							printEnum(output, int(value),
									enumRepeatedFieldDescriptor.enumType)
						} else {
							output.writeUTFBytes(value.toString())
						}
					}
				}
			}
			output.writeByte(newLine)
		}	
		
		public static function printToUTFBytes(output:IDataOutput,
				message:Message,
				singleLineMode:Boolean = true):void {
			printMessageFields(output, message,
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