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

package org.springframework.http.codec.support;

import org.springframework.core.SpringProperties;
import org.springframework.core.codec.*;
import org.springframework.http.codec.*;
import org.springframework.http.codec.json.*;
import org.springframework.http.codec.multipart.DefaultPartHttpMessageReader;
import org.springframework.http.codec.multipart.MultipartHttpMessageReader;
import org.springframework.http.codec.multipart.MultipartHttpMessageWriter;
import org.springframework.http.codec.multipart.SynchronossPartHttpMessageReader;
import org.springframework.http.codec.protobuf.ProtobufDecoder;
import org.springframework.http.codec.protobuf.ProtobufEncoder;
import org.springframework.http.codec.protobuf.ProtobufHttpMessageWriter;
import org.springframework.http.codec.xml.Jaxb2XmlDecoder;
import org.springframework.http.codec.xml.Jaxb2XmlEncoder;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * {@link CodecConfigurer.DefaultCodecs}的默认实现，作为客户端和服务器特定变体的基础。
 *
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 */
class BaseDefaultCodecs implements CodecConfigurer.DefaultCodecs, CodecConfigurer.DefaultCodecConfig {

	/**
	 * 由 {@code spring.xml.ignore} 系统属性控制的布尔标志，指示Spring是否忽略XML，即不初始化与XML相关的基础设施。
	 * <p>默认值为 "false"。
	 */
	private static final boolean shouldIgnoreXml = SpringProperties.getFlag("spring.xml.ignore");

	/**
	 * Jackson2是否存在
	 */
	static final boolean jackson2Present;

	/**
	 * Jackson2 Smile是否存在
	 */
	private static final boolean jackson2SmilePresent;

	/**
	 * Jaxb2是否存在
	 */
	private static final boolean jaxb2Present;

	/**
	 * protoBuf是否存在
	 */
	private static final boolean protobufPresent;

	/**
	 * 同步多部分是否存在
	 */
	static final boolean synchronossMultipartPresent;

	/**
	 * Netty ByteBuf是否存在
	 */
	static final boolean nettyByteBufPresent;

	/**
	 * Kotlin序列化Json是否存在
	 */
	static final boolean kotlinSerializationJsonPresent;

	static {
		ClassLoader classLoader = BaseCodecConfigurer.class.getClassLoader();
		jackson2Present = ClassUtils.isPresent("com.fasterxml.jackson.databind.ObjectMapper", classLoader) &&
				ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator", classLoader);
		jackson2SmilePresent = ClassUtils.isPresent("com.fasterxml.jackson.dataformat.smile.SmileFactory", classLoader);
		jaxb2Present = ClassUtils.isPresent("javax.xml.bind.Binder", classLoader);
		protobufPresent = ClassUtils.isPresent("com.google.protobuf.Message", classLoader);
		synchronossMultipartPresent = ClassUtils.isPresent("org.synchronoss.cloud.nio.multipart.NioMultipartParser", classLoader);
		nettyByteBufPresent = ClassUtils.isPresent("io.netty.buffer.ByteBuf", classLoader);
		kotlinSerializationJsonPresent = ClassUtils.isPresent("kotlinx.serialization.json.Json", classLoader);
	}

	/**
	 * Jackson2 Json解码器
	 */
	@Nullable
	private Decoder<?> jackson2JsonDecoder;

	/**
	 * Jackson2 Json编码器
	 */
	@Nullable
	private Encoder<?> jackson2JsonEncoder;

	/**
	 * Jackson2 Smile解码器
	 */
	@Nullable
	private Encoder<?> jackson2SmileEncoder;

	/**
	 * Jackson2 Smile编码器
	 */
	@Nullable
	private Decoder<?> jackson2SmileDecoder;

	/**
	 * protobuf解码器
	 */
	@Nullable
	private Decoder<?> protobufDecoder;

	/**
	 * protobuf编码器
	 */
	@Nullable
	private Encoder<?> protobufEncoder;

	/**
	 * Jaxb2解码器
	 */
	@Nullable
	private Decoder<?> jaxb2Decoder;

	/**
	 * Jaxb2编码器
	 */
	@Nullable
	private Encoder<?> jaxb2Encoder;

	/**
	 * kotlin序列化Json解码器
	 */
	@Nullable
	private Decoder<?> kotlinSerializationJsonDecoder;

	/**
	 * kotlin序列化Json编码器
	 */
	@Nullable
	private Encoder<?> kotlinSerializationJsonEncoder;

	/**
	 * 编解码消费函数
	 */
	@Nullable
	private Consumer<Object> codecConsumer;

	/**
	 * 最大内存大小
	 */
	@Nullable
	private Integer maxInMemorySize;

	/**
	 * 是否启用日志记录请求详细信息
	 */
	@Nullable
	private Boolean enableLoggingRequestDetails;

	/**
	 * 是否注册默认值
	 */
	private boolean registerDefaults = true;


	//要使用的默认读取器和写入器实例
	/**
	 * 类型读取器列表
	 */
	private final List<HttpMessageReader<?>> typedReaders = new ArrayList<>();

	/**
	 * 对象读取器列表
	 */
	private final List<HttpMessageReader<?>> objectReaders = new ArrayList<>();

	/**
	 * 类型写入器列表
	 */
	private final List<HttpMessageWriter<?>> typedWriters = new ArrayList<>();

	/**
	 * 对象写入器列表
	 */
	private final List<HttpMessageWriter<?>> objectWriters = new ArrayList<>();


	BaseDefaultCodecs() {
		// 初始化读取器
		initReaders();
		// 初始化写入器
		initWriters();
	}

	/**
	 * 重置并初始化类型化读取器和对象读取器。
	 *
	 * @since 5.3.3
	 */
	protected void initReaders() {
		// 初始化类型读取器
		initTypedReaders();
		// 初始化对象读取器
		initObjectReaders();
	}

	/**
	 * 重置并初始化类型化写入器和对象写入器。
	 *
	 * @since 5.3.3
	 */
	protected void initWriters() {
		// 初始化类型写入器
		initTypedWriters();
		// 初始化对象写入器
		initObjectWriters();
	}

	/**
	 * 创建给定 {@link BaseDefaultCodecs} 的深层副本。
	 */
	protected BaseDefaultCodecs(BaseDefaultCodecs other) {
		this.jackson2JsonDecoder = other.jackson2JsonDecoder;
		this.jackson2JsonEncoder = other.jackson2JsonEncoder;
		this.jackson2SmileDecoder = other.jackson2SmileDecoder;
		this.jackson2SmileEncoder = other.jackson2SmileEncoder;
		this.protobufDecoder = other.protobufDecoder;
		this.protobufEncoder = other.protobufEncoder;
		this.jaxb2Decoder = other.jaxb2Decoder;
		this.jaxb2Encoder = other.jaxb2Encoder;
		this.kotlinSerializationJsonDecoder = other.kotlinSerializationJsonDecoder;
		this.kotlinSerializationJsonEncoder = other.kotlinSerializationJsonEncoder;
		this.codecConsumer = other.codecConsumer;
		this.maxInMemorySize = other.maxInMemorySize;
		this.enableLoggingRequestDetails = other.enableLoggingRequestDetails;
		this.registerDefaults = other.registerDefaults;
		this.typedReaders.addAll(other.typedReaders);
		this.objectReaders.addAll(other.objectReaders);
		this.typedWriters.addAll(other.typedWriters);
		this.objectWriters.addAll(other.objectWriters);
	}

	@Override
	public void jackson2JsonDecoder(Decoder<?> decoder) {
		this.jackson2JsonDecoder = decoder;
		// 初始化对象读取器
		initObjectReaders();
	}

	@Override
	public void jackson2JsonEncoder(Encoder<?> encoder) {
		this.jackson2JsonEncoder = encoder;
		// 初始化对象写入器
		initObjectWriters();
		// 初始化类型写入器
		initTypedWriters();
	}

	@Override
	public void jackson2SmileDecoder(Decoder<?> decoder) {
		this.jackson2SmileDecoder = decoder;
		// 初始化对象读取器
		initObjectReaders();
	}

	@Override
	public void jackson2SmileEncoder(Encoder<?> encoder) {
		this.jackson2SmileEncoder = encoder;
		// 初始化对象写入器
		initObjectWriters();
		// 初始化类型写入器
		initTypedWriters();
	}

	@Override
	public void protobufDecoder(Decoder<?> decoder) {
		this.protobufDecoder = decoder;
		// 初始化类型读取器
		initTypedReaders();
	}

	@Override
	public void protobufEncoder(Encoder<?> encoder) {
		this.protobufEncoder = encoder;
		// 初始化类型写入器
		initTypedWriters();
	}

	@Override
	public void jaxb2Decoder(Decoder<?> decoder) {
		this.jaxb2Decoder = decoder;
		// 初始化对象读取器
		initObjectReaders();
	}

	@Override
	public void jaxb2Encoder(Encoder<?> encoder) {
		this.jaxb2Encoder = encoder;
		// 初始化对象写入器
		initObjectWriters();
	}

	@Override
	public void kotlinSerializationJsonDecoder(Decoder<?> decoder) {
		this.kotlinSerializationJsonDecoder = decoder;
		// 初始化对象读取器
		initObjectReaders();
	}

	@Override
	public void kotlinSerializationJsonEncoder(Encoder<?> encoder) {
		this.kotlinSerializationJsonEncoder = encoder;
		// 初始化对象写入器
		initObjectWriters();
	}

	@Override
	public void configureDefaultCodec(Consumer<Object> codecConsumer) {
		this.codecConsumer = (this.codecConsumer != null ?
				this.codecConsumer.andThen(codecConsumer) : codecConsumer);
		// 初始化读取器
		initReaders();
		// 初始化写入器
		initWriters();
	}

	@Override
	public void maxInMemorySize(int byteCount) {
		if (!ObjectUtils.nullSafeEquals(this.maxInMemorySize, byteCount)) {
			// 如果当前的 最大内存大小 和 字节数量 不相等
			this.maxInMemorySize = byteCount;
			// 初始化读取器
			initReaders();
		}
	}

	@Override
	@Nullable
	public Integer maxInMemorySize() {
		return this.maxInMemorySize;
	}

	@Override
	public void enableLoggingRequestDetails(boolean enable) {
		if (!ObjectUtils.nullSafeEquals(this.enableLoggingRequestDetails, enable)) {
			// 如果当前的 启用日志记录请求详细信息 和 传入的布尔值 不相等
			this.enableLoggingRequestDetails = enable;
			// 初始化读取器
			initReaders();
			// 初始化写入器
			initWriters();
		}
	}

	@Override
	@Nullable
	public Boolean isEnableLoggingRequestDetails() {
		return this.enableLoggingRequestDetails;
	}

	/**
	 * 委托方法，从 {@link BaseCodecConfigurer#registerDefaults} 中使用。
	 */
	void registerDefaults(boolean registerDefaults) {
		if (this.registerDefaults != registerDefaults) {
			this.registerDefaults = registerDefaults;
			// 初始化读取器
			initReaders();
			// 初始化写入器
			initWriters();
		}
	}


	/**
	 * 返回支持特定类型的读取器。
	 */
	final List<HttpMessageReader<?>> getTypedReaders() {
		return this.typedReaders;
	}

	/**
	 * 重置并初始化类型化读取器。
	 *
	 * @since 5.3.3
	 */
	protected void initTypedReaders() {
		// 清空 类型读取器 集合
		this.typedReaders.clear();
		// 如果没有注册默认的读取器，则直接返回
		if (!this.registerDefaults) {
			return;
		}
		// 向 类型读取器 添加 ByteArrayDecoder 的 DecoderHttpMessageReader
		addCodec(this.typedReaders, new DecoderHttpMessageReader<>(new ByteArrayDecoder()));
		// 向 类型读取器 添加 ByteBufferDecoder 的 DecoderHttpMessageReader
		addCodec(this.typedReaders, new DecoderHttpMessageReader<>(new ByteBufferDecoder()));
		// 向 类型读取器 添加 DataBufferDecoder 的 DecoderHttpMessageReader
		addCodec(this.typedReaders, new DecoderHttpMessageReader<>(new DataBufferDecoder()));
		// 如果存在 Netty 的 ByteBuf，则向 类型读取器 添加 NettyByteBufDecoder 的 DecoderHttpMessageReader
		if (nettyByteBufPresent) {
			addCodec(this.typedReaders, new DecoderHttpMessageReader<>(new NettyByteBufDecoder()));
		}
		// 向 类型读取器 添加 ResourceDecoder 的 ResourceHttpMessageReader
		addCodec(this.typedReaders, new ResourceHttpMessageReader(new ResourceDecoder()));
		// 向 类型读取器 添加 纯文本 的 DecoderHttpMessageReader
		addCodec(this.typedReaders, new DecoderHttpMessageReader<>(StringDecoder.textPlainOnly()));
		// 如果存在 ProtobufDecoder，则使用它，否则使用默认的 ProtobufDecoder，向 类型读取器 添加 ProtobufDecoder 的 DecoderHttpMessageReader
		if (protobufPresent) {
			addCodec(this.typedReaders, new DecoderHttpMessageReader<>(this.protobufDecoder != null ?
					(ProtobufDecoder) this.protobufDecoder : new ProtobufDecoder()));
		}
		// 向 类型读取器 添加 FormHttpMessageReader
		addCodec(this.typedReaders, new FormHttpMessageReader());

		// 扩展 类型读取器，可能根据客户端和服务器的不同做进一步扩展
		extendTypedReaders(this.typedReaders);
	}

	/**
	 * 初始化编解码器并将其添加到列表中。
	 *
	 * @since 5.1.13
	 */
	protected <T> void addCodec(List<T> codecs, T codec) {
		// 初始化编解码器
		initCodec(codec);
		// 将编解码器添加到列表
		codecs.add(codec);
	}

	/**
	 * 将应用程序配置的 {@link #maxInMemorySize()} 和 {@link #enableLoggingRequestDetails} 应用于给定的编解码器，
	 * 包括其可能包含的任何编解码器。
	 *
	 * @param codec 可能是编解码器的对象，可以为 null
	 */
	@SuppressWarnings("rawtypes")
	private void initCodec(@Nullable Object codec) {
		if (codec instanceof DecoderHttpMessageReader) {
			// 如果编解码器是DecoderHttpMessageReader的实例，则获取其内部的解码器
			codec = ((DecoderHttpMessageReader) codec).getDecoder();
		} else if (codec instanceof EncoderHttpMessageWriter) {
			// 如果 编解码对象 是EncoderHttpMessageWriter的实例，则获取其内部的编码器
			codec = ((EncoderHttpMessageWriter<?>) codec).getEncoder();
		}

		// 如果 编解码对象 为null，则直接返回
		if (codec == null) {
			return;
		}

		Integer size = this.maxInMemorySize;
		if (size != null) {
			// 如果 编解码对象 是AbstractDataBufferDecoder的实例，则设置其最大内存大小
			if (codec instanceof AbstractDataBufferDecoder) {
				((AbstractDataBufferDecoder<?>) codec).setMaxInMemorySize(size);
			}
			// 如果存在protobuf，并且 编解码对象 是ProtobufDecoder的实例，则设置其最大消息大小
			if (protobufPresent) {
				if (codec instanceof ProtobufDecoder) {
					((ProtobufDecoder) codec).setMaxMessageSize(size);
				}
			}
			// 如果存在 Kotlin序列化Json，并且 编解码对象 是KotlinSerializationJsonDecoder的实例，则设置其最大内存大小
			if (kotlinSerializationJsonPresent) {
				if (codec instanceof KotlinSerializationJsonDecoder) {
					((KotlinSerializationJsonDecoder) codec).setMaxInMemorySize(size);
				}
			}
			// 如果存在 Jackson2，并且 编解码对象 是AbstractJackson2Decoder的实例，则设置其最大内存大小
			if (jackson2Present) {
				if (codec instanceof AbstractJackson2Decoder) {
					((AbstractJackson2Decoder) codec).setMaxInMemorySize(size);
				}
			}
			// 如果存在 Jaxb2，并且不忽略XML，并且 编解码对象 是Jaxb2XmlDecoder的实例，则设置其最大内存大小
			if (jaxb2Present && !shouldIgnoreXml) {
				if (codec instanceof Jaxb2XmlDecoder) {
					((Jaxb2XmlDecoder) codec).setMaxInMemorySize(size);
				}
			}
			// 如果 编解码对象 是FormHttpMessageReader的实例，则设置其最大内存大小
			if (codec instanceof FormHttpMessageReader) {
				((FormHttpMessageReader) codec).setMaxInMemorySize(size);
			}
			// 如果 编解码对象 是ServerSentEventHttpMessageReader的实例，则设置其最大内存大小
			if (codec instanceof ServerSentEventHttpMessageReader) {
				((ServerSentEventHttpMessageReader) codec).setMaxInMemorySize(size);
			}
			// 如果 编解码对象 是DefaultPartHttpMessageReader的实例，则设置其最大内存大小
			if (codec instanceof DefaultPartHttpMessageReader) {
				((DefaultPartHttpMessageReader) codec).setMaxInMemorySize(size);
			}
			// 如果存在 同步多部分，并且 编解码对象 是SynchronossPartHttpMessageReader的实例，则设置其最大内存大小
			if (synchronossMultipartPresent) {
				if (codec instanceof SynchronossPartHttpMessageReader) {
					((SynchronossPartHttpMessageReader) codec).setMaxInMemorySize(size);
				}
			}
		}

		Boolean enable = this.enableLoggingRequestDetails;
		if (enable != null) {
			// 如果 编解码对象 是FormHttpMessageReader的实例，则设置其是否启用请求详情日志记录
			if (codec instanceof FormHttpMessageReader) {
				((FormHttpMessageReader) codec).setEnableLoggingRequestDetails(enable);
			}
			// 如果 编解码对象 是MultipartHttpMessageReader的实例，则设置其是否启用请求详情日志记录
			if (codec instanceof MultipartHttpMessageReader) {
				((MultipartHttpMessageReader) codec).setEnableLoggingRequestDetails(enable);
			}
			// 如果 编解码对象 是DefaultPartHttpMessageReader的实例，则设置其是否启用请求详情日志记录
			if (codec instanceof DefaultPartHttpMessageReader) {
				((DefaultPartHttpMessageReader) codec).setEnableLoggingRequestDetails(enable);
			}
			// 如果存在 同步多部分，并且 编解码对象 是SynchronossPartHttpMessageReader的实例，则设置其是否启用请求详情日志记录
			if (synchronossMultipartPresent) {
				if (codec instanceof SynchronossPartHttpMessageReader) {
					((SynchronossPartHttpMessageReader) codec).setEnableLoggingRequestDetails(enable);
				}
			}
			// 如果 编解码对象 是FormHttpMessageWriter的实例，则设置其是否启用请求详情日志记录
			if (codec instanceof FormHttpMessageWriter) {
				((FormHttpMessageWriter) codec).setEnableLoggingRequestDetails(enable);
			}
			// 如果 编解码对象 是MultipartHttpMessageWriter的实例，则设置其是否启用请求详情日志记录
			if (codec instanceof MultipartHttpMessageWriter) {
				((MultipartHttpMessageWriter) codec).setEnableLoggingRequestDetails(enable);
			}
		}

		// 如果 编解码消费函数 不为null，则将 编解码对象 传递给 编解码消费函数 处理
		if (this.codecConsumer != null) {
			this.codecConsumer.accept(codec);
		}

		// 递归处理嵌套的 编解码器列表
		if (codec instanceof MultipartHttpMessageReader) {
			// 如果 编解码对象 是MultipartHttpMessageReader的实例，则递归初始化其部分读取器
			initCodec(((MultipartHttpMessageReader) codec).getPartReader());
		} else if (codec instanceof MultipartHttpMessageWriter) {
			// 如果 编解码对象 是MultipartHttpMessageWriter的实例，则递归初始化其表单写入器
			initCodec(((MultipartHttpMessageWriter) codec).getFormWriter());
		} else if (codec instanceof ServerSentEventHttpMessageReader) {
			// 如果 编解码对象 是ServerSentEventHttpMessageReader的实例，则递归初始化其解码器
			initCodec(((ServerSentEventHttpMessageReader) codec).getDecoder());
		} else if (codec instanceof ServerSentEventHttpMessageWriter) {
			// 如果 编解码对象 是ServerSentEventHttpMessageWriter的实例，则递归初始化其编码器
			initCodec(((ServerSentEventHttpMessageWriter) codec).getEncoder());
		}
	}

	/**
	 * 客户端或服务器特定的类型化读取器的钩子。
	 *
	 * @param typedReaders 类型化读取器列表
	 */
	protected void extendTypedReaders(List<HttpMessageReader<?>> typedReaders) {
	}

	/**
	 * 返回对象读取器（JSON、XML、SSE）。
	 *
	 * @return 对象读取器列表
	 */
	final List<HttpMessageReader<?>> getObjectReaders() {
		return this.objectReaders;
	}

	/**
	 * 重置并初始化对象读取器。
	 *
	 * @since 5.3.3
	 */
	protected void initObjectReaders() {
		// 清空已有的 对象读取器列表
		this.objectReaders.clear();
		// 如果没有注册默认的解码器，则直接返回
		if (!this.registerDefaults) {
			return;
		}
		//如果存在 Kotlin序列化Json，则添加Kotlin Serialization JSON解码器到 对象读取器列表
		if (kotlinSerializationJsonPresent) {
			addCodec(this.objectReaders, new DecoderHttpMessageReader<>(getKotlinSerializationJsonDecoder()));
		}
		// 如果存在 Jackson2，则添加Jackson 2 JSON解码器到 对象读取器列表
		if (jackson2Present) {
			addCodec(this.objectReaders, new DecoderHttpMessageReader<>(getJackson2JsonDecoder()));
		}
		// 如果存在 Jackson2 Smile，则添加Jackson 2 Smile解码器到 对象读取器列表
		if (jackson2SmilePresent) {
			addCodec(this.objectReaders, new DecoderHttpMessageReader<>(this.jackson2SmileDecoder != null ?
					(Jackson2SmileDecoder) this.jackson2SmileDecoder : new Jackson2SmileDecoder()));
		}
		// 如果存在 Jaxb2，并且不忽略XML，则添加Jaxb2 XML解码器到 对象读取器列表
		if (jaxb2Present && !shouldIgnoreXml) {
			addCodec(this.objectReaders, new DecoderHttpMessageReader<>(this.jaxb2Decoder != null ?
					(Jaxb2XmlDecoder) this.jaxb2Decoder : new Jaxb2XmlDecoder()));
		}

		// 扩展特定于客户端与服务器的 对象读取器列表
		extendObjectReaders(this.objectReaders);
	}

	/**
	 * 客户端或服务器特定的对象读取器的钩子。
	 *
	 * @param objectReaders 对象读取器列表
	 */
	protected void extendObjectReaders(List<HttpMessageReader<?>> objectReaders) {
	}

	/**
	 * 返回需要位于所有其他读取器之后的读取器。
	 *
	 * @return 需要位于最后的读取器列表
	 */
	final List<HttpMessageReader<?>> getCatchAllReaders() {
		// 如果没有注册默认的解码器，则返回一个空的列表
		if (!this.registerDefaults) {
			return Collections.emptyList();
		}
		// 创建一个新的HttpMessageReader列表
		List<HttpMessageReader<?>> readers = new ArrayList<>();
		// 将所有MIME类型的StringDecoder解码器添加到 读取器列表中
		addCodec(readers, new DecoderHttpMessageReader<>(StringDecoder.allMimeTypes()));
		// 返回 读取器列表
		return readers;
	}

	/**
	 * 返回支持特定类型的所有写入器。
	 *
	 * @return 类型化写入器列表
	 */
	@SuppressWarnings({"rawtypes"})
	final List<HttpMessageWriter<?>> getTypedWriters() {
		return this.typedWriters;
	}

	/**
	 * 重置并初始化类型化写入器。
	 *
	 * @since 5.3.3
	 */
	protected void initTypedWriters() {
		// 清空已有的 类型写入器列表
		this.typedWriters.clear();
		// 如果没有注册默认的编码器，则直接返回
		if (!this.registerDefaults) {
			return;
		}
		// 将基础类型的编码器添加到 类型写入器列表 中
		this.typedWriters.addAll(getBaseTypedWriters());
		// 扩展特定于客户端与服务器的 类型写入器列表
		extendTypedWriters(this.typedWriters);
	}

	/**
	 * 返回仅包含“基本”类型化写入器的列表，即客户端和服务器共同使用的写入器。
	 *
	 * @return 基本类型化写入器列表
	 */
	final List<HttpMessageWriter<?>> getBaseTypedWriters() {
		// 如果没有注册默认的编码器，则返回一个空的列表
		if (!this.registerDefaults) {
			return Collections.emptyList();
		}
		// 创建一个新的HttpMessageWriter列表
		List<HttpMessageWriter<?>> writers = new ArrayList<>();
		// 将ByteArrayEncoder编码器添加到 写入器列表 中
		addCodec(writers, new EncoderHttpMessageWriter<>(new ByteArrayEncoder()));
		// 将ByteBufferEncoder编码器添加到 写入器列表 中
		addCodec(writers, new EncoderHttpMessageWriter<>(new ByteBufferEncoder()));
		// 将DataBufferEncoder编码器添加到 写入器列表 中
		addCodec(writers, new EncoderHttpMessageWriter<>(new DataBufferEncoder()));
		// 如果存在 Netty ByteBuf，则将NettyByteBufEncoder编码器添加到 写入器列表 中
		if (nettyByteBufPresent) {
			addCodec(writers, new EncoderHttpMessageWriter<>(new NettyByteBufEncoder()));
		}
		// 将ResourceHttpMessageWriter编码器添加到 写入器列表 中
		addCodec(writers, new ResourceHttpMessageWriter());
		// 将仅支持纯文本的CharSequenceEncoder编码器添加到 写入器列表 中
		addCodec(writers, new EncoderHttpMessageWriter<>(CharSequenceEncoder.textPlainOnly()));
		// 如果存在 protoBuf，则将ProtobufEncoder编码器添加到 写入器列表 中
		if (protobufPresent) {
			addCodec(writers, new ProtobufHttpMessageWriter(this.protobufEncoder != null ?
					(ProtobufEncoder) this.protobufEncoder : new ProtobufEncoder()));
		}
		// 返回 写入器列表 
		return writers;
	}

	/**
	 * 客户端或服务器特定的类型化写入器的钩子。
	 *
	 * @param typedWriters 类型化写入器列表
	 */
	protected void extendTypedWriters(List<HttpMessageWriter<?>> typedWriters) {
	}

	/**
	 * 返回对象写入器（JSON、XML、SSE）。
	 *
	 * @return 对象写入器列表
	 */
	final List<HttpMessageWriter<?>> getObjectWriters() {
		return this.objectWriters;
	}

	/**
	 * 重置并初始化对象写入器。
	 *
	 * @since 5.3.3
	 */
	protected void initObjectWriters() {
		// 清空已有的 对象写入器列表 
		this.objectWriters.clear();
		// 如果没有注册默认的编码器，则直接返回
		if (!this.registerDefaults) {
			return;
		}
		// 将基础对象的编码器添加到 对象写入器列表 中
		this.objectWriters.addAll(getBaseObjectWriters());
		// 扩展特定于客户端与服务器的 对象写入器列表 
		extendObjectWriters(this.objectWriters);
	}

	/**
	 * 返回仅包含“基本”对象写入器的列表，即客户端和服务器共同使用的写入器。
	 *
	 * @return 基本对象写入器列表
	 */
	final List<HttpMessageWriter<?>> getBaseObjectWriters() {
		// 创建一个新的HttpMessageWriter列表
		List<HttpMessageWriter<?>> writers = new ArrayList<>();
		// 如果存在 Kotlin序列化Json，则添加Kotlin Serialization JSON编码器到 写入器列表
		if (kotlinSerializationJsonPresent) {
			addCodec(writers, new EncoderHttpMessageWriter<>(getKotlinSerializationJsonEncoder()));
		}
		// 如果存在 Jackson2，则添加Jackson 2 JSON编码器到 写入器列表
		if (jackson2Present) {
			addCodec(writers, new EncoderHttpMessageWriter<>(getJackson2JsonEncoder()));
		}
		// 如果存在 Jackson2 Smile，则添加Jackson 2 Smile编码器到 写入器列表
		if (jackson2SmilePresent) {
			addCodec(writers, new EncoderHttpMessageWriter<>(this.jackson2SmileEncoder != null ?
					(Jackson2SmileEncoder) this.jackson2SmileEncoder : new Jackson2SmileEncoder()));
		}
		// 如果存在 Jaxb2，并且不忽略XML，则添加Jaxb2 XML编码器到 写入器列表
		if (jaxb2Present && !shouldIgnoreXml) {
			addCodec(writers, new EncoderHttpMessageWriter<>(this.jaxb2Encoder != null ?
					(Jaxb2XmlEncoder) this.jaxb2Encoder : new Jaxb2XmlEncoder()));
		}
		// 返回 写入器列表 
		return writers;
	}

	/**
	 * 客户端或服务器特定的对象写入器的钩子。
	 *
	 * @param objectWriters 对象写入器列表
	 */
	protected void extendObjectWriters(List<HttpMessageWriter<?>> objectWriters) {
	}

	/**
	 * 返回需要位于最后的写入器列表，即在所有其他写入器之后。
	 *
	 * @return 需要在最后的写入器列表
	 */
	List<HttpMessageWriter<?>> getCatchAllWriters() {
		// 如果没有注册默认的编码器，则返回一个空的列表
		if (!this.registerDefaults) {
			return Collections.emptyList();
		}
		// 创建一个新的HttpMessageWriter列表
		List<HttpMessageWriter<?>> result = new ArrayList<>();
		// 将支持所有MIME类型的CharSequenceEncoder编码器添加到 结果列表 中
		result.add(new EncoderHttpMessageWriter<>(CharSequenceEncoder.allMimeTypes()));
		// 返回 结果列表
		return result;
	}

	void applyDefaultConfig(BaseCodecConfigurer.DefaultCustomCodecs customCodecs) {
		// 应用默认配置到自定义编解码器的类型化读取器
		applyDefaultConfig(customCodecs.getTypedReaders());
		// 应用默认配置到自定义编解码器的对象读取器
		applyDefaultConfig(customCodecs.getObjectReaders());
		// 应用默认配置到自定义编解码器的类型化写入器
		applyDefaultConfig(customCodecs.getTypedWriters());
		// 应用默认配置到自定义编解码器的对象写入器
		applyDefaultConfig(customCodecs.getObjectWriters());
		// 对自定义编解码器的每个默认配置消费者应用当前对象
		customCodecs.getDefaultConfigConsumers().forEach(consumer -> consumer.accept(this));
	}

	private void applyDefaultConfig(Map<?, Boolean> readers) {
		// 对readers的每个条目进行流处理
		readers.entrySet().stream()
				// 过滤出值为true的条目
				.filter(Map.Entry::getValue)
				// 将过滤后的条目的键（编码器）映射出来
				.map(Map.Entry::getKey)
				// 对每个键（编码器）调用initCodec方法进行初始化
				.forEach(this::initCodec);
	}


	// 在子类中使用的访问器...

	protected Decoder<?> getJackson2JsonDecoder() {
		// 如果 Jackson2 Json解码器 为空，则创建一个新的Jackson2JsonDecoder实例
		if (this.jackson2JsonDecoder == null) {
			this.jackson2JsonDecoder = new Jackson2JsonDecoder();
		}
		// 返回 Jackson2 Json解码器
		return this.jackson2JsonDecoder;
	}

	protected Encoder<?> getJackson2JsonEncoder() {
		// 如果 Jackson2 Json编码器 为空，则创建一个新的Jackson2JsonEncoder实例
		if (this.jackson2JsonEncoder == null) {
			this.jackson2JsonEncoder = new Jackson2JsonEncoder();
		}
		// 返回 Jackson2 Json编码器
		return this.jackson2JsonEncoder;
	}

	protected Decoder<?> getKotlinSerializationJsonDecoder() {
		// 如果  kotlin序列化Json解码器 为空，则创建一个新的KotlinSerializationJsonDecoder实例
		if (this.kotlinSerializationJsonDecoder == null) {
			this.kotlinSerializationJsonDecoder = new KotlinSerializationJsonDecoder();
		}
		// 返回  kotlin序列化Json解码器
		return this.kotlinSerializationJsonDecoder;
	}

	protected Encoder<?> getKotlinSerializationJsonEncoder() {
		// 如果 kotlin序列化Json编码器 为空，则创建一个新的KotlinSerializationJsonEncoder实例
		if (this.kotlinSerializationJsonEncoder == null) {
			this.kotlinSerializationJsonEncoder = new KotlinSerializationJsonEncoder();
		}
		// 返回 kotlin序列化Json编码器
		return this.kotlinSerializationJsonEncoder;
	}

}
