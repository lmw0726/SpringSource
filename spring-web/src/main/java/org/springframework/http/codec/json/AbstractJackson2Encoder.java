/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.http.codec.json;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.ByteArrayBuilder;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import org.reactivestreams.Publisher;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.CodecException;
import org.springframework.core.codec.EncodingException;
import org.springframework.core.codec.Hints;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageEncoder;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 提供对Jackson 2.9编码的支持方法的基类。对于非流式使用情况，为了性能原因，
 * {@link Flux} 元素在序列化之前会被收集到一个{@link List}中。
 *
 * @author Sebastien Deleuze
 * @author Arjen Poutsma
 * @since 5.0
 */
public abstract class AbstractJackson2Encoder extends Jackson2CodecSupport implements HttpMessageEncoder<Object> {

	/**
	 * 新的一行的分隔符
	 */
	private static final byte[] NEWLINE_SEPARATOR = {'\n'};

	/**
	 * 编码名称 —— 编码映射
	 */
	private static final Map<String, JsonEncoding> ENCODINGS;

	static {
		ENCODINGS = CollectionUtils.newHashMap(JsonEncoding.values().length);
		// 遍历JSON编码枚举
		for (JsonEncoding encoding : JsonEncoding.values()) {
			// 将编码名称和JSON编码缓存起来。
			ENCODINGS.put(encoding.getJavaName(), encoding);
		}
		// 将美国编码设置为UTF-8编码
		ENCODINGS.put("US-ASCII", JsonEncoding.UTF8);
	}

	/**
	 * 流媒体类型列表
	 */
	private final List<MediaType> streamingMediaTypes = new ArrayList<>(1);


	/**
	 * 使用Jackson {@link ObjectMapper}的构造函数。
	 *
	 * @param mapper    使用的ObjectMapper
	 * @param mimeTypes 支持的MIME类型
	 */
	protected AbstractJackson2Encoder(ObjectMapper mapper, MimeType... mimeTypes) {
		super(mapper, mimeTypes);
	}


	/**
	 * 配置“流式”媒体类型，对于这些类型，刷新操作应该自动执行而不是在流的末尾执行。
	 *
	 * @param mediaTypes 要配置的流式媒体类型列表
	 */
	public void setStreamingMediaTypes(List<MediaType> mediaTypes) {
		this.streamingMediaTypes.clear();
		this.streamingMediaTypes.addAll(mediaTypes);
	}


	@Override
	public boolean canEncode(ResolvableType elementType, @Nullable MimeType mimeType) {
		// 如果不支持该 MIME 类型，则返回 false
		if (!supportsMimeType(mimeType)) {
			return false;
		}
		// 如果 MIME 类型不为 null，且包含字符集信息
		if (mimeType != null && mimeType.getCharset() != null) {
			// 获取MIME 类型的字符集
			Charset charset = mimeType.getCharset();
			// 如果字符集不在支持的编码列表中，则返回 false
			if (!ENCODINGS.containsKey(charset.name())) {
				return false;
			}
		}
		// 选择合适的 对象映射器
		ObjectMapper mapper = selectObjectMapper(elementType, mimeType);
		// 如果未找到合适的 对象映射器，则返回 false
		if (mapper == null) {
			return false;
		}
		// 获取元素类型的类对象
		Class<?> clazz = elementType.toClass();
		// 如果元素类型是 字符串，则返回 false
		if (String.class.isAssignableFrom(elementType.resolve(clazz))) {
			return false;
		}
		// 如果元素类型是 对象，则返回 true
		if (Object.class == clazz) {
			return true;
		}
		// 如果日志级别不是调试模式
		if (!logger.isDebugEnabled()) {
			// 返回 对象映射器 是否能序列化该类
			return mapper.canSerialize(clazz);
		} else {
			// 如果日志级别是调试模式
			AtomicReference<Throwable> causeRef = new AtomicReference<>();
			// 检查 对象映射器 是否能序列化该类，并捕获可能的异常
			if (mapper.canSerialize(clazz, causeRef)) {
				return true;
			}
			// 如果不能序列化，记录警告日志
			logWarningIfNecessary(clazz, causeRef.get());
			return false;
		}
	}

	@Override
	public Flux<DataBuffer> encode(Publisher<?> inputStream, DataBufferFactory bufferFactory,
								   ResolvableType elementType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		Assert.notNull(inputStream, "'inputStream' must not be null");
		Assert.notNull(bufferFactory, "'bufferFactory' must not be null");
		Assert.notNull(elementType, "'elementType' must not be null");

		// 如果 输入流 是 Mono 类型
		if (inputStream instanceof Mono) {
			// 将 Mono 转换为 Flux，并对每个元素进行编码
			return Mono.from(inputStream)
					.map(value -> encodeValue(value, bufferFactory, elementType, mimeType, hints))
					.flux();
		} else {
			// 获取流媒体类型的分隔符
			byte[] separator = getStreamingMediaTypeSeparator(mimeType);
			if (separator != null) {
				// 如果是流式处理
				try {
					// 选择合适的 对象映射器
					ObjectMapper mapper = selectObjectMapper(elementType, mimeType);
					if (mapper == null) {
						// 如果没有找到合适的 对象映射器，抛出非法状态异常
						throw new IllegalStateException("No ObjectMapper for " + elementType);
					}
					// 创建 对象编写器
					ObjectWriter writer = createObjectWriter(mapper, elementType, mimeType, null, hints);
					// 创建字节数组构建器
					ByteArrayBuilder byteBuilder = new ByteArrayBuilder(writer.getFactory()._getBufferRecycler());
					// 从 Mime类型 获取JSON编码
					JsonEncoding encoding = getJsonEncoding(mimeType);
					// 创建 Json生成器
					JsonGenerator generator = mapper.getFactory().createGenerator(byteBuilder, encoding);
					// 创建序列编写器
					SequenceWriter sequenceWriter = writer.writeValues(generator);

					// 将 输入流 转换为 Flux，并对每个元素进行流式编码
					return Flux.from(inputStream)
							.map(value -> encodeStreamingValue(value, bufferFactory, hints, sequenceWriter, byteBuilder,
									separator))
							.doAfterTerminate(() -> {
								try {
									// 释放资源
									byteBuilder.release();
									// 关闭生成器
									generator.close();
								} catch (IOException ex) {
									logger.error("Could not close Encoder resources", ex);
								}
							});
				} catch (IOException ex) {
					// 处理 IO异常 并返回 Flux.error
					return Flux.error(ex);
				}
			} else {
				// 如果不是流式处理
				// 创建带有 元素类型 泛型的 List 类型
				ResolvableType listType = ResolvableType.forClassWithGenerics(List.class, elementType);
				// 将 输入流 转换为 List，并对 List 进行编码
				return Flux.from(inputStream)
						.collectList()
						.map(list -> encodeValue(list, bufferFactory, listType, mimeType, hints))
						.flux();
			}

		}
	}

	@Override
	public DataBuffer encodeValue(Object value, DataBufferFactory bufferFactory,
								  ResolvableType valueType, @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		Class<?> jsonView = null;
		FilterProvider filters = null;
		// 如果传入的值是 MappingJacksonValue 类型
		if (value instanceof MappingJacksonValue) {
			// 将值转换为 MappingJacksonValue 类型的容器
			MappingJacksonValue container = (MappingJacksonValue) value;
			// 获取容器中的实际值
			value = container.getValue();
			// 获取实际值的类型
			valueType = ResolvableType.forInstance(value);
			// 获取容器中的序列化视图
			jsonView = container.getSerializationView();
			// 获取容器中的过滤器
			filters = container.getFilters();
		}

		// 选择合适的 对象映射
		ObjectMapper mapper = selectObjectMapper(valueType, mimeType);
		// 如果没有找到合适的 对象映射，抛出异常
		if (mapper == null) {
			throw new IllegalStateException("No ObjectMapper for " + valueType);
		}

		// 创建 对象编写器
		ObjectWriter writer = createObjectWriter(mapper, valueType, mimeType, jsonView, hints);
		// 如果有过滤器，则将过滤器添加到 对象编写器 中
		if (filters != null) {
			writer = writer.with(filters);
		}

		// 创建 字节数组生成器，用于构建字节数组
		ByteArrayBuilder byteBuilder = new ByteArrayBuilder(writer.getFactory()._getBufferRecycler());
		try {
			// 从 Mime类型 获取 JSON编码
			JsonEncoding encoding = getJsonEncoding(mimeType);

			// 记录要序列化的值
			logValue(hints, value);

			// 使用 try-with-resources 创建 JsonGenerator，并将值写入生成器
			try (JsonGenerator generator = mapper.getFactory().createGenerator(byteBuilder, encoding)) {
				// 将值写入生成器
				writer.writeValue(generator, value);
				// 刷新生成器
				generator.flush();
			} catch (InvalidDefinitionException ex) {
				// 如果出现类型定义错误，抛出 编解码器异常
				throw new CodecException("Type definition error: " + ex.getType(), ex);
			} catch (JsonProcessingException ex) {
				// 如果出现 JSON 处理错误，抛出 编码异常
				throw new EncodingException("JSON encoding error: " + ex.getOriginalMessage(), ex);
			} catch (IOException ex) {
				// 如果出现 IO 错误，抛出 非法状态异常
				throw new IllegalStateException("Unexpected I/O error while writing to byte array builder", ex);
			}

			// 将字节数组转换为 数据缓冲区
			byte[] bytes = byteBuilder.toByteArray();
			DataBuffer buffer = bufferFactory.allocateBuffer(bytes.length);
			// 将字节数组写入数据缓冲区
			buffer.write(bytes);
			// 触碰 数据缓冲区，更新缓冲区状态
			Hints.touchDataBuffer(buffer, hints, logger);

			// 返回 数据缓冲区
			return buffer;
		} finally {
			// 释放 字节数组生成器
			byteBuilder.release();
		}
	}

	private DataBuffer encodeStreamingValue(Object value, DataBufferFactory bufferFactory, @Nullable Map<String, Object> hints,
											SequenceWriter sequenceWriter, ByteArrayBuilder byteArrayBuilder, byte[] separator) {

		// 记录要序列化的值
		logValue(hints, value);

		// 尝试写入值到 序列写入器
		try {
			sequenceWriter.write(value);
			// 刷新 写入器
			sequenceWriter.flush();
		} catch (InvalidDefinitionException ex) {
			// 如果出现类型定义错误，抛出 编解码器异常
			throw new CodecException("Type definition error: " + ex.getType(), ex);
		} catch (JsonProcessingException ex) {
			// 如果出现 JSON 处理错误，抛出 编码异常
			throw new EncodingException("JSON encoding error: " + ex.getOriginalMessage(), ex);
		} catch (IOException ex) {
			// 如果出现 IO 错误，抛出 非法状态异常
			throw new IllegalStateException("Unexpected I/O error while writing to byte array builder", ex);
		}

		// 将 字节数组生成器 中的字节数组转换为 字节数组
		byte[] bytes = byteArrayBuilder.toByteArray();
		// 重置 字节数组生成器
		byteArrayBuilder.reset();

		// 定义偏移量和长度
		int offset;
		int length;
		// 如果字节数组的第一个字节是空格
		if (bytes.length > 0 && bytes[0] == ' ') {
			// 序列写入器 在值之间写入了一个不必要的空格
			offset = 1;
			length = bytes.length - 1;
		} else {
			offset = 0;
			length = bytes.length;
		}
		// 分配一个新的 数据缓冲区，其长度为实际数据长度加上分隔符的长度
		DataBuffer buffer = bufferFactory.allocateBuffer(length + separator.length);
		// 将字节数组中的有效数据写入缓冲区
		buffer.write(bytes, offset, length);
		// 将分隔符写入缓冲区
		buffer.write(separator);
		// 更新缓冲区状态
		Hints.touchDataBuffer(buffer, hints, logger);

		// 返回缓冲区
		return buffer;
	}

	private void logValue(@Nullable Map<String, Object> hints, Object value) {
		// 如果日志记录未被禁止
		if (!Hints.isLoggingSuppressed(hints)) {
			// 使用 Log格式工具 进行调试日志记录
			LogFormatUtils.traceDebug(logger, traceOn -> {
				// 格式化要记录的值
				String formatted = LogFormatUtils.formatValue(value, !traceOn);
				// 返回格式化后的日志信息，并添加日志前缀
				return Hints.getLogPrefix(hints) + "Encoding [" + formatted + "]";
			});
		}
	}

	private ObjectWriter createObjectWriter(
			ObjectMapper mapper, ResolvableType valueType, @Nullable MimeType mimeType,
			@Nullable Class<?> jsonView, @Nullable Map<String, Object> hints) {

		// 获取 Java类型 对象
		JavaType javaType = getJavaType(valueType.getType(), null);

		// 如果 Json视图 为空，且 提示 不为空，则从 提示 中获取 Json视图
		if (jsonView == null && hints != null) {
			jsonView = (Class<?>) hints.get(Jackson2CodecSupport.JSON_VIEW_HINT);
		}

		// 根据 Json视图 是否为空选择合适的 对象编写器
		ObjectWriter writer = (jsonView != null ? mapper.writerWithView(jsonView) : mapper.writer());

		// 如果 Java类型 是容器类型，则设置 对象编写器 的序列化类型为 Java类型
		if (javaType.isContainerType()) {
			writer = writer.forType(javaType);
		}

		// 自定义 编写器 并返回
		return customizeWriter(writer, mimeType, valueType, hints);
	}

	protected ObjectWriter customizeWriter(ObjectWriter writer, @Nullable MimeType mimeType,
										   ResolvableType elementType, @Nullable Map<String, Object> hints) {

		return writer;
	}

	/**
	 * 返回用于给定MIME类型的分隔符。
	 * <p>默认情况下，如果给定的MIME类型是配置的{@link #setStreamingMediaTypes(List) streaming} MIME类型之一，
	 * 则此方法返回换行符{@code "\n"}。
	 *
	 * @param mimeType 调用方请求的MIME类型
	 * @return 要使用的分隔符，如果不兼容则返回{@code null}
	 * @since 5.3
	 */
	@Nullable
	protected byte[] getStreamingMediaTypeSeparator(@Nullable MimeType mimeType) {
		// 遍历流媒体类型列表
		for (MediaType streamingMediaType : this.streamingMediaTypes) {
			// 检查当前流媒体类型是否与给定的 Mime类型 兼容
			if (streamingMediaType.isCompatibleWith(mimeType)) {
				// 如果兼容，返回换行符分隔符
				return NEWLINE_SEPARATOR;
			}
		}
		// 如果没有找到兼容的流媒体类型，返回 null
		return null;
	}

	/**
	 * 确定用于给定MIME类型的JSON编码。
	 *
	 * @param mimeType 调用方请求的MIME类型
	 * @return 要使用的JSON编码（永远不会为{@code null}）
	 * @since 5.0.5
	 */
	protected JsonEncoding getJsonEncoding(@Nullable MimeType mimeType) {
		// 如果 Mime类型 不为空，并且其字符集不为 空
		if (mimeType != null && mimeType.getCharset() != null) {
			// 获取字符集
			Charset charset = mimeType.getCharset();
			// 根据字符集名称从 编码 映射中获取 Json编码
			JsonEncoding result = ENCODINGS.get(charset.name());
			// 如果找到对应的 Json编码，则返回该编码
			if (result != null) {
				return result;
			}
		}
		// 如果未找到匹配的 Json编码，则返回默认的 UTF-8 编码
		return JsonEncoding.UTF8;
	}


	// HttpMessageEncoder

	@Override
	public List<MimeType> getEncodableMimeTypes() {
		return getMimeTypes();
	}

	@Override
	public List<MimeType> getEncodableMimeTypes(ResolvableType elementType) {
		return getMimeTypes(elementType);
	}

	@Override
	public List<MediaType> getStreamingMediaTypes() {
		return Collections.unmodifiableList(this.streamingMediaTypes);
	}

	@Override
	public Map<String, Object> getEncodeHints(@Nullable ResolvableType actualType, ResolvableType elementType,
											  @Nullable MediaType mediaType, ServerHttpRequest request, ServerHttpResponse response) {

		return (actualType != null ? getHints(actualType) : Hints.none());
	}


	// Jackson2CodecSupport

	@Override
	protected <A extends Annotation> A getAnnotation(MethodParameter parameter, Class<A> annotType) {
		return parameter.getMethodAnnotation(annotType);
	}

}
