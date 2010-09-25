// vim: tabstop=4 shiftwidth=4

// Copyright (c) 2010 , NetEase.com,Inc. All rights reserved.
//
// Author: Yang Bo (pop.atry@gmail.com)
//
// Use, modification and distribution are subject to the "New BSD License"
// as listed at <url: http://www.opensource.org/licenses/bsd-license.php >.

package com.netease.protocGenAs3;
import static google.protobuf.compiler.Plugin.*;
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
		// 如果 proto instanceOf Scope ，则这个 Scope 对另一 Scope 的引用。
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
		private Scope<?> getRoot() {
			Scope<?> scope = this;
			while (scope.parent != null) {
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
			if (parent == null || parent.fullName == null ||
					parent.fullName.equals("")) {
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
		public static Scope<Object> root() {
			return new Scope<Object>(null, null, false, "");
		}
	}
	private static void addExtensionToScope(Scope<?> scope,
			FieldDescriptorProto efdp, boolean export) {
		StringBuilder sb = new StringBuilder();
		appendLowerCamelCase(sb, efdp.getName());
		scope.addChild(sb.toString(), efdp, export);
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
		Scope<Object> root = Scope.root();
		List<String> filesToGenerate = request.getFileToGenerateList();
		for (FileDescriptorProto fdp : request.getProtoFileList()) {
			Scope<?> packageScope = fdp.hasPackage() ?
					root.findOrCreate(fdp.getPackage()) : root;
			boolean export = filesToGenerate.contains(fdp.getName());
			if (fdp.getServiceCount() != 0) {
				System.err.println("Warning: Service is not supported.");
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
	@SuppressWarnings("fallthrough")
	private static String getImportType(Scope<?> scope,
			FieldDescriptorProto fdp) {
		switch (fdp.getType()) {
		case TYPE_ENUM:
			if (!fdp.hasDefaultValue()) {
				return null;
			}
			// fall-through
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
	private static String getActionScript3Type(Scope<?> scope,
			FieldDescriptorProto fdp) {
		switch (fdp.getType()) {
		case TYPE_DOUBLE:
		case TYPE_FLOAT:
			return "Number";
		case TYPE_INT32:
		case TYPE_FIXED32:
		case TYPE_SFIXED32:
		case TYPE_SINT32:
		case TYPE_ENUM:
			return "int";
		case TYPE_UINT32:
			return "uint";
		case TYPE_BOOL:
			return "Boolean";
		case TYPE_INT64:
		case TYPE_FIXED64:
		case TYPE_SFIXED64:
		case TYPE_SINT64:
			return "Int64";
		case TYPE_UINT64:
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
	private static void appendWriteFunction(StringBuilder content,
			Scope<?> scope, FieldDescriptorProto fdp) {
		switch (fdp.getLabel()) {
		case LABEL_REQUIRED:
			throw new IllegalArgumentException();
		case LABEL_OPTIONAL:
			content.append("Extension.writeFunction(WireType.");
			content.append(getActionScript3WireType(fdp.getType()));
			content.append(", ");
			break;
		case LABEL_REPEATED:
			if (fdp.hasOptions() && fdp.getOptions().getPacked()) {
				content.append("Extension.packedRepeatedWriteFunction(");
			} else {
				content.append("Extension.repeatedWriteFunction(WireType.");
				content.append(getActionScript3WireType(fdp.getType()));
				content.append(", ");
			}
			break;
		}
		content.append("WriteUtils.write_");
		content.append(fdp.getType().name());
		content.append(")");
	}
	private static void appendReadFunction(StringBuilder content,
			Scope<?> scope, FieldDescriptorProto fdp) {
		if (fdp.getType() == FieldDescriptorProto.Type.TYPE_MESSAGE) {
			switch (fdp.getLabel()) {
			case LABEL_REQUIRED:
				throw new IllegalArgumentException();
			case LABEL_OPTIONAL:
				content.append("Extension.messageReadFunction(");
				break;
			case LABEL_REPEATED:
				assert(!(fdp.hasOptions() && fdp.getOptions().getPacked()));
				content.append("Extension.repeatedMessageReadFunction(");
				break;
			}
			content.append(getActionScript3Type(scope, fdp));
			content.append(")");
		} else {
			switch (fdp.getLabel()) {
			case LABEL_REQUIRED:
				throw new IllegalArgumentException();
			case LABEL_OPTIONAL:
				content.append("Extension.readFunction(");
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
					content.append("Extension.packedRepeatedReadFunction(");
					break;
					default:
					content.append("Extension.repeatedReadFunction(");
				}
				break;
			}
			content.append("ReadUtils.read_");
			content.append(fdp.getType().name());
			content.append(")");
		}
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
		case TYPE_FIXED64:
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
			sb.append('\"');
			for (int i = 0; i < value.length(); i++) {
				char c = value.charAt(i);
				switch(c) {
				case '\"':
				case '\\':
					sb.append('\\');
				}
				sb.append(c);
			}
			sb.append('\"');
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
		boolean upper = false;
		for (int i = 0; i < s.length(); i++) {
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
		content.append("\timport flash.utils.IExternalizable;\n");
		content.append("\timport flash.utils.IDataInput;\n");
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
		if (scope.proto.hasOptions() &&
				scope.proto.getOptions().getExtension(Options.as3Bindable)) {
			content.append("\t[Bindable]\n");
		}
		content.append("\t// @@protoc_insertion_point(class_metadata)\n");
		if (scope.proto.getExtensionRangeCount() > 0) {
			content.append("\tpublic dynamic final class ");
			content.append(scope.proto.getName());
			content.append(" extends com.netease.protobuf.ExtensibleMessage implements flash.utils.IExternalizable {\n");
			content.append("\t\t[ArrayElementType(\"Function\")]\n");
			content.append("\t\tpublic static const extensionWriteFunctions:Array = [];\n\n");
			content.append("\t\t[ArrayElementType(\"Function\")]\n");
			content.append("\t\tpublic static const extensionReadFunctions:Array = [];\n\n");
		} else {
			content.append("\tpublic final class ");
			content.append(scope.proto.getName());
			content.append(" extends com.netease.protobuf.Message implements flash.utils.IExternalizable {\n");
		}
		for (FieldDescriptorProto efdp : scope.proto.getExtensionList()) {
			initializerContent.append("import ");
			initializerContent.append(scope.fullName);
			initializerContent.append(";\n");
			initializerContent.append("void(");
			initializerContent.append(scope.fullName);
			initializerContent.append(".");
			appendLowerCamelCase(initializerContent, efdp.getName());
			initializerContent.append(");\n");
			String extendee = scope.find(efdp.getExtendee()).fullName;
			content.append("\t\tpublic static const ");
			appendLowerCamelCase(content, efdp.getName());
			content.append(":uint = ");
			content.append(efdp.getNumber());
			content.append(";\n\n");
			content.append("\t\t{\n");
			content.append("\t\t\t");
			content.append(extendee);
			content.append(".extensionReadFunctions[");
			appendLowerCamelCase(content, efdp.getName());
			content.append("] = ");
			appendReadFunction(content, scope, efdp);
			content.append(";\n");
			content.append("\t\t\t");
			content.append(extendee);
			content.append(".extensionWriteFunctions[");
			appendLowerCamelCase(content, efdp.getName());
			content.append("] = ");
			appendWriteFunction(content, scope, efdp);
			content.append(";\n");
			content.append("\t\t}\n\n");
		}
		for (FieldDescriptorProto fdp : scope.proto.getFieldList()) {
			if (fdp.getType() == FieldDescriptorProto.Type.TYPE_GROUP) {
				System.err.println("Warning: Group is not supported.");
				continue;
			}
			assert(fdp.hasLabel());
			switch (fdp.getLabel()) {
			case LABEL_OPTIONAL:
				content.append("\t\tprivate var _");
				appendLowerCamelCase(content, fdp.getName());
				content.append(":");
				content.append(getActionScript3Type(scope, fdp));
				content.append(";\n\n");

				if (isValueType(fdp.getType())) {
					content.append("\t\tprivate var _has");
					appendUpperCamelCase(content, fdp.getName());
					content.append(":Boolean = false;\n\n");
				}
				content.append("\t\tpublic function remove");
				appendUpperCamelCase(content, fdp.getName());
				content.append("():void {\n");
				if (isValueType(fdp.getType())) {
					content.append("\t\t\t_has");
					appendUpperCamelCase(content, fdp.getName());
					content.append(" = false;\n");
					content.append("\t\t\t_");
					appendLowerCamelCase(content, fdp.getName());
					content.append(" = new ");
					content.append(getActionScript3Type(scope, fdp));
					content.append("();\n");
				} else {
					content.append("\t\t\t_");
					appendLowerCamelCase(content, fdp.getName());
					content.append(" = null;\n");
				}
				content.append("\t\t}\n\n");

				content.append("\t\tpublic function get has");
				appendUpperCamelCase(content, fdp.getName());
				content.append("():Boolean {\n");
				if (isValueType(fdp.getType())) {
					content.append("\t\t\treturn _has");
					appendUpperCamelCase(content, fdp.getName());
					content.append(";\n");
				} else {
					content.append("\t\t\treturn null != _");
					appendLowerCamelCase(content, fdp.getName());
					content.append(";\n");
				}
				content.append("\t\t}\n\n");

				content.append("\t\tpublic function set ");
				appendLowerCamelCase(content, fdp.getName());
				content.append("(value:");
				content.append(getActionScript3Type(scope, fdp));
				content.append("):void {\n");
				if (isValueType(fdp.getType())) {
					content.append("\t\t\t_has");
					appendUpperCamelCase(content, fdp.getName());
					content.append(" = true;\n");
				}
				content.append("\t\t\t_");
				appendLowerCamelCase(content, fdp.getName());
				content.append(" = value;\n");
				content.append("\t\t}\n\n");

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
				content.append("\t\t\treturn _");
				appendLowerCamelCase(content, fdp.getName());
				content.append(";\n");
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
		content.append("\t\t/**\n\t\t *  @private\n\t\t */\n\t\tpublic override function writeToBuffer(output:WritingBuffer):void {\n");
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
				content.append("\t\t\t\tWriteUtils.writeTag(output, WireType.");
				content.append(getActionScript3WireType(fdp.getType()));
				content.append(", ");
				content.append(Integer.toString(fdp.getNumber()));
				content.append(");\n");
				content.append("\t\t\t\tWriteUtils.write_");
				content.append(fdp.getType().name());
				content.append("(output, _");
				appendLowerCamelCase(content, fdp.getName());
				content.append(");\n");
				content.append("\t\t\t}\n");
				break;
			case LABEL_REQUIRED:
				content.append("\t\t\tWriteUtils.writeTag(output, WireType.");
				content.append(getActionScript3WireType(fdp.getType()));
				content.append(", ");
				content.append(Integer.toString(fdp.getNumber()));
				content.append(");\n");
				content.append("\t\t\tWriteUtils.write_");
				content.append(fdp.getType().name());
				content.append("(output, ");
				appendLowerCamelCase(content, fdp.getName());
				content.append(");\n");
				break;
			case LABEL_REPEATED:
				if (fdp.hasOptions() && fdp.getOptions().getPacked()) {
					content.append("\t\t\tif (");
					appendLowerCamelCase(content, fdp.getName());
					content.append(" != null && ");
					appendLowerCamelCase(content, fdp.getName());
					content.append(".length > 0) {\n");
					content.append("\t\t\t\tWriteUtils.writeTag(output, WireType.LENGTH_DELIMITED, ");
					content.append(Integer.toString(fdp.getNumber()));
					content.append(");\n");
					content.append("\t\t\t\tWriteUtils.writePackedRepeated(output, WriteUtils.write_");
					content.append(fdp.getType().name());
					content.append(", ");
					appendLowerCamelCase(content, fdp.getName());
					content.append(");\n");
					content.append("\t\t\t}\n");
				} else {
					content.append("\t\t\tfor (var ");
					appendLowerCamelCase(content, fdp.getName());
					content.append("Index:uint = 0; ");
					appendLowerCamelCase(content, fdp.getName());
					content.append("Index < ");
					appendLowerCamelCase(content, fdp.getName());
					content.append(".length; ++");
					appendLowerCamelCase(content, fdp.getName());
					content.append("Index) {\n");
					content.append("\t\t\t\tWriteUtils.writeTag(output, WireType.");
					content.append(getActionScript3WireType(fdp.getType()));
					content.append(", ");
					content.append(Integer.toString(fdp.getNumber()));
					content.append(");\n");
					content.append("\t\t\t\tWriteUtils.write_");
					content.append(fdp.getType().name());
					content.append("(output, ");
					appendLowerCamelCase(content, fdp.getName());
					content.append("[");
					appendLowerCamelCase(content, fdp.getName());
					content.append("Index]);\n");
					content.append("\t\t\t}\n");
				}
				break;
			}
		}
		if (scope.proto.getExtensionRangeCount() > 0) {
			content.append("\t\t\tfor (var tagNumber:* in this) {\n");
			content.append("\t\t\t\tvar writeFunction:Function = extensionWriteFunctions[tagNumber];\n");
			content.append("\t\t\t\tif (writeFunction == null) {\n");
			content.append("\t\t\t\t\tthrow new IOError('Attemp to write an unknown field.')\n");
			content.append("\t\t\t\t}\n");
			content.append("\t\t\t\twriteFunction(output, this, tagNumber);\n");
			content.append("\t\t\t}\n");
		}
		content.append("\t\t}\n\n");
		content.append("\t\tpublic function readExternal(input:IDataInput):void {\n");
		for (FieldDescriptorProto fdp : scope.proto.getFieldList()) {
			if (fdp.getType() == FieldDescriptorProto.Type.TYPE_GROUP) {
				System.err.println("Warning: Group is not supported.");
				continue;
			}
			switch (fdp.getLabel()) {
			case LABEL_OPTIONAL:
			case LABEL_REQUIRED:
				content.append("\t\t\tvar ");
				appendLowerCamelCase(content, fdp.getName());
				content.append("Count:uint = 0;\n");
				break;
			}
		}
		content.append("\t\t\twhile (input.bytesAvailable != 0) {\n");
		content.append("\t\t\t\tvar tag:Tag = ReadUtils.readTag(input);\n");
		content.append("\t\t\t\tswitch (tag.number) {\n");
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
				appendLowerCamelCase(content, fdp.getName());
				content.append("Count != 0) {\n");
				content.append("\t\t\t\t\t\tthrow new IOError('Bad data format: ");
				content.append(scope.proto.getName());
				content.append('.');
				appendLowerCamelCase(content, fdp.getName());
				content.append(" cannot be set twice.');\n");
				content.append("\t\t\t\t\t}\n");
				content.append("\t\t\t\t\t++");
				appendLowerCamelCase(content, fdp.getName());
				content.append("Count;\n");
				if (fdp.getType() == FieldDescriptorProto.Type.TYPE_MESSAGE) {
					content.append("\t\t\t\t\t");
					appendLowerCamelCase(content, fdp.getName());
					content.append(" = new ");
					content.append(getActionScript3Type(scope, fdp));
					content.append(";\n");
					content.append("\t\t\t\t\tReadUtils.read_TYPE_MESSAGE(input, ");
					appendLowerCamelCase(content, fdp.getName());
					content.append(");\n");
				} else {
					content.append("\t\t\t\t\t");
					appendLowerCamelCase(content, fdp.getName());
					content.append(" = ReadUtils.read_");
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
					content.append("\t\t\t\t\tif (tag.wireType == WireType.LENGTH_DELIMITED) {\n");
					content.append("\t\t\t\t\t\tReadUtils.readPackedRepeated(input, ReadUtils.read_");
					content.append(fdp.getType().name());
					content.append(", ");
					appendLowerCamelCase(content, fdp.getName());
					content.append(");\n");
					content.append("\t\t\t\t\t\tbreak;\n");
					content.append("\t\t\t\t\t}\n");
				}
				content.append("\t\t\t\t\t");
				appendLowerCamelCase(content, fdp.getName());
				content.append(".push(ReadUtils.read_");
				content.append(fdp.getType().name());
				content.append("(input");
				if (fdp.getType() == FieldDescriptorProto.Type.TYPE_MESSAGE) {
					content.append(", new ");
					content.append(getActionScript3Type(scope, fdp));
				}
				content.append("));\n");
				break;
			}
			content.append("\t\t\t\t\tbreak;\n");
		}
		content.append("\t\t\t\tdefault:\n");
		if (scope.proto.getExtensionRangeCount() > 0) {
			content.append("\t\t\t\t\tvar readFunction:Function = extensionReadFunctions[tag.number];\n");
			content.append("\t\t\t\t\tif (readFunction != null) {\n");
			content.append("\t\t\t\t\t\treadFunction(input, this, tag);\n");
			content.append("\t\t\t\t\t\tbreak;\n");
			content.append("\t\t\t\t\t}\n");
		}
		content.append("\t\t\t\t\tReadUtils.skip(input, tag.wireType);\n");
		content.append("\t\t\t\t}\n");
		content.append("\t\t\t}\n");
		for (FieldDescriptorProto fdp : scope.proto.getFieldList()) {
			if (fdp.getType() == FieldDescriptorProto.Type.TYPE_GROUP) {
				System.err.println("Warning: Group is not supported.");
				continue;
			}
			switch (fdp.getLabel()) {
			case LABEL_REQUIRED:
				content.append("\t\t\tif (");
				appendLowerCamelCase(content, fdp.getName());
				content.append("Count != 1) {\n");
				content.append("\t\t\t\tthrow new IOError('Bad data format: ");
				content.append(scope.proto.getName());
				content.append('.');
				appendLowerCamelCase(content, fdp.getName());
				content.append(" must be set.');\n");
				content.append("\t\t\t}\n");
				break;
			}
		}
		content.append("\t\t}\n\n");
		content.append("\t}\n");
	}
	private static void writeExtension(Scope<FieldDescriptorProto> scope,
			StringBuilder content, StringBuilder initializerContent) {
		initializerContent.append("import ");
		initializerContent.append(scope.fullName);
		initializerContent.append(";\n");
		initializerContent.append("void(");
		initializerContent.append(scope.fullName);
		initializerContent.append(");\n");
		content.append("\timport com.netease.protobuf.*;\n");
		if (scope.proto.getType() == FieldDescriptorProto.Type.TYPE_MESSAGE) {
			content.append("\timport ");
			content.append(
					scope.parent.find(scope.proto.getTypeName()).fullName);
			content.append(";\n");
		}
		String extendee = scope.parent.find(scope.proto.getExtendee()).fullName;
		content.append("\timport ");
		content.append(extendee);
		content.append(";\n");
		content.append("\tpublic const ");
		appendLowerCamelCase(content, scope.proto.getName());
		content.append(":uint = ");
		content.append(scope.proto.getNumber());
		content.append(";\n");
		content.append("\t{\n");
		content.append("\t\t");
		content.append(extendee);
		content.append(".extensionReadFunctions[");
		appendLowerCamelCase(content, scope.proto.getName());
		content.append("] = ");
		appendReadFunction(content, scope.parent, scope.proto);
		content.append(";\n");
		content.append("\t\t");
		content.append(extendee);
		content.append(".extensionWriteFunctions[");
		appendLowerCamelCase(content, scope.proto.getName());
		content.append("] = ");
		appendWriteFunction(content, scope.parent, scope.proto);
		content.append(";\n");
		content.append("\t}\n");
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
	private static void writeFiles(Scope<?> root,
			CodeGeneratorResponse.Builder responseBuilder,
			StringBuilder initializerContent) {
		for (Map.Entry<String, Scope<?>> entry : root.children.entrySet()) {
			Scope<?> scope = entry.getValue();
			if (scope.export) {
				StringBuilder content = new StringBuilder();
				writeFile(scope, content, initializerContent);
				responseBuilder.addFile(
					CodeGeneratorResponse.File.newBuilder().
						setName(scope.fullName.replace('.', '/') + ".as").
						setContent(content.toString()).
					build()
				);
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
			// 出错，报告给 protoc ，然后退出。
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
