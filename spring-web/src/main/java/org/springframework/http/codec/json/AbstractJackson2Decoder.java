/*
 * Copyright 2002-2021 the original author or authors.
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import org.reactivestreams.Publisher;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.CodecException;
import org.springframework.core.codec.DecodingException;
import org.springframework.core.codec.Hints;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.codec.HttpMessageDecoder;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Jackson 2.9解码的抽象基类，利用非阻塞解析。
 *
 * <p>兼容Jackson 2.9.7及更高版本。
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @author Arjen Poutsma
 * @see <a href="https://github.com/FasterXML/jackson-core/issues/57" target="_blank">添加对非阻塞（"async"）JSON解析的支持</a>
 * @since 5.0
 */
public abstract class AbstractJackson2Decoder extends Jackson2CodecSupport implements HttpMessageDecoder<Object> {

	/**
	 * 最大内存大小
	 */
	private int maxInMemorySize = 256 * 1024;


	/**
	 * 使用Jackson {@link ObjectMapper}的构造方法。
	 */
	protected AbstractJackson2Decoder(ObjectMapper mapper, MimeType... mimeTypes) {
		super(mapper, mimeTypes);
	}


	/**
	 * 设置此解码器可以缓冲的最大字节数。这可以是整个输入的大小（当作为整体解码时），或者是JSON流中一个顶级JSON对象的大小。
	 * 当超过限制时，将引发{@link DataBufferLimitException}。
	 * <p>默认情况下，此值设置为256K。
	 *
	 * @param byteCount 要缓冲的最大字节数，或者为-1表示无限制
	 * @since 5.1.11
	 */
	public void setMaxInMemorySize(int byteCount) {
		this.maxInMemorySize = byteCount;
	}

	/**
	 * 返回{@link #setMaxInMemorySize 设置的}字节限制。
	 *
	 * @return 配置的字节限制
	 * @since 5.1.11
	 */
	public int getMaxInMemorySize() {
		return this.maxInMemorySize;
	}


	@Override
	public boolean canDecode(ResolvableType elementType, @Nullable MimeType mimeType) {
		// 根据元素类型和 MIME 类型选择合适的 对象映射器
		ObjectMapper mapper = selectObjectMapper(elementType, mimeType);
		// 如果未找到合适的 对象映射器，则返回 false
		if (mapper == null) {
			return false;
		}
		// 构建 Java类型 对象以描述要反序列化的类型
		JavaType javaType = mapper.constructType(elementType.getType());
		// 跳过 字符串 类型的处理：字符序列解码器 + "*/*" 在此之后处理
		if (CharSequence.class.isAssignableFrom(elementType.toClass()) || !supportsMimeType(mimeType)) {
			// 如果元素类型是字符序列，或者不是支持的Mime类型，返回false
			return false;
		}
		// 如果日志级别不是调试级别
		if (!logger.isDebugEnabled()) {
			// 使用 canDeserialize 判断是否可以反序列化
			return mapper.canDeserialize(javaType);
		} else {
			AtomicReference<Throwable> causeRef = new AtomicReference<>();
			if (mapper.canDeserialize(javaType, causeRef)) {
				// 如果可以反序列化，返回 true
				return true;
			}
			// 如有必要，记录警告日志
			logWarningIfNecessary(javaType, causeRef.get());
			// 返回 false
			return false;
		}
	}

	@Override
	public Flux<Object> decode(Publisher<DataBuffer> input, ResolvableType elementType,
							   @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		// 根据元素类型和 MIME 类型选择合适的 对象映射器
		ObjectMapper mapper = selectObjectMapper(elementType, mimeType);
		// 如果未找到合适的 对象映射器，则抛出 非法状态异常
		if (mapper == null) {
			throw new IllegalStateException("No ObjectMapper for " + elementType);
		}

		// 检查是否强制使用 BigDecimal 处理浮点数
		boolean forceUseOfBigDecimal = mapper.isEnabled(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
		// 如果元素类型是 BigDecimal，则强制使用 BigDecimal
		if (BigDecimal.class.equals(elementType.getType())) {
			forceUseOfBigDecimal = true;
		}

		// 处理输入数据流，返回处理后的 Flux<DataBuffer>
		Flux<DataBuffer> processed = processInput(input, elementType, mimeType, hints);
		// 使用 Jackson2Tokenizer 对处理后的数据流进行标记化处理，返回 Flux<TokenBuffer>
		Flux<TokenBuffer> tokens = Jackson2Tokenizer.tokenize(processed, mapper.getFactory(), mapper,
				true, forceUseOfBigDecimal, getMaxInMemorySize());

		// 获取用于读取数据的 对象映射器
		ObjectReader reader = getObjectReader(mapper, elementType, hints);

		// 处理标记化后的 TokenBuffer 数据流
		return tokens.handle((tokenBuffer, sink) -> {
			try {
				// 使用 对象读取器 读取 TokenBuffer 中的数据并转换为相应对象
				Object value = reader.readValue(tokenBuffer.asParser(mapper));
				// 记录转换后的值及相关提示信息
				logValue(value, hints);
				// 如果转换后的值不为空
				if (value != null) {
					// 发送给下一个处理器
					sink.next(value);
				}
			} catch (IOException ex) {
				// 处理读取过程中的异常，并向下游发送错误信号
				sink.error(processException(ex));
			}
		});
	}

	/**
	 * 处理输入的发布者以生成Flux流。默认实现返回{@link Flux#from(Publisher)}，
	 * 但子类可以选择自定义此行为。
	 *
	 * @param input       要处理的{@code DataBuffer}输入流
	 * @param elementType 输出流中元素的预期类型
	 * @param mimeType    输入流关联的MIME类型（可选）
	 * @param hints       关于如何进行编码的附加信息
	 * @return 处理后的Flux流
	 * @since 5.1.14
	 */
	protected Flux<DataBuffer> processInput(Publisher<DataBuffer> input, ResolvableType elementType,
											@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		return Flux.from(input);
	}

	@Override
	public Mono<Object> decodeToMono(Publisher<DataBuffer> input, ResolvableType elementType,
									 @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		return DataBufferUtils.join(input, this.maxInMemorySize)
				// 将输入的 Flux<DataBuffer> 数据流合并为单个 DataBuffer
				.flatMap(dataBuffer -> Mono.justOrEmpty(decode(dataBuffer, elementType, mimeType, hints)));
	}

	@Override
	public Object decode(DataBuffer dataBuffer, ResolvableType targetType,
						 @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) throws DecodingException {
		// 根据目标类型和 MIME 类型选择合适的 对象映射器
		ObjectMapper mapper = selectObjectMapper(targetType, mimeType);
		// 如果未找到合适的 对象映射器，则抛出 非法状态异常
		if (mapper == null) {
			throw new IllegalStateException("No ObjectMapper for " + targetType);
		}

		try {
			// 获取用于读取数据的 对象读取器
			ObjectReader objectReader = getObjectReader(mapper, targetType, hints);
			// 使用 对象读取器 从 DataBuffer 的输入流中读取数据并转换为相应对象
			Object value = objectReader.readValue(dataBuffer.asInputStream());
			// 记录转换后的值及相关提示信息
			logValue(value, hints);
			return value;
		} catch (IOException ex) {
			// 处理读取过程中的异常，并将其封装并抛出
			throw processException(ex);
		} finally {
			// 释放 DataBuffer 资源
			DataBufferUtils.release(dataBuffer);
		}
	}

	private ObjectReader getObjectReader(
			ObjectMapper mapper, ResolvableType elementType, @Nullable Map<String, Object> hints) {

		Assert.notNull(elementType, "'elementType' must not be null");
		// 获取与 元素类型 相关的上下文类
		Class<?> contextClass = getContextClass(elementType);
		// 如果未找到上下文类，并且有提示信息
		if (contextClass == null && hints != null) {
			// 从提示信息中获取实际类型的上下文类
			contextClass = getContextClass((ResolvableType) hints.get(ACTUAL_TYPE_HINT));
		}
		// 根据 元素类型 的类型和上下文类获取对应的 Java类型
		JavaType javaType = getJavaType(elementType.getType(), contextClass);
		// 获取 JSON 视图配置
		Class<?> jsonView = (hints != null ? (Class<?>) hints.get(Jackson2CodecSupport.JSON_VIEW_HINT) : null);
		// 根据是否有 JSON 视图配置选择不同的 对象映射器 配置
		return jsonView != null ?
				mapper.readerWithView(jsonView).forType(javaType) :
				mapper.readerFor(javaType);
	}

	@Nullable
	private Class<?> getContextClass(@Nullable ResolvableType elementType) {
		// 根据 元素类型 获取对应的 方法参数 对象
		MethodParameter param = (elementType != null ? getParameter(elementType) : null);
		// 如果 方法参数 对象不为 null，则返回其所属的类
		return (param != null ? param.getContainingClass() : null);
	}

	private void logValue(@Nullable Object value, @Nullable Map<String, Object> hints) {
		// 如果未禁止日志记录
		if (!Hints.isLoggingSuppressed(hints)) {
			// 输出调试日志
			LogFormatUtils.traceDebug(logger, traceOn -> {
				// 格式化值
				String formatted = LogFormatUtils.formatValue(value, !traceOn);
				// 构造日志信息
				return Hints.getLogPrefix(hints) + "Decoded [" + formatted + "]";
			});
		}
	}

	private CodecException processException(IOException ex) {
		// 如果异常类型为 InvalidDefinitionException
		if (ex instanceof InvalidDefinitionException) {
			// 获取异常中的类型信息
			JavaType type = ((InvalidDefinitionException) ex).getType();
			// 抛出 编解码器异常，包含类型定义错误信息
			return new CodecException("Type definition error: " + type, ex);
		}
		// 如果异常类型为 JsonProcessingException
		if (ex instanceof JsonProcessingException) {
			// 获取原始的 JSON 处理异常消息
			String originalMessage = ((JsonProcessingException) ex).getOriginalMessage();
			// 抛出 解码异常，包含 JSON 解析错误信息
			return new DecodingException("JSON decoding error: " + originalMessage, ex);
		}
		// 其他情况，抛出 解码异常，表示在解析输入流时发生 I/O 错误
		return new DecodingException("I/O error while parsing input stream", ex);
	}


	// HttpMessageDecoder

	@Override
	public Map<String, Object> getDecodeHints(ResolvableType actualType, ResolvableType elementType,
											  ServerHttpRequest request, ServerHttpResponse response) {

		return getHints(actualType);
	}

	@Override
	public List<MimeType> getDecodableMimeTypes() {
		return getMimeTypes();
	}

	@Override
	public List<MimeType> getDecodableMimeTypes(ResolvableType targetType) {
		return getMimeTypes(targetType);
	}

	// Jackson2CodecSupport

	@Override
	protected <A extends Annotation> A getAnnotation(MethodParameter parameter, Class<A> annotType) {
		return parameter.getParameterAnnotation(annotType);
	}

}
