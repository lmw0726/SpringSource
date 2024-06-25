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

package org.springframework.http.codec.protobuf;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.Message;
import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.DecodingException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferLimitException;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.MimeType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

/**
 * 使用 <a href="https://developers.google.com/protocol-buffers/">Google Protocol Buffers</a> 读取 {@link com.google.protobuf.Message} 的 {@code Decoder}。
 *
 * <p>通过 {@link #decode(Publisher, ResolvableType, MimeType, Map)} 方法反序列化的 Flux 预期使用 <a href="https://developers.google.com/protocol-buffers/docs/techniques?hl=en#streaming">分隔的 Protobuf 消息</a>，每条消息之前会指定消息的大小。通过 {@link #decodeToMono(Publisher, ResolvableType, MimeType, Map)} 方法反序列化的单个值预期使用常规的 Protobuf 消息格式（消息前没有附加大小信息）。
 *
 * <p>请注意，Protobuf 消息的默认实例生成空字节数组，因此通过网络发送的 {@code Mono.just(Msg.getDefaultInstance())} 将被反序列化为空的 {@link Mono}。
 *
 * <p>要生成 {@code Message} Java 类，您需要安装 {@code protoc} 二进制文件。
 *
 * <p>该解码器要求使用 Protobuf 3 或更高版本，并支持使用官方 {@code "com.google.protobuf:protobuf-java"} 库的 {@code "application/x-protobuf"} 和 {@code "application/octet-stream"} 格式。
 *
 * @author Sebastien Deleuze
 * @see ProtobufEncoder
 * @since 5.1
 */
public class ProtobufDecoder extends ProtobufCodecSupport implements Decoder<Message> {

	/**
	 * 默认的消息聚合最大大小。
	 */
	protected static final int DEFAULT_MESSAGE_MAX_SIZE = 256 * 1024;

	/**
	 * 类型 —— 方法 映射
	 */
	private static final ConcurrentMap<Class<?>, Method> methodCache = new ConcurrentReferenceHashMap<>();

	/**
	 * 扩展注册表
	 */
	private final ExtensionRegistry extensionRegistry;

	/**
	 * 最大消息大小
	 */
	private int maxMessageSize = DEFAULT_MESSAGE_MAX_SIZE;


	/**
	 * 构造一个新的 {@code ProtobufDecoder}。
	 */
	public ProtobufDecoder() {
		this(ExtensionRegistry.newInstance());
	}

	/**
	 * 构造一个新的 {@code ProtobufDecoder}，允许使用消息扩展注册器进行初始化。
	 *
	 * @param extensionRegistry 消息扩展注册器
	 */
	public ProtobufDecoder(ExtensionRegistry extensionRegistry) {
		Assert.notNull(extensionRegistry, "ExtensionRegistry must not be null");
		this.extensionRegistry = extensionRegistry;
	}


	/**
	 * 设置每条消息允许的最大大小。
	 * <p>默认情况下，设置为 256K。
	 *
	 * @param maxMessageSize 每条消息的最大大小，设置为 -1 表示无限制
	 */
	public void setMaxMessageSize(int maxMessageSize) {
		this.maxMessageSize = maxMessageSize;
	}

	/**
	 * 返回配置的消息大小限制 {@link #setMaxMessageSize}。
	 *
	 * @since 5.1.11
	 */
	public int getMaxMessageSize() {
		return this.maxMessageSize;
	}


	@Override
	public boolean canDecode(ResolvableType elementType, @Nullable MimeType mimeType) {
		return Message.class.isAssignableFrom(elementType.toClass()) && supportsMimeType(mimeType);
	}

	@Override
	public Flux<Message> decode(Publisher<DataBuffer> inputStream, ResolvableType elementType,
								@Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		// 创建一个MessageDecoderFunction对象
		MessageDecoderFunction decoderFunction =
				new MessageDecoderFunction(elementType, this.maxMessageSize);

		// 从 输入流 创建一个Flux流，通过 解码器函数 将其映射为可迭代对象
		return Flux.from(inputStream)
				.flatMapIterable(decoderFunction)
				.doOnTerminate(decoderFunction::discard);
	}

	@Override
	public Mono<Message> decodeToMono(Publisher<DataBuffer> inputStream, ResolvableType elementType,
									  @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) {

		// 将 输入流 中的数据缓冲区合并为一个DataBuffer，并限制最大消息大小为 最大消息大小
		return DataBufferUtils.join(inputStream, this.maxMessageSize)
				// 对合并后的DataBuffer进行映射操作，解码为指定的数据类型
				.map(dataBuffer -> decode(dataBuffer, elementType, mimeType, hints));
	}

	@Override
	public Message decode(DataBuffer dataBuffer, ResolvableType targetType,
						  @Nullable MimeType mimeType, @Nullable Map<String, Object> hints) throws DecodingException {

		try {
			// 根据 目标类型 获取消息构建器
			Message.Builder builder = getMessageBuilder(targetType.toClass());
			// 将 数据缓冲区 转换为ByteBuffer
			ByteBuffer buffer = dataBuffer.asByteBuffer();
			// 从ByteBuffer中读取数据，并使用 扩展注册表 解析数据到 消息构建器 中
			builder.mergeFrom(CodedInputStream.newInstance(buffer), this.extensionRegistry);
			// 构建并返回消息
			return builder.build();
		} catch (IOException ex) {
			// 捕获IO异常 并抛出 解码异常
			throw new DecodingException("I/O error while parsing input stream", ex);
		} catch (Exception ex) {
			// 捕获其他异常并抛出 解码异常，包含详细错误信息
			throw new DecodingException("Could not read Protobuf message: " + ex.getMessage(), ex);
		} finally {
			// 无论如何最终释放 数据缓冲区 资源
			DataBufferUtils.release(dataBuffer);
		}
	}


	/**
	 * 根据给定的类创建一个新的 {@code Message.Builder} 实例。
	 * <p>该方法使用 ConcurrentHashMap 缓存方法查找结果。
	 *
	 * @param clazz 要创建 {@code Message.Builder} 实例的类
	 * @return 给定类的新 {@code Message.Builder} 实例
	 * @throws Exception 如果无法获取方法或调用失败
	 */
	private static Message.Builder getMessageBuilder(Class<?> clazz) throws Exception {
		// 从 方法缓存 中获取 类型 对应的 方法 对象
		Method method = methodCache.get(clazz);
		// 如果 方法 为null
		if (method == null) {
			// 则从 类型 中获取名为"newBuilder"的方法
			method = clazz.getMethod("newBuilder");
			// 将其放入 方法缓存 中
			methodCache.put(clazz, method);
		}
		// 调用获取到的 方法 对象
		return (Message.Builder) method.invoke(clazz);
	}

	@Override
	public List<MimeType> getDecodableMimeTypes() {
		return getMimeTypes();
	}


	private class MessageDecoderFunction implements Function<DataBuffer, Iterable<? extends Message>> {
		/**
		 * 元素类型
		 */
		private final ResolvableType elementType;

		/**
		 * 最大消息大小
		 */
		private final int maxMessageSize;

		/**
		 * 输出数据缓存
		 */
		@Nullable
		private DataBuffer output;

		/**
		 * 要读取的消息字节大小
		 */
		private int messageBytesToRead;

		/**
		 * 偏移量
		 */
		private int offset;


		public MessageDecoderFunction(ResolvableType elementType, int maxMessageSize) {
			this.elementType = elementType;
			this.maxMessageSize = maxMessageSize;
		}


		@Override
		public Iterable<? extends Message> apply(DataBuffer input) {
			try {
				// 创建一个用于存储消息的列表
				List<Message> messages = new ArrayList<>();
				int remainingBytesToRead;
				int chunkBytesToRead;

				// 循环读取输入流中的数据
				do {
					// 如果 输出缓冲区为null
					if (this.output == null) {
						// 如果读取不到输入消息大小，直接返回消息列表
						if (!readMessageSize(input)) {
							return messages;
						}
						// 如果最大消息大小大于0，要读取的消息字节大小 大于 最大消息大小
						if (this.maxMessageSize > 0 && this.messageBytesToRead > this.maxMessageSize) {
							// 抛出异常
							throw new DataBufferLimitException(
									"The number of bytes to read for message " +
											"(" + this.messageBytesToRead + ") exceeds " +
											"the configured limit (" + this.maxMessageSize + ")");
						}
						// 根据消息大小分配输出缓冲区
						this.output = input.factory().allocateBuffer(this.messageBytesToRead);
					}

					// 计算本次要读取的字节数
					chunkBytesToRead = Math.min(this.messageBytesToRead, input.readableByteCount());
					// 更新剩余字节数
					remainingBytesToRead = input.readableByteCount() - chunkBytesToRead;

					// 读取字节数组
					byte[] bytesToWrite = new byte[chunkBytesToRead];
					input.read(bytesToWrite, 0, chunkBytesToRead);
					// 写入输出缓冲区
					this.output.write(bytesToWrite);
					// 更新 要读取的消息字节大小
					this.messageBytesToRead -= chunkBytesToRead;

					// 如果消息已经完全读取
					if (this.messageBytesToRead == 0) {
						// 解析并构建消息对象
						CodedInputStream stream = CodedInputStream.newInstance(this.output.asByteBuffer());
						// 释放输出缓冲区
						DataBufferUtils.release(this.output);
						this.output = null;
						// 合并消息
						Message message = getMessageBuilder(this.elementType.toClass())
								.mergeFrom(stream, extensionRegistry)
								.build();
						// 添加到列表中
						messages.add(message);
					}
					// 如果要读取的剩余字节数量没有，则结束循环
				} while (remainingBytesToRead > 0);

				// 返回解析得到的消息列表
				return messages;
			} catch (DecodingException ex) {
				// 捕获 解码异常 并重新抛出
				throw ex;
			} catch (IOException ex) {
				// 捕获 IO异常，并抛出 解码异常
				throw new DecodingException("I/O error while parsing input stream", ex);
			} catch (Exception ex) {
				// 捕获其他异常并抛出 解码异常，包含详细错误信息
				throw new DecodingException("Could not read Protobuf message: " + ex.getMessage(), ex);
			} finally {
				// 无论如何最终释放输入资源
				DataBufferUtils.release(input);
			}
		}

		/**
		 * 从输入流中解析消息大小作为变长整数(varint)，并在需要时更新{@code messageBytesToRead}和{@code offset}字段以允许处理即将到来的数据块。
		 * 参考自{@link CodedInputStream#readRawVarint32(int, java.io.InputStream)}
		 *
		 * <p>当成功解析消息大小时返回{@code true}，当消息大小被截断时返回{@code false}。
		 * 详情参见<a href="https://developers.google.com/protocol-buffers/docs/encoding#varints">Base 128 Varints</a>。
		 *
		 * @param input 输入的数据缓冲区
		 * @return 解析成功时返回{@code true}，消息大小被截断时返回{@code false}
		 */
		private boolean readMessageSize(DataBuffer input) {
			if (this.offset == 0) {
				// 如果偏移量为0，且输入流中没有可读字节数，则返回false
				if (input.readableByteCount() == 0) {
					return false;
				}
				// 读取第一个字节
				int firstByte = input.read();
				// 如果第一个字节的最高位为0，则表示消息大小为该字节的低7位
				if ((firstByte & 0x80) == 0) {
					this.messageBytesToRead = firstByte;
					return true;
				}
				// 否则，从第一个字节中获取低7位作为消息大小
				this.messageBytesToRead = firstByte & 0x7f;
				this.offset = 7;
			}

			// 如果偏移量小于32，则继续读取后续字节
			if (this.offset < 32) {
				for (; this.offset < 32; this.offset += 7) {
					// 如果输入流中没有可读字节数，则返回false
					if (input.readableByteCount() == 0) {
						return false;
					}
					// 读取下一个字节
					final int b = input.read();
					// 将字节中的低7位与消息大小进行位运算并更新消息大小
					this.messageBytesToRead |= (b & 0x7f) << this.offset;
					// 如果字节的最高位为0，则表示消息大小已读取完整
					if ((b & 0x80) == 0) {
						// 重置偏移量并返回true
						this.offset = 0;
						return true;
					}
				}
			}

			// 继续读取最多64位
			for (; this.offset < 64; this.offset += 7) {
				// 如果输入流中没有可读字节数，则返回false
				if (input.readableByteCount() == 0) {
					return false;
				}
				// 读取下一个字节
				final int b = input.read();
				// 如果字节的最高位为0，则表示消息大小已读取完整，重置偏移量并返回true
				if ((b & 0x80) == 0) {
					this.offset = 0;
					return true;
				}
			}

			// 如果执行到这里，则说明消息大小超出了64位的限制
			// 重置偏移量
			this.offset = 0;
			// 抛出 解码异常
			throw new DecodingException("Cannot parse message size: malformed varint");
		}

		public void discard() {
			if (this.output != null) {
				// 如果输出缓冲区不为空，是否输出缓冲去
				DataBufferUtils.release(this.output);
			}
		}
	}

}
