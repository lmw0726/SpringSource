/*
 * Copyright 2002-2019 the original author or authors.
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

import com.google.protobuf.CodedOutputStream;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.Message;
import com.google.protobuf.TextFormat;
import com.google.protobuf.util.JsonFormat;
import com.googlecode.protobuf.format.FormatFactory;
import com.googlecode.protobuf.format.ProtobufFormatter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ConcurrentReferenceHashMap;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

import static org.springframework.http.MediaType.*;

/**
 * 通过 <a href="https://developers.google.com/protocol-buffers/">Google Protocol Buffers</a> 读取和写入 {@link com.google.protobuf.Message com.google.protobuf.Messages} 的 {@code HttpMessageConverter}。
 *
 * <p>要生成 {@code Message} Java 类，需要安装 {@code protoc} 二进制文件。</p>
 *
 * <p>此转换器默认支持 {@code "application/x-protobuf"} 和 {@code "text/plain"}，使用官方 {@code "com.google.protobuf:protobuf-java"} 库。
 * 可以使用以下附加库之一在类路径上支持其他格式：
 * <ul>
 * <li>使用第三方库 {@code "com.googlecode.protobuf-java-format:protobuf-java-format"} 支持 {@code "application/json"}、{@code "application/xml"} 和 {@code "text/html"}（仅写入）</li>
 * <li>使用官方 {@code "com.google.protobuf:protobuf-java-util"} 支持 {@code "application/json"}，适用于 Protobuf 3（请参见 {@link ProtobufJsonFormatHttpMessageConverter} 进行可配置变体）</li>
 * </ul>
 * </p>
 *
 * <p>需要 Protobuf 2.6 或更高版本（以及格式化的 Protobuf Java Format 1.4 或更高版本）。此转换器将自适应到 Protobuf 3 和其默认的 {@code protobuf-java-util} JSON 格式，如果基于 Protobuf 2 的 {@code protobuf-java-format} 不可用；但是，对于更明确的 Protobuf 3 JSON 设置，请考虑使用 {@link ProtobufJsonFormatHttpMessageConverter}。</p>
 *
 * @author Alex Antonov
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @see FormatFactory
 * @see JsonFormat
 * @see ProtobufJsonFormatHttpMessageConverter
 * @since 4.1
 */
public class ProtobufHttpMessageConverter extends AbstractHttpMessageConverter<Message> {

	/**
	 * 转换器使用的默认字符集。
	 */
	public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	/**
	 * Protobuf 的媒体类型 {@code application/x-protobuf}。
	 */
	public static final MediaType PROTOBUF = new MediaType("application", "x-protobuf", DEFAULT_CHARSET);

	/**
	 * 包含 Protobuf 模式的 HTTP 头。
	 */
	public static final String X_PROTOBUF_SCHEMA_HEADER = "X-Protobuf-Schema";

	/**
	 * 包含 Protobuf 消息的 HTTP 头。
	 */
	public static final String X_PROTOBUF_MESSAGE_HEADER = "X-Protobuf-Message";

	/**
	 * 方法缓存
	 */
	private static final Map<Class<?>, Method> methodCache = new ConcurrentReferenceHashMap<>();

	/**
	 * 扩展注册表
	 */
	final ExtensionRegistry extensionRegistry;

	/**
	 * Protobuf格式支持
	 */
	@Nullable
	private final ProtobufFormatSupport protobufFormatSupport;


	/**
	 * 构造一个新的 {@code ProtobufHttpMessageConverter}。
	 */
	public ProtobufHttpMessageConverter() {
		this(null, null);
	}

	/**
	 * 构造一个新的 {@code ProtobufHttpMessageConverter}，可以通过初始化器注册消息扩展。
	 *
	 * @param registryInitializer 用于消息扩展的初始化器
	 * @deprecated 自 Spring Framework 5.1 起弃用，请使用 {@link #ProtobufHttpMessageConverter(ExtensionRegistry)} 替代
	 */
	@Deprecated
	public ProtobufHttpMessageConverter(@Nullable ExtensionRegistryInitializer registryInitializer) {
		this(null, null);
		if (registryInitializer != null) {
			// 如果注册初始化器不为空，则初始化扩展注册表
			registryInitializer.initializeExtensionRegistry(this.extensionRegistry);
		}
	}

	/**
	 * 构造一个新的 {@code ProtobufHttpMessageConverter}，使用指定的注册表来指定协议消息扩展。
	 *
	 * @param extensionRegistry 要填充的注册表
	 */
	public ProtobufHttpMessageConverter(ExtensionRegistry extensionRegistry) {
		this(null, extensionRegistry);
	}

	ProtobufHttpMessageConverter(@Nullable ProtobufFormatSupport formatSupport,
								 @Nullable ExtensionRegistry extensionRegistry) {

		if (formatSupport != null) {
			// 如果给定的格式支持对象不为 null，则使用它来设置 Protobuf格式支持
			this.protobufFormatSupport = formatSupport;
		} else if (ClassUtils.isPresent("com.googlecode.protobuf.format.FormatFactory", getClass().getClassLoader())) {
			// 如果类路径中存在 com.googlecode.protobuf.format.FormatFactory，则创建一个 Protobuf Java格式支持 对象
			this.protobufFormatSupport = new ProtobufJavaFormatSupport();
		} else if (ClassUtils.isPresent("com.google.protobuf.util.JsonFormat", getClass().getClassLoader())) {
			// 如果类路径中存在 com.google.protobuf.util.JsonFormat，则创建一个 Protobuf Java Util支持 对象
			this.protobufFormatSupport = new ProtobufJavaUtilSupport(null, null);
		} else {
			// 如果以上条件均不满足，则将 Protobuf格式支持 设置为 null
			this.protobufFormatSupport = null;
		}

		// 根据 Protobuf格式支持 的值设置支持的媒体类型
		setSupportedMediaTypes(Arrays.asList(this.protobufFormatSupport != null ?
				this.protobufFormatSupport.supportedMediaTypes() : new MediaType[] {PROTOBUF, TEXT_PLAIN}));

		// 如果 扩展注册表 为 null，则创建一个新的 扩展注册表 对象
		this.extensionRegistry = (extensionRegistry == null ? ExtensionRegistry.newInstance() : extensionRegistry);
	}


	@Override
	protected boolean supports(Class<?> clazz) {
		return Message.class.isAssignableFrom(clazz);
	}

	@Override
	protected MediaType getDefaultContentType(Message message) {
		return PROTOBUF;
	}

	@Override
	protected Message readInternal(Class<? extends Message> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		// 获取输入消息的内容类型
		MediaType contentType = inputMessage.getHeaders().getContentType();
		if (contentType == null) {
			// 如果内容类型为空，默认使用 PROTOBUF 类型
			contentType = PROTOBUF;
		}

		// 获取内容类型的字符集
		Charset charset = contentType.getCharset();
		// 如果字符集为空，则使用默认字符集
		if (charset == null) {
			charset = DEFAULT_CHARSET;
		}

		// 获取消息的 构建器 对象
		Message.Builder builder = getMessageBuilder(clazz);

		// 根据内容类型执行不同的消息解析操作
		if (PROTOBUF.isCompatibleWith(contentType)) {
			// 如果内容类型兼容于 Protocol Buffers，则使用 构建器 对象合并输入流中的数据
			builder.mergeFrom(inputMessage.getBody(), this.extensionRegistry);
		} else if (TEXT_PLAIN.isCompatibleWith(contentType)) {
			// 如果内容类型兼容于纯文本，创建一个输入流读取器
			InputStreamReader reader = new InputStreamReader(inputMessage.getBody(), charset);
			// 将输入流读取器的数据合并到 构建器 对象中
			TextFormat.merge(reader, this.extensionRegistry, builder);
		} else if (this.protobufFormatSupport != null) {
			// 如果支持自定义的 Protocol Buffers 格式化支持，则使用该支持进行合并操作
			this.protobufFormatSupport.merge(
					inputMessage.getBody(), charset, contentType, this.extensionRegistry, builder);
		}

		// 构建并返回最终的消息对象
		return builder.build();
	}

	/**
	 * 为给定的类创建一个新的 {@code Message.Builder} 实例。
	 * <p>此方法使用 ConcurrentReferenceHashMap 缓存方法查找结果。</p>
	 */
	private Message.Builder getMessageBuilder(Class<? extends Message> clazz) {
		try {
			// 尝试从方法缓存中获取方法
			Method method = methodCache.get(clazz);
			if (method == null) {
				// 如果方法缓存中不存在该方法，则通过反射获取类的 newBuilder 方法
				method = clazz.getMethod("newBuilder");
				// 将获取到的方法放入方法缓存中
				methodCache.put(clazz, method);
			}
			// 使用反射调用 newBuilder 方法，返回 Message.Builder 对象
			return (Message.Builder) method.invoke(clazz);
		} catch (Exception ex) {
			// 捕获异常，抛出 Http消息转换异常 异常
			throw new HttpMessageConversionException(
					"Invalid Protobuf Message type: no invocable newBuilder() method on " + clazz, ex);
		}
	}


	@Override
	protected boolean canWrite(@Nullable MediaType mediaType) {
		return (super.canWrite(mediaType) ||
				(this.protobufFormatSupport != null && this.protobufFormatSupport.supportsWriteOnly(mediaType)));
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void writeInternal(Message message, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {

		// 获取输出消息的内容类型
		MediaType contentType = outputMessage.getHeaders().getContentType();
		if (contentType == null) {
			// 如果内容类型为空，则使用默认内容类型
			contentType = getDefaultContentType(message);
			// 断言确保默认内容类型不为空
			Assert.state(contentType != null, "No content type");
		}

		// 获取内容类型的字符集，如果为空则使用默认字符集
		Charset charset = contentType.getCharset();
		if (charset == null) {
			charset = DEFAULT_CHARSET;
		}

		// 根据内容类型执行不同的输出操作
		if (PROTOBUF.isCompatibleWith(contentType)) {
			// 如果内容类型兼容于 Protocol Buffers，则执行 Protocol Buffers 的输出操作
			// 设置 Protocol Buffers 相关的头部信息
			setProtoHeader(outputMessage, message);
			// 创建 CodedOutputStream 对象
			CodedOutputStream codedOutputStream = CodedOutputStream.newInstance(outputMessage.getBody());
			// 将消息写入到 CodedOutputStream 中
			message.writeTo(codedOutputStream);
			// 刷新输出流
			codedOutputStream.flush();
		} else if (TEXT_PLAIN.isCompatibleWith(contentType)) {
			// 如果内容类型兼容于文本纯文本，则执行纯文本输出操作（已在 Protobuf 3.9 中弃用）
			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputMessage.getBody(), charset);
			// 使用 TextFormat 打印消息到输出流中
			TextFormat.print(message, outputStreamWriter);
			// 刷新输出流
			outputStreamWriter.flush();
			// 刷新消息体
			outputMessage.getBody().flush();
		} else if (this.protobufFormatSupport != null) {
			// 如果支持自定义的 Protocol Buffers 格式化支持，则使用该支持进行输出操作
			this.protobufFormatSupport.print(message, outputMessage.getBody(), contentType, charset);
			// 刷新消息体
			outputMessage.getBody().flush();
		}
	}

	/**
	 * 在响应内容类型为 "application/x-protobuf" 的消息时，设置 "X-Protobuf-*" HTTP 头部。
	 * <p><b>注意:</b> 在调用 <code>outputMessage.getBody()</code> 之前不应该调用此方法，因为它会写入 HTTP 头部（使它们只读）。</p>
	 *
	 * @param response 响应消息
	 * @param message  要发送的消息
	 */
	private void setProtoHeader(HttpOutputMessage response, Message message) {
		// 设置响应头 X-Protobuf-Schema，包含消息类型对应的文件名
		response.getHeaders().set(X_PROTOBUF_SCHEMA_HEADER, message.getDescriptorForType().getFile().getName());

		// 设置响应头 X-Protobuf-Message，包含消息类型的完全限定名
		response.getHeaders().set(X_PROTOBUF_MESSAGE_HEADER, message.getDescriptorForType().getFullName());
	}


	/**
	 * Protobuf格式支持接口。
	 */
	interface ProtobufFormatSupport {

		MediaType[] supportedMediaTypes();

		boolean supportsWriteOnly(@Nullable MediaType mediaType);

		void merge(InputStream input, Charset charset, MediaType contentType,
				   ExtensionRegistry extensionRegistry, Message.Builder builder)
				throws IOException, HttpMessageConversionException;

		void print(Message message, OutputStream output, MediaType contentType, Charset charset)
				throws IOException, HttpMessageConversionException;
	}


	/**
	 * {@link ProtobufFormatSupport} 的实现，当使用 {@code com.googlecode.protobuf.format.FormatFactory} 时使用。
	 */
	static class ProtobufJavaFormatSupport implements ProtobufFormatSupport {

		/**
		 * JSON 格式化器
		 */
		private final ProtobufFormatter jsonFormatter;

		/**
		 * XML 格式化器
		 */
		private final ProtobufFormatter xmlFormatter;

		/**
		 * HTML 格式化器
		 */
		private final ProtobufFormatter htmlFormatter;

		/**
		 * 构造函数，使用 {@code FormatFactory} 创建的 {@code ProtobufFormatter} 实例。
		 */
		public ProtobufJavaFormatSupport() {
			// 创建一个 格式工厂 实例
			FormatFactory formatFactory = new FormatFactory();

			// 使用 格式工厂 创建 JSON 格式化器
			this.jsonFormatter = formatFactory.createFormatter(FormatFactory.Formatter.JSON);

			// 使用 格式工厂 创建 XML 格式化器
			this.xmlFormatter = formatFactory.createFormatter(FormatFactory.Formatter.XML);

			// 使用 格式工厂 创建 HTML 格式化器
			this.htmlFormatter = formatFactory.createFormatter(FormatFactory.Formatter.HTML);
		}

		@Override
		public MediaType[] supportedMediaTypes() {
			return new MediaType[]{PROTOBUF, TEXT_PLAIN, APPLICATION_XML, APPLICATION_JSON};
		}

		@Override
		public boolean supportsWriteOnly(@Nullable MediaType mediaType) {
			return TEXT_HTML.isCompatibleWith(mediaType);
		}

		@Override
		public void merge(InputStream input, Charset charset, MediaType contentType,
						  ExtensionRegistry extensionRegistry, Message.Builder builder)
				throws IOException, HttpMessageConversionException {

			if (contentType.isCompatibleWith(APPLICATION_JSON)) {
				// 如果内容类型兼容于 JSON，则使用 JSON 格式化器执行消息合并操作
				this.jsonFormatter.merge(input, charset, extensionRegistry, builder);
			} else if (contentType.isCompatibleWith(APPLICATION_XML)) {
				// 如果内容类型兼容于 XML，则使用 XML 格式化器执行消息合并操作
				this.xmlFormatter.merge(input, charset, extensionRegistry, builder);
			} else {
				// 如果以上条件均不满足，则抛出异常，指示无法处理该内容类型的消息
				throw new HttpMessageConversionException(
						"protobuf-java-format does not support parsing " + contentType);
			}
		}

		@Override
		public void print(Message message, OutputStream output, MediaType contentType, Charset charset)
				throws IOException, HttpMessageConversionException {

			if (contentType.isCompatibleWith(APPLICATION_JSON)) {
				// 如果内容类型兼容于 JSON，则使用 JSON 格式化器打印消息到输出流中
				this.jsonFormatter.print(message, output, charset);
			} else if (contentType.isCompatibleWith(APPLICATION_XML)) {
				// 如果内容类型兼容于 XML，则使用 XML 格式化器打印消息到输出流中
				this.xmlFormatter.print(message, output, charset);
			} else if (contentType.isCompatibleWith(TEXT_HTML)) {
				// 如果内容类型兼容于 HTML，则使用 HTML 格式化器打印消息到输出流中
				this.htmlFormatter.print(message, output, charset);
			} else {
				// 如果以上条件均不满足，则抛出异常，指示无法处理该内容类型的消息
				throw new HttpMessageConversionException(
						"protobuf-java-format does not support printing " + contentType);
			}
		}
	}


	/**
	 * {@link ProtobufFormatSupport} 的实现，当使用 {@code com.google.protobuf.util.JsonFormat} 时使用。
	 */
	static class ProtobufJavaUtilSupport implements ProtobufFormatSupport {
		/**
		 * JSON格式解析器
		 */
		private final JsonFormat.Parser parser;

		/**
		 * JSON格式打印器
		 */
		private final JsonFormat.Printer printer;

		/**
		 * 构造函数，使用给定的 {@link JsonFormat.Parser} 和 {@link JsonFormat.Printer}，如果为 null，则使用默认实例。
		 *
		 * @param parser  JSON 解析器配置
		 * @param printer JSON 打印机配置
		 */
		public ProtobufJavaUtilSupport(@Nullable JsonFormat.Parser parser, @Nullable JsonFormat.Printer printer) {
			this.parser = (parser != null ? parser : JsonFormat.parser());
			this.printer = (printer != null ? printer : JsonFormat.printer());
		}

		@Override
		public MediaType[] supportedMediaTypes() {
			return new MediaType[]{PROTOBUF, TEXT_PLAIN, APPLICATION_JSON};
		}

		@Override
		public boolean supportsWriteOnly(@Nullable MediaType mediaType) {
			return false;
		}

		@Override
		public void merge(InputStream input, Charset charset, MediaType contentType,
						  ExtensionRegistry extensionRegistry, Message.Builder builder)
				throws IOException, HttpMessageConversionException {

			if (contentType.isCompatibleWith(MediaType.APPLICATION_JSON)) {
				// 如果内容类型兼容于 应用程序JSON
				InputStreamReader reader = new InputStreamReader(input, charset);
				// 使用 读取器 合并到 消息构建器 中
				this.parser.merge(reader, builder);
			} else {
				// 否则，抛出消息转换异常
				throw new HttpMessageConversionException(
						"protobuf-java-util does not support parsing " + contentType);
			}
		}

		@Override
		public void print(Message message, OutputStream output, MediaType contentType, Charset charset)
				throws IOException, HttpMessageConversionException {

			if (contentType.isCompatibleWith(MediaType.APPLICATION_JSON)) {
				// 如果内容类型兼容于 应用程序JSON
				OutputStreamWriter writer = new OutputStreamWriter(output, charset);
				// 将消息追加到 writer
				this.printer.appendTo(message, writer);
				// 刷新 writer
				writer.flush();
			} else {
				// 否则，抛出消息转换异常
				throw new HttpMessageConversionException(
						"protobuf-java-util does not support printing " + contentType);
			}
		}
	}

}
