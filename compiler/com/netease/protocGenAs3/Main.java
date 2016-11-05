// vim: fileencoding=utf-8 tabstop=4 shiftwidth=4

// Copyright (c) 2010 , NetEase.com,Inc. All rights reserved.
//
// Author: Yang Bo (pop.atry@gmail.com)
//
// Use, modification and distribution are subject to the "New BSD License"
// as listed at <url: http://www.opensource.org/licenses/bsd-license.php >.

package com.netease.protocGenAs3;
import static com.google.protobuf.compiler.PluginProtos.*;
import static com.google.protobuf.DescriptorProtos.*;
import com.google.protobuf.*;
import java.io.*;
import java.util.*;
import java.math.*;
public final class Main {
	private static final String[] ACTIONSCRIPT_KEYWORDS = {
		"as", "break", "case", "catch", "class", "const", "continue", "default",
		"delete", "do", "else", "extends", "false", "finally", "for",
		"function", "if", "implements", "import", "in", "instanceof",
		"interface", "internal", "is", "native", "new", "null", "package",
		"private", "protected", "public", "return", "super", "switch", "this",
		"throw", "to", "true", "try", "typeof", "use", "var", "void", "while",
		"with"
	};
	private static final class Scope<Proto> {
		// 如果 proto instanceOf Scope ，则这个 Scope 是对另一 Scope 的引用
		public final String fullName;
		public final Scope<?> parent;
		public final Proto proto;
		public final boolean export;
		public final HashMap<String, Scope<?>> children =
				new HashMap<String, Scope<?>>();
		private Scope<?> find(String[] pathElements, int i) {
			Scope<?> result = this;
			for (; i < pathElements.length; i++) {
				String name = pathElements[i];
				if (result.children.containsKey(name)) {
					result = result.children.get(name);
				} else {
					return null;
				}
			}
			while (result.proto instanceof Scope) {
				result = (Scope<?>)result.proto;
			}
			return result;
		}
		public boolean isRoot() {
			return parent == null;
		}
		private Scope<?> getRoot() {
			Scope<?> scope = this;
			while (!scope.isRoot()) {
				scope = scope.parent;
			}
			return scope;
		}
		public Scope<?> find(String path) {
			String[] pathElements = path.split("\\.");
			if (pathElements[0].equals("")) {
				return getRoot().find(pathElements, 1);
			} else {
				for (Scope<?> scope = this; scope != null; scope = scope.parent) {
					Scope<?> result = scope.find(pathElements, 0);
					if (result != null) {
						return result;
					}
				}
				return null;
			}
		}
		private Scope<?> findOrCreate(String[] pathElements, int i) {
			Scope<?> scope = this;
			for (; i < pathElements.length; i++) {
				String name = pathElements[i];
				if (scope.children.containsKey(name)) {
					scope = scope.children.get(name);
				} else {
					Scope<Object> child =
							new Scope<Object>(scope, null, false, name);
					scope.children.put(name, child);
					scope = child;
				}
			}
			return scope;
		}
		public Scope<?> findOrCreate(String path) {
			String[] pathElements = path.split("\\.");
			if (pathElements[0].equals("")) {
				return getRoot().findOrCreate(pathElements, 1);
			} else {
				return findOrCreate(pathElements, 0);
			}
		}
		private Scope(Scope<?> parent, Proto proto, boolean export,
				String name) {
			this.parent = parent;
			this.proto = proto;
			this.export = export;
			if (isRoot() || parent.isRoot()) {
				fullName = name; 
			} else {
				fullName = parent.fullName + '.' + name;
			}
		}
		public <ChildProto> Scope<ChildProto> addChild(
				String name, ChildProto proto, boolean export) {
			assert(name != null);
			assert(!name.equals(""));
			Scope<ChildProto> child =
					new Scope<ChildProto>(this, proto, export, name);
			if(children.containsKey(child)) {
				throw new IllegalArgumentException();
			}
			children.put(name, child);
			return child;
		}
		public static Scope<Object> newRoot() {
			return new Scope<Object>(null, null, false, "");
		}
	}
	private static void addServiceToScope(Scope<?> scope,
			ServiceDescriptorProto sdp, boolean export) {
		scope.addChild(sdp.getName(), sdp, export);
	}
	private static void addExtensionToScope(Scope<?> scope,
			FieldDescriptorProto efdp, boolean export) {
		scope.addChild(efdp.getName().toUpperCase(), efdp, export);
	}
	private static void addEnumToScope(Scope<?> scope, EnumDescriptorProto edp,
			boolean export) {
		assert(edp.hasName());
		Scope<EnumDescriptorProto> enumScope =
				scope.addChild(edp.getName(), edp, export);
		for (EnumValueDescriptorProto evdp : edp.getValueList()) {
			Scope<EnumValueDescriptorProto> enumValueScope =
					enumScope.addChild(evdp.getName(), evdp, false);
			scope.addChild(evdp.getName(), enumValueScope, false);
		}
	}
	private static void addMessageToScope(Scope<?> scope, DescriptorProto dp,
			boolean export) {
		Scope<DescriptorProto> messageScope =
				scope.addChild(dp.getName(), dp, export);
		for (EnumDescriptorProto edp : dp.getEnumTypeList()) {
			addEnumToScope(messageScope, edp, export);
		}
		for (DescriptorProto nested: dp.getNestedTypeList()) {
			addMessageToScope(messageScope, nested, export);
		}
	}
	private static Scope<Object> buildScopeTree(CodeGeneratorRequest request) {
		Scope<Object> root = Scope.newRoot();
		List<String> filesToGenerate = request.getFileToGenerateList();
		for (FileDescriptorProto fdp : request.getProtoFileList()) {
			Scope<?> packageScope = fdp.hasPackage() ?
					root.findOrCreate(fdp.getPackage()) : root;
			boolean export = filesToGenerate.contains(fdp.getName());
			for (ServiceDescriptorProto sdp : fdp.getServiceList()) {
				addServiceToScope(packageScope, sdp, export);
			}
			for (FieldDescriptorProto efdp : fdp.getExtensionList()) {
				addExtensionToScope(packageScope, efdp, export);
			}
			for (EnumDescriptorProto edp : fdp.getEnumTypeList()) {
				addEnumToScope(packageScope, edp, export);
			}
			for (DescriptorProto dp : fdp.getMessageTypeList()) {
				addMessageToScope(packageScope, dp, export);
			}
		}
		return root;
	}
	private static String getImportType(Scope<?> scope,
			FieldDescriptorProto fdp) {
		switch (fdp.getType()) {
		case TYPE_ENUM:
		case TYPE_MESSAGE:
			Scope<?> typeScope = scope.find(fdp.getTypeName());
			if (typeScope == null) {
				throw new IllegalArgumentException(
						fdp.getTypeName() + " not found.");
			}
			return typeScope.fullName;
		case TYPE_BYTES:
			return "flash.utils.ByteArray";
		default:
			return null;
		}
	}
	private static boolean isValueType(FieldDescriptorProto.Type type) {
		switch (type) {
		case TYPE_DOUBLE:
		case TYPE_FLOAT:
		case TYPE_INT32:
		case TYPE_FIXED32:
		case TYPE_BOOL:
		case TYPE_UINT32:
		case TYPE_SFIXED32:
		case TYPE_SINT32:
		case TYPE_ENUM:
			return true;
		default:
			return false;
		}
	}
	static final int VARINT = 0;
	static final int FIXED_64_BIT = 1;
	static final int LENGTH_DELIMITED = 2;
	static final int FIXED_32_BIT = 5;
	private static int getWireType(
			FieldDescriptorProto.Type type) {
		switch (type) {
		case TYPE_DOUBLE:
		case TYPE_FIXED64:
		case TYPE_SFIXED64:
			return FIXED_64_BIT;
		case TYPE_FLOAT:
		case TYPE_FIXED32:
		case TYPE_SFIXED32:
			return FIXED_32_BIT;
		case TYPE_INT32:
		case TYPE_SINT32:
		case TYPE_UINT32:
		case TYPE_BOOL:
		case TYPE_INT64:
		case TYPE_UINT64:
		case TYPE_SINT64:
		case TYPE_ENUM:
			return VARINT;
		case TYPE_STRING:
		case TYPE_MESSAGE:
		case TYPE_BYTES:
			return LENGTH_DELIMITED;
		default:
			throw new IllegalArgumentException();
		}
	}
	private static String getActionScript3WireType(
			FieldDescriptorProto.Type type) {
		switch (type) {
		case TYPE_DOUBLE:
		case TYPE_FIXED64:
		case TYPE_SFIXED64:
			return "FIXED_64_BIT";
		case TYPE_FLOAT:
		case TYPE_FIXED32:
		case TYPE_SFIXED32:
			return "FIXED_32_BIT";
		case TYPE_INT32:
		case TYPE_SINT32:
		case TYPE_UINT32:
		case TYPE_BOOL:
		case TYPE_INT64:
		case TYPE_UINT64:
		case TYPE_SINT64:
		case TYPE_ENUM:
			return "VARINT";
		case TYPE_STRING:
		case TYPE_MESSAGE:
		case TYPE_BYTES:
			return "LENGTH_DELIMITED";
		default:
			throw new IllegalArgumentException();
		}
	}
	private static String getActionScript3LogicType(Scope<?> scope,
			FieldDescriptorProto fdp) {
		if (fdp.getType() == FieldDescriptorProto.Type.TYPE_ENUM) {
			Scope<?> typeScope = scope.find(fdp.getTypeName());
			if (typeScope == null) {
				throw new IllegalArgumentException(
						fdp.getTypeName() + " not found.");
			}
			if (typeScope == scope) {
				// workaround for mxmlc's bug.
				return typeScope.fullName.substring(
						typeScope.fullName.lastIndexOf('.') + 1);
			}
			return typeScope.fullName;
		} else {
			return getActionScript3Type(scope, fdp);
		}
	}
	private static String getActionScript3Type(Scope<?> scope,
			FieldDescriptorProto fdp) {
		switch (fdp.getType()) {
		case TYPE_DOUBLE:
		case TYPE_FLOAT:
			return "Number";
		case TYPE_INT32:
		case TYPE_SFIXED32:
		case TYPE_SINT32:
		case TYPE_ENUM:
			return "int";
		case TYPE_UINT32:
		case TYPE_FIXED32:
			return "uint";
		case TYPE_BOOL:
			return "Boolean";
		case TYPE_INT64:
		case TYPE_SFIXED64:
		case TYPE_SINT64:
			return "Int64";
		case TYPE_UINT64:
		case TYPE_FIXED64:
			return "UInt64";
		case TYPE_STRING:
			return "String";
		case TYPE_MESSAGE:
			Scope<?> typeScope = scope.find(fdp.getTypeName());
			if (typeScope == null) {
				throw new IllegalArgumentException(
						fdp.getTypeName() + " not found.");
			}
			if (typeScope == scope) {
				// workaround for mxmlc's bug.
				return typeScope.fullName.substring(
						typeScope.fullName.lastIndexOf('.') + 1);
			}
			return typeScope.fullName;
		case TYPE_BYTES:
			return "flash.utils.ByteArray";
		default:
			throw new IllegalArgumentException();
		}
	}
	private static void appendQuotedString(StringBuilder sb, String value) {
		sb.append('\"');
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			switch (c) {
			case '\"':
			case '\\':
				sb.append('\\');
				sb.append(c);
				break;
			default:
				if (c >= 128 || Character.isISOControl(c)) {
					sb.append("\\u");
					sb.append(String.format("%04X", new Integer(c)));
				} else {
					sb.append(c);
				}
			}
		}
		sb.append('\"');
	}
	private static void appendDefaultValue(StringBuilder sb, Scope<?> scope,
			FieldDescriptorProto fdp) {
		String value = fdp.getDefaultValue();
		switch (fdp.getType()) {
		case TYPE_DOUBLE:
		case TYPE_FLOAT:
			if (value.equals("nan")) {
				sb.append("NaN");
			} else if (value.equals("inf")) {
				sb.append("Infinity");
			} else if (value.equals("-inf")) {
				sb.append("-Infinity");
			} else {
				sb.append(value);
			}
			break;
		case TYPE_UINT64:
		case TYPE_FIXED64:
			{
				long v = new BigInteger(value).longValue();
				sb.append("new UInt64(");
				sb.append(Long.toString(v & 0xFFFFFFFFL));
				sb.append(", ");
				sb.append(Long.toString((v >>> 32) & 0xFFFFFFFFL));
				sb.append(")");
			}
			break;
		case TYPE_INT64:
		case TYPE_SFIXED64:
		case TYPE_SINT64:
			{
				long v = Long.parseLong(value);
				sb.append("new Int64(");
				sb.append(Long.toString(v & 0xFFFFFFFFL));
				sb.append(", ");
				sb.append(Integer.toString((int)v >>> 32));
				sb.append(")");
			}
			break;
		case TYPE_INT32:
		case TYPE_FIXED32:
		case TYPE_SFIXED32:
		case TYPE_SINT32:
		case TYPE_UINT32:
		case TYPE_BOOL:
			sb.append(value);
			break;
		case TYPE_STRING:
			appendQuotedString(sb, value);
			break;
		case TYPE_ENUM:
			sb.append(scope.find(fdp.getTypeName()).
					children.get(value).fullName);
			break;
		case TYPE_BYTES:
			sb.append("stringToByteArray(");
			sb.append("\"");
			sb.append(value);
			sb.append("\")");
			break;
		default:
			throw new IllegalArgumentException();
		}
	}
	private static void appendLowerCamelCase(StringBuilder sb, String s) {
		if (Arrays.binarySearch(ACTIONSCRIPT_KEYWORDS, s) >= 0) {
			sb.append("__");
		}
		sb.append(Character.toLowerCase(s.charAt(0)));
		boolean upper = false;
		for (int i = 1; i < s.length(); i++) {
			char c = s.charAt(i);
			if (upper) {
				if (Character.isLowerCase(c)) {
					sb.append(Character.toUpperCase(c));
					upper = false;
					continue;
				} else {
					sb.append('_');
				}
			}
			upper = c == '_';
			if (!upper) {
				sb.append(c);
			}
		}
	}
	private static void appendUpperCamelCase(StringBuilder sb, String s) {
		sb.append(Character.toUpperCase(s.charAt(0)));
		boolean upper = false;
		for (int i = 1; i < s.length(); i++) {
			char c = s.charAt(i);
			if (upper) {
				if (Character.isLowerCase(c)) {
					sb.append(Character.toUpperCase(c));
					upper = false;
					continue;
				} else {
					sb.append('_');
				}
			}
			upper = c == '_';
			if (!upper) {
				sb.append(c);
			}
		}
	}
	private static void writeMessage(Scope<DescriptorProto> scope,
			StringBuilder content, StringBuilder initializerContent) {
		content.append("\timport com.netease.protobuf.*;\n");
		content.append("\tuse namespace com.netease.protobuf.used_by_generated_code;\n");
		content.append("\timport com.netease.protobuf.fieldDescriptors.*;\n");
		content.append("\timport flash.utils.Endian;\n");
		content.append("\timport flash.utils.IDataInput;\n");
		content.append("\timport flash.utils.IDataOutput;\n");
		content.append("\timport flash.utils.IExternalizable;\n");
		content.append("\timport flash.errors.IOError;\n");
		HashSet<String> importTypes = new HashSet<String>();
		for (FieldDescriptorProto efdp : scope.proto.getExtensionList()) {
			importTypes.add(scope.find(efdp.getExtendee()).fullName);
			if (efdp.getType().equals(FieldDescriptorProto.Type.TYPE_MESSAGE)) {
				importTypes.add(scope.find(efdp.getTypeName()).fullName);
			}
			String importType = getImportType(scope, efdp);
			if (importType != null) {
				importTypes.add(importType);
			}
		}
		for (FieldDescriptorProto fdp : scope.proto.getFieldList()) {
			String importType = getImportType(scope, fdp);
			if (importType != null) {
				importTypes.add(importType);
			}
		}
		for (String importType : importTypes) {
			content.append("\timport ");
			content.append(importType);
			content.append(";\n");
		}
		content.append("\t// @@protoc_insertion_point(imports)\n\n");
		String remoteClassAlias;
		if (scope.proto.hasOptions()) {
			if (scope.proto.getOptions().hasExtension(Options.as3AmfAlias)) {
				remoteClassAlias = scope.proto.getOptions().getExtension(Options.as3AmfAlias);
			} else if (scope.proto.getOptions().getExtension(Options.as3AmfAutoAlias)) {
				remoteClassAlias = scope.fullName;
			} else {
				remoteClassAlias = null;
			}
			if (remoteClassAlias != null) {
				content.append("\t[RemoteClass(alias=");
				appendQuotedString(content, remoteClassAlias);
				content.append(")]\n");
			}
			if (scope.proto.getOptions().getExtension(Options.as3Bindable)) {
				content.append("\t[Bindable]\n");
			}
		} else {
			remoteClassAlias = null;
		}
		content.append("\t// @@protoc_insertion_point(class_metadata)\n");
		content.append("\tpublic dynamic final class ");
		content.append(scope.proto.getName());
		content.append(" extends com.netease.protobuf.Message");
		if (remoteClassAlias != null) {
			content.append(" implements flash.utils.IExternalizable {\n");
			content.append("\t\tpublic final function writeExternal(output:flash.utils.IDataOutput):void {\n");
			content.append("\t\t\twriteDelimitedTo(output);\n");
			content.append("\t\t}\n\n");
			content.append("\t\tpublic final function readExternal(input:flash.utils.IDataInput):void {\n");
			content.append("\t\t\tmergeDelimitedFrom(input);\n");
			content.append("\t\t}\n\n");
		} else {
			content.append(" {\n");
		}
		if (scope.proto.getExtensionRangeCount() > 0) {
			content.append("\t\t[ArrayElementType(\"Function\")]\n");
			content.append("\t\tpublic static const extensionReadFunctions:Array = [];\n\n");
		}
		if (scope.proto.getExtensionCount() > 0) {
			initializerContent.append("import ");
			initializerContent.append(scope.fullName);
			initializerContent.append(";\n");
			initializerContent.append("if(!");
			initializerContent.append(scope.fullName);
			initializerContent.append(") throw new Error();\n");
		}
		for (FieldDescriptorProto efdp : scope.proto.getExtensionList()) {
			if (efdp.getType() == FieldDescriptorProto.Type.TYPE_GROUP) {
				System.err.println("Warning: Group is not supported.");
				continue;
			}
			String extendee = scope.find(efdp.getExtendee()).fullName;
			content.append("\t\t/**\n\t\t *  @private\n\t\t */\n");
			content.append("\t\tpublic static const ");
			content.append(efdp.getName().toUpperCase());
			content.append(":");
			appendFieldDescriptorClass(content, efdp);
			content.append(" = ");
			appendFieldDescriptor(content, scope, efdp);
			content.append(";\n\n");
			if (efdp.hasDefaultValue()) {
				content.append("\t\t");
				content.append(extendee);
				content.append(".prototype[");
				content.append(efdp.getName().toUpperCase());
				content.append("] = ");
				appendDefaultValue(content, scope, efdp);
				content.append(";\n\n");
			}
			appendExtensionReadFunction(content, "\t\t", scope, efdp);
		}
		int valueTypeCount = 0;
		for (FieldDescriptorProto fdp : scope.proto.getFieldList()) {
			if (fdp.getType() == FieldDescriptorProto.Type.TYPE_GROUP) {
				System.err.println("Warning: Group is not supported.");
				continue;
			}
			content.append("\t\t/**\n\t\t *  @private\n\t\t */\n");
			content.append("\t\tpublic static const ");
			content.append(fdp.getName().toUpperCase());
			content.append(":");
			appendFieldDescriptorClass(content, fdp);
			content.append(" = ");
			appendFieldDescriptor(content, scope, fdp);
			content.append(";\n\n");
			assert(fdp.hasLabel());
			switch (fdp.getLabel()) {
			case LABEL_OPTIONAL:
				content.append("\t\tprivate var ");
				content.append(fdp.getName());
				content.append("$field:");
				content.append(getActionScript3Type(scope, fdp));
				content.append(";\n\n");

				if (isValueType(fdp.getType())) {
					final int valueTypeId = valueTypeCount++;
					final int valueTypeField = valueTypeId / 32;
					final int valueTypeBit = valueTypeId % 32;
					if (valueTypeBit == 0) {
						content.append("\t\tprivate var hasField$");
						content.append(valueTypeField);
						content.append(":uint = 0;\n\n");
					}
					
					content.append("\t\tpublic function clear");
					appendUpperCamelCase(content, fdp.getName());
					content.append("():void {\n");
					content.append("\t\t\thasField$");
					content.append(valueTypeField);
					content.append(" &= 0x");
					content.append(Integer.toHexString(~(1 << valueTypeBit)));
					content.append(";\n");

					content.append("\t\t\t");
					content.append(fdp.getName());
					content.append("$field = new ");
					content.append(getActionScript3Type(scope, fdp));
					content.append("();\n");
					content.append("\t\t}\n\n");

					content.append("\t\tpublic function get has");
					appendUpperCamelCase(content, fdp.getName());
					content.append("():Boolean {\n");
					content.append("\t\t\treturn (hasField$");
					content.append(valueTypeField);
					content.append(" & 0x");
					content.append(Integer.toHexString(1 << valueTypeBit));
					content.append(") != 0;\n");
					content.append("\t\t}\n\n");

					content.append("\t\tpublic function set ");
					appendLowerCamelCase(content, fdp.getName());
					content.append("(value:");
					content.append(getActionScript3Type(scope, fdp));
					content.append("):void {\n");
					content.append("\t\t\thasField$");
					content.append(valueTypeField);
					content.append(" |= 0x");
					content.append(Integer.toHexString(1 << valueTypeBit));
					content.append(";\n");
					content.append("\t\t\t");
					content.append(fdp.getName());
					content.append("$field = value;\n");
					content.append("\t\t}\n\n");
				} else {
					content.append("\t\tpublic function clear");
					appendUpperCamelCase(content, fdp.getName());
					content.append("():void {\n");
					content.append("\t\t\t");
					content.append(fdp.getName());
					content.append("$field = null;\n");
					content.append("\t\t}\n\n");

					content.append("\t\tpublic function get has");
					appendUpperCamelCase(content, fdp.getName());
					content.append("():Boolean {\n");
					content.append("\t\t\treturn ");
					content.append(fdp.getName());
					content.append("$field != null;\n");
					content.append("\t\t}\n\n");

					content.append("\t\tpublic function set ");
					appendLowerCamelCase(content, fdp.getName());
					content.append("(value:");
					content.append(getActionScript3Type(scope, fdp));
					content.append("):void {\n");
					content.append("\t\t\t");
					content.append(fdp.getName());
					content.append("$field = value;\n");
					content.append("\t\t}\n\n");
				}

				content.append("\t\tpublic function get ");
				appendLowerCamelCase(content, fdp.getName());
				content.append("():");
				content.append(getActionScript3Type(scope, fdp));
				content.append(" {\n");
				if (fdp.hasDefaultValue()) {
					content.append("\t\t\tif(!has");
					appendUpperCamelCase(content, fdp.getName());
					content.append(") {\n");
					content.append("\t\t\t\treturn ");
					appendDefaultValue(content, scope, fdp);
					content.append(";\n");
					content.append("\t\t\t}\n");
				}
				content.append("\t\t\treturn ");
				content.append(fdp.getName());
				content.append("$field;\n");
				content.append("\t\t}\n\n");
				break;
			case LABEL_REQUIRED:
				content.append("\t\tpublic var ");
				appendLowerCamelCase(content, fdp.getName());
				content.append(":");
				content.append(getActionScript3Type(scope, fdp));
				if (fdp.hasDefaultValue()) {
					content.append(" = ");
					appendDefaultValue(content, scope, fdp);
				}
				content.append(";\n\n");
				break;
			case LABEL_REPEATED:
				content.append("\t\t[ArrayElementType(\"");
				content.append(getActionScript3Type(scope, fdp));
				content.append("\")]\n");
				content.append("\t\tpublic var ");
				appendLowerCamelCase(content, fdp.getName());
				content.append(":Array = [];\n\n");
				break;
			default:
				throw new IllegalArgumentException();
			}
		}
		content.append("\t\t/**\n\t\t *  @private\n\t\t */\n\t\toverride com.netease.protobuf.used_by_generated_code final function writeToBuffer(output:com.netease.protobuf.WritingBuffer):void {\n");
		for (FieldDescriptorProto fdp : scope.proto.getFieldList()) {
			if (fdp.getType() == FieldDescriptorProto.Type.TYPE_GROUP) {
				System.err.println("Warning: Group is not supported.");
				continue;
			}
			switch (fdp.getLabel()) {
			case LABEL_OPTIONAL:
				content.append("\t\t\tif (");
				content.append("has");
				appendUpperCamelCase(content, fdp.getName());
				content.append(") {\n");
				content.append("\t\t\t\tcom.netease.protobuf.WriteUtils.writeTag(output, com.netease.protobuf.WireType.");
				content.append(getActionScript3WireType(fdp.getType()));
				content.append(", ");
				content.append(Integer.toString(fdp.getNumber()));
				content.append(");\n");
				content.append("\t\t\t\tcom.netease.protobuf.WriteUtils.write_");
				content.append(fdp.getType().name());
				content.append("(output, ");
				content.append(fdp.getName());
				content.append("$field);\n");
				content.append("\t\t\t}\n");
				break;
			case LABEL_REQUIRED:
				content.append("\t\t\tcom.netease.protobuf.WriteUtils.writeTag(output, com.netease.protobuf.WireType.");
				content.append(getActionScript3WireType(fdp.getType()));
				content.append(", ");
				content.append(Integer.toString(fdp.getNumber()));
				content.append(");\n");
				content.append("\t\t\tcom.netease.protobuf.WriteUtils.write_");
				content.append(fdp.getType().name());
				content.append("(output, this.");
				appendLowerCamelCase(content, fdp.getName());
				content.append(");\n");
				break;
			case LABEL_REPEATED:
				if (fdp.hasOptions() && fdp.getOptions().getPacked()) {
					content.append("\t\t\tif (this.");
					appendLowerCamelCase(content, fdp.getName());
					content.append(" != null && this.");
					appendLowerCamelCase(content, fdp.getName());
					content.append(".length > 0) {\n");
					content.append("\t\t\t\tcom.netease.protobuf.WriteUtils.writeTag(output, com.netease.protobuf.WireType.LENGTH_DELIMITED, ");
					content.append(Integer.toString(fdp.getNumber()));
					content.append(");\n");
					content.append("\t\t\t\tcom.netease.protobuf.WriteUtils.writePackedRepeated(output, com.netease.protobuf.WriteUtils.write_");
					content.append(fdp.getType().name());
					content.append(", this.");
					appendLowerCamelCase(content, fdp.getName());
					content.append(");\n");
					content.append("\t\t\t}\n");
				} else {
					content.append("\t\t\tfor (var ");
					appendLowerCamelCase(content, fdp.getName());
					content.append("$index:uint = 0; ");
					appendLowerCamelCase(content, fdp.getName());
					content.append("$index < this.");
					appendLowerCamelCase(content, fdp.getName());
					content.append(".length; ++");
					appendLowerCamelCase(content, fdp.getName());
					content.append("$index) {\n");
					content.append("\t\t\t\tcom.netease.protobuf.WriteUtils.writeTag(output, com.netease.protobuf.WireType.");
					content.append(getActionScript3WireType(fdp.getType()));
					content.append(", ");
					content.append(Integer.toString(fdp.getNumber()));
					content.append(");\n");
					content.append("\t\t\t\tcom.netease.protobuf.WriteUtils.write_");
					content.append(fdp.getType().name());
					content.append("(output, this.");
					appendLowerCamelCase(content, fdp.getName());
					content.append("[");
					appendLowerCamelCase(content, fdp.getName());
					content.append("$index]);\n");
					content.append("\t\t\t}\n");
				}
				break;
			}
		}
		content.append("\t\t\tfor (var fieldKey:* in this) {\n");
		if (scope.proto.getExtensionRangeCount() > 0) {
			content.append("\t\t\t\tsuper.writeExtensionOrUnknown(output, fieldKey);\n");
		} else {
			content.append("\t\t\t\tsuper.writeUnknown(output, fieldKey);\n");
		}
		content.append("\t\t\t}\n");
		content.append("\t\t}\n\n");
		content.append("\t\t/**\n\t\t *  @private\n\t\t */\n");
		content.append("\t\toverride com.netease.protobuf.used_by_generated_code final function readFromSlice(input:flash.utils.IDataInput, bytesAfterSlice:uint):void {\n");
		for (FieldDescriptorProto fdp : scope.proto.getFieldList()) {
			if (fdp.getType() == FieldDescriptorProto.Type.TYPE_GROUP) {
				System.err.println("Warning: Group is not supported.");
				continue;
			}
			switch (fdp.getLabel()) {
			case LABEL_OPTIONAL:
			case LABEL_REQUIRED:
				content.append("\t\t\tvar ");
				content.append(fdp.getName());
				content.append("$count:uint = 0;\n");
				break;
			}
		}
		content.append("\t\t\twhile (input.bytesAvailable > bytesAfterSlice) {\n");
		content.append("\t\t\t\tvar tag:uint = com.netease.protobuf.ReadUtils.read_TYPE_UINT32(input);\n");
		content.append("\t\t\t\tswitch (tag >> 3) {\n");
		for (FieldDescriptorProto fdp : scope.proto.getFieldList()) {
			if (fdp.getType() == FieldDescriptorProto.Type.TYPE_GROUP) {
				System.err.println("Warning: Group is not supported.");
				continue;
			}
			content.append("\t\t\t\tcase ");
			content.append(Integer.toString(fdp.getNumber()));
			content.append(":\n");
			switch (fdp.getLabel()) {
			case LABEL_OPTIONAL:
			case LABEL_REQUIRED:
				content.append("\t\t\t\t\tif (");
				content.append(fdp.getName());
				content.append("$count != 0) {\n");
				content.append("\t\t\t\t\t\tthrow new flash.errors.IOError('Bad data format: ");
				content.append(scope.proto.getName());
				content.append('.');
				appendLowerCamelCase(content, fdp.getName());
				content.append(" cannot be set twice.');\n");
				content.append("\t\t\t\t\t}\n");
				content.append("\t\t\t\t\t++");
				content.append(fdp.getName());
				content.append("$count;\n");
				if (fdp.getType() == FieldDescriptorProto.Type.TYPE_MESSAGE) {
					content.append("\t\t\t\t\tthis.");
					appendLowerCamelCase(content, fdp.getName());
					content.append(" = new ");
					content.append(getActionScript3Type(scope, fdp));
					content.append("();\n");
					content.append("\t\t\t\t\tcom.netease.protobuf.ReadUtils.read_TYPE_MESSAGE(input, this.");
					appendLowerCamelCase(content, fdp.getName());
					content.append(");\n");
				} else {
					content.append("\t\t\t\t\tthis.");
					appendLowerCamelCase(content, fdp.getName());
					content.append(" = com.netease.protobuf.ReadUtils.read_");
					content.append(fdp.getType().name());
					content.append("(input);\n");
				}
				break;
			case LABEL_REPEATED:
				switch (fdp.getType()) {
					case TYPE_DOUBLE:
					case TYPE_FLOAT:
					case TYPE_BOOL:
					case TYPE_INT32:
					case TYPE_FIXED32:
					case TYPE_UINT32:
					case TYPE_SFIXED32:
					case TYPE_SINT32:
					case TYPE_INT64:
					case TYPE_FIXED64:
					case TYPE_UINT64:
					case TYPE_SFIXED64:
					case TYPE_SINT64:
					case TYPE_ENUM:
						content.append("\t\t\t\t\tif ((tag & 7) == com.netease.protobuf.WireType.LENGTH_DELIMITED) {\n");
						content.append("\t\t\t\t\t\tcom.netease.protobuf.ReadUtils.readPackedRepeated(input, com.netease.protobuf.ReadUtils.read_");
						content.append(fdp.getType().name());
						content.append(", this.");
						appendLowerCamelCase(content, fdp.getName());
						content.append(");\n");
						content.append("\t\t\t\t\t\tbreak;\n");
						content.append("\t\t\t\t\t}\n");
				}
				if (fdp.getType() == FieldDescriptorProto.Type.TYPE_MESSAGE) {
					content.append("\t\t\t\t\tthis.");
					appendLowerCamelCase(content, fdp.getName());
					content.append(".push(");
					content.append("com.netease.protobuf.ReadUtils.read_TYPE_MESSAGE(input, ");
					content.append("new ");
					content.append(getActionScript3Type(scope, fdp));
					content.append("()));\n");
				} else {
					content.append("\t\t\t\t\tthis.");
					appendLowerCamelCase(content, fdp.getName());
					content.append(".push(com.netease.protobuf.ReadUtils.read_");
					content.append(fdp.getType().name());
					content.append("(input));\n");
				}
				break;
			}
			content.append("\t\t\t\t\tbreak;\n");
		}
		content.append("\t\t\t\tdefault:\n");
		if (scope.proto.getExtensionRangeCount() > 0) {
			content.append("\t\t\t\t\tsuper.readExtensionOrUnknown(extensionReadFunctions, input, tag);\n");
		} else {
			content.append("\t\t\t\t\tsuper.readUnknown(input, tag);\n");
		}
		content.append("\t\t\t\t\tbreak;\n");
		content.append("\t\t\t\t}\n");
		content.append("\t\t\t}\n");
		content.append("\t\t}\n\n");
		content.append("\t}\n");
	}
	private static void appendFieldDescriptorClass(StringBuilder content,
			FieldDescriptorProto fdp) {
		switch (fdp.getLabel()) {
		case LABEL_REQUIRED:
		case LABEL_OPTIONAL:
			break;
		case LABEL_REPEATED:
			content.append("Repeated");
			break;
		default:
			throw new IllegalArgumentException();
		}
		content.append("FieldDescriptor_");
		content.append(fdp.getType().name());
	}
	private static void appendFieldDescriptor(StringBuilder content,
			Scope<?> scope,
			FieldDescriptorProto fdp) {
		content.append("new ");
		appendFieldDescriptorClass(content, fdp);
		content.append("(");
		if (scope.parent == null) {
			appendQuotedString(content, fdp.getName());
		} else {
			appendQuotedString(content, scope.fullName + '.' + fdp.getName());
		}
		content.append(", ");
		if (fdp.hasExtendee()) {
			if (scope.proto instanceof DescriptorProto) {
				appendQuotedString(content, scope.fullName + '/' + fdp.getName().toUpperCase());
			} else {
				if (scope.parent == null) {
					appendQuotedString(content, fdp.getName().toUpperCase());
				} else {
					appendQuotedString(content, scope.fullName + '.' + fdp.getName().toUpperCase());
				}
			}
		} else {
			StringBuilder fieldName = new StringBuilder();
			appendLowerCamelCase(fieldName, fdp.getName());
			appendQuotedString(content, fieldName.toString());
		}
		content.append(", (");
		switch (fdp.getLabel()) {
		case LABEL_REQUIRED:
		case LABEL_OPTIONAL:
			content.append(Integer.toString(fdp.getNumber()));
			content.append(" << 3) | com.netease.protobuf.WireType.");
			content.append(getActionScript3WireType(fdp.getType()));
			break;
		case LABEL_REPEATED:
			if (fdp.hasOptions() && fdp.getOptions().getPacked()) {
				content.append(Integer.toString(fdp.getNumber()));
				content.append(" << 3) | com.netease.protobuf.WireType.LENGTH_DELIMITED");
			} else {
				content.append(Integer.toString(fdp.getNumber()));
				content.append(" << 3) | com.netease.protobuf.WireType.");
				content.append(getActionScript3WireType(fdp.getType()));
			}
			break;
		}
		switch (fdp.getType()) {
		case TYPE_MESSAGE:
			if (scope.proto instanceof DescriptorProto) {
				content.append(", function():Class { return ");
				content.append(getActionScript3LogicType(scope, fdp));
				content.append("; }");
			} else {
				content.append(", ");
				content.append(getActionScript3LogicType(scope, fdp));
			}
			break;
		case TYPE_ENUM:
			content.append(", ");
			content.append(getActionScript3LogicType(scope, fdp));
			break;
		}
		content.append(")");
	}
	private static void appendExtensionReadFunction(StringBuilder content,
			String tabs, 
			Scope<?> scope,
			FieldDescriptorProto fdp) {
		String extendee = scope.find(fdp.getExtendee()).fullName;
		switch (fdp.getLabel()) {
		case LABEL_REQUIRED:
		case LABEL_OPTIONAL:
			content.append(tabs);
			content.append(extendee);
			content.append(".extensionReadFunctions[(");
			content.append(Integer.toString(fdp.getNumber()));
			content.append(" << 3) | com.netease.protobuf.WireType.");
			content.append(getActionScript3WireType(fdp.getType()));
			content.append("] = ");
			content.append(fdp.getName().toUpperCase());
			content.append(".read;\n\n");
			break;
		case LABEL_REPEATED:
			content.append(tabs);
			content.append(extendee);
			content.append(".extensionReadFunctions[(");
			content.append(Integer.toString(fdp.getNumber()));
			content.append(" << 3) | com.netease.protobuf.WireType.");
			content.append(getActionScript3WireType(fdp.getType()));
			content.append("] = ");
			content.append(fdp.getName().toUpperCase());
			content.append(".readNonPacked;\n\n");
			switch (fdp.getType()) {
			case TYPE_MESSAGE:
			case TYPE_BYTES:
			case TYPE_STRING:
				break;
			default:
				content.append(tabs);
				content.append(extendee);
				content.append(".extensionReadFunctions[(");
				content.append(Integer.toString(fdp.getNumber()));
				content.append(" << 3) | com.netease.protobuf.WireType.LENGTH_DELIMITED] = ");
				content.append(fdp.getName().toUpperCase());
				content.append(".readPacked;\n\n");
				break;
			}
			break;
		}
	}
	private static void writeExtension(Scope<FieldDescriptorProto> scope,
			StringBuilder content, StringBuilder initializerContent) {
		initializerContent.append("import ");
		initializerContent.append(scope.fullName);
		initializerContent.append(";\n");
		initializerContent.append("if(!");
		initializerContent.append(scope.fullName);
		initializerContent.append(") throw new Error;\n");
		content.append("\timport com.netease.protobuf.*;\n");
		content.append("\timport com.netease.protobuf.fieldDescriptors.*;\n");
		String importType = getImportType(scope.parent, scope.proto);
		if (importType != null) {
			content.append("\timport ");
			content.append(importType);
			content.append(";\n");
		}
		String extendee = scope.parent.find(scope.proto.getExtendee()).fullName;
		content.append("\timport ");
		content.append(extendee);
		content.append(";\n");
		content.append("\t// @@protoc_insertion_point(imports)\n\n");

		content.append("\t// @@protoc_insertion_point(constant_metadata)\n");
		content.append("\t/**\n\t *  @private\n\t */\n");
		content.append("\tpublic const ");
		content.append(scope.proto.getName().toUpperCase());
		content.append(":");
		appendFieldDescriptorClass(content, scope.proto);
		content.append(" = ");
		appendFieldDescriptor(content, scope.parent, scope.proto);
		content.append(";\n\n");
		if (scope.proto.hasDefaultValue()) {
			content.append("\t");
			content.append(extendee);
			content.append(".prototype[");
			content.append(scope.proto.getName().toUpperCase());
			content.append("] = ");
			appendDefaultValue(content, scope.parent, scope.proto);
			content.append(";\n\n");
		}
		appendExtensionReadFunction(content, "\t", scope.parent, scope.proto);
	}

	private static void writeEnum(Scope<EnumDescriptorProto> scope,
			StringBuilder content) {
		content.append("\tpublic final class ");
		content.append(scope.proto.getName());
		content.append(" {\n");
		for (EnumValueDescriptorProto evdp : scope.proto.getValueList()) {
			content.append("\t\tpublic static const ");
			content.append(evdp.getName());
			content.append(":int = ");
			content.append(evdp.getNumber());
			content.append(";\n");
		}
		content.append("\t}\n");
	}
	@SuppressWarnings("unchecked")
	private static void writeFile(Scope<?> scope, StringBuilder content,
			StringBuilder initializerContent) {
		content.append("package ");
		content.append(scope.parent.fullName);
		content.append(" {\n");
		if (scope.proto instanceof DescriptorProto) {
			writeMessage((Scope<DescriptorProto>)scope, content,
					initializerContent);
		} else if (scope.proto instanceof EnumDescriptorProto) {
			writeEnum((Scope<EnumDescriptorProto>)scope, content);
		} else if (scope.proto instanceof FieldDescriptorProto) {
			Scope<FieldDescriptorProto> fdpScope =
					(Scope<FieldDescriptorProto>)scope;
			if (fdpScope.proto.getType() ==
					FieldDescriptorProto.Type.TYPE_GROUP) {
				System.err.println("Warning: Group is not supported.");
			} else {
				writeExtension(fdpScope, content, initializerContent);
			}
		} else {
			throw new IllegalArgumentException();
		}
		content.append("}\n");
	}
	
	@SuppressWarnings("unchecked")
	private static void writeFiles(Scope<?> root,
			CodeGeneratorResponse.Builder responseBuilder,
			StringBuilder initializerContent) {
		for (Map.Entry<String, Scope<?>> entry : root.children.entrySet()) {
			Scope<?> scope = entry.getValue();
			if (scope.export) {
				if (scope.proto instanceof ServiceDescriptorProto) {
					ServiceDescriptorProto serviceProto = (ServiceDescriptorProto)scope.proto;
					if (serviceProto.getOptions().getExtension(Options.as3ClientSideService) ||
						serviceProto.getOptions().getExtension(Options.as3ServerSideService)) {
						StringBuilder classContent = new StringBuilder();
						writeServiceClass((Scope<ServiceDescriptorProto>)scope, classContent);
						responseBuilder.addFile(
							CodeGeneratorResponse.File.newBuilder().
								setName(scope.fullName.replace('.', '/') + ".as").
								setContent(classContent.toString()).
							build()
						);
						StringBuilder interfaceContent = new StringBuilder();
						writeServiceInterface((Scope<ServiceDescriptorProto>)scope, interfaceContent);
						String[] servicePath = scope.fullName.split("\\.");
						StringBuilder sb = new StringBuilder();
						int i = 0; 
						for (; i < servicePath.length - 1; i++) {
							sb.append(servicePath[i]);
							sb.append('/');
						}
						sb.append('I');
						sb.append(servicePath[i]);
						sb.append(".as");

						responseBuilder.addFile(
							CodeGeneratorResponse.File.newBuilder().
								setName(sb.toString()).
								setContent(interfaceContent.toString()).
							build()
						);
					}
				}
                else
                {
                    StringBuilder content = new StringBuilder();
                    writeFile(scope, content, initializerContent);
                    responseBuilder.addFile(
                        CodeGeneratorResponse.File.newBuilder().
                            setName(scope.fullName.replace('.', '/') + ".as").
                            setContent(content.toString()).
                        build()
                    );
                }
			}
			writeFiles(scope, responseBuilder, initializerContent);
		}
	}
	private static void writeFiles(Scope<?> root,
			CodeGeneratorResponse.Builder responseBuilder) {
		StringBuilder initializerContent = new StringBuilder();
		initializerContent.append("{\n");
		writeFiles(root, responseBuilder, initializerContent);
		initializerContent.append("}\n");
		responseBuilder.addFile(
			CodeGeneratorResponse.File.newBuilder().
				setName("initializer.as.inc").
				setContent(initializerContent.toString()).
			build()
		);
	}
	private static void writeServiceClass(Scope<ServiceDescriptorProto> scope,
			StringBuilder content) {
		content.append("package ");
		content.append(scope.parent.fullName);
		content.append(" {\n");
		HashSet<String> importTypes = new HashSet<String>();
		for (MethodDescriptorProto mdp : scope.proto.getMethodList()) {
			importTypes.add(scope.find(mdp.getInputType()).fullName);
			if (scope.proto.getOptions().getExtension(Options.as3ClientSideService)) {
				importTypes.add(scope.find(mdp.getOutputType()).fullName);
			}
		}
		for (String importType : importTypes) {
			content.append("\timport ");
			content.append(importType);
			content.append(";\n");
		}
		content.append("\timport google.protobuf.*;\n");
		content.append("\timport flash.utils.*;\n");
		content.append("\timport com.netease.protobuf.*;\n");
		content.append("\t// @@protoc_insertion_point(imports)\n\n");
		content.append("\tpublic final class ");
		content.append(scope.proto.getName());
		if (scope.proto.getOptions().getExtension(Options.as3ClientSideService)) {
			content.append(" implements ");
			if (scope.parent.isRoot()) {
				content.append("I");
			} else {
				content.append(scope.parent.fullName);
				content.append(".I");
			}
			content.append(scope.proto.getName());
		}
		content.append(" {\n");

		if (scope.proto.hasOptions()) {
			content.append("\t\tpublic static const OPTIONS_BYTES:flash.utils.ByteArray = com.netease.protobuf.stringToByteArray(\"");
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			try {
				scope.proto.getOptions().writeTo(buffer);
			} catch (IOException e) {
				throw new IllegalStateException("ByteArrayOutputStream should not throw IOException!", e);
			}
			for (byte b : buffer.toByteArray()) {
				content.append("\\x");
				content.append(Character.forDigit((b & 0xF0) >>> 4, 16));
				content.append(Character.forDigit(b & 0x0F, 16));
			}
			content.append("\");\n\n");
			content.append("\t\tpublic static function getOptions():google.protobuf.ServiceOptions\n");
			content.append("\t\t{\n");
			content.append("\t\t\tOPTIONS_BYTES.position = 0;\n");
			content.append("\t\t\tconst options:google.protobuf.ServiceOptions = new google.protobuf.ServiceOptions();\n");
			content.append("\t\t\toptions.mergeFrom(OPTIONS_BYTES);\n\n");
			content.append("\t\t\treturn options;\n");
			content.append("\t\t}\n\n");
		}
		
		boolean hasMethodOptions = false;
		for (MethodDescriptorProto mdp : scope.proto.getMethodList()) {
			if (mdp.hasOptions()) {
				if (!hasMethodOptions) {
					hasMethodOptions = true;
					content.append("\t\tpublic static const OPTIONS_BYTES_BY_METHOD_NAME:Object =\n");
					content.append("\t\t{\n");
				} else {
					content.append(",\n");
				}
				content.append("\t\t\t\"");
				content.append(scope.fullName);
				content.append(".");
				content.append(mdp.getName());
				content.append("\" : stringToByteArray(\"");
				ByteArrayOutputStream buffer = new ByteArrayOutputStream();
				try {
					mdp.getOptions().writeTo(buffer);
				} catch (IOException e) {
					throw new IllegalStateException("ByteArrayOutputStream should not throw IOException!", e);
				}
				for (byte b : buffer.toByteArray()) {
					content.append("\\x");
					content.append(Character.forDigit((b & 0xF0) >>> 4, 16));
					content.append(Character.forDigit(b & 0x0F, 16));
				}
				content.append("\")");
			}
		}
		if (hasMethodOptions) {
				content.append("\n\t\t};\n\n");
		}

		if (scope.proto.getOptions().getExtension(Options.as3ServerSideService)) {
			content.append("\t\tpublic static const REQUEST_CLASSES_BY_METHOD_NAME:Object =\n");
			content.append("\t\t{\n");
			boolean comma = false;
			for (MethodDescriptorProto mdp : scope.proto.getMethodList()) {
				if (comma) {
					content.append(",\n");
				} else {
					comma = true;
				}
				content.append("\t\t\t\"");
				content.append(scope.fullName);
				content.append(".");
				content.append(mdp.getName());
				content.append("\" : ");
				content.append(scope.find(mdp.getInputType()).fullName);
			}
			content.append("\n\t\t};\n\n");


			content.append("\t\tpublic static function callMethod(service:");
			if (scope.parent.isRoot()) {
				content.append("I");
			} else {
				content.append(scope.parent.fullName);
				content.append(".I");
			}
			content.append(scope.proto.getName());
			content.append(", methodName:String, request:com.netease.protobuf.Message, responseHandler:Function):void {\n");
			content.append("\t\t\tswitch (methodName) {\n");
			for (MethodDescriptorProto mdp : scope.proto.getMethodList()) {
				content.append("\t\t\t\tcase \"");
				content.append(scope.fullName);
				content.append(".");
				content.append(mdp.getName());
				content.append("\":\n");
				content.append("\t\t\t\t{\n");
				content.append("\t\t\t\t\tservice.");
				appendLowerCamelCase(content, mdp.getName());
				content.append("(");
				content.append(scope.find(mdp.getInputType()).fullName);
				content.append("(request), responseHandler);\n");
				content.append("\t\t\t\t\tbreak;\n");
				content.append("\t\t\t\t}\n");
			}
			content.append("\t\t\t}\n");
			content.append("\t\t}\n\n");
		}

		if (scope.proto.getOptions().getExtension(Options.as3ClientSideService)) {
			content.append("\t\tpublic var sendFunction:Function;\n\n");
			for (MethodDescriptorProto mdp : scope.proto.getMethodList()) {
				content.append("\t\tpublic function ");
				appendLowerCamelCase(content, mdp.getName());
				content.append("(request:");
				content.append(scope.find(mdp.getInputType()).fullName);
				content.append(", responseHandler:Function):void {\n");
				content.append("\t\t\tsendFunction(\"");
				content.append(scope.fullName);
				content.append(".");
				content.append(mdp.getName());
				content.append("\", request, responseHandler, ");
				content.append(scope.find(mdp.getOutputType()).fullName);
				content.append(");\n");
				content.append("\t\t}\n\n");
			}
		}
		content.append("\t}\n");
        content.append("}\n");
	}
	
	private static void writeServiceInterface(
			Scope<ServiceDescriptorProto> scope,
			StringBuilder content) {
		content.append("package ");
		content.append(scope.parent.fullName);
		content.append(" {\n");
		HashSet<String> importTypes = new HashSet<String>();
		for (MethodDescriptorProto mdp : scope.proto.getMethodList()) {
			importTypes.add(scope.find(mdp.getInputType()).fullName);
		}
		for (String importType : importTypes) {
			content.append("\timport ");
			content.append(importType);
			content.append(";\n");
		}
		content.append("\t// @@protoc_insertion_point(imports)\n\n");
		content.append("\tpublic interface I");
		content.append(scope.proto.getName());
		content.append(" {\n\n");
		for (MethodDescriptorProto mdp : scope.proto.getMethodList()) {
			content.append("\t\tfunction ");
			appendLowerCamelCase(content, mdp.getName());
			content.append("(input:");
			content.append(scope.find(mdp.getInputType()).fullName);
            content.append(", done:Function):void;\n\n");
		}
		content.append("\t}\n");
        content.append("}\n");
	}

	public static void main(String[] args) throws IOException {
		ExtensionRegistry registry = ExtensionRegistry.newInstance();
		Options.registerAllExtensions(registry);
		CodeGeneratorRequest request = CodeGeneratorRequest.
				parseFrom(System.in, registry);
		CodeGeneratorResponse response;
		try {
			Scope<Object> root = buildScopeTree(request);
			CodeGeneratorResponse.Builder responseBuilder =
					CodeGeneratorResponse.newBuilder();
			writeFiles(root, responseBuilder);
			response = responseBuilder.build();
		} catch (Exception e) {
			// 出错，报告给 protoc ，然后退出
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			pw.flush();
			CodeGeneratorResponse.newBuilder().setError(sw.toString()).
					build().writeTo(System.out);
			System.out.flush();
			return;
		}
		response.writeTo(System.out);
		System.out.flush();
	}
}
