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

package org.springframework.web.reactive.result.view;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.ui.Model;

import java.util.Collection;
import java.util.Map;

/**
 * HTML渲染的公共API。在Spring WebFlux控制器中支持作为返回值。类似于在Spring MVC控制器中使用{@code ModelAndView}作为返回值。
 *
 * <p>控制器通常返回一个{@link String}视图名称，并依赖于“隐式”模型，该模型也可以注入到控制器方法中。或者控制器可以返回模型属性并依赖于基于请求路径选择默认视图名称。
 *
 * <p>{@code Rendering}可用于将视图名称与模型属性组合，设置HTTP状态码或头部信息，以及其他更高级的重定向场景选项。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface Rendering {

	/**
	 * 返回所选的{@link String}视图名称或{@link View}对象。
	 *
	 * @return 所选的视图名称或View对象。
	 */
	@Nullable
	Object view();

	/**
	 * 返回要添加到模型中的属性。
	 *
	 * @return 要添加到模型中的属性
	 */
	Map<String, Object> modelAttributes();

	/**
	 * 返回要设置到响应的HTTP状态码。
	 *
	 * @return 要设置到响应的HTTP状态码
	 */
	@Nullable
	HttpStatus status();

	/**
	 * 返回要添加到响应的头部信息。
	 *
	 * @return 要添加到响应的头部信息
	 */
	HttpHeaders headers();

	/**
	 * 基于给定的视图名称创建一个新的响应渲染构建器。
	 *
	 * @param name 要解析为{@link View}的视图名称
	 * @return 构建器
	 */
	static Builder<?> view(String name) {
		return new DefaultRenderingBuilder(name);
	}

	/**
	 * 创建一个新的重定向{@link RedirectView}的构建器。
	 *
	 * @param url 重定向的URL
	 * @return 构建器
	 */
	static RedirectBuilder redirectTo(String url) {
		return new DefaultRenderingBuilder(new RedirectView(url));
	}

	/**
	 * 定义{@link Rendering}的构建器。
	 *
	 * @param <B> 构建器类型的自引用
	 */
	interface Builder<B extends Builder<B>> {

		/**
		 * 使用提供的名称添加给定的模型属性。
		 *
		 * @param name  属性名称
		 * @param value 属性值
		 * @return 当前构建器类型
		 * @see Model#addAttribute(String, Object)
		 */

		B modelAttribute(String name, Object value);

		/**
		 * 使用{@link org.springframework.core.Conventions#getVariableName 生成的名称}向模型添加属性。
		 *
		 * @param value 属性值
		 * @return 当前构建器
		 * @see Model#addAttribute(Object)
		 */
		B modelAttribute(Object value);

		/**
		 * 使用{@link org.springframework.core.Conventions#getVariableName 生成的名称}向模型添加所有给定的属性。
		 *
		 * @param values 给定的属性值
		 * @return 当前构建器
		 * @see Model#addAllAttributes(Collection)
		 */
		B modelAttributes(Object... values);

		/**
		 * 将给定的属性添加到模型中。
		 *
		 * @param map 属性值对
		 * @return 当前构建器
		 * @see Model#addAllAttributes(Map)
		 */
		B model(Map<String, ?> map);

		/**
		 * 指定响应的状态。
		 *
		 * @param status HTTP状态
		 * @return 当前构建器
		 */
		B status(HttpStatus status);

		/**
		 * 指定要添加到响应的头部信息。
		 *
		 * @param headerName   请求头
		 * @param headerValues 请求头值
		 * @return 当前构建器
		 */
		B header(String headerName, String... headerValues);

		/**
		 * 指定要添加到响应的头部信息。
		 *
		 * @param headers 请求头
		 * @return 当前构建器
		 */
		B headers(HttpHeaders headers);

		/**
		 * 构建{@link Rendering}实例。
		 *
		 * @return 渲染对象
		 */

		Rendering build();
	}


	/**
	 * 扩展{@link Builder}，用于重定向场景的额外选项。
	 */
	interface RedirectBuilder extends Builder<RedirectBuilder> {

		/**
		 * 是否应将提供的重定向URL前缀与应用程序上下文路径（如果有）结合起来。
		 * <p>默认情况下，此项设置为{@code true}。
		 *
		 * @param contextRelative 是否是相关上下文
		 * @return 重定向构建器
		 * @see RedirectView#setContextRelative(boolean)
		 */
		RedirectBuilder contextRelative(boolean contextRelative);

		/**
		 * 是否将当前URL的查询字符串追加到目标重定向URL中。
		 * <p>默认情况下，此项设置为{@code false}。
		 *
		 * @param propagate 是否是传播属性
		 * @return 重定向构建器
		 * @see RedirectView#setPropagateQuery(boolean)
		 */
		RedirectBuilder propagateQuery(boolean propagate);
	}

}
