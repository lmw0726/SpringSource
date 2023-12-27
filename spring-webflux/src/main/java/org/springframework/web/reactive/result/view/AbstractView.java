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

package org.springframework.web.reactive.result.view;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.ReactiveAdapter;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.validation.BindingResult;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link View} 实现的基类。
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 5.0
 */
public abstract class AbstractView implements View, BeanNameAware, ApplicationContextAware {

	/**
	 * 在bean工厂中用于 RequestDataValueProcessor 的已知名称。
	 */
	public static final String REQUEST_DATA_VALUE_PROCESSOR_BEAN_NAME = "requestDataValueProcessor";

	/**
	 * 可供子类使用的日志记录器。
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * 适配器注册表
	 */
	private final ReactiveAdapterRegistry adapterRegistry;

	/**
	 * 媒体类型列表
	 */
	private final List<MediaType> mediaTypes = new ArrayList<>(4);

	/**
	 * 默认字符集
	 */
	private Charset defaultCharset = StandardCharsets.UTF_8;

	/**
	 * 请求上下文属性
	 */
	@Nullable
	private String requestContextAttribute;

	/**
	 * Bean 名称
	 */
	@Nullable
	private String beanName;

	/**
	 * 应用程序上下文
	 */
	@Nullable
	private ApplicationContext applicationContext;


	public AbstractView() {
		this(ReactiveAdapterRegistry.getSharedInstance());
	}

	public AbstractView(ReactiveAdapterRegistry reactiveAdapterRegistry) {
		this.adapterRegistry = reactiveAdapterRegistry;
		this.mediaTypes.add(ViewResolverSupport.DEFAULT_CONTENT_TYPE);
	}


	/**
	 * 设置此视图支持的媒体类型。
	 * <p>默认为 {@code "text/html;charset=UTF-8"}。
	 */
	public void setSupportedMediaTypes(List<MediaType> supportedMediaTypes) {
		Assert.notEmpty(supportedMediaTypes, "MediaType List must not be empty");
		this.mediaTypes.clear();
		this.mediaTypes.addAll(supportedMediaTypes);
	}

	/**
	 * 获取此视图配置的支持的媒体类型。
	 */
	@Override
	public List<MediaType> getSupportedMediaTypes() {
		return this.mediaTypes;
	}

	/**
	 * 设置此视图的默认字符集，在{@linkplain #setSupportedMediaTypes(List) 内容类型}不包含字符集时使用。
	 * <p>默认为 {@linkplain StandardCharsets#UTF_8 UTF 8}。
	 */
	public void setDefaultCharset(Charset defaultCharset) {
		Assert.notNull(defaultCharset, "'defaultCharset' must not be null");
		this.defaultCharset = defaultCharset;
	}

	/**
	 * 获取默认字符集，在{@linkplain #setSupportedMediaTypes(List) 内容类型}不包含字符集时使用。
	 */
	public Charset getDefaultCharset() {
		return this.defaultCharset;
	}

	/**
	 * 设置此视图的{@code RequestContext}属性名称。
	 * <p>默认为无（{@code null}）。
	 */
	public void setRequestContextAttribute(@Nullable String requestContextAttribute) {
		this.requestContextAttribute = requestContextAttribute;
	}

	/**
	 * 获取此视图的{@code RequestContext}属性名称（如果有）。
	 */
	@Nullable
	public String getRequestContextAttribute() {
		return this.requestContextAttribute;
	}

	/**
	 * 设置视图的名称。有助于可追踪性。
	 * <p>在构建视图时，框架代码必须调用此方法。
	 */
	@Override
	public void setBeanName(@Nullable String beanName) {
		this.beanName = beanName;
	}

	/**
	 * 获取视图的名称。
	 * <p>如果视图配置正确，名称永远不应为{@code null}。
	 */
	@Nullable
	public String getBeanName() {
		return this.beanName;
	}

	@Override
	public void setApplicationContext(@Nullable ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

	@Nullable
	public ApplicationContext getApplicationContext() {
		return this.applicationContext;
	}

	/**
	 * 获取实际使用的{@link ApplicationContext}。
	 *
	 * @return {@code ApplicationContext}（永远不为{@code null}）
	 * @throws IllegalStateException 如果无法获取ApplicationContext
	 * @see #getApplicationContext()
	 */
	protected final ApplicationContext obtainApplicationContext() {
		ApplicationContext applicationContext = getApplicationContext();
		Assert.state(applicationContext != null, "No ApplicationContext");
		return applicationContext;
	}


	/**
	 * 准备要渲染的模型。
	 *
	 * @param model       属性名称作为键，相应模型对象作为值的映射（如果模型为空，则映射也可以是{@code null}）
	 * @param contentType 选择用于渲染的内容类型，应与{@link #getSupportedMediaTypes() 支持的媒体类型}之一匹配
	 * @param exchange    当前交换
	 * @return 表示渲染成功与否的{@code Mono}
	 */
	@Override
	public Mono<Void> render(@Nullable Map<String, ?> model, @Nullable MediaType contentType,
							 ServerWebExchange exchange) {

		if (logger.isDebugEnabled()) {
			logger.debug(exchange.getLogPrefix() + "View " + formatViewName() +
					", model " + (model != null ? model : Collections.emptyMap()));
		}

		// 如果内容类型不为空，设置响应头的内容类型
		if (contentType != null) {
			exchange.getResponse().getHeaders().setContentType(contentType);
		}

		// 获取模型属性并合并模型
		return getModelAttributes(model, exchange).flatMap(mergedModel -> {
			// 是否暴露请求上下文？
			if (this.requestContextAttribute != null) {
				// 将请求上下文放入合并后的模型中
				mergedModel.put(this.requestContextAttribute, createRequestContext(exchange, mergedModel));
			}
			// 调用内部渲染方法
			return renderInternal(mergedModel, contentType, exchange);
		});

	}

	/**
	 * 准备用于渲染的模型。
	 * <p>默认实现创建一个组合输出Map，其中包括模型和静态属性，前者优先。
	 */
	protected Mono<Map<String, Object>> getModelAttributes(
			@Nullable Map<String, ?> model, ServerWebExchange exchange) {

		// 初始化属性映射
		Map<String, Object> attributes;
		if (model != null) {
			// 如果模型不为空，则创建线程安全的属性映射，将非空值放入其中
			attributes = new ConcurrentHashMap<>(model.size());
			for (Map.Entry<String, ?> entry : model.entrySet()) {
				if (entry.getValue() != null) {
					attributes.put(entry.getKey(), entry.getValue());
				}
			}
		} else {
			// 如果模型为空，则创建空的属性映射
			attributes = new ConcurrentHashMap<>(0);
		}

		// 解析异步属性，并在终止时移除绑定上下文属性
		return resolveAsyncAttributes(attributes)
				.then(resolveAsyncAttributes(attributes, exchange))
				.doOnTerminate(() -> exchange.getAttributes().remove(BINDING_CONTEXT_ATTRIBUTE))
				.thenReturn(attributes);
	}

	/**
	 * 使用配置的{@link ReactiveAdapterRegistry}来将异步属性适配为{@code Mono<T>}或{@code Mono<List<T>>}，然后等待将它们解析为实际值。
	 * 当返回的{@code Mono<Void>}完成时，模型中的异步属性将被其对应的解析值替换。
	 *
	 * @return result 一个在模型准备就绪时完成的{@code Mono}
	 * @since 5.1.8
	 */
	protected Mono<Void> resolveAsyncAttributes(Map<String, Object> model, ServerWebExchange exchange) {
		// 初始化异步属性列表
		List<Mono<?>> asyncAttributes = null;

		for (Map.Entry<String, ?> entry : model.entrySet()) {
			Object value = entry.getValue();
			if (value == null) {
				// 如果属性值为空，继续处理下一个属性值
				continue;
			}
			// 获取适配器，检查是否为异步类型
			ReactiveAdapter adapter = this.adapterRegistry.getAdapter(null, value);
			if (adapter != null) {
				// 如果异步属性列表为空，则初始化
				if (asyncAttributes == null) {
					asyncAttributes = new ArrayList<>();
				}
				String name = entry.getKey();
				// 如果为多值异步属性，使用Flux处理，并将结果收集到模型中
				if (adapter.isMultiValue()) {
					asyncAttributes.add(
							Flux.from(adapter.toPublisher(value))
									.collectList()
									.doOnSuccess(result -> model.put(name, result)));
				} else {
					// 如果为单值异步属性，使用Mono处理，根据结果更新模型，并添加绑定结果
					asyncAttributes.add(
							Mono.from(adapter.toPublisher(value))
									.doOnSuccess(result -> {
										if (result != null) {
											model.put(name, result);
											//添加绑定结果
											addBindingResult(name, result, model, exchange);
										} else {
											//如果结果为空，则移除属性名称
											model.remove(name);
										}
									}));
				}
			}
		}

		// 返回异步属性的组合或空的Mono
		return asyncAttributes != null ? Mono.when(asyncAttributes) : Mono.empty();

	}

	/**
	 * 将绑定结果添加到模型中。
	 *
	 * @param name     属性名
	 * @param value    属性值
	 * @param model    当前模型
	 * @param exchange 当前交换对象
	 */
	private void addBindingResult(String name, Object value, Map<String, Object> model, ServerWebExchange exchange) {
		BindingContext context = exchange.getAttribute(BINDING_CONTEXT_ATTRIBUTE);
		// 如果上下文为null或值为数组、集合、Map或简单值类型，则直接返回
		if (context == null || value.getClass().isArray() || value instanceof Collection ||
				value instanceof Map || BeanUtils.isSimpleValueType(value.getClass())) {
			return;
		}
		// 创建数据绑定器并获取绑定结果
		BindingResult result = context.createDataBinder(exchange, value, name).getBindingResult();
		// 将绑定结果放入模型中
		model.put(BindingResult.MODEL_KEY_PREFIX + name, result);
	}

	/**
	 * 使用配置的{@link ReactiveAdapterRegistry}将异步属性适配为{@code Mono<T>}或{@code Mono<List<T>>}，
	 * 然后等待将它们解析为实际值。当返回的{@code Mono<Void>}完成时，
	 * 模型中的异步属性将被其对应的已解析值替换。
	 *
	 * @return result 当模型准备就绪时完成的{@code Mono}
	 * @deprecated 从5.1.8开始，此方法仍然被调用，但它是一个空操作。
	 * 请改用{@link #resolveAsyncAttributes(Map, ServerWebExchange)}。该方法在此方法之后被调用并执行实际工作。
	 */
	@Deprecated
	protected Mono<Void> resolveAsyncAttributes(Map<String, Object> model) {
		return Mono.empty();
	}

	/**
	 * 创建一个{@link RequestContext}以在指定的属性名下公开。
	 * <p>默认实现为给定的交换和模型创建一个标准的{@code RequestContext}实例。
	 * <p>子类中可以重写此方法以创建自定义实例。
	 *
	 * @param exchange 当前交换对象
	 * @param model    一个组合输出映射（永不为{@code null}），其中动态值优先于静态属性
	 * @return {@code RequestContext}实例
	 * @see #setRequestContextAttribute
	 */
	protected RequestContext createRequestContext(ServerWebExchange exchange, Map<String, Object> model) {
		return new RequestContext(exchange, model, obtainApplicationContext(), getRequestDataValueProcessor());
	}

	/**
	 * 获取要使用的{@link RequestDataValueProcessor}。
	 * <p>默认实现在{@link #getApplicationContext() ApplicationContext}中查找名为
	 * {@link #REQUEST_DATA_VALUE_PROCESSOR_BEAN_NAME}的{@code RequestDataValueProcessor} bean。
	 *
	 * @return {@code RequestDataValueProcessor}，如果在应用程序上下文中没有则返回{@code null}
	 */
	@Nullable
	protected RequestDataValueProcessor getRequestDataValueProcessor() {
		ApplicationContext context = getApplicationContext();
		if (context != null && context.containsBean(REQUEST_DATA_VALUE_PROCESSOR_BEAN_NAME)) {
			return context.getBean(REQUEST_DATA_VALUE_PROCESSOR_BEAN_NAME, RequestDataValueProcessor.class);
		}
		return null;
	}

	/**
	 * 子类必须实现此方法来实际渲染视图。
	 *
	 * @param renderAttributes 组合输出映射（永不为{@code null}），其中动态值优先于静态属性
	 * @param contentType      选择用于渲染的内容类型，应与{@linkplain #getSupportedMediaTypes()支持的媒体类型}之一匹配
	 * @param exchange         当前交换对象
	 * @return 代表渲染成功与否的{@code Mono}
	 */
	protected abstract Mono<Void> renderInternal(Map<String, Object> renderAttributes,
												 @Nullable MediaType contentType, ServerWebExchange exchange);


	@Override
	public String toString() {
		return getClass().getName() + ": " + formatViewName();
	}

	/**
	 * 格式化视图名称。
	 *
	 * @return 视图名称的格式化字符串
	 */
	protected String formatViewName() {
		return (getBeanName() != null ?
				"name '" + getBeanName() + "'" : "[" + getClass().getSimpleName() + "]");
	}

}
