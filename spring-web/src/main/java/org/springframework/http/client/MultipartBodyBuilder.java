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

package org.springframework.http.client;

import org.reactivestreams.Publisher;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.core.ResolvableTypeProvider;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.Part;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 准备多部分请求的主体，生成一个 {@code MultiValueMap<String, HttpEntity>}。
 * 部件可以是具体的值，也可以是通过 Reactor 的 {@code Mono}、{@code Flux} 等异步类型，
 * 这些类型注册在 {@link org.springframework.core.ReactiveAdapterRegistry ReactiveAdapterRegistry} 中。
 *
 * <p>此构建器旨在与反应式 {@link org.springframework.web.reactive.function.client.WebClient WebClient} 一起使用。
 * 对于使用 {@code RestTemplate} 的多部分请求，只需创建并填充一个 {@code MultiValueMap<String, HttpEntity>}，
 * 如 {@link org.springframework.http.converter.FormHttpMessageConverter FormHttpMessageConverter} 的 Javadoc 中所示，
 * 以及参考文档中的<a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/integration.html#rest-template-multipart">相关部分</a>。
 *
 * <p>以下是使用此构建器的示例：
 * <pre class="code">
 *
 * // 添加表单字段
 * MultipartBodyBuilder builder = new MultipartBodyBuilder();
 * builder.part("form field", "form value").header("foo", "bar");
 *
 * // 添加文件部件
 * Resource image = new ClassPathResource("image.jpg");
 * builder.part("image", image).header("foo", "bar");
 *
 * // 添加内容（例如 JSON）
 * Account account = ...
 * builder.part("account", account).header("foo", "bar");
 *
 * // 添加来自 Publisher 的内容
 * Mono&lt;Account&gt; accountMono = ...
 * builder.asyncPart("account", accountMono).header("foo", "bar");
 *
 * // 构建并使用
 * MultiValueMap&lt;String, HttpEntity&lt;?&gt;&gt; multipartBody = builder.build();
 *
 * Mono&lt;Void&gt; result = webClient.post()
 *     .uri("...")
 *     .body(multipartBody)
 *     .retrieve()
 *     .bodyToMono(Void.class)
 * </pre>
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @see <a href="https://tools.ietf.org/html/rfc7578">RFC 7578</a>
 * @since 5.0.2
 */
public final class MultipartBodyBuilder {
	/**
	 * 部分名称 —— 默认部分构建器 映射
	 */
	private final LinkedMultiValueMap<String, DefaultPartBuilder> parts = new LinkedMultiValueMap<>();


	/**
	 * 创建一个新的空实例 {@code MultipartBodyBuilder}。
	 */
	public MultipartBodyBuilder() {
	}


	/**
	 * 添加一个部件，Object 可以是：
	 * <ul>
	 * <li>String -- 表单字段
	 * <li>{@link org.springframework.core.io.Resource Resource} -- 文件部件
	 * <li>Object -- 要编码的内容（例如 JSON）
	 * <li>{@link HttpEntity} -- 部件内容和头部，尽管通常更容易通过返回的构建器添加头部
	 * <li>{@link Part} -- 来自服务器请求的部件
	 * </ul>
	 *
	 * @param name 要添加的部件名称
	 * @param part 部件数据
	 * @return 允许进一步自定义部件头部的构建器
	 */
	public PartBuilder part(String name, Object part) {
		return part(name, part, null);
	}

	/**
	 * {@link #part(String, Object)} 的变体，还接受一个 MediaType。
	 *
	 * @param name        要添加的部件名称
	 * @param part        部件数据
	 * @param contentType 用于帮助编码部件的媒体类型
	 * @return 允许进一步自定义部件头部的构建器
	 */
	public PartBuilder part(String name, Object part, @Nullable MediaType contentType) {
		Assert.hasLength(name, "'name' must not be empty");
		Assert.notNull(part, "'part' must not be null");

		// 如果 部分 是 Part 的实例
		if (part instanceof Part) {
			// 将 部分 强制转换为 Part 类型
			Part partObject = (Part) part;
			// 创建异步 部分构建器 对象
			PartBuilder builder = asyncPart(name, partObject.content(), DataBuffer.class);
			// 如果 部分对象 的 头部 不为空
			if (!partObject.headers().isEmpty()) {
				// 设置 头部
				builder.headers(headers -> {
					// 将 部分对象 的 所有头部 放入 当前头部
					headers.putAll(partObject.headers());
					// 获取文件名
					String filename = headers.getContentDisposition().getFilename();
					// 重置为参数名称
					headers.setContentDispositionFormData(name, filename);
				});
			}
			// 如果 内容类型 非空
			if (contentType != null) {
				// 设置内容类型
				builder.contentType(contentType);
			}
			// 返回构建器对象
			return builder;
		}

		// 如果 部分 是 PublisherEntity 的实例
		if (part instanceof PublisherEntity<?, ?>) {
			// 创建 发布者部件生成器 对象
			PublisherPartBuilder<?, ?> builder = new PublisherPartBuilder<>(name, (PublisherEntity<?, ?>) part);
			// 如果 内容 非空
			if (contentType != null) {
				// 设置内容类型
				builder.contentType(contentType);
			}
			// 将构建器添加到 部分映射 中
			this.parts.add(name, builder);
			// 返回构建器对象
			return builder;
		}

		// 声明 部分主体 对象
		Object partBody;
		// 声明 部分头部 对象
		HttpHeaders partHeaders = null;
		// 如果 部分 是 Http实体 的实例
		if (part instanceof HttpEntity) {
			// 获取 部分 的主体内容
			partBody = ((HttpEntity<?>) part).getBody();
			// 初始化 部分头部 对象
			partHeaders = new HttpHeaders();
			// 将 部分的所有头部 放入 部分头部 中
			partHeaders.putAll(((HttpEntity<?>) part).getHeaders());
		} else {
			// 否则直接将 部分 赋值给 部分主体
			partBody = part;
		}

		if (partBody instanceof Publisher) {
			// 如果部分主体是发布者，则抛出异常
			throw new IllegalArgumentException(
					"Use asyncPart(String, Publisher, Class)" +
							" or asyncPart(String, Publisher, ParameterizedTypeReference) or" +
							" or MultipartBodyBuilder.PublisherEntity");
		}

		// 创建 默认部分构建器
		DefaultPartBuilder builder = new DefaultPartBuilder(name, partHeaders, partBody);
		if (contentType != null) {
			// 如果内容类型不为空，则设置到构建器中
			builder.contentType(contentType);
		}
		// 将 名称 和 发布者部分构建器 添加到 部分映射 中
		this.parts.add(name, builder);
		// 返回创建的 发布者部分构建器
		return builder;
	}

	/**
	 * 从 {@link Publisher} 内容添加一个部件。
	 *
	 * @param name         要添加的部件名称
	 * @param publisher    部件内容的 Publisher
	 * @param elementClass 发布者包含的元素类型
	 * @return 允许进一步自定义部件头部的构建器
	 */
	public <T, P extends Publisher<T>> PartBuilder asyncPart(String name, P publisher, Class<T> elementClass) {
		Assert.hasLength(name, "'name' must not be empty");
		Assert.notNull(publisher, "'publisher' must not be null");
		Assert.notNull(elementClass, "'elementClass' must not be null");
		// 创建发布者部分构建器
		PublisherPartBuilder<T, P> builder = new PublisherPartBuilder<>(name, null, publisher, elementClass);
		// 将 名称 和 发布者部分构建器 添加到 部分映射 中
		this.parts.add(name, builder);
		// 返回创建的 发布者部分构建器
		return builder;
	}

	/**
	 * {@link #asyncPart(String, Publisher, Class)} 的变体，
	 * 使用 {@link ParameterizedTypeReference} 来提供元素类型信息。
	 *
	 * @param name          要添加的部件名称
	 * @param publisher     部件内容的 Publisher
	 * @param typeReference 发布者包含的元素类型
	 * @return 允许进一步自定义部件头部的构建器
	 */
	public <T, P extends Publisher<T>> PartBuilder asyncPart(
			String name, P publisher, ParameterizedTypeReference<T> typeReference) {

		Assert.hasLength(name, "'name' must not be empty");
		Assert.notNull(publisher, "'publisher' must not be null");
		Assert.notNull(typeReference, "'typeReference' must not be null");
		// 创建发布者部分构建器
		PublisherPartBuilder<T, P> builder = new PublisherPartBuilder<>(name, null, publisher, typeReference);
		// 将 名称 和 发布者部分构建器 添加到 部分映射 中
		this.parts.add(name, builder);
		// 返回创建的 发布者部分构建器
		return builder;
	}

	/**
	 * 返回一个包含已配置部件的 {@code MultiValueMap}。
	 */
	public MultiValueMap<String, HttpEntity<?>> build() {
		// 创建一个新的 结果集 对象
		MultiValueMap<String, HttpEntity<?>> result = new LinkedMultiValueMap<>(this.parts.size());

		// 遍历 默认部分构建器映射 中的每个条目
		for (Map.Entry<String, List<DefaultPartBuilder>> entry : this.parts.entrySet()) {
			// 遍历每个条目中的 默认部分构建器映射 列表
			for (DefaultPartBuilder builder : entry.getValue()) {
				// 使用 默认部分构建器映射 构建 Http实体 对象
				HttpEntity<?> entity = builder.build();
				// 将构建的 Http实体 对象添加到 结果集 中，键为当前条目的键
				result.add(entry.getKey(), entity);
			}
		}

		// 返回构建的 结果集
		return result;
	}


	/**
	 * 一个允许进一步自定义部件头部的构建器接口。
	 */
	public interface PartBuilder {

		/**
		 * 设置部件的 {@linkplain MediaType 媒体类型}。
		 *
		 * @param contentType 媒体类型
		 * @see HttpHeaders#setContentType(MediaType)
		 * @since 5.2
		 */
		PartBuilder contentType(MediaType contentType);

		/**
		 * 为文件部件设置文件名参数。对于基于 {@link org.springframework.core.io.Resource Resource} 的
		 * 部件，这通常不必要，因为它们可以暴露文件名，但对于 {@link Publisher} 部件可能有用。
		 *
		 * @param filename 要在 Content-Disposition 中设置的文件名
		 * @since 5.2
		 */
		PartBuilder filename(String filename);

		/**
		 * 添加部件头部的值。
		 *
		 * @param headerName   部件头部的名称
		 * @param headerValues 部件头部的值
		 * @return 这个构建器
		 * @see HttpHeaders#addAll(String, List)
		 */
		PartBuilder header(String headerName, String... headerValues);

		/**
		 * 通过给定的消费者操作部件头部。
		 *
		 * @param headersConsumer 用于操作部件头部的消费者
		 * @return 这个构建器
		 */
		PartBuilder headers(Consumer<HttpHeaders> headersConsumer);
	}


	private static class DefaultPartBuilder implements PartBuilder {

		/**
		 * 名称
		 */
		private final String name;

		/**
		 * Http头部
		 */
		@Nullable
		protected HttpHeaders headers;

		@Nullable
		protected final Object body;

		public DefaultPartBuilder(String name, @Nullable HttpHeaders headers, @Nullable Object body) {
			this.name = name;
			this.headers = headers;
			this.body = body;
		}

		@Override
		public PartBuilder contentType(MediaType contentType) {
			initHeadersIfNecessary().setContentType(contentType);
			return this;
		}

		@Override
		public PartBuilder filename(String filename) {
			initHeadersIfNecessary().setContentDispositionFormData(this.name, filename);
			return this;
		}

		@Override
		public PartBuilder header(String headerName, String... headerValues) {
			initHeadersIfNecessary().addAll(headerName, Arrays.asList(headerValues));
			return this;
		}

		@Override
		public PartBuilder headers(Consumer<HttpHeaders> headersConsumer) {
			headersConsumer.accept(initHeadersIfNecessary());
			return this;
		}

		private HttpHeaders initHeadersIfNecessary() {
			if (this.headers == null) {
				// 如果Http头部为空，则创建一个Http头部
				this.headers = new HttpHeaders();
			}
			// 返回该Http头部
			return this.headers;
		}

		public HttpEntity<?> build() {
			return new HttpEntity<>(this.body, this.headers);
		}
	}


	private static class PublisherPartBuilder<S, P extends Publisher<S>> extends DefaultPartBuilder {
		/**
		 * 可解析类型
		 */
		private final ResolvableType resolvableType;

		public PublisherPartBuilder(String name, @Nullable HttpHeaders headers, P body, Class<S> elementClass) {
			super(name, headers, body);
			this.resolvableType = ResolvableType.forClass(elementClass);
		}

		public PublisherPartBuilder(String name, @Nullable HttpHeaders headers, P body,
									ParameterizedTypeReference<S> typeRef) {

			super(name, headers, body);
			this.resolvableType = ResolvableType.forType(typeRef);
		}

		public PublisherPartBuilder(String name, PublisherEntity<S, P> other) {
			super(name, other.getHeaders(), other.getBody());
			this.resolvableType = other.getResolvableType();
		}

		@Override
		@SuppressWarnings("unchecked")
		public HttpEntity<?> build() {
			P publisher = (P) this.body;
			Assert.state(publisher != null, "Publisher must not be null");
			return new PublisherEntity<>(this.headers, publisher, this.resolvableType);
		}
	}


	/**
	 * {@link HttpEntity} 的专门化，用于与基于 {@link Publisher} 的主体一起使用，我们还需要跟踪元素类型。
	 *
	 * @param <T> 发布者中包含的类型
	 * @param <P> 发布者
	 */
	static final class PublisherEntity<T, P extends Publisher<T>> extends HttpEntity<P>
			implements ResolvableTypeProvider {
		/**
		 * 可解析类型
		 */
		private final ResolvableType resolvableType;

		PublisherEntity(
				@Nullable MultiValueMap<String, String> headers, P publisher, ResolvableType resolvableType) {

			super(publisher, headers);
			Assert.notNull(publisher, "'publisher' must not be null");
			Assert.notNull(resolvableType, "'resolvableType' must not be null");
			this.resolvableType = resolvableType;
		}

		/**
		 * 返回 {@code Publisher} 主体的元素类型。
		 */
		@Override
		@NonNull
		public ResolvableType getResolvableType() {
			return this.resolvableType;
		}
	}

}
