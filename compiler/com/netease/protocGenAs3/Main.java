// vim: tabstop=4 shiftwidth=4

// Copyright (c) 2010 , 杨博 (Yang Bo) All rights reserved.
//
//         pop.atry@gmail.com
//
// Use, modification and distribution are subject to the "New BSD License"
// as listed at <url: http://www.opensource.org/licenses/bsd-license.php >.

package com.netease.protocGenAs3;
import static google.protobuf.compiler.Plugin.*;
import static com.google.protobuf.DescriptorProtos.*;
import java.io.*;
import java.util.regex.*;
import java.util.*;
public final class Main {
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
		if (dp.getExtensionCount() != 0) {
			System.err.println("Warning: Extension is not supported.");
		}
		if (dp.getExtensionRangeCount() != 0) {
			System.err.println("Warning: Extension is not supported.");
		}
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
			String packageName = fdp.hasPackage() ? fdp.getPackage() : "";
			Scope<?> packageScope = root.findOrCreate(packageName);
			boolean export = filesToGenerate.contains(fdp.getName());
			if (fdp.getServiceCount() != 0) {
				System.err.println("Warning: Service is not supported.");
			}
			if (fdp.getExtensionCount() != 0) {
				System.err.println("Warning: Extension is not supported.");
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
		case TYPE_INT64:
		case TYPE_UINT64:
		case TYPE_FIXED64:
		case TYPE_SFIXED64:
		case TYPE_SINT64:
			return "com.hurlant.math.BigInteger";
		case TYPE_ENUM:
			if (!fdp.hasDefaultValue()) {
				return null;
			}
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
	private static String getActionScript3Type(Scope<DescriptorProto> scope,
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
		case TYPE_UINT64:
		case TYPE_FIXED64:
		case TYPE_SFIXED64:
		case TYPE_SINT64:
			return "com.hurlant.math.BigInteger";
		case TYPE_STRING:
			return "String";
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
			throw new IllegalArgumentException();
		}
	}
	private static void appendDefaultValue(StringBuilder sb, Scope<?> scope,
			FieldDescriptorProto fdp) {
		String value = fdp.getDefaultValue();
		switch (fdp.getType()) {
		case TYPE_DOUBLE:
		case TYPE_FLOAT:
		case TYPE_INT32:
		case TYPE_FIXED32:
		case TYPE_SFIXED32:
		case TYPE_SINT32:
		case TYPE_UINT32:
		case TYPE_BOOL:
		case TYPE_INT64:
		case TYPE_UINT64:
		case TYPE_FIXED64:
		case TYPE_SFIXED64:
		case TYPE_SINT64:
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
			throw new IllegalArgumentException("Default value (" +
					value + ") for bytes type is not supported.");
		default:
			throw new IllegalArgumentException();
		}
	}
	private static void appendLowerCamelCase(StringBuilder sb, String s) {
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
			StringBuilder content) {
		content.append("\timport com.netease.protobuf.*;\n");
		content.append("\timport flash.utils.IExternalizable;\n");
		content.append("\timport flash.utils.IDataOutput;\n");
		content.append("\timport flash.utils.IDataInput;\n");
		content.append("\timport flash.errors.IOError;\n");
		for (FieldDescriptorProto fdp : scope.proto.getFieldList()) {
			String importType = getImportType(scope, fdp);
			if (importType != null) {
				content.append("\timport ");
				content.append(importType);
				content.append(";\n");
			}
		}
		content.append("\tpublic final class ");
		content.append(scope.proto.getName());
		content.append(" implements IExternalizable {\n");
		for (FieldDescriptorProto fdp : scope.proto.getFieldList()) {
			assert(fdp.hasLabel());
			switch (fdp.getLabel()) {
			case LABEL_OPTIONAL:
				if (isValueType(fdp.getType())) {
					content.append("\t\tprivate var has");
					appendUpperCamelCase(content, fdp.getName());
					content.append("_:Boolean = false;\n");
					content.append("\t\tpublic function get has");
					appendUpperCamelCase(content, fdp.getName());
					content.append("():Boolean {\n");
					content.append("\t\t\treturn has");
					appendUpperCamelCase(content, fdp.getName());
					content.append("_;\n");
					content.append("\t\t}\n");

					content.append("\t\tprivate var ");
					appendLowerCamelCase(content, fdp.getName());
					content.append("_:");
					content.append(getActionScript3Type(scope, fdp));
					if (fdp.hasDefaultValue()) {
						content.append(" = ");
						appendDefaultValue(content, scope, fdp);
					}
					content.append(";\n");

					content.append("\t\tpublic function set ");
					appendLowerCamelCase(content, fdp.getName());
					content.append("(value:");
					content.append(getActionScript3Type(scope, fdp));
					content.append("):void {\n");
					content.append("\t\t\thas");
					appendUpperCamelCase(content, fdp.getName());
					content.append("_ = true;\n");
					content.append("\t\t\t");
					appendLowerCamelCase(content, fdp.getName());
					content.append("_ = value;\n");
					content.append("\t\t}\n");

					content.append("\t\tpublic function get ");
					appendLowerCamelCase(content, fdp.getName());
					content.append("():");
					content.append(getActionScript3Type(scope, fdp));
					content.append(" {\n");
					content.append("\t\t\treturn ");
					appendLowerCamelCase(content, fdp.getName());
					content.append("_;\n");
					content.append("\t\t}\n");
				} else {
					content.append("\t\tpublic var ");
					appendLowerCamelCase(content, fdp.getName());
					content.append(":");
					content.append(getActionScript3Type(scope, fdp));
					if (fdp.hasDefaultValue()) {
						content.append(" = ");
						appendDefaultValue(content, scope, fdp);
					}
					content.append(";\n");
				}
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
				content.append(";\n");
				break;
			case LABEL_REPEATED:
				content.append("\t\t[ArrayElementType(\"");
				content.append(getActionScript3Type(scope, fdp));
				content.append("\")]\n");
				content.append("\t\tpublic var ");
				appendLowerCamelCase(content, fdp.getName());
				content.append(":Array = [];\n");
				break;
			default:
				throw new IllegalArgumentException();
			}
		}
		content.append("\t\tpublic function writeExternal(output:IDataOutput):void {\n");
		for (FieldDescriptorProto fdp : scope.proto.getFieldList()) {
			switch (fdp.getLabel()) {
			case LABEL_OPTIONAL:
				content.append("\t\t\tif (");
				if (isValueType(fdp.getType())) {
					content.append("has");
					appendUpperCamelCase(content, fdp.getName());
				} else {
					appendLowerCamelCase(content, fdp.getName());
					content.append(" != null");
				}
				content.append(") {\n");
				content.append("\t\t\t\tWriteUtils.writeTag(output, WireType.");
				content.append(getActionScript3WireType(fdp.getType()));
				content.append(", ");
				content.append(Integer.toString(fdp.getNumber()));
				content.append(");\n");
				content.append("\t\t\t\tWriteUtils.write_");
				content.append(fdp.getType().name());
				content.append("(output, ");
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
					throw new RuntimeException(
							"Packed repeated filed is not supported.");
				} else {
					content.append("\t\t\tfor each(var ");
					appendLowerCamelCase(content, fdp.getName());
					content.append("Element:");
					content.append(getActionScript3Type(scope, fdp));
					content.append(" in ");
					appendLowerCamelCase(content, fdp.getName());
					content.append(") {\n");
					content.append("\t\t\t\tWriteUtils.writeTag(output, WireType.");
					content.append(getActionScript3WireType(fdp.getType()));
					content.append(", ");
					content.append(Integer.toString(fdp.getNumber()));
					content.append(");\n");
					content.append("\t\t\t\tWriteUtils.write_");
					content.append(fdp.getType().name());
					content.append("(output, ");
					appendLowerCamelCase(content, fdp.getName());
					content.append("Element);\n");
					content.append("\t\t\t}\n");
				}
				break;
			}
		}
		content.append("\t\t}\n");
		content.append("\t\tpublic function readExternal(input:IDataInput):void {\n");
		for (FieldDescriptorProto fdp : scope.proto.getFieldList()) {
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
			content.append("\t\t\t\tcase ");
			content.append(Integer.toString(fdp.getNumber()));
			content.append(":\n");
			switch (fdp.getLabel()) {
			case LABEL_OPTIONAL:
			case LABEL_REQUIRED:
				content.append("\t\t\t\t\tif (");
				appendLowerCamelCase(content, fdp.getName());
				content.append("Count != 0) {\n");
				content.append("\t\t\t\t\t\tthrow new IOError();\n");
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
				content.append("\t\t\t\t\tbreak;\n");
				break;
			case LABEL_REPEATED:
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
				content.append("\t\t\t\t\tbreak;\n");
				break;
			}
		}
		content.append("\t\t\t\tdefault:\n");
		content.append("\t\t\t\t\tReadUtils.skip(input, tag);\n");
		content.append("\t\t\t\t}\n");
		content.append("\t\t\t}\n");
		for (FieldDescriptorProto fdp : scope.proto.getFieldList()) {
			switch (fdp.getLabel()) {
			case LABEL_REQUIRED:
				content.append("\t\t\tif (");
				appendLowerCamelCase(content, fdp.getName());
				content.append("Count != 1) {\n");
				content.append("\t\t\t\tthrow new IOError();\n");
				content.append("\t\t\t}\n");
				break;
			}
		}
		content.append("\t\t}\n");
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
	private static void writeFile(Scope<?> scope, StringBuilder content) {
		content.append("package ");
		content.append(scope.parent.fullName);
		content.append(" {\n");
		if (scope.proto instanceof DescriptorProto) {
			writeMessage((Scope<DescriptorProto>)scope, content);
		} else if (scope.proto instanceof EnumDescriptorProto) {
			writeEnum((Scope<EnumDescriptorProto>)scope, content);
		} else {
			throw new IllegalArgumentException();
		}
		content.append("}\n");
	}
	private static void writeFiles(Scope<?> root,
			CodeGeneratorResponse.Builder responseBuilder) {
		for (Map.Entry<String, Scope<?>> entry : root.children.entrySet()) {
			Scope<?> scope = entry.getValue();
			if (scope.export) {
				StringBuilder content = new StringBuilder();
				writeFile(scope, content);
				responseBuilder.addFile(
					CodeGeneratorResponse.File.newBuilder().
						setName(scope.fullName.replace('.', '/') + ".as").
						setContent(content.toString()).
					build()
				);
			}
			writeFiles(scope, responseBuilder);
		}
	}
	public static void main(String[] args) throws IOException {
		CodeGeneratorRequest request = CodeGeneratorRequest.
				parseFrom(System.in);
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
