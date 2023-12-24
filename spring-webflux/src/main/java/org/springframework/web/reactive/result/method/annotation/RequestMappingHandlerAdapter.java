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

package org.springframework.web.reactive.result.method.annotation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.support.WebBindingInitializer;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.HandlerAdapter;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.result.method.InvocableHandlerMethod;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * 支持调用{@link org.springframework.web.bind.annotation.RequestMapping @RequestMapping}
 * 处理程序方法的类。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class RequestMappingHandlerAdapter implements HandlerAdapter, ApplicationContextAware, InitializingBean {

	private static final Log logger = LogFactory.getLog(RequestMappingHandlerAdapter.class);

	/**
	 * 用于处理 HTTP 消息读取的列表
	 */
	private List<HttpMessageReader<?>> messageReaders = Collections.emptyList();

	/**
	 * Web 数据绑定初始化器，用于配置全局数据绑定
	 */
	@Nullable
	private WebBindingInitializer webBindingInitializer;

	/**
	 * 用于配置控制器方法参数解析器的配置器
	 */
	@Nullable
	private ArgumentResolverConfigurer argumentResolverConfigurer;

	/**
	 * 反应式适配器注册表，用于处理反应式类型的适配器
	 */
	@Nullable
	private ReactiveAdapterRegistry reactiveAdapterRegistry;

	/**
	 * 可配置的应用程序上下文，用于获取应用程序上下文相关信息
	 */
	@Nullable
	private ConfigurableApplicationContext applicationContext;

	/**
	 * 控制器方法解析器，用于解析和缓存控制器方法的相关信息
	 */
	@Nullable
	private ControllerMethodResolver methodResolver;

	/**
	 * 模型初始化器，用于协助 RequestMappingHandlerAdapter 进行默认模型初始化
	 */
	@Nullable
	private ModelInitializer modelInitializer;


	/**
	 * 配置HTTP消息读取器以反序列化请求体。
	 * <p>默认情况下，设置为{@link ServerCodecConfigurer}的读取器，具有默认值。
	 *
	 * @param messageReaders HTTP消息读取器列表
	 */
	public void setMessageReaders(List<HttpMessageReader<?>> messageReaders) {
		Assert.notNull(messageReaders, "'messageReaders' must not be null");
		this.messageReaders = messageReaders;
	}

	/**
	 * 返回HTTP消息读取器的配置器。
	 *
	 * @return HTTP消息读取器列表
	 */
	public List<HttpMessageReader<?>> getMessageReaders() {
		return this.messageReaders;
	}

	/**
	 * 提供一个WebBindingInitializer，用于对每个DataBinder实例应用“全局”初始化。
	 *
	 * @param webBindingInitializer Web 数据绑定初始化器
	 */
	public void setWebBindingInitializer(@Nullable WebBindingInitializer webBindingInitializer) {
		this.webBindingInitializer = webBindingInitializer;
	}

	/**
	 * 返回配置的WebBindingInitializer，如果没有则返回{@code null}。
	 *
	 * @return Web 数据绑定初始化器
	 */
	@Nullable
	public WebBindingInitializer getWebBindingInitializer() {
		return this.webBindingInitializer;
	}

	/**
	 * 配置控制器方法参数的解析器。
	 *
	 * @param configurer 控制器方法参数解析器配置器
	 */
	public void setArgumentResolverConfigurer(@Nullable ArgumentResolverConfigurer configurer) {
		this.argumentResolverConfigurer = configurer;
	}

	/**
	 * 返回配置的控制器方法参数的解析器。
	 *
	 * @return 控制器方法参数解析器配置器，如果未设置则返回null
	 */
	@Nullable
	public ArgumentResolverConfigurer getArgumentResolverConfigurer() {
		return this.argumentResolverConfigurer;
	}

	/**
	 * 配置用于适应各种反应性类型的注册表。
	 * <p>默认情况下，这是带有默认设置的{@link ReactiveAdapterRegistry}的实例。
	 *
	 * @param registry 响应式适配器注册表
	 */
	public void setReactiveAdapterRegistry(@Nullable ReactiveAdapterRegistry registry) {
		this.reactiveAdapterRegistry = registry;
	}

	/**
	 * 返回配置的适应反应性类型的注册表。
	 *
	 * @return 配置的适应反应性类型的注册表。
	 */
	@Nullable
	public ReactiveAdapterRegistry getReactiveAdapterRegistry() {
		return this.reactiveAdapterRegistry;
	}

	/**
	 * 期望{@link ConfigurableApplicationContext}用于解析方法参数默认值中的表达式，
	 * 以及检测{@code @ControllerAdvice} bean。
	 *
	 * @param applicationContext 应用程序上下文
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		if (applicationContext instanceof ConfigurableApplicationContext) {
			this.applicationContext = (ConfigurableApplicationContext) applicationContext;
		}
	}

	/**
	 * 在设置属性后执行的初始化操作。
	 *
	 * @throws Exception 如果发生初始化异常
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		// 确保应用程序上下文不为 null
		Assert.notNull(this.applicationContext, "ApplicationContext is required");

		// 如果消息读取器列表为空，则使用默认的 ServerCodecConfigurer 创建一个，并获取其中的读取器列表
		if (CollectionUtils.isEmpty(this.messageReaders)) {
			ServerCodecConfigurer codecConfigurer = ServerCodecConfigurer.create();
			//初始化 处理 HTTP 消息的读取器
			this.messageReaders = codecConfigurer.getReaders();
		}

		// 如果参数解析器配置器为 null，则创建一个默认的参数解析器配置器
		if (this.argumentResolverConfigurer == null) {
			//自定义参数解析器
			this.argumentResolverConfigurer = new ArgumentResolverConfigurer();
		}

		// 如果反应式适配器注册表为 null，则使用共享实例获取一个
		if (this.reactiveAdapterRegistry == null) {
			this.reactiveAdapterRegistry = ReactiveAdapterRegistry.getSharedInstance();
		}

		// 使用上述初始化的属性和对象创建控制器方法解析器
		this.methodResolver = new ControllerMethodResolver(
				this.argumentResolverConfigurer, this.reactiveAdapterRegistry, this.applicationContext, this.messageReaders);

		// 使用控制器方法解析器创建模型初始化器
		this.modelInitializer = new ModelInitializer(this.methodResolver, this.reactiveAdapterRegistry);
	}

	/**
	 * 判断处理程序是否被支持。
	 *
	 * @param handler 处理程序对象
	 * @return 如果处理程序是 HandlerMethod 类型，则返回 true；否则返回 false
	 */
	@Override
	public boolean supports(Object handler) {
		return handler instanceof HandlerMethod;
	}

	/**
	 * 处理请求的方法。
	 *
	 * @param exchange 服务器网络交换对象
	 * @param handler  处理程序对象
	 * @return 处理结果的 Mono
	 */
	@Override
	public Mono<HandlerResult> handle(ServerWebExchange exchange, Object handler) {
		// 将处理程序对象转换为 HandlerMethod 类型
		HandlerMethod handlerMethod = (HandlerMethod) handler;

		// 确保方法解析器和模型初始化器已经被初始化
		Assert.state(this.methodResolver != null && this.modelInitializer != null, "Not initialized");

		// 创建用于处理 @InitBinder 方法的绑定上下文
		InitBinderBindingContext bindingContext = new InitBinderBindingContext(
				getWebBindingInitializer(), this.methodResolver.getInitBinderMethods(handlerMethod));

		// 获取对应处理程序方法的可调用处理程序方法
		InvocableHandlerMethod invocableMethod = this.methodResolver.getRequestMappingMethod(handlerMethod);

		// 定义用于处理异常的函数
		Function<Throwable, Mono<HandlerResult>> exceptionHandler =
				ex -> handleException(ex, handlerMethod, bindingContext, exchange);

		// 初始化模型并执行处理程序方法，并设置异常处理器和保存模型的操作
		return this.modelInitializer
				// 初始化模型
				.initModel(handlerMethod, bindingContext, exchange)
				// 执行处理程序方法
				.then(Mono.defer(() -> invocableMethod.invoke(exchange, bindingContext)))
				// 设置异常处理器
				.doOnNext(result -> result.setExceptionHandler(exceptionHandler))
				// 保存模型
				.doOnNext(result -> bindingContext.saveModel())
				// 处理错误并恢复
				.onErrorResume(exceptionHandler);

	}

	/**
	 * 处理异常情况的方法。
	 *
	 * @param exception      异常对象
	 * @param handlerMethod  处理程序方法
	 * @param bindingContext 绑定上下文
	 * @param exchange       服务器网络交换对象
	 * @return 处理结果的 Mono
	 */
	private Mono<HandlerResult> handleException(Throwable exception, HandlerMethod handlerMethod,
												BindingContext bindingContext, ServerWebExchange exchange) {

		// 确保方法解析器已经被初始化
		Assert.state(this.methodResolver != null, "Not initialized");

		// 移除处理可能已经存在的可生产的媒体类型属性，清除响应头中的内容相关的标头信息
		exchange.getAttributes().remove(HandlerMapping.PRODUCIBLE_MEDIA_TYPES_ATTRIBUTE);
		exchange.getResponse().getHeaders().clearContentHeaders();

		// 获取用于处理特定异常的 @ExceptionHandler 方法
		InvocableHandlerMethod invocable = this.methodResolver.getExceptionHandlerMethod(exception, handlerMethod);

		// 如果找到了适合处理该异常的 @ExceptionHandler 方法
		if (invocable != null) {
			ArrayList<Throwable> exceptions = new ArrayList<>();
			try {
				// 记录用于处理的 @ExceptionHandler 方法
				if (logger.isDebugEnabled()) {
					logger.debug(exchange.getLogPrefix() + "Using @ExceptionHandler " + invocable);
				}

				// 清除绑定上下文中的模型属性
				bindingContext.getModel().asMap().clear();

				// 将异常及其导致的所有异常放入列表中
				Throwable exToExpose = exception;
				while (exToExpose != null) {
					// 循环获取导致异常的嵌套异常，并放入异常列表中
					exceptions.add(exToExpose);
					Throwable cause = exToExpose.getCause();
					exToExpose = (cause != exToExpose ? cause : null);
				}
				Object[] arguments = new Object[exceptions.size() + 1];
				//ArrayList中的高效数组复制调用
				exceptions.toArray(arguments);
				arguments[arguments.length - 1] = handlerMethod;

				// 调用 @ExceptionHandler 方法处理异常
				return invocable.invoke(exchange, bindingContext, arguments);
			} catch (Throwable invocationEx) {
				// 捕获并记录 @ExceptionHandler 方法中可能发生的异常
				if (!exceptions.contains(invocationEx) && logger.isWarnEnabled()) {
					logger.warn(exchange.getLogPrefix() + "Failure in @ExceptionHandler " + invocable, invocationEx);
				}
			}
		}

		// 如果没有找到合适的 @ExceptionHandler 方法，则直接返回异常
		return Mono.error(exception);

	}

}
