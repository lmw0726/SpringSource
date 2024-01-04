/*
 * Copyright 2002-2019 the original author or authors.
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

import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.WebExceptionHandler;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.i18n.LocaleContextResolver;

import java.util.List;
import java.util.function.Consumer;

/**
 * 定义用于处理 {@link HandlerFunction HandlerFunctions} 的策略。
 *
 * <p>此类的实例是不可变的。通常通过可变的 {@link Builder} 创建实例：
 * 可以通过 {@link #builder()} 来设置默认策略，
 * 或者通过 {@link #empty()} 从头开始。
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @see RouterFunctions#toHttpHandler(RouterFunction, HandlerStrategies)
 * @since 5.0
 */
public interface HandlerStrategies {

	/**
	 * 返回用于请求体转换的 {@link HttpMessageReader HttpMessageReaders}。
	 *
	 * @return 消息读取器
	 */
	List<HttpMessageReader<?>> messageReaders();

	/**
	 * 返回用于响应体转换的 {@link HttpMessageWriter HttpMessageWriters}。
	 *
	 * @return 消息写入器
	 */
	List<HttpMessageWriter<?>> messageWriters();

	/**
	 * 返回用于视图名称解析的 {@link ViewResolver ViewResolvers}。
	 *
	 * @return 视图解析器
	 */
	List<ViewResolver> viewResolvers();

	/**
	 * 返回用于过滤请求和响应的 {@link WebFilter WebFilters}。
	 *
	 * @return web过滤器
	 */
	List<WebFilter> webFilters();

	/**
	 * 返回用于处理异常的 {@link WebExceptionHandler WebExceptionHandlers}。
	 *
	 * @return 异常处理器
	 */
	List<WebExceptionHandler> exceptionHandlers();

	/**
	 * 返回用于解析语言环境上下文的 {@link LocaleContextResolver}。
	 *
	 * @return 语言环境上下文解析器
	 */
	LocaleContextResolver localeContextResolver();


	// 静态生成器方法

	/**
	 * 使用默认初始化返回一个新的 {@code HandlerStrategies}。
	 *
	 * @return 新的 {@code HandlerStrategies}
	 */
	static HandlerStrategies withDefaults() {
		return builder().build();
	}

	/**
	 * 返回一个可变的 {@code HandlerStrategies} 构建器，使用默认初始化。
	 *
	 * @return 构建器
	 */
	static Builder builder() {
		DefaultHandlerStrategiesBuilder builder = new DefaultHandlerStrategiesBuilder();
		builder.defaultConfiguration();
		return builder;
	}

	/**
	 * 返回一个可变的空 {@code HandlerStrategies} 构建器。
	 *
	 * @return 构建器
	 */
	static Builder empty() {
		return new DefaultHandlerStrategiesBuilder();
	}


	/**
	 * 用于构建 {@link HandlerStrategies} 的可变构建器。
	 */
	interface Builder {

		/**
		 * 自定义服务器端HTTP消息读取器和写入器的列表。
		 *
		 * @param consumer 用于自定义编解码器的消费者
		 * @return 当前构建器实例
		 */
		Builder codecs(Consumer<ServerCodecConfigurer> consumer);

		/**
		 * 将给定的视图解析器添加到此构建器。
		 *
		 * @param viewResolver 要添加的视图解析器
		 * @return 当前构建器实例
		 */
		Builder viewResolver(ViewResolver viewResolver);

		/**
		 * 将给定的Web过滤器添加到此构建器。
		 *
		 * @param filter 要添加的过滤器
		 * @return 当前构建器实例
		 */
		Builder webFilter(WebFilter filter);

		/**
		 * 将给定的异常处理器添加到此构建器。
		 *
		 * @param exceptionHandler 要添加的异常处理器
		 * @return 当前构建器实例
		 */
		Builder exceptionHandler(WebExceptionHandler exceptionHandler);

		/**
		 * 将给定的语言环境上下文解析器添加到此构建器。
		 *
		 * @param localeContextResolver 要添加的语言环境上下文解析器
		 * @return 当前构建器实例
		 */
		Builder localeContextResolver(LocaleContextResolver localeContextResolver);

		/**
		 * 构建 {@link HandlerStrategies}。
		 *
		 * @return 构建的策略
		 */
		HandlerStrategies build();
	}

}
