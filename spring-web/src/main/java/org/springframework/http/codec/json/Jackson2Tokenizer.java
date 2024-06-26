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

package org.springframework.http.codec.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.async.ByteArrayFeeder;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.DefaultDeserializationContext;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import org.springframework.core.codec.DecodingException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.io.buffer.DataBufferUtils;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * {@link Function} 用于将任意大小的 JSON 流、字节数组块转换为 {@code Flux<TokenBuffer>}，
 * 其中每个 token buffer 都是一个格式良好的 JSON 对象。
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 5.0
 */
final class Jackson2Tokenizer {
	/**
	 * Json解析器
	 */
	private final JsonParser parser;

	/**
	 * 反序列化上下文
	 */
	private final DeserializationContext deserializationContext;

	/**
	 * 是否标记数组元素
	 */
	private final boolean tokenizeArrayElements;

	/**
	 * 是否强制使用BigDecimal
	 */
	private final boolean forceUseOfBigDecimal;

	/**
	 * 最大内存大小
	 */
	private final int maxInMemorySize;

	/**
	 * 对象深度
	 */
	private int objectDepth;

	/**
	 * 数组深度
	 */
	private int arrayDepth;

	/**
	 * 字节计数
	 */
	private int byteCount;

	/**
	 * 令牌缓冲区
	 */
	private TokenBuffer tokenBuffer;

	/**
	 * 输入喂入器
	 * TODO: 在 Jackson 支持时更改为 ByteBufferFeeder
	 * 查看 https://github.com/FasterXML/jackson-core/issues/478
	 */
	private final ByteArrayFeeder inputFeeder;


	private Jackson2Tokenizer(JsonParser parser, DeserializationContext deserializationContext,
							  boolean tokenizeArrayElements, boolean forceUseOfBigDecimal, int maxInMemorySize) {

		this.parser = parser;
		this.deserializationContext = deserializationContext;
		this.tokenizeArrayElements = tokenizeArrayElements;
		this.forceUseOfBigDecimal = forceUseOfBigDecimal;
		this.inputFeeder = (ByteArrayFeeder) this.parser.getNonBlockingInputFeeder();
		this.maxInMemorySize = maxInMemorySize;
		this.tokenBuffer = createToken();
	}


	private List<TokenBuffer> tokenize(DataBuffer dataBuffer) {
		// 获取数据缓冲区的可读字节数
		int bufferSize = dataBuffer.readableByteCount();

		// 创建一个字节数组
		byte[] bytes = new byte[bufferSize];

		// 将数据缓冲区中的数据读取到字节数组中
		dataBuffer.read(bytes);

		// 释放数据缓冲区
		DataBufferUtils.release(dataBuffer);

		try {
			// 将字节数组的数据提供给输入喂入器
			this.inputFeeder.feedInput(bytes, 0, bytes.length);

			// 解析 令牌缓冲区 并返回结果列表
			List<TokenBuffer> result = parseTokenBufferFlux();

			// 断言内存大小是否符合预期
			assertInMemorySize(bufferSize, result);

			// 返回解析后的 令牌缓冲区 列表
			return result;
		} catch (JsonProcessingException ex) {
			// 处理 JSON 解码异常
			throw new DecodingException("JSON decoding error: " + ex.getOriginalMessage(), ex);
		} catch (IOException ex) {
			// 处理 IO 异常
			throw Exceptions.propagate(ex);
		}
	}

	private Flux<TokenBuffer> endOfInput() {
		// 延迟创建 Flux 对象
		return Flux.defer(() -> {
			// 结束输入流
			this.inputFeeder.endOfInput();
			try {
				// 解析 令牌缓冲区 并 创建 Flux 对象
				return Flux.fromIterable(parseTokenBufferFlux());
			} catch (JsonProcessingException ex) {
				// 处理 JSON 解码异常
				throw new DecodingException("JSON decoding error: " + ex.getOriginalMessage(), ex);
			} catch (IOException ex) {
				// 处理 IO 异常
				throw Exceptions.propagate(ex);
			}
		});
	}

	private List<TokenBuffer> parseTokenBufferFlux() throws IOException {
		// 创建一个 令牌缓冲区 列表
		List<TokenBuffer> result = new ArrayList<>();

		// SPR-16151: Smile 数据格式使用 null 来分隔文档
		boolean previousNull = false;

		// 当解析器未关闭时循环解析 JSON 数据
		while (!this.parser.isClosed()) {
			// 获取下一个 JSON 令牌
			JsonToken token = this.parser.nextToken();

			// 如果令牌为 不可用
			// 或者为 null 且前一个令牌也为 null
			if (token == JsonToken.NOT_AVAILABLE ||
					(token == null && previousNull)) {
				// 跳出循环
				break;
			} else if (token == null) {
				// 如果前一个令牌为 null，将前一个token为空的标志设置为true
				previousNull = true;
				// 继续下一个循环
				continue;
			} else {
				// 否则将前一个token为空的标志设置为false
				previousNull = false;
			}

			// 更新对象和数组的深度
			updateDepth(token);

			// 根据配置选择处理 JSON 令牌的方式
			if (!this.tokenizeArrayElements) {
				// 如果不需要解析数组元素，则按普通方式处理令牌
				processTokenNormal(token, result);
			} else {
				// 如果需要解析数组元素，则按数组方式处理令牌
				processTokenArray(token, result);
			}
		}

		// 返回解析后的 令牌缓冲区 列表
		return result;
	}

	private void updateDepth(JsonToken token) {
		// 根据 JSON 令牌类型处理对象和数组的深度
		switch (token) {
			case START_OBJECT:
				// 如果令牌是 开始对象，增加对象深度
				this.objectDepth++;
				break;
			case END_OBJECT:
				// 如果令牌是 结束对象，减少对象深度
				this.objectDepth--;
				break;
			case START_ARRAY:
				// 如果令牌是 开始数组，增加数组深度
				this.arrayDepth++;
				break;
			case END_ARRAY:
				// 如果令牌是 结束数组，减少数组深度
				this.arrayDepth--;
				break;
		}
	}

	private void processTokenNormal(JsonToken token, List<TokenBuffer> result) throws IOException {
		// 将当前事件从解析器复制到 令牌缓冲区 中
		this.tokenBuffer.copyCurrentEvent(this.parser);

		// 如果令牌是结构结束或标量值，并且 对象深度 为 0 且 数组深度 为 0
		if ((token.isStructEnd() || token.isScalarValue()) && this.objectDepth == 0 && this.arrayDepth == 0) {
			// 将当前 令牌缓冲区 添加到结果列表中
			result.add(this.tokenBuffer);
			// 创建一个新的 令牌缓冲区
			this.tokenBuffer = createToken();
		}
	}

	private void processTokenArray(JsonToken token, List<TokenBuffer> result) throws IOException {
		// 如果当前的 JSON 令牌不是顶级数组令牌
		if (!isTopLevelArrayToken(token)) {
			// 将当前事件从解析器复制到 令牌缓冲区 中
			this.tokenBuffer.copyCurrentEvent(this.parser);
		}

		// 如果 对象深度 为 0 ，
		// 且 数组深度 为 0 或 1 ，
		// 且令牌是 结束对象 或 标量值
		if (this.objectDepth == 0 && (this.arrayDepth == 0 || this.arrayDepth == 1) &&
				(token == JsonToken.END_OBJECT || token.isScalarValue())) {
			// 将当前 令牌缓冲区 添加到结果列表中
			result.add(this.tokenBuffer);
			// 创建一个新的 令牌缓冲区
			this.tokenBuffer = createToken();
		}
	}

	private TokenBuffer createToken() {
		// 创建一个用于存储解析的 JSON 数据的 TokenBuffer 对象
		TokenBuffer tokenBuffer = new TokenBuffer(this.parser, this.deserializationContext);

		// 强制使用 BigDecimal 来处理数字类型的数据
		tokenBuffer.forceUseOfBigDecimal(this.forceUseOfBigDecimal);

		// 返回创建的 TokenBuffer 对象
		return tokenBuffer;
	}

	private boolean isTopLevelArrayToken(JsonToken token) {
		return this.objectDepth == 0 && ((token == JsonToken.START_ARRAY && this.arrayDepth == 1) ||
				(token == JsonToken.END_ARRAY && this.arrayDepth == 0));
	}

	private void assertInMemorySize(int currentBufferSize, List<TokenBuffer> result) {
		// 如果 最大内存大小 大于等于 0
		if (this.maxInMemorySize >= 0) {
			// 如果 结果 不为空
			if (!result.isEmpty()) {
				// 将 字节计数 重置为 0
				this.byteCount = 0;
			} else if (currentBufferSize > Integer.MAX_VALUE - this.byteCount) {
				// 如果 当前缓冲区大小 加上 字节计数 超过了 Integer.MAX_VALUE，抛出限制异常
				raiseLimitException();
			} else {
				// 累加当前缓冲区大小到 字节计数
				this.byteCount += currentBufferSize;
				// 如果 字节计数 超过了 最大内存大小，抛出限制异常
				if (this.byteCount > this.maxInMemorySize) {
					raiseLimitException();
				}
			}
		}
	}

	private void raiseLimitException() {
		throw new DataBufferLimitException(
				"Exceeded limit on max bytes per JSON object: " + this.maxInMemorySize);
	}


	/**
	 * 将给定的 {@code Flux<DataBuffer>} 标记化为 {@code Flux<TokenBuffer>}。
	 *
	 * @param dataBuffers          源数据缓冲区
	 * @param jsonFactory          用于创建 JSON 解析器的工厂
	 * @param objectMapper         当前的 ObjectMapper 实例
	 * @param tokenizeArrays       如果为 {@code true} 且 "顶级" JSON 对象是数组，则在接收到每个元素后立即单独返回
	 * @param forceUseOfBigDecimal 如果为 {@code true}，则在源中遇到的任何浮点值将使用 {@link java.math.BigDecimal}
	 * @param maxInMemorySize      最大内存大小，用于限制解析过程中使用的内存
	 * @return 解析后的 token 缓冲区流
	 */
	public static Flux<TokenBuffer> tokenize(Flux<DataBuffer> dataBuffers, JsonFactory jsonFactory,
											 ObjectMapper objectMapper, boolean tokenizeArrays, boolean forceUseOfBigDecimal, int maxInMemorySize) {

		try {
			// 创建一个非阻塞的字节数组解析器
			JsonParser parser = jsonFactory.createNonBlockingByteArrayParser();

			// 获取 ObjectMapper 的反序列化上下文
			DeserializationContext context = objectMapper.getDeserializationContext();

			// 如果反序列化上下文是 DefaultDeserializationContext 的实例
			if (context instanceof DefaultDeserializationContext) {
				// 创建一个新的反序列化上下文实例
				context = ((DefaultDeserializationContext) context).createInstance(
						objectMapper.getDeserializationConfig(), parser, objectMapper.getInjectableValues());
			}

			// 创建 Jackson2Tokenizer 对象，用于将 JSON 数据进行标记化处理
			Jackson2Tokenizer tokenizer =
					new Jackson2Tokenizer(parser, context, tokenizeArrays, forceUseOfBigDecimal, maxInMemorySize);

			// 将数据缓冲区进行连接并标记化处理，最后与 输入结束结果 进行连接
			return dataBuffers.concatMapIterable(tokenizer::tokenize).concatWith(tokenizer.endOfInput());
		} catch (IOException ex) {
			// 如果发生 IO异常，则返回一个错误的 Flux 对象
			return Flux.error(ex);
		}
	}

}
