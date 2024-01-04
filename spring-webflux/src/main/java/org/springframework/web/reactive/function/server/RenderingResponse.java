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

package org.springframework.web.reactive.function.server;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

/**
 * {@link ServerResponse} 的渲染特定子类型，公开了模型和模板数据。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 5.0
 */
public interface RenderingResponse extends ServerResponse {

	/**
	 * 返回要渲染的模板名称。
	 */
	String name();

	/**
	 * 返回不可修改的模型映射。
	 */
	Map<String, Object> model();



	// 建造器

	/**
	 * 根据给定响应的模板名称、状态码、头部和模型创建一个构建器。
	 *
	 * @param other 要从中复制值的响应对象
	 * @return 创建的构建器
	 */
	static Builder from(RenderingResponse other) {
		return new DefaultRenderingResponseBuilder(other);
	}

	/**
	 * 使用给定的模板名称创建一个构建器。
	 *
	 * @param name 要渲染的模板名称
	 * @return 创建的构建器
	 */
	static Builder create(String name) {
		return new DefaultRenderingResponseBuilder(name);
	}


	/**
	 * 定义 {@code RenderingResponse} 的构建器。
	 */
	interface Builder {

		/**
		 * 使用 {@linkplain org.springframework.core.Conventions#getVariableName 生成的名称}，
		 * 将提供的属性添加到模型中。
		 *
		 * @param attribute 模型属性值（永不为 {@code null}）
		 * @return Builder对象，用于链式调用
		 */
		Builder modelAttribute(Object attribute);

		/**
		 * 使用提供的名称将属性值添加到模型中。
		 *
		 * @param name 属性的名称（永不为 {@code null}）
		 * @param value 模型属性值（可以为 {@code null}）
		 * @return Builder对象，用于链式调用
		 */
		Builder modelAttribute(String name, @Nullable Object value);

		/**
		 * 将提供的数组中的所有属性复制到模型中，为每个元素使用属性名称生成。
		 *
		 * @param attributes 属性数组
		 * @return Builder对象，用于链式调用
		 * @see #modelAttribute(Object)
		 */
		Builder modelAttributes(Object... attributes);

		/**
		 * 将提供的 {@code Collection} 中的所有属性复制到模型中，为每个元素使用属性名称生成。
		 *
		 * @param attributes 属性集合
		 * @return Builder对象，用于链式调用
		 * @see #modelAttribute(Object)
		 */
		Builder modelAttributes(Collection<?> attributes);

		/**
		 * 将提供的 {@code Map} 中的所有属性复制到模型中。
		 *
		 * @param attributes 属性映射
		 * @return Builder对象，用于链式调用
		 * @see #modelAttribute(String, Object)
		 */
		Builder modelAttributes(Map<String, ?> attributes);

		/**
		 * 在给定名称下添加给定的头值（或值）。
		 *
		 * @param headerName 头部名称
		 * @param headerValues 头部值
		 * @return Builder对象，用于链式调用
		 * @see HttpHeaders#add(String, String)
		 */
		Builder header(String headerName, String... headerValues);

		/**
		 * 将给定的头部复制到实体的头部映射中。
		 *
		 * @param headers 要复制的现有HttpHeaders
		 * @return Builder对象，用于链式调用
		 * @see HttpHeaders#add(String, String)
		 */
		Builder headers(HttpHeaders headers);

		/**
		 * 设置HTTP状态。
		 *
		 * @param status 响应状态
		 * @return Builder对象，用于链式调用
		 */
		Builder status(HttpStatus status);

		/**
		 * 设置HTTP状态。
		 *
		 * @param status 响应状态
		 * @return Builder对象，用于链式调用
		 * @since 5.0.3
		 */
		Builder status(int status);

		/**
		 * 向响应中添加给定的Cookie。
		 *
		 * @param cookie 要添加的Cookie
		 * @return Builder对象，用于链式调用
		 */
		Builder cookie(ResponseCookie cookie);

		/**
		 * 使用给定的Consumer操作此响应的Cookies。Consumer中提供的Cookies是“活动”的，
		 * 因此Consumer可用于{@linkplain MultiValueMap#set(Object, Object) 覆盖}现有Cookies，
		 * {@linkplain MultiValueMap#remove(Object) 删除}Cookies，或使用任何其他{@link MultiValueMap}方法。
		 *
		 * @param cookiesConsumer 操作Cookies的函数
		 * @return Builder对象，用于链式调用
		 */
		Builder cookies(Consumer<MultiValueMap<String, ResponseCookie>> cookiesConsumer);

		/**
		 * 构建响应。
		 *
		 * @return 构建的响应对象
		 */
		Mono<RenderingResponse> build();
	}

}
