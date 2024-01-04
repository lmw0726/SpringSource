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

import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.util.Assert;
import org.springframework.web.reactive.handler.WebFluxResponseStatusExceptionHandler;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.WebExceptionHandler;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.i18n.AcceptHeaderLocaleContextResolver;
import org.springframework.web.server.i18n.LocaleContextResolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * {@link HandlerStrategies.Builder} 接口的默认实现。
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
class DefaultHandlerStrategiesBuilder implements HandlerStrategies.Builder {

	/**
	 * 用于配置服务器编解码器的实例
	 */
	private final ServerCodecConfigurer codecConfigurer = ServerCodecConfigurer.create();

	/**
	 * 视图解析器列表
	 */
	private final List<ViewResolver> viewResolvers = new ArrayList<>();

	/**
	 * Web过滤器列表
	 */
	private final List<WebFilter> webFilters = new ArrayList<>();

	/**
	 * 异常处理器列表
	 */
	private final List<WebExceptionHandler> exceptionHandlers = new ArrayList<>();

	/**
	 * 语言环境上下文解析器，默认为 AcceptHeaderLocaleContextResolver
	 */
	private LocaleContextResolver localeContextResolver = new AcceptHeaderLocaleContextResolver();

	/**
	 * 构造函数，初始化DefaultHandlerStrategiesBuilder对象。
	 */
	public DefaultHandlerStrategiesBuilder() {
		this.codecConfigurer.registerDefaults(false);
	}

	/**
	 * 配置默认值。
	 */
	public void defaultConfiguration() {
		this.codecConfigurer.registerDefaults(true);
		this.exceptionHandlers.add(new WebFluxResponseStatusExceptionHandler());
		this.localeContextResolver = new AcceptHeaderLocaleContextResolver();
	}

	@Override
	public HandlerStrategies.Builder codecs(Consumer<ServerCodecConfigurer> consumer) {
		consumer.accept(this.codecConfigurer);
		return this;
	}

	@Override
	public HandlerStrategies.Builder viewResolver(ViewResolver viewResolver) {
		Assert.notNull(viewResolver, "ViewResolver must not be null");
		this.viewResolvers.add(viewResolver);
		return this;
	}

	@Override
	public HandlerStrategies.Builder webFilter(WebFilter filter) {
		Assert.notNull(filter, "WebFilter must not be null");
		this.webFilters.add(filter);
		return this;
	}

	@Override
	public HandlerStrategies.Builder exceptionHandler(WebExceptionHandler exceptionHandler) {
		Assert.notNull(exceptionHandler, "WebExceptionHandler must not be null");
		this.exceptionHandlers.add(exceptionHandler);
		return this;
	}

	@Override
	public HandlerStrategies.Builder localeContextResolver(LocaleContextResolver localeContextResolver) {
		Assert.notNull(localeContextResolver, "LocaleContextResolver must not be null");
		this.localeContextResolver = localeContextResolver;
		return this;
	}

	@Override
	public HandlerStrategies build() {
		return new DefaultHandlerStrategies(this.codecConfigurer.getReaders(),
				this.codecConfigurer.getWriters(), this.viewResolvers, this.webFilters,
				this.exceptionHandlers, this.localeContextResolver);
	}


	/**
	 * 默认的 HandlerStrategies 实现类。
	 */
	private static class DefaultHandlerStrategies implements HandlerStrategies {

		/**
		 * 用于存储 HTTP 消息读取器的列表。
		 */
		private final List<HttpMessageReader<?>> messageReaders;

		/**
		 * 用于存储 HTTP 消息写入器的列表。
		 */
		private final List<HttpMessageWriter<?>> messageWriters;

		/**
		 * 用于存储视图解析器的列表。
		 */
		private final List<ViewResolver> viewResolvers;

		/**
		 * 用于存储 Web 过滤器的列表。
		 */
		private final List<WebFilter> webFilters;

		/**
		 * 用于存储 Web 异常处理器的列表。
		 */
		private final List<WebExceptionHandler> exceptionHandlers;

		/**
		 * 用于存储语言环境上下文解析器的实例。
		 */
		private final LocaleContextResolver localeContextResolver;

		/**
		 * 构造方法，初始化 DefaultHandlerStrategies 对象。
		 *
		 * @param messageReaders        消息读取器列表
		 * @param messageWriters        消息写入器列表
		 * @param viewResolvers         视图解析器列表
		 * @param webFilters            web过滤器列表
		 * @param exceptionHandlers     异常处理器列表
		 * @param localeContextResolver 语言环境上下文解析器
		 */
		public DefaultHandlerStrategies(
				List<HttpMessageReader<?>> messageReaders,
				List<HttpMessageWriter<?>> messageWriters,
				List<ViewResolver> viewResolvers,
				List<WebFilter> webFilters,
				List<WebExceptionHandler> exceptionHandlers,
				LocaleContextResolver localeContextResolver) {

			this.messageReaders = unmodifiableCopy(messageReaders);
			this.messageWriters = unmodifiableCopy(messageWriters);
			this.viewResolvers = unmodifiableCopy(viewResolvers);
			this.webFilters = unmodifiableCopy(webFilters);
			this.exceptionHandlers = unmodifiableCopy(exceptionHandlers);
			this.localeContextResolver = localeContextResolver;
		}

		/**
		 * 返回一个不可修改的列表副本。
		 */
		private static <T> List<T> unmodifiableCopy(List<? extends T> list) {
			return Collections.unmodifiableList(new ArrayList<>(list));
		}

		@Override
		public List<HttpMessageReader<?>> messageReaders() {
			return this.messageReaders;
		}

		@Override
		public List<HttpMessageWriter<?>> messageWriters() {
			return this.messageWriters;
		}

		@Override
		public List<ViewResolver> viewResolvers() {
			return this.viewResolvers;
		}

		@Override
		public List<WebFilter> webFilters() {
			return this.webFilters;
		}

		@Override
		public List<WebExceptionHandler> exceptionHandlers() {
			return this.exceptionHandlers;
		}

		@Override
		public LocaleContextResolver localeContextResolver() {
			return this.localeContextResolver;
		}
	}

}
