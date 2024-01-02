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

package org.springframework.web.reactive.function;

import org.reactivestreams.Publisher;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.ResolvableType;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 提供 {@link BodyInserter} 实现的静态工厂方法。
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Sebastien Deleuze
 * @since 5.0
 */
public abstract class BodyInserters {

	/**
	 * 创建一个用于表示Resource类的ResolvableType对象
	 */
	private static final ResolvableType RESOURCE_TYPE = ResolvableType.forClass(Resource.class);

	/**
	 * 创建一个用于表示ServerSentEvent类的ResolvableType对象
	 */
	private static final ResolvableType SSE_TYPE = ResolvableType.forClass(ServerSentEvent.class);

	/**
	 * 创建一个用于表示MultiValueMap<String, String>类型的ResolvableType对象，表示表单数据
	 */
	private static final ResolvableType FORM_DATA_TYPE =
			ResolvableType.forClassWithGenerics(MultiValueMap.class, String.class, String.class);

	/**
	 * 创建一个用于表示MultiValueMap<String, Object>类型的ResolvableType对象，表示多部分数据
	 */
	private static final ResolvableType MULTIPART_DATA_TYPE = ResolvableType.forClassWithGenerics(
			MultiValueMap.class, String.class, Object.class);

	/**
	 * 创建一个将空内容插入到响应的BodyInserter对象，不对响应进行修改
	 */
	private static final BodyInserter<Void, ReactiveHttpOutputMessage> EMPTY_INSERTER =
			(response, context) -> response.setComplete();

	/**
	 * 获取一个ReactiveAdapterRegistry的共享实例
	 */
	private static final ReactiveAdapterRegistry registry = ReactiveAdapterRegistry.getSharedInstance();


	/**
	 * 返回一个不进行写入的插入器。
	 *
	 * @param <T> 插入器的类型
	 * @return 插入器
	 */
	@SuppressWarnings("unchecked")
	public static <T> BodyInserter<T, ReactiveHttpOutputMessage> empty() {
		return (BodyInserter<T, ReactiveHttpOutputMessage>) EMPTY_INSERTER;
	}

	/**
	 * 返回一个用于写入给定值的插入器。
	 * <p>或者，可以考虑在 {@link org.springframework.web.reactive.function.client.WebClient WebClient} 和
	 * {@link org.springframework.web.reactive.function.server.ServerResponse ServerResponse} 上使用 {@code bodyValue(Object)} 的快捷方式。
	 *
	 * @param body 要写入的值
	 * @param <T>  body 的类型
	 * @return 用于写入单个值的插入器
	 * @throws IllegalArgumentException 如果 {@code body} 是 {@link Publisher}，或是由 {@link ReactiveAdapterRegistry#getSharedInstance()} 支持的类型的实例，
	 *                                  则应使用 {@link #fromPublisher(Publisher, Class)} 或 {@link #fromProducer(Object, Class)}。
	 * @see #fromPublisher(Publisher, Class)
	 * @see #fromProducer(Object, Class)
	 */
	public static <T> BodyInserter<T, ReactiveHttpOutputMessage> fromValue(T body) {
		Assert.notNull(body, "'body' must not be null");
		Assert.isNull(registry.getAdapter(body.getClass()), "'body' should be an object, for reactive types use a variant specifying a publisher/producer and its related element type");
		return (message, context) ->
				writeWithMessageWriters(message, context, Mono.just(body), ResolvableType.forInstance(body), null);
	}

	/**
	 * 返回一个用于写入给定对象的插入器。
	 * <p>或者，可以考虑在 {@link org.springframework.web.reactive.function.client.WebClient WebClient} 和
	 * {@link org.springframework.web.reactive.function.server.ServerResponse ServerResponse} 上使用 {@code bodyValue(Object)} 的快捷方式。
	 *
	 * @param body 要写入响应的对象
	 * @param <T>  body 的类型
	 * @return 用于写入单个对象的插入器
	 * @throws IllegalArgumentException 如果 {@code body} 是 {@link Publisher}，或是由 {@link ReactiveAdapterRegistry#getSharedInstance()} 支持的类型的实例，
	 *                                  则应使用 {@link #fromPublisher(Publisher, Class)} 或 {@link #fromProducer(Object, Class)}。
	 * @see #fromPublisher(Publisher, Class)
	 * @see #fromProducer(Object, Class)
	 * @deprecated 从 Spring Framework 5.2 开始，弃用，建议使用 {@link #fromValue(Object)}
	 */
	@Deprecated
	public static <T> BodyInserter<T, ReactiveHttpOutputMessage> fromObject(T body) {
		return fromValue(body);
	}

	/**
	 * 返回一个用于写入给定值的生产者的插入器，该值必须是 {@link Publisher} 或其他可通过 {@link ReactiveAdapterRegistry} 转换为 {@code Publisher} 的生产者。
	 * <p>或者，可以考虑在 {@link org.springframework.web.reactive.function.client.WebClient WebClient} 和
	 * {@link org.springframework.web.reactive.function.server.ServerResponse ServerResponse} 上使用 {@code body} 的快捷方式。
	 *
	 * @param <T>          body 的类型
	 * @param producer     生成 body 值的来源。
	 * @param elementClass 要生成的值的类
	 * @return 用于写入生成器的插入器
	 * @since 5.2
	 */
	public static <T> BodyInserter<T, ReactiveHttpOutputMessage> fromProducer(T producer, Class<?> elementClass) {
		Assert.notNull(producer, "'producer' must not be null");
		Assert.notNull(elementClass, "'elementClass' must not be null");
		ReactiveAdapter adapter = ReactiveAdapterRegistry.getSharedInstance().getAdapter(producer.getClass());
		Assert.notNull(adapter, "'producer' type is unknown to ReactiveAdapterRegistry");
		return (message, context) ->
				writeWithMessageWriters(message, context, producer, ResolvableType.forClass(elementClass), adapter);
	}

	/**
	 * 创建一个写入值生产者的插入器，该值必须是一个 {@link Publisher} 或者是另一个通过 {@link ReactiveAdapterRegistry} 可以适应为 {@code Publisher} 的生产者。
	 * <p>或者，考虑使用 {@link org.springframework.web.reactive.function.client.WebClient WebClient} 和 {@link org.springframework.web.reactive.function.server.ServerResponse ServerResponse} 上的 {@code body} 快捷方式。
	 *
	 * @param <T>            body 的类型
	 * @param producer       产生 body 值的源。
	 * @param elementTypeRef 要产生的值的类型
	 * @return 写入生产者的插入器
	 * @since 5.2
	 */
	public static <T> BodyInserter<T, ReactiveHttpOutputMessage> fromProducer(
			T producer, ParameterizedTypeReference<?> elementTypeRef) {

		Assert.notNull(producer, "'producer' must not be null");
		Assert.notNull(elementTypeRef, "'elementTypeRef' must not be null");
		ReactiveAdapter adapter = ReactiveAdapterRegistry.getSharedInstance().getAdapter(producer.getClass());
		Assert.notNull(adapter, "'producer' type is unknown to ReactiveAdapterRegistry");
		return (message, context) ->
				writeWithMessageWriters(message, context, producer, ResolvableType.forType(elementTypeRef), adapter);
	}

	/**
	 * 创建一个写入给定的 {@link Publisher} 的插入器。
	 * <p>或者，考虑使用 {@link org.springframework.web.reactive.function.client.WebClient WebClient} 和 {@link org.springframework.web.reactive.function.server.ServerResponse ServerResponse} 上的 {@code body} 快捷方式。
	 *
	 * @param publisher    要写入的 {@link Publisher}。
	 * @param elementClass publisher 中元素的类
	 * @param <T>          publisher 中包含的元素的类型
	 * @param <P>          {@code Publisher} 的类型
	 * @return 写入 {@code Publisher} 的插入器
	 */
	public static <T, P extends Publisher<T>> BodyInserter<P, ReactiveHttpOutputMessage> fromPublisher(
			P publisher, Class<T> elementClass) {

		Assert.notNull(publisher, "'publisher' must not be null");
		Assert.notNull(elementClass, "'elementClass' must not be null");
		return (message, context) ->
				writeWithMessageWriters(message, context, publisher, ResolvableType.forClass(elementClass), null);
	}

	/**
	 * 创建一个写入给定的 {@link Publisher} 的插入器。
	 * <p>或者，考虑使用 {@link org.springframework.web.reactive.function.client.WebClient WebClient} 和 {@link org.springframework.web.reactive.function.server.ServerResponse ServerResponse} 上的 {@code body} 快捷方式。
	 *
	 * @param publisher      要写入的 {@link Publisher}。
	 * @param elementTypeRef publisher 中元素的类型
	 * @param <T>            publisher 中包含的元素的类型
	 * @param <P>            {@code Publisher} 的类型
	 * @return 写入 {@code Publisher} 的插入器
	 */
	public static <T, P extends Publisher<T>> BodyInserter<P, ReactiveHttpOutputMessage> fromPublisher(
			P publisher, ParameterizedTypeReference<T> elementTypeRef) {

		Assert.notNull(publisher, "'publisher' must not be null");
		Assert.notNull(elementTypeRef, "'elementTypeRef' must not be null");
		return (message, context) ->
				writeWithMessageWriters(message, context, publisher, ResolvableType.forType(elementTypeRef.getType()), null);
	}

	/**
	 * 创建一个写入给定的 {@code Resource} 的插入器。
	 * <p>如果资源可以解析为 {@linkplain Resource#getFile() 文件}，将使用 <a href="https://en.wikipedia.org/wiki/Zero-copy">零拷贝</a> 进行复制。
	 *
	 * @param resource 要写入输出消息的资源
	 * @param <T>      {@code Resource} 的类型
	 * @return 写入 {@code Publisher} 的插入器
	 */
	public static <T extends Resource> BodyInserter<T, ReactiveHttpOutputMessage> fromResource(T resource) {
		Assert.notNull(resource, "'resource' must not be null");
		return (outputMessage, context) -> {
			// 使用 RESOURCE_TYPE 构建 ResolvableType 对象
			ResolvableType elementType = RESOURCE_TYPE;

			// 根据给定的 context 和 elementType 查找合适的写入器
			HttpMessageWriter<Resource> writer = findWriter(context, elementType, null);

			// 获取输出消息的内容类型
			MediaType contentType = outputMessage.getHeaders().getContentType();

			// 将资源包装为 Mono，并调用 write 方法进行写入
			return write(Mono.just(resource), elementType, contentType, outputMessage, context, writer);
		};
	}

	/**
	 * 创建一个用于写入给定的 {@code ServerSentEvent} 发布者的插入器。
	 * <p>或者，您可以通过 {@link #fromPublisher(Publisher, Class)} 或 {@link #fromProducer(Object, Class)} 提供事件数据对象，
	 * 并将 "Content-Type" 设置为 {@link MediaType#TEXT_EVENT_STREAM text/event-stream}。
	 *
	 * @param eventsPublisher 要写入响应体的 {@code ServerSentEvent} 发布者
	 * @param <T>             {@link ServerSentEvent} 中数据元素的类型
	 * @return 写入 {@code ServerSentEvent} 发布者的插入器
	 * @see <a href="https://www.w3.org/TR/eventsource/">Server-Sent Events W3C recommendation</a>
	 */
	// 参数化供服务器端使用
	public static <T, S extends Publisher<ServerSentEvent<T>>> BodyInserter<S, ServerHttpResponse> fromServerSentEvents(
			S eventsPublisher) {

		Assert.notNull(eventsPublisher, "'eventsPublisher' must not be null");
		return (serverResponse, context) -> {
			// 使用 SSE_TYPE 构建 ResolvableType 对象
			ResolvableType elementType = SSE_TYPE;

			// 设置媒体类型为 TEXT_EVENT_STREAM
			MediaType mediaType = MediaType.TEXT_EVENT_STREAM;

			// 根据给定的 context、elementType 和 mediaType 查找合适的写入器
			HttpMessageWriter<ServerSentEvent<T>> writer = findWriter(context, elementType, mediaType);

			// 调用 write 方法将事件发布器（eventsPublisher）写入到 serverResponse 中
			return write(eventsPublisher, elementType, mediaType, serverResponse, context, writer);
		};
	}

	/**
	 * 返回一个 {@link FormInserter} 用于将给定的 {@code MultiValueMap} 写入为 URL 编码的表单数据。
	 * 返回的插入器允许通过 {@link FormInserter#with(String, Object)} 添加更多条目。
	 * <p>请注意，您还可以在 {@code WebClient} 和 {@code WebTestClient} 的请求构建器中使用 {@code bodyValue(Object)} 方法。
	 * 在这种情况下，不需要设置请求内容类型，只需确保映射仅包含字符串值，否则它将被解释为多部分请求。
	 *
	 * @param formData 要写入输出消息的表单数据
	 * @return 允许添加更多表单数据的插入器
	 */
	public static FormInserter<String> fromFormData(MultiValueMap<String, String> formData) {
		return new DefaultFormInserter().with(formData);
	}

	/**
	 * 返回一个 {@link FormInserter} 用于将给定的键值对写入为 URL 编码的表单数据。
	 * 返回的插入器允许通过 {@link FormInserter#with(String, Object)} 添加更多条目。
	 *
	 * @param name  要添加到表单中的键
	 * @param value 要添加到表单中的值
	 * @return 允许添加更多表单数据的插入器
	 */
	public static FormInserter<String> fromFormData(String name, String value) {
		Assert.notNull(name, "'name' must not be null");
		Assert.notNull(value, "'value' must not be null");
		return new DefaultFormInserter().with(name, value);
	}

	/**
	 * 返回一个 {@link MultipartInserter} 以将给定的 {@code MultiValueMap} 写入为多部分数据。映射中的值可以是一个对象或一个 {@link HttpEntity}。
	 * <p>请注意，您还可以使用 {@link MultipartBodyBuilder} 在外部构建多部分数据，并将结果映射直接传递给 {@code WebClient} 中的 {@code bodyValue(Object)} 快捷方法。
	 *
	 * @param multipartData 要写入输出消息的表单数据
	 * @return 允许添加更多部分的插入器
	 * @see MultipartBodyBuilder
	 */
	public static MultipartInserter fromMultipartData(MultiValueMap<String, ?> multipartData) {
		Assert.notNull(multipartData, "'multipartData' must not be null");
		return new DefaultMultipartInserter().withInternal(multipartData);
	}

	/**
	 * 返回一个 {@link MultipartInserter} 以将给定的部分写入为多部分数据。映射中的值可以是一个对象或一个 {@link HttpEntity}。
	 * <p>请注意，您还可以使用 {@link MultipartBodyBuilder} 在外部构建多部分数据，并将结果映射直接传递给 {@code WebClient} 中的 {@code bodyValue(Object)} 快捷方法。
	 *
	 * @param name  部分名称
	 * @param value 部分值，一个对象或 {@code HttpEntity}
	 * @return 允许添加更多部分的插入器
	 */
	public static MultipartInserter fromMultipartData(String name, Object value) {
		Assert.notNull(name, "'name' must not be null");
		Assert.notNull(value, "'value' must not be null");
		return new DefaultMultipartInserter().with(name, value);
	}

	/**
	 * 返回一个 {@link MultipartInserter} 以将给定的异步部分写入为多部分数据。
	 * <p>请注意，您还可以使用 {@link MultipartBodyBuilder} 在外部构建多部分数据，并将结果映射直接传递给 {@code WebClient} 中的 {@code bodyValue(Object)} 快捷方法。
	 *
	 * @param name         部分名称
	 * @param publisher    形成部分值的发布者
	 * @param elementClass {@code publisher} 中包含的类
	 * @return 允许添加更多部分的插入器
	 */
	public static <T, P extends Publisher<T>> MultipartInserter fromMultipartAsyncData(
			String name, P publisher, Class<T> elementClass) {

		return new DefaultMultipartInserter().withPublisher(name, publisher, elementClass);
	}

	/**
	 * {@link #fromMultipartAsyncData(String, Publisher, Class)} 的变体，它接受 {@link ParameterizedTypeReference} 作为元素类型，允许指定泛型类型信息。
	 * <p>请注意，您还可以使用 {@link MultipartBodyBuilder} 在外部构建多部分数据，并将结果映射直接传递给 {@code WebClient} 中的 {@code bodyValue(Object)} 快捷方法。
	 *
	 * @param name          部分名称
	 * @param publisher     形成部分值的发布者
	 * @param typeReference {@code publisher} 中包含的类型
	 * @return 允许添加更多部分的插入器
	 */
	public static <T, P extends Publisher<T>> MultipartInserter fromMultipartAsyncData(
			String name, P publisher, ParameterizedTypeReference<T> typeReference) {

		return new DefaultMultipartInserter().withPublisher(name, publisher, typeReference);
	}

	/**
	 * 返回一个插入器，用于将给定的 {@code Publisher<DataBuffer>} 写入到消息体中。
	 *
	 * @param publisher 要写入的数据缓冲区发布者
	 * @param <T>       发布者的类型
	 * @return 用于直接写入消息体的插入器
	 * @see ReactiveHttpOutputMessage#writeWith(Publisher)
	 */
	public static <T extends Publisher<DataBuffer>> BodyInserter<T, ReactiveHttpOutputMessage> fromDataBuffers(
			T publisher) {

		Assert.notNull(publisher, "'publisher' must not be null");
		return (outputMessage, context) -> outputMessage.writeWith(publisher);
	}


	/**
	 * 使用消息编写器将内容写入到响应式 HTTP 输出消息中。
	 *
	 * @param outputMessage 输出消息
	 * @param context       写入上下文
	 * @param body          要写入的内容
	 * @param bodyType      内容的 ResolvableType
	 * @param adapter       适配器，用于将非发布者类型的内容转换为发布者类型
	 * @param <M>           响应式 HTTP 输出消息的类型
	 * @return 表示写入完成或错误的 Mono
	 */
	private static <M extends ReactiveHttpOutputMessage> Mono<Void> writeWithMessageWriters(
			M outputMessage, BodyInserter.Context context, Object body, ResolvableType bodyType, @Nullable ReactiveAdapter adapter) {

		Publisher<?> publisher;

		// 如果 body 是 Publisher 类型，则直接使用
		if (body instanceof Publisher) {
			publisher = (Publisher<?>) body;
		} else if (adapter != null) {
			// 如果存在适配器，则使用适配器将 body 转换为 Publisher
			publisher = adapter.toPublisher(body);
		} else {
			// 否则，将 body 封装为 Mono
			publisher = Mono.just(body);
		}

		// 获取输出消息的 MediaType
		MediaType mediaType = outputMessage.getHeaders().getContentType();

		// 寻找可以写入指定类型和 MediaType 的消息写入器
		return context.messageWriters().stream()
				.filter(messageWriter -> messageWriter.canWrite(bodyType, mediaType))
				.findFirst()
				.map(BodyInserters::cast)
				.map(writer -> write(publisher, bodyType, mediaType, outputMessage, context, writer))
				.orElseGet(() -> Mono.error(unsupportedError(bodyType, context, mediaType)));
	}

	/**
	 * 创建不支持的媒体类型异常。
	 *
	 * @param bodyType  内容的 ResolvableType
	 * @param context   写入上下文
	 * @param mediaType 不受支持的媒体类型
	 * @return 不支持的媒体类型异常
	 */
	private static UnsupportedMediaTypeException unsupportedError(
			ResolvableType bodyType, BodyInserter.Context context, @Nullable MediaType mediaType) {

		// 获取支持的可写入的媒体类型列表
		List<MediaType> supportedMediaTypes = context.messageWriters().stream()
				.flatMap(reader -> reader.getWritableMediaTypes(bodyType).stream())
				.collect(Collectors.toList());

		// 抛出不支持的媒体类型异常，提供当前媒体类型、支持的媒体类型列表和body类型
		return new UnsupportedMediaTypeException(mediaType, supportedMediaTypes, bodyType);
	}

	/**
	 * 写入操作，将输入内容写入响应消息。
	 *
	 * @param input     要写入的内容
	 * @param type      内容的 ResolvableType
	 * @param mediaType 媒体类型（可为空）
	 * @param message   响应消息
	 * @param context   插入上下文
	 * @param writer    HttpMessageWriter 实例
	 * @param <T>       内容类型
	 * @return 写入操作的 Mono
	 */
	private static <T> Mono<Void> write(
			Publisher<? extends T> input, ResolvableType type, @Nullable MediaType mediaType,
			ReactiveHttpOutputMessage message, BodyInserter.Context context, HttpMessageWriter<T> writer) {

		// 使用上下文的 ServerRequest 获取消息
		return context.serverRequest()
				.map(request -> {
					ServerHttpResponse response = (ServerHttpResponse) message;
					// 如果存在请求，则使用请求和响应进行写入
					return writer.write(input, type, type, mediaType, request, response, context.hints());
				})
				.orElseGet(() -> {
					// 如果不存在请求，则直接使用消息进行写入
					return writer.write(input, type, mediaType, message, context.hints());
				});
	}

	/**
	 * 查找匹配的 HttpMessageWriter。
	 *
	 * @param context     插入上下文
	 * @param elementType 内容类型的 ResolvableType
	 * @param mediaType   媒体类型（可为空）
	 * @param <T>         内容类型
	 * @return 匹配的 HttpMessageWriter
	 * @throws IllegalStateException 若找不到对应的 HttpMessageWriter 抛出异常
	 */
	private static <T> HttpMessageWriter<T> findWriter(
			BodyInserter.Context context, ResolvableType elementType, @Nullable MediaType mediaType) {

		return context.messageWriters().stream()
				.filter(messageWriter -> messageWriter.canWrite(elementType, mediaType))
				.findFirst()
				.map(BodyInserters::<T>cast)
				.orElseThrow(() -> new IllegalStateException(
						"No HttpMessageWriter for \"" + mediaType + "\" and \"" + elementType + "\""));
	}

	/**
	 * 将 HttpMessageWriter 转换为指定类型的写入器。
	 *
	 * @param messageWriter 要转换的 HttpMessageWriter
	 * @param <T>           目标类型
	 * @return 目标类型的 HttpMessageWriter
	 */
	@SuppressWarnings("unchecked")
	private static <T> HttpMessageWriter<T> cast(HttpMessageWriter<?> messageWriter) {
		return (HttpMessageWriter<T>) messageWriter;
	}


	/**
	 * {@link BodyInserter} 的扩展，允许添加表单数据或多部分表单数据。
	 *
	 * @param <T> 值类型
	 */
	public interface FormInserter<T> extends BodyInserter<MultiValueMap<String, T>, ClientHttpRequest> {

		// FormInserter 参数化为 ClientHttpRequest（仅用于客户端使用）

		/**
		 * 将指定的键值对添加到表单中。
		 *
		 * @param key   要添加的键
		 * @param value 要添加的值
		 * @return 用于添加更多部分的此插入器
		 */
		FormInserter<T> with(String key, T value);

		/**
		 * 将指定的值添加到表单中。
		 *
		 * @param values 要添加的值
		 * @return 用于添加更多部分的此插入器
		 */
		FormInserter<T> with(MultiValueMap<String, T> values);

	}


	/**
	 * {@link FormInserter} 的扩展，允许添加异步部分。
	 */
	public interface MultipartInserter extends FormInserter<Object> {

		/**
		 * 使用基于 {@link Publisher} 的内容添加异步部分。
		 *
		 * @param name         要添加的部分的名称
		 * @param publisher    部分内容
		 * @param elementClass publisher 中包含的元素类型
		 * @return 用于添加更多部分的此插入器
		 */
		<T, P extends Publisher<T>> MultipartInserter withPublisher(String name, P publisher,
																	Class<T> elementClass);

		/**
		 * {@link #withPublisher(String, Publisher, Class)} 的变体，接受 {@link ParameterizedTypeReference}
		 * 用于元素类型，允许指定泛型类型信息。
		 *
		 * @param name          要添加的键
		 * @param publisher     要添加的值的发布者
		 * @param typeReference {@code publisher} 中包含的元素类型
		 * @return 用于添加更多部分的此插入器
		 */
		<T, P extends Publisher<T>> MultipartInserter withPublisher(String name, P publisher,
																	ParameterizedTypeReference<T> typeReference);

	}


	/**
	 * 默认的 FormInserter 实现类，用于添加表单数据。
	 */
	private static class DefaultFormInserter implements FormInserter<String> {

		/**
		 * 表单数据
		 */
		private final MultiValueMap<String, String> data = new LinkedMultiValueMap<>();

		/**
		 * 将指定的键值对添加到表单中。
		 *
		 * @param key   要添加的键
		 * @param value 要添加的值
		 * @return 用于添加更多部分的插入器
		 */
		@Override
		public FormInserter<String> with(String key, @Nullable String value) {
			this.data.add(key, value);
			return this;
		}

		/**
		 * 将指定的值添加到表单中。
		 *
		 * @param values 要添加的值
		 * @return 用于添加更多部分的插入器
		 */
		@Override
		public FormInserter<String> with(MultiValueMap<String, String> values) {
			this.data.addAll(values);
			return this;
		}

		/**
		 * 将构建的表单数据插入到给定的输出消息中。
		 *
		 * @param outputMessage 要插入的请求消息
		 * @param context       使用的上下文
		 * @return 表示完成或错误的 Mono
		 */
		@Override
		public Mono<Void> insert(ClientHttpRequest outputMessage, Context context) {
			// 查找 HTTP 消息写入器
			HttpMessageWriter<MultiValueMap<String, String>> messageWriter =
					findWriter(context, FORM_DATA_TYPE, MediaType.APPLICATION_FORM_URLENCODED);

			// 使用消息写入器写入数据
			return messageWriter.write(Mono.just(this.data), FORM_DATA_TYPE,
					MediaType.APPLICATION_FORM_URLENCODED,
					outputMessage, context.hints());
		}
	}


	/**
	 * 默认的 MultipartInserter 实现类，用于添加异步部分和构建多部分内容。
	 */
	private static class DefaultMultipartInserter implements MultipartInserter {

		/**
		 * 多部分内容构建器
		 */
		private final MultipartBodyBuilder builder = new MultipartBodyBuilder();

		/**
		 * 将指定的键值对添加到表单中。
		 *
		 * @param key   要添加的键
		 * @param value 要添加的值
		 * @return 用于添加更多部分的插入器
		 */
		@Override
		public MultipartInserter with(String key, Object value) {
			this.builder.part(key, value);
			return this;
		}

		/**
		 * 将指定的值添加到表单中。
		 *
		 * @param values 要添加的值
		 * @return 用于添加更多部分的插入器
		 */
		@Override
		public MultipartInserter with(MultiValueMap<String, Object> values) {
			return withInternal(values);
		}

		/**
		 * 将指定的值添加到表单中。
		 *
		 * @param values 要添加的值
		 * @return 用于添加更多部分的插入器
		 */
		@SuppressWarnings("unchecked")
		private MultipartInserter withInternal(MultiValueMap<String, ?> values) {
			values.forEach((key, valueList) -> {
				for (Object value : valueList) {
					this.builder.part(key, value);
				}
			});
			return this;
		}

		/**
		 * 添加具有基于 Publisher 的内容的异步部分。
		 *
		 * @param name         要添加的部分的名称
		 * @param publisher    部分内容
		 * @param elementClass 发布器中包含的元素类型
		 * @return 用于添加更多部分的插入器
		 */
		@Override
		public <T, P extends Publisher<T>> MultipartInserter withPublisher(
				String name, P publisher, Class<T> elementClass) {

			this.builder.asyncPart(name, publisher, elementClass);
			return this;
		}

		/**
		 * 添加具有基于 Publisher 的内容的异步部分。
		 *
		 * @param name          要添加的部分的名称
		 * @param publisher     部分内容
		 * @param typeReference 发布器中包含的元素类型的参数化类型引用，允许指定泛型类型信息
		 * @return 用于添加更多部分的插入器
		 */
		@Override
		public <T, P extends Publisher<T>> MultipartInserter withPublisher(
				String name, P publisher, ParameterizedTypeReference<T> typeReference) {

			this.builder.asyncPart(name, publisher, typeReference);
			return this;
		}

		/**
		 * 将构建的多部分内容插入到给定的输出消息中。
		 *
		 * @param outputMessage 要插入的响应消息
		 * @param context       使用的上下文
		 * @return 表示完成或错误的 Mono
		 */
		@Override
		public Mono<Void> insert(ClientHttpRequest outputMessage, Context context) {
			// 获取处理MultiValueMap<String, HttpEntity<?>>类型的HttpMessageWriter
			HttpMessageWriter<MultiValueMap<String, HttpEntity<?>>> messageWriter =
					findWriter(context, MULTIPART_DATA_TYPE, MediaType.MULTIPART_FORM_DATA);

			// 构建的MultiValueMap，包含HttpEntity实体
			MultiValueMap<String, HttpEntity<?>> body = this.builder.build();

			// 调用messageWriter将body写入outputMessage
			return messageWriter.write(
					// 将body封装为Mono
					Mono.just(body),
					// 写入的数据类型
					MULTIPART_DATA_TYPE,
					// 媒体类型
					MediaType.MULTIPART_FORM_DATA,
					// 输出消息
					outputMessage,
					// 上下文提示信息
					context.hints()
			);

		}
	}

}
