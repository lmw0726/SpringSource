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

package org.springframework.web.reactive;

import org.springframework.http.codec.multipart.Part;
import org.springframework.lang.Nullable;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.support.BindingAwareConcurrentModel;
import org.springframework.web.bind.support.WebBindingInitializer;
import org.springframework.web.bind.support.WebExchangeDataBinder;
import org.springframework.web.server.ServerErrorException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * 辅助绑定请求数据到对象并提供访问控制器特定属性的共享 {@link Model} 的上下文。
 *
 * <p>提供创建 {@link WebExchangeDataBinder} 的方法，用于对特定目标、命令对象应用数据绑定和验证，或者对于没有目标对象的情况下从请求值进行简单类型转换。
 *
 * <p>请求的默认模型的容器。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @since 5.0
 */
public class BindingContext {
	/**
	 * Web绑定初始化器
	 */
	@Nullable
	private final WebBindingInitializer initializer;
	/**
	 * 模型
	 */
	private final Model model = new BindingAwareConcurrentModel();


	/**
	 * 创建一个新的 {@code BindingContext}。
	 */
	public BindingContext() {
		this(null);
	}

	/**
	 * 使用给定的初始化器创建一个新的 {@code BindingContext}。
	 *
	 * @param initializer 要应用的绑定初始化器（可以为 {@code null}）
	 */
	public BindingContext(@Nullable WebBindingInitializer initializer) {
		this.initializer = initializer;
	}


	/**
	 * 返回默认模型。
	 */
	public Model getModel() {
		return this.model;
	}


	/**
	 * 创建一个 {@link WebExchangeDataBinder} 以在目标对象上应用数据绑定和验证。
	 *
	 * @param exchange 当前交换对象
	 * @param target   要为其创建数据绑定器的对象
	 * @param name     目标对象的名称
	 * @return 创建的数据绑定器
	 * @throws ServerErrorException 如果 {@code @InitBinder} 方法调用失败
	 */
	public WebExchangeDataBinder createDataBinder(ServerWebExchange exchange, @Nullable Object target, String name) {
		WebExchangeDataBinder dataBinder = new ExtendedWebExchangeDataBinder(target, name);
		if (this.initializer != null) {
			this.initializer.initBinder(dataBinder);
		}
		return initDataBinder(dataBinder, exchange);
	}

	/**
	 * 初始化给定交换对象的数据绑定器实例。
	 *
	 * @throws ServerErrorException 如果 {@code @InitBinder} 方法调用失败
	 */
	protected WebExchangeDataBinder initDataBinder(WebExchangeDataBinder binder, ServerWebExchange exchange) {
		return binder;
	}

	/**
	 * 创建一个 {@link WebExchangeDataBinder}，用于将请求值转换为简单类型，而无需目标对象。
	 *
	 * @param exchange 当前交换对象
	 * @param name     目标对象的名称
	 * @return 创建的数据绑定器
	 * @throws ServerErrorException 如果 {@code @InitBinder} 方法调用失败
	 */
	public WebExchangeDataBinder createDataBinder(ServerWebExchange exchange, String name) {
		return createDataBinder(exchange, null, name);
	}


	/**
	 * {@link WebExchangeDataBinder} 的扩展变体，添加了路径变量。
	 */
	private static class ExtendedWebExchangeDataBinder extends WebExchangeDataBinder {

		public ExtendedWebExchangeDataBinder(@Nullable Object target, String objectName) {
			super(target, objectName);
		}

		/**
		 * 获取要绑定的值的方法。
		 *
		 * @param exchange 服务器WebExchange对象，表示当前的HTTP请求和响应
		 * @return 一个 {@link Mono}，包含要绑定的值的映射
		 */
		@Override
		public Mono<Map<String, Object>> getValuesToBind(ServerWebExchange exchange) {
			// 获取URI模板变量
			Map<String, String> vars = exchange.getAttributeOrDefault(
					HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, Collections.emptyMap());
			// 获取查询参数
			MultiValueMap<String, String> queryParams = exchange.getRequest().getQueryParams();
			// 获取表单数据
			Mono<MultiValueMap<String, String>> formData = exchange.getFormData();
			// 获取多部分数据
			Mono<MultiValueMap<String, Part>> multipartData = exchange.getMultipartData();

			return Mono.zip(Mono.just(vars), Mono.just(queryParams), formData, multipartData)
					.map(tuple -> {
						// 创建一个结果映射
						Map<String, Object> result = new TreeMap<>();
						// 处理URI模板变量
						tuple.getT1().forEach(result::put);
						// 将查询参数添加到结果中
						tuple.getT2().forEach((key, values) -> addBindValue(result, key, values));
						// 将表单数据添加到结果中
						tuple.getT3().forEach((key, values) -> addBindValue(result, key, values));
						// 将多部分数据添加到结果中
						tuple.getT4().forEach((key, values) -> addBindValue(result, key, values));
						return result;
					});
		}

	}

}
