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

package org.springframework.web.reactive.result.view;

import org.springframework.beans.BeanUtils;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.*;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.support.WebExchangeDataBinder;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.HandlerResult;
import org.springframework.web.reactive.HandlerResultHandler;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.result.HandlerResultHandlerSupport;
import org.springframework.web.server.NotAcceptableStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

/**
 * {@code HandlerResultHandler} 封装了支持以下返回类型的视图解析算法：
 * <ul>
 * <li>{@link Void} 或没有值 -- 默认视图名称</li>
 * <li>{@link String} -- 视图名称，除非有 {@code @ModelAttribute} 注解</li>
 * <li>{@link View} -- 要渲染的视图</li>
 * <li>{@link Model} -- 要添加到模型的属性</li>
 * <li>{@link Map} -- 要添加到模型的属性</li>
 * <li>{@link Rendering} -- 用于视图解析的用例驱动 API</li>
 * <li>{@link ModelAttribute @ModelAttribute} -- 模型的属性</li>
 * <li>非简单值 -- 模型的属性</li>
 * </ul>
 *
 * <p>基于 String 的视图名称通过配置的 {@link ViewResolver} 实例解析为要用于渲染的 {@link View}。
 * 如果未指定视图（例如，通过返回 {@code null} 或与模型相关的返回值），将选择默认视图名称。
 *
 * <p>默认情况下，此解析器的顺序为 {@link Ordered#LOWEST_PRECEDENCE}，通常需要在顺序中较晚，
 * 因为它将任何 String 返回值解释为视图名称或任何非简单值类型解释为模型属性，
 * 而其他结果处理程序可能根据注解的存在以不同方式解释相同的返回值，例如对于 {@code @ResponseBody}。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class ViewResolutionResultHandler extends HandlerResultHandlerSupport implements HandlerResultHandler, Ordered {

	/**
	 * 表示无值的占位对象
	 */
	private static final Object NO_VALUE = new Object();

	/**
	 * 表示无值的 Mono 对象
	 */
	private static final Mono<Object> NO_VALUE_MONO = Mono.just(NO_VALUE);

	/**
	 * 视图解析器列表
	 */
	private final List<ViewResolver> viewResolvers = new ArrayList<>(4);

	/**
	 * 默认视图列表
	 */
	private final List<View> defaultViews = new ArrayList<>(4);


	/**
	 * 基本构造函数使用默认的 {@link ReactiveAdapterRegistry}。
	 *
	 * @param viewResolvers       要使用的解析器
	 * @param contentTypeResolver 用于确定请求的内容类型
	 */
	public ViewResolutionResultHandler(List<ViewResolver> viewResolvers, RequestedContentTypeResolver contentTypeResolver) {
		this(viewResolvers, contentTypeResolver, ReactiveAdapterRegistry.getSharedInstance());
	}

	/**
	 * 使用 {@link ReactiveAdapterRegistry} 实例的构造函数。
	 *
	 * @param viewResolvers       要使用的视图解析器
	 * @param contentTypeResolver 用于确定请求的内容类型
	 * @param registry            用于适应响应式类型的注册表
	 */
	public ViewResolutionResultHandler(List<ViewResolver> viewResolvers, RequestedContentTypeResolver contentTypeResolver, ReactiveAdapterRegistry registry) {
		super(contentTypeResolver, registry);
		this.viewResolvers.addAll(viewResolvers);
		AnnotationAwareOrderComparator.sort(this.viewResolvers);
	}


	/**
	 * 返回一个只读的视图解析器列表。
	 */
	public List<ViewResolver> getViewResolvers() {
		return Collections.unmodifiableList(this.viewResolvers);
	}

	/**
	 * 设置在解析视图名称并尝试满足最佳匹配内容类型时始终考虑的默认视图。
	 */
	public void setDefaultViews(@Nullable List<View> defaultViews) {
		this.defaultViews.clear();
		if (defaultViews != null) {
			this.defaultViews.addAll(defaultViews);
		}
	}

	/**
	 * 返回配置的默认 {@code View} 列表。
	 */
	public List<View> getDefaultViews() {
		return this.defaultViews;
	}

	/**
	 * 检查该处理器是否支持给定的 {@link HandlerResult}。
	 *
	 * @param result 要检查的结果对象
	 * @return 是否可以使用给定结果的布尔值
	 */
	@Override
	public boolean supports(HandlerResult result) {
		// 检查返回类型是否有模型注解
		if (hasModelAnnotation(result.getReturnTypeSource())) {
			return true;
		}

		// 获取返回类型并尝试获取适配器
		Class<?> type = result.getReturnType().toClass();
		ReactiveAdapter adapter = getAdapter(result);

		// 检查适配器和类型是否指示无值
		if (adapter != null) {
			if (adapter.isNoValue()) {
				return true;
			}
			type = result.getReturnType().getGeneric().toClass();
		}

		// 检查类型是否为 CharSequence、Rendering、Model、Map、View 或非简单属性类型
		return (CharSequence.class.isAssignableFrom(type) ||
				Rendering.class.isAssignableFrom(type) ||
				Model.class.isAssignableFrom(type) ||
				Map.class.isAssignableFrom(type) ||
				View.class.isAssignableFrom(type) ||
				!BeanUtils.isSimpleProperty(type));
	}

	/**
	 * 处理给定的结果，修改响应头和/或向响应中写入数据。
	 *
	 * @param exchange 当前的服务器交换对象
	 * @param result   处理结果对象
	 * @return {@code Mono<Void>}，表示请求处理完成的情况
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Mono<Void> handleResult(ServerWebExchange exchange, HandlerResult result) {
		Mono<Object> valueMono;
		ResolvableType valueType;
		ReactiveAdapter adapter = getAdapter(result);

		// 根据返回值和适配器判断响应类型
		if (adapter != null) {
			// 处理单一值的情况
			if (adapter.isMultiValue()) {
				throw new IllegalArgumentException(
						"Multi-value reactive types not supported in view resolution: " + result.getReturnType());
			}

			// 如果适配器返回无值，则创建一个空的Mono
			valueMono = (result.getReturnValue() != null ?
					Mono.from(adapter.toPublisher(result.getReturnValue())) : Mono.empty());

			valueType = (adapter.isNoValue() ? ResolvableType.forClass(Void.class) :
					result.getReturnType().getGeneric());
		} else {
			// 如果没有适配器，直接使用返回值创建Mono
			valueMono = Mono.justOrEmpty(result.getReturnValue());
			valueType = result.getReturnType();
		}

		// 根据valueMono的情况进行下一步处理
		return valueMono
				.switchIfEmpty(exchange.isNotModified() ? Mono.empty() : NO_VALUE_MONO)
				.flatMap(returnValue -> {
					Mono<List<View>> viewsMono;
					// 获取返回值中的模型
					Model model = result.getModel();
					// 获取返回类型的方法参数
					MethodParameter parameter = result.getReturnTypeSource();
					// 获取当前请求的区域设置
					Locale locale = LocaleContextHolder.getLocale(exchange.getLocaleContext());

					// 获取返回值类型的Class对象
					Class<?> clazz = valueType.toClass();
					// 如果类型为Object，使用实际返回值的Class对象
					if (clazz == Object.class) {
						clazz = returnValue.getClass();
					}


					// 根据返回值类型和其他条件解析视图
					if (returnValue == NO_VALUE || clazz == void.class || clazz == Void.class) {
						//如果返回值没有值，或者返回类型为void或者Void，解析默认的视图名称
						viewsMono = resolveViews(getDefaultViewName(exchange), locale);
					} else if (CharSequence.class.isAssignableFrom(clazz) && !hasModelAnnotation(parameter)) {
						// 如果返回值类型为CharSequence类型或者方法参数没有@ModelAttribute注解，按照返回值解析视图名称。
						viewsMono = resolveViews(returnValue.toString(), locale);
					} else if (Rendering.class.isAssignableFrom(clazz)) {
						// 处理Rendering类型的返回值
						Rendering render = (Rendering) returnValue;
						// 获取http状态
						HttpStatus status = render.status();
						if (status != null) {
							exchange.getResponse().setStatusCode(status);
						}
						// 设置http响应的请求头
						exchange.getResponse().getHeaders().putAll(render.headers());
						// 设置模型属性
						model.addAllAttributes(render.modelAttributes());
						// 获取视图对象
						Object view = render.view();
						if (view == null) {
							//如果视图对象为空，则获取默认视图名称
							view = getDefaultViewName(exchange);
						}
						viewsMono = (view instanceof String ? resolveViews((String) view, locale) :
								Mono.just(Collections.singletonList((View) view)));
					} else if (Model.class.isAssignableFrom(clazz)) {
						// 处理Model类型的返回值
						// 将返回值转换为Model并添加其所有属性到模型中
						model.addAllAttributes(((Model) returnValue).asMap());
						// 使用默认的视图名称解析视图，获取视图列表的Mono
						viewsMono = resolveViews(getDefaultViewName(exchange), locale);

					} else if (Map.class.isAssignableFrom(clazz) && !hasModelAnnotation(parameter)) {
						// 处理Map类型的返回值
						model.addAllAttributes((Map<String, ?>) returnValue);
						// 使用默认的视图名称解析视图，获取视图列表的Mono
						viewsMono = resolveViews(getDefaultViewName(exchange), locale);
					} else if (View.class.isAssignableFrom(clazz)) {
						// 处理View类型的返回值
						viewsMono = Mono.just(Collections.singletonList((View) returnValue));
					} else {
						// 处理其他类型的返回值
						String name = getNameForReturnValue(parameter);
						model.addAttribute(name, returnValue);
						// 使用默认的视图名称解析视图，获取视图列表的Mono
						viewsMono = resolveViews(getDefaultViewName(exchange), locale);
					}
					// 更新绑定结果并渲染视图
					BindingContext bindingContext = result.getBindingContext();
					updateBindingResult(bindingContext, exchange);
					return viewsMono.flatMap(views -> render(views, model.asMap(), bindingContext, exchange));
				});
	}


	/**
	 * 是否有@ModelAttribute注解
	 *
	 * @param parameter 方法参数
	 * @return 是否有@ModelAttribute注解
	 */
	private boolean hasModelAnnotation(MethodParameter parameter) {
		return parameter.hasMethodAnnotation(ModelAttribute.class);
	}

	/**
	 * 当控制器没有指定时选择默认视图名称。
	 * 使用请求路径去掉前导和尾随斜杠。
	 */
	private String getDefaultViewName(ServerWebExchange exchange) {
		String path = exchange.getRequest().getPath().pathWithinApplication().value();
		if (path.startsWith("/")) {
			path = path.substring(1);
		}
		if (path.endsWith("/")) {
			path = path.substring(0, path.length() - 1);
		}
		return StringUtils.stripFilenameExtension(path);
	}

	/**
	 * 解析视图。
	 *
	 * @param viewName 视图名称
	 * @param locale   区域设置
	 * @return 视图列表
	 */
	private Mono<List<View>> resolveViews(String viewName, Locale locale) {
		return Flux.fromIterable(getViewResolvers())
				.concatMap(resolver -> resolver.resolveViewName(viewName, locale))
				.collectList()
				.map(views -> {
					if (views.isEmpty()) {
						throw new IllegalStateException(
								"Could not resolve view with name '" + viewName + "'.");
					}
					//添加默认视图名称列表
					views.addAll(getDefaultViews());
					return views;
				});
	}

	/**
	 * 获取返回值的名称。
	 *
	 * @param returnType 方法参数类型
	 * @return 返回值的名称
	 */
	private String getNameForReturnValue(MethodParameter returnType) {
		return Optional.ofNullable(returnType.getMethodAnnotation(ModelAttribute.class))
				.filter(ann -> StringUtils.hasText(ann.value()))
				.map(ModelAttribute::value)
				.orElseGet(() -> Conventions.getVariableNameForParameter(returnType));
	}


	/**
	 * 更新绑定结果。
	 *
	 * @param context  绑定上下文
	 * @param exchange 服务器网络交换
	 */
	private void updateBindingResult(BindingContext context, ServerWebExchange exchange) {
		// 从上下文中获取模型并转换为Map
		Map<String, Object> model = context.getModel().asMap();

		// 遍历模型中的每个键值对
		for (Map.Entry<String, Object> entry : model.entrySet()) {
			// 获取键
			String name = entry.getKey();
			// 获取值
			Object value = entry.getValue();

			// 检查是否是绑定的候选项
			if (isBindingCandidate(name, value)) {
				// 如果模型中不包含对应的绑定结果，则创建WebExchangeDataBinder并放入模型中
				if (!model.containsKey(BindingResult.MODEL_KEY_PREFIX + name)) {
					WebExchangeDataBinder binder = context.createDataBinder(exchange, value, name);
					model.put(BindingResult.MODEL_KEY_PREFIX + name, binder.getBindingResult());
				}
			}
		}

	}


	/**
	 * 判断是否可以进行绑定的候选项。
	 *
	 * @param name  名称
	 * @param value 值
	 * @return 是否为绑定候选项
	 */
	private boolean isBindingCandidate(String name, @Nullable Object value) {
		// 名称不以BindingResult.MODEL_KEY_PREFIX开头
		return (!name.startsWith(BindingResult.MODEL_KEY_PREFIX) &&
				// 值不为空
				value != null &&
				// 值不是数组
				!value.getClass().isArray() &&
				// 值不是集合
				!(value instanceof Collection) &&
				// 值不是映射
				!(value instanceof Map) &&
				// 值不是可适配的
				getAdapterRegistry().getAdapter(null, value) == null &&
				// 值不是简单数据类型
				!BeanUtils.isSimpleValueType(value.getClass()));

	}

	/**
	 * 使用一组视图渲染模型数据，并根据最佳的媒体类型呈现内容。
	 *
	 * @param views          视图列表
	 * @param model          模型数据
	 * @param bindingContext 绑定上下文
	 * @param exchange       服务器Web交换
	 * @return 渲染后的结果
	 * @throws NotAcceptableStatusException 如果未找到可接受的媒体类型
	 */
	private Mono<? extends Void> render(List<View> views, Map<String, Object> model,
										BindingContext bindingContext, ServerWebExchange exchange) throws NotAcceptableStatusException {
		for (View view : views) {
			// 如果是重定向视图
			if (view.isRedirectView()) {
				// 渲染重定向视图
				return renderWith(view, model, null, exchange, bindingContext);
			}
		}

		// 获取媒体类型并选择最佳媒体类型
		List<MediaType> mediaTypes = getMediaTypes(views);
		MediaType bestMediaType;
		try {
			// 选择最佳媒体类型
			bestMediaType = selectMediaType(exchange, () -> mediaTypes);
		} catch (NotAcceptableStatusException ex) {
			HttpStatus statusCode = exchange.getResponse().getStatusCode();
			if (statusCode != null && statusCode.isError()) {
				if (logger.isDebugEnabled()) {
					// 忽略错误响应内容
					logger.debug("Ignoring error response content (if any). " + ex.getReason());
				}
				return Mono.empty();
			}
			throw ex;
		}

		if (bestMediaType != null) {
			//最佳媒体类型存在
			for (View view : views) {
				for (MediaType mediaType : view.getSupportedMediaTypes()) {
					if (mediaType.isCompatibleWith(bestMediaType)) {
						//当前媒体类型适配最佳媒体类型，渲染视图
						return renderWith(view, model, mediaType, exchange, bindingContext);
					}
				}
			}
		}

		// 抛出不可接受的状态异常
		throw new NotAcceptableStatusException(mediaTypes);
	}


	/**
	 * 使用视图渲染，将模型数据呈现为响应内容。
	 *
	 * @param view           视图
	 * @param model          模型数据
	 * @param mediaType      媒体类型（可为空）
	 * @param exchange       服务器Web交换
	 * @param bindingContext 绑定上下文
	 * @return 渲染后的结果
	 */
	private Mono<? extends Void> renderWith(View view, Map<String, Object> model,
											@Nullable MediaType mediaType, ServerWebExchange exchange, BindingContext bindingContext) {

		exchange.getAttributes().put(View.BINDING_CONTEXT_ATTRIBUTE, bindingContext);
		return view.render(model, mediaType, exchange)
				.doOnTerminate(() -> exchange.getAttributes().remove(View.BINDING_CONTEXT_ATTRIBUTE));
	}

	/**
	 * 获取视图支持的媒体类型列表。
	 *
	 * @param views 视图列表
	 * @return 支持的媒体类型列表
	 */
	private List<MediaType> getMediaTypes(List<View> views) {
		return views.stream()
				.flatMap(view -> view.getSupportedMediaTypes().stream())
				.collect(Collectors.toList());
	}

}
