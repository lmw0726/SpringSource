/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.reactive.result.view;

import org.reactivestreams.Publisher;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Encoder;
import org.springframework.core.codec.Hints;
import org.springframework.http.MediaType;
import org.springframework.http.codec.EncoderHttpMessageWriter;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * {@code View} 用于使用 {@link HttpMessageWriter} 写入模型属性。
 * 由 {@code View} 写入模型属性与 {@link HttpMessageWriter} 一起使用。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class HttpMessageWriterView implements View {

	/**
	 * HTTP消息写入器
	 */
	private final HttpMessageWriter<?> writer;
	/**
	 * 模型键集合
	 */
	private final Set<String> modelKeys = new HashSet<>(4);
	/**
	 * 是否可以写入Map类型
	 */
	private final boolean canWriteMap;


	/**
	 * 使用 {@code Encoder} 构造函数。
	 */
	public HttpMessageWriterView(Encoder<?> encoder) {
		this(new EncoderHttpMessageWriter<>(encoder));
	}

	/**
	 * 使用完全初始化的 {@link HttpMessageWriter} 构造函数。
	 */
	public HttpMessageWriterView(HttpMessageWriter<?> writer) {
		Assert.notNull(writer, "HttpMessageWriter is required");
		this.writer = writer;
		this.canWriteMap = writer.canWrite(ResolvableType.forClass(Map.class), null);
	}


	/**
	 * 返回配置的消息写入器。
	 */
	public HttpMessageWriter<?> getMessageWriter() {
		return this.writer;
	}

	/**
	 * {@inheritDoc}
	 * <p>{@link HttpMessageWriterView} 的此方法的实现委托给 {@link HttpMessageWriter#getWritableMediaTypes()}。
	 */
	@Override
	public List<MediaType> getSupportedMediaTypes() {
		return this.writer.getWritableMediaTypes();
	}

	/**
	 * 设置应由此视图呈现的模型中的属性。
	 * 当设置后，将忽略所有其他模型属性。
	 * 匹配的属性进一步缩小为 {@link HttpMessageWriter#canWrite}。
	 * 匹配的属性处理方式如下：
	 * <ul>
	 *     <li>0：不会写入到响应主体。
	 *     <li>1：将匹配的属性传递给写入器。
	 *     <li>2..N：如果写入器支持 {@link Map}，则写入所有匹配项；否则引发 {@link IllegalStateException}。
	 * </ul>
	 */
	public void setModelKeys(@Nullable Set<String> modelKeys) {
		this.modelKeys.clear();
		if (modelKeys != null) {
			this.modelKeys.addAll(modelKeys);
		}
	}

	/**
	 * 返回配置的模型键。
	 */
	public final Set<String> getModelKeys() {
		return this.modelKeys;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Mono<Void> render(
			@Nullable Map<String, ?> model, @Nullable MediaType contentType, ServerWebExchange exchange) {

		Object value = getObjectToRender(model);
		return (value != null ? write(value, contentType, exchange) : exchange.getResponse().setComplete());
	}

	/**
	 * 获取要渲染的对象。
	 *
	 * @param model 模型，可以为 {@code null}
	 * @return 要渲染的对象，可能为 {@code null}
	 */
	@Nullable
	private Object getObjectToRender(@Nullable Map<String, ?> model) {
		// 如果模型为空，返回null
		if (model == null) {
			return null;
		}

		// 根据匹配条件过滤模型键值对
		Map<String, ?> result = model.entrySet().stream()
				.filter(this::isMatch)
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		// 如果过滤后的结果为空，返回null
		if (result.isEmpty()) {
			return null;
		} else if (result.size() == 1) {
			// 如果结果只有一个键值对，返回该键值对的值
			return result.values().iterator().next();
		} else if (this.canWriteMap) {
			// 如果可以写入Map类型，返回结果
			return result;
		} else {
			// 如果无法写入Map类型，抛出异常
			throw new IllegalStateException("Multiple matches found: " + result + " but " +
					"Map rendering is not supported by " + getMessageWriter().getClass().getName());
		}
	}

	/**
	 * 检查模型中的条目是否匹配。
	 *
	 * @param entry 模型中的条目
	 * @return 如果条目匹配，则为 {@code true}；否则为 {@code false}
	 */
	private boolean isMatch(Map.Entry<String, ?> entry) {
		// 如果值为空，返回false
		if (entry.getValue() == null) {
			return false;
		}

		// 如果模型键集合非空且不包含当前键，返回false
		if (!getModelKeys().isEmpty() && !getModelKeys().contains(entry.getKey())) {
			return false;
		}

		// 获取值的可解析类型
		ResolvableType type = ResolvableType.forInstance(entry.getValue());

		// 检查消息写入器是否能够写入该类型
		return getMessageWriter().canWrite(type, null);
	}

	/**
	 * 将值写入响应。
	 *
	 * @param value         要写入的值
	 * @param contentType   内容类型
	 * @param exchange      当前交换
	 * @return 表示渲染是否成功的 {@code Mono}
	 */
	@SuppressWarnings("unchecked")
	private <T> Mono<Void> write(T value, @Nullable MediaType contentType, ServerWebExchange exchange) {
		// 创建包含值的 Mono
		Publisher<T> input = Mono.justOrEmpty(value);

		// 获取值的可解析类型
		ResolvableType elementType = ResolvableType.forClass(value.getClass());

		// 使用消息写入器将值写入响应
		return ((HttpMessageWriter<T>) this.writer).write(
				input, elementType, contentType, exchange.getResponse(),
				Hints.from(Hints.LOG_PREFIX_HINT, exchange.getLogPrefix())
		);

	}

}
