/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.http.converter.protobuf;

import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.util.JsonFormat;

import org.springframework.lang.Nullable;

/**
 * {@link ProtobufHttpMessageConverter} 的子类，强制使用 Protobuf 3 及其官方库 {@code "com.google.protobuf:protobuf-java-util"} 处理 JSON。
 *
 * <p>最重要的是，此类通过 {@link JsonFormat} 实用工具允许自定义 JSON 解析器和打印机配置。
 * 如果没有提供特殊的解析器或打印机配置，则将使用默认的变体。
 *
 * <p>需要使用 Protobuf 3.x 和 {@code "com.google.protobuf:protobuf-java-util"} 3.x，
 * 推荐使用 3.3 或更高版本。
 *
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @see JsonFormat#parser()
 * @see JsonFormat#printer()
 * @see #ProtobufJsonFormatHttpMessageConverter(com.google.protobuf.util.JsonFormat.Parser, com.google.protobuf.util.JsonFormat.Printer)
 * @since 5.0
 */
public class ProtobufJsonFormatHttpMessageConverter extends ProtobufHttpMessageConverter {

	/**
	 * 使用默认的 {@link com.google.protobuf.util.JsonFormat.Parser JsonFormat.Parser} 实例、
	 * {@link com.google.protobuf.util.JsonFormat.Printer JsonFormat.Printer} 实例和 {@link ExtensionRegistry} 的构造函数。
	 */
	public ProtobufJsonFormatHttpMessageConverter() {
		this(null, null);
	}

	/**
	 * 使用给定的 {@link com.google.protobuf.util.JsonFormat.Parser JsonFormat.Parser} 实例、
	 * {@link com.google.protobuf.util.JsonFormat.Printer JsonFormat.Printer} 实例和默认的 {@link ExtensionRegistry} 的构造函数。
	 *
	 * @param parser  JSON 解析器配置
	 * @param printer JSON 打印机配置
	 */
	public ProtobufJsonFormatHttpMessageConverter(
			@Nullable JsonFormat.Parser parser, @Nullable JsonFormat.Printer printer) {

		this(parser, printer, (ExtensionRegistry) null);
	}

	/**
	 * 使用给定的 {@link com.google.protobuf.util.JsonFormat.Parser JsonFormat.Parser} 实例、
	 * {@link com.google.protobuf.util.JsonFormat.Printer JsonFormat.Printer} 实例和 {@link ExtensionRegistry} 的构造函数。
	 *
	 * @param parser            JSON 解析器配置
	 * @param printer           JSON 打印机配置
	 * @param extensionRegistry 消息扩展注册表
	 */
	public ProtobufJsonFormatHttpMessageConverter(
			@Nullable JsonFormat.Parser parser, @Nullable JsonFormat.Printer printer, @Nullable ExtensionRegistry extensionRegistry) {
		super(new ProtobufJavaUtilSupport(parser, printer), extensionRegistry);
	}

	/**
	 * 使用给定的 {@code JsonFormat.Parser} 和 {@code JsonFormat.Printer} 配置构造一个新的 {@code ProtobufJsonFormatHttpMessageConverter}，
	 * 还接受一个初始化器，允许注册消息扩展。
	 *
	 * @param parser              JSON 解析器配置
	 * @param printer             JSON 打印机配置
	 * @param registryInitializer 消息扩展注册器初始化器
	 * @deprecated 自 5.1 起，推荐使用 {@link #ProtobufJsonFormatHttpMessageConverter(com.google.protobuf.util.JsonFormat.Parser, com.google.protobuf.util.JsonFormat.Printer, ExtensionRegistry)}
	 */
	@Deprecated
	public ProtobufJsonFormatHttpMessageConverter(
			@Nullable JsonFormat.Parser parser, @Nullable JsonFormat.Printer printer, @Nullable ExtensionRegistryInitializer registryInitializer) {
		super(new ProtobufJavaUtilSupport(parser, printer), null);
		if (registryInitializer != null) {
			// 如果注册初始化器不为空，则初始化扩展注册表
			registryInitializer.initializeExtensionRegistry(this.extensionRegistry);
		}
	}

}
