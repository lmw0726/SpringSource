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

package org.springframework.web.reactive.result.method.annotation;

import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.accept.RequestedContentTypeResolver;
import org.springframework.web.reactive.accept.RequestedContentTypeResolverBuilder;
import org.springframework.web.reactive.result.condition.ConsumesRequestCondition;
import org.springframework.web.reactive.result.condition.RequestCondition;
import org.springframework.web.reactive.result.method.RequestMappingInfo;
import org.springframework.web.reactive.result.method.RequestMappingInfoHandlerMapping;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Predicate;

/**
 * 该类是 {@link RequestMappingInfoHandlerMapping} 的扩展，从类级别和方法级别的
 * {@link RequestMapping @RequestMapping} 注解中创建 {@link RequestMappingInfo} 实例。
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 5.0
 */
public class RequestMappingHandlerMapping extends RequestMappingInfoHandlerMapping
		implements EmbeddedValueResolverAware {

	/**
	 * 一个映射路径前缀的映射表
	 */
	private final Map<String, Predicate<Class<?>>> pathPrefixes = new LinkedHashMap<>();

	/**
	 * 请求的内容类型解析器，默认使用 RequestedContentTypeResolverBuilder 构建的解析器
	 */
	private RequestedContentTypeResolver contentTypeResolver = new RequestedContentTypeResolverBuilder().build();

	/**
	 * 可空的字符串值解析器
	 */
	@Nullable
	private StringValueResolver embeddedValueResolver;

	/**
	 * RequestMappingInfo 构建器的配置信息
	 */
	private RequestMappingInfo.BuilderConfiguration config = new RequestMappingInfo.BuilderConfiguration();


	/**
	 * 配置要应用于控制器方法的路径前缀。
	 *
	 * <p>前缀用于丰富每个 {@code @RequestMapping} 方法的映射，
	 * 其控制器类型与映射中的相应 {@code Predicate} 匹配。使用第一个匹配的
	 * Predicate 对应的前缀，假定输入映射具有可预测的顺序。
	 *
	 * <p>考虑使用 {@link org.springframework.web.method.HandlerTypePredicate
	 * HandlerTypePredicate} 来分组控制器。
	 *
	 * @param prefixes 包含路径前缀的映射
	 * @see org.springframework.web.method.HandlerTypePredicate
	 * @since 5.1
	 */
	public void setPathPrefixes(Map<String, Predicate<Class<?>>> prefixes) {
		this.pathPrefixes.clear();
		prefixes.entrySet().stream()
				.filter(entry -> StringUtils.hasText(entry.getKey()))
				.forEach(entry -> this.pathPrefixes.put(entry.getKey(), entry.getValue()));
	}

	/**
	 * 获取配置的路径前缀，作为只读、可能为空的映射。
	 *
	 * @return 包含路径前缀的映射
	 * @since 5.1
	 */
	public Map<String, Predicate<Class<?>>> getPathPrefixes() {
		return Collections.unmodifiableMap(this.pathPrefixes);
	}

	/**
	 * 设置要使用的 {@link RequestedContentTypeResolver} 以确定请求的媒体类型。
	 * 如果未设置，将使用默认构造函数。
	 *
	 * @param contentTypeResolver 请求内容类型解析器
	 */
	public void setContentTypeResolver(RequestedContentTypeResolver contentTypeResolver) {
		Assert.notNull(contentTypeResolver, "'contentTypeResolver' must not be null");
		this.contentTypeResolver = contentTypeResolver;
	}

	/**
	 * 获取配置的 {@link RequestedContentTypeResolver}。
	 *
	 * @return 请求内容类型解析器
	 */
	public RequestedContentTypeResolver getContentTypeResolver() {
		return this.contentTypeResolver;
	}

	/**
	 * 设置字符串值解析器，用于解析嵌入式值。
	 *
	 * @param resolver 字符串值解析器
	 */
	@Override
	public void setEmbeddedValueResolver(StringValueResolver resolver) {
		this.embeddedValueResolver = resolver;
	}

	/**
	 * 在设置属性后进行初始化操作。创建 {@link RequestMappingInfo.BuilderConfiguration}
	 * 并设置模式解析器和内容类型解析器。
	 * 调用父类的 {@code afterPropertiesSet} 方法完成初始化。
	 */
	@Override
	public void afterPropertiesSet() {
		// 创建一个 RequestMappingInfo.BuilderConfiguration 对象
		this.config = new RequestMappingInfo.BuilderConfiguration();

		// 设置 PatternParser，用于解析路径模式
		this.config.setPatternParser(getPathPatternParser());

		// 设置 ContentTypeResolver，用于解析内容类型
		this.config.setContentTypeResolver(getContentTypeResolver());

		// 调用父类的 afterPropertiesSet() 方法，完成其他属性的初始化
		super.afterPropertiesSet();
	}


	/**
	 * {@inheritDoc}
	 * 期望处理程序具有类型级别的 @{@link Controller} 注解。
	 */
	@Override
	protected boolean isHandler(Class<?> beanType) {
		//有@Controller注解 或者 @RequestMapping注解
		return (AnnotatedElementUtils.hasAnnotation(beanType, Controller.class) ||
				AnnotatedElementUtils.hasAnnotation(beanType, RequestMapping.class));
	}

	/**
	 * 使用方法级别和类型级别的 @{@link RequestMapping} 注解创建 {@link RequestMappingInfo}。
	 *
	 * @return 创建的 {@link RequestMappingInfo}，如果方法没有 {@code @RequestMapping} 注解，则为 {@code null}。
	 * @see #getCustomMethodCondition(Method)
	 * @see #getCustomTypeCondition(Class)
	 */
	@Override
	@Nullable
	protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
		// 创建一个请求映射信息对象，根据方法创建
		RequestMappingInfo info = createRequestMappingInfo(method);

		// 如果方法创建的映射信息对象不为 null
		if (info != null) {
			// 根据处理程序类型创建请求映射信息对象
			RequestMappingInfo typeInfo = createRequestMappingInfo(handlerType);

			// 如果处理程序类型创建的映射信息对象不为 null
			if (typeInfo != null) {
				// 将处理程序类型的映射信息对象与方法创建的映射信息对象合并
				info = typeInfo.combine(info);
			}

			// 遍历路径前缀与处理程序类型的映射关系
			for (Map.Entry<String, Predicate<Class<?>>> entry : this.pathPrefixes.entrySet()) {
				// 如果路径前缀对应的谓词测试通过了处理程序类型
				if (entry.getValue().test(handlerType)) {
					String prefix = entry.getKey();

					// 如果存在嵌入式值解析器，则解析路径前缀中的嵌入式值
					if (this.embeddedValueResolver != null) {
						prefix = this.embeddedValueResolver.resolveStringValue(prefix);
					}

					// 使用路径前缀创建请求映射信息对象，并与之前的信息对象进行合并
					info = RequestMappingInfo.paths(prefix).options(this.config).build().combine(info);
					break;
				}
			}
		}

		// 返回最终的请求映射信息对象
		return info;

	}

	/**
	 * 根据提供的 {@code annotatedElement} 是类还是方法，委托给 {@link #createRequestMappingInfo(RequestMapping, RequestCondition)}，
	 * 提供相应的自定义 {@link RequestCondition}。
	 *
	 * @param element 要处理的元素，可以是类或方法
	 * @return 如果有 {@code @RequestMapping} 注解则返回创建的 {@link RequestMappingInfo}，否则返回 {@code null}
	 * @see #getCustomTypeCondition(Class)
	 * @see #getCustomMethodCondition(Method)
	 */
	@Nullable
	private RequestMappingInfo createRequestMappingInfo(AnnotatedElement element) {
		// 使用 AnnotatedElementUtils 查找给定元素上合并的 RequestMapping 注解
		RequestMapping requestMapping = AnnotatedElementUtils.findMergedAnnotation(element, RequestMapping.class);

		// 根据元素的类型获取对应的自定义条件
		RequestCondition<?> condition = (element instanceof Class ?
				getCustomTypeCondition((Class<?>) element) : getCustomMethodCondition((Method) element));

		// 如果找到了 RequestMapping 注解，则使用注解信息和自定义条件创建 RequestMappingInfo 对象；否则返回 null
		return (requestMapping != null ? createRequestMappingInfo(requestMapping, condition) : null);
	}

	/**
	 * 提供自定义类型级别的请求条件。
	 * 自定义 {@link RequestCondition} 可以是任何类型，只要从此方法的所有调用中返回相同的条件类型，
	 * 以确保自定义请求条件可以组合和比较。
	 *
	 * <p>考虑扩展 {@link org.springframework.web.reactive.result.condition.AbstractRequestCondition
	 * AbstractRequestCondition} 用于自定义条件类型，并使用
	 * {@link org.springframework.web.reactive.result.condition.CompositeRequestCondition
	 * CompositeRequestCondition} 提供多个自定义条件。
	 *
	 * @param handlerType 要为其创建条件的处理程序类型
	 * @return 条件，或 {@code null}
	 */
	@SuppressWarnings("UnusedParameters")
	@Nullable
	protected RequestCondition<?> getCustomTypeCondition(Class<?> handlerType) {
		return null;
	}

	/**
	 * 提供自定义方法级别的请求条件。
	 * 自定义 {@link RequestCondition} 可以是任何类型，只要从此方法的所有调用中返回相同的条件类型，
	 * 以确保自定义请求条件可以组合和比较。
	 * <p>考虑扩展 {@link org.springframework.web.reactive.result.condition.AbstractRequestCondition
	 * AbstractRequestCondition} 用于自定义条件类型，并使用
	 * {@link org.springframework.web.reactive.result.condition.CompositeRequestCondition
	 * CompositeRequestCondition} 提供多个自定义条件。
	 *
	 * @param method 要为其创建条件的处理程序方法
	 * @return 条件，或 {@code null}
	 */
	@SuppressWarnings("UnusedParameters")
	@Nullable
	protected RequestCondition<?> getCustomMethodCondition(Method method) {
		return null;
	}

	/**
	 * 从提供的 {@link RequestMapping @RequestMapping} 注解创建 {@link RequestMappingInfo}，
	 * 该注解可以是直接声明的注解、元注解或在注解层次结构中合成的注解属性的综合结果。
	 *
	 * @param requestMapping  提供的注解
	 * @param customCondition 自定义的请求条件
	 * @return 创建的 {@link RequestMappingInfo} 实例，如果方法没有 {@code @RequestMapping} 注解则返回 {@code null}
	 */
	protected RequestMappingInfo createRequestMappingInfo(
			RequestMapping requestMapping, @Nullable RequestCondition<?> customCondition) {

		// 创建一个 RequestMappingInfo.Builder 对象，并根据 RequestMapping 注解的属性设置映射信息
		RequestMappingInfo.Builder builder = RequestMappingInfo
				// 解析路径模式中的嵌入式值
				.paths(resolveEmbeddedValuesInPatterns(requestMapping.path()))
				// 设置请求方法
				.methods(requestMapping.method())
				// 设置请求参数
				.params(requestMapping.params())
				// 设置请求头信息
				.headers(requestMapping.headers())
				// 设置请求的消耗内容类型
				.consumes(requestMapping.consumes())
				// 设置请求的生成内容类型
				.produces(requestMapping.produces())
				// 设置映射名称
				.mappingName(requestMapping.name());

		// 如果存在自定义条件，设置自定义条件
		if (customCondition != null) {
			builder.customCondition(customCondition);
		}

		// 使用配置信息构建 RequestMappingInfo 对象并返回
		return builder.options(this.config).build();
	}

	/**
	 * 解析给定模式数组中的占位符值。
	 *
	 * @return 更新后的模式的新数组
	 */
	protected String[] resolveEmbeddedValuesInPatterns(String[] patterns) {
		// 如果不存在嵌入式值解析器，则直接返回原始的路径模式数组
		if (this.embeddedValueResolver == null) {
			return patterns;
		} else {
			// 如果存在嵌入式值解析器，则对路径模式数组中的每个模式进行解析，并将解析后的模式存储到新的数组中
			String[] resolvedPatterns = new String[patterns.length];
			for (int i = 0; i < patterns.length; i++) {
				resolvedPatterns[i] = this.embeddedValueResolver.resolveStringValue(patterns[i]);
			}
			// 返回解析后的路径模式数组
			return resolvedPatterns;
		}
	}

	/**
	 * 注册请求映射信息并更新请求内容类型条件。
	 *
	 * @param mapping 请求映射信息
	 * @param handler 处理程序对象
	 * @param method  方法对象
	 */
	@Override
	public void registerMapping(RequestMappingInfo mapping, Object handler, Method method) {
		super.registerMapping(mapping, handler, method);
		updateConsumesCondition(mapping, method);
	}

	/**
	 * 注册处理程序方法并更新请求内容类型条件。
	 *
	 * @param handler 处理程序对象
	 * @param method  方法对象
	 * @param mapping 请求映射信息
	 */
	@Override
	protected void registerHandlerMethod(Object handler, Method method, RequestMappingInfo mapping) {
		super.registerHandlerMethod(handler, method, mapping);
		updateConsumesCondition(mapping, method);
	}


	/**
	 * 更新请求映射信息中的请求内容类型条件。
	 *
	 * @param info   请求映射信息
	 * @param method 方法对象
	 */
	private void updateConsumesCondition(RequestMappingInfo info, Method method) {
		// 从 RequestMappingInfo 中获取请求的内容类型条件
		ConsumesRequestCondition condition = info.getConsumesCondition();

		// 如果条件不为空（即指定了请求的内容类型）
		if (!condition.isEmpty()) {
			// 遍历处理方法的参数
			for (Parameter parameter : method.getParameters()) {
				// 获取参数上的 @RequestBody 注解
				MergedAnnotation<RequestBody> annot = MergedAnnotations.from(parameter).get(RequestBody.class);

				// 如果参数上存在 @RequestBody 注解
				if (annot.isPresent()) {
					// 设置请求的内容类型是否为必需，根据 @RequestBody 注解中的 required 属性
					condition.setBodyRequired(annot.getBoolean("required"));
					break; // 找到第一个带 @RequestBody 注解的参数后跳出循环
				}
			}
		}
	}

	/**
	 * 初始化 CORS 配置信息。
	 *
	 * @param handler     处理程序对象
	 * @param method      处理程序方法
	 * @param mappingInfo 请求映射信息
	 * @return 初始化后的 CORS 配置信息，如果无跨域注解则返回 null
	 */
	@Override
	protected CorsConfiguration initCorsConfiguration(Object handler, Method method, RequestMappingInfo mappingInfo) {
		// 创建一个 HandlerMethod 对象，用于表示处理程序（handler）和方法
		HandlerMethod handlerMethod = createHandlerMethod(handler, method);

		// 获取处理程序的类类型和方法上的 @CrossOrigin 注解信息
		Class<?> beanType = handlerMethod.getBeanType();
		CrossOrigin typeAnnotation = AnnotatedElementUtils.findMergedAnnotation(beanType, CrossOrigin.class);
		CrossOrigin methodAnnotation = AnnotatedElementUtils.findMergedAnnotation(method, CrossOrigin.class);

		// 如果类级别和方法级别都不存在 @CrossOrigin 注解，则返回 null
		if (typeAnnotation == null && methodAnnotation == null) {
			return null;
		}

		// 创建一个 CorsConfiguration 对象，并根据类级别和方法级别的 @CrossOrigin 注解更新配置信息
		CorsConfiguration config = new CorsConfiguration();
		updateCorsConfig(config, typeAnnotation);
		updateCorsConfig(config, methodAnnotation);

		// 如果允许的方法为空，则根据映射信息中的方法条件添加允许的方法
		if (CollectionUtils.isEmpty(config.getAllowedMethods())) {
			for (RequestMethod allowedMethod : mappingInfo.getMethodsCondition().getMethods()) {
				config.addAllowedMethod(allowedMethod.name());
			}
		}

		// 应用默认的许可值到 CorsConfiguration 对象并返回
		return config.applyPermitDefaultValues();
	}

	/**
	 * 更新 CORS 配置信息。
	 *
	 * @param config     要更新的 CORS 配置
	 * @param annotation 跨域注解
	 * @throws IllegalStateException 如果 allowCredentials 值不是 "true"、"false" 或空字符串时
	 */
	private void updateCorsConfig(CorsConfiguration config, @Nullable CrossOrigin annotation) {
		// 如果注解为 null，则直接返回，不进行任何操作
		if (annotation == null) {
			return;
		}

		// 遍历注解中的 origins 属性，并将解析后的值添加到 CorsConfiguration 中的 allowed origins 中
		for (String origin : annotation.origins()) {
			config.addAllowedOrigin(resolveCorsAnnotationValue(origin));
		}

		// 遍历注解中的 originPatterns 属性，并将解析后的值添加到 CorsConfiguration 中的 allowed origin patterns 中
		for (String patterns : annotation.originPatterns()) {
			config.addAllowedOriginPattern(resolveCorsAnnotationValue(patterns));
		}

		// 遍历注解中的 methods 属性，并将方法名添加到 CorsConfiguration 中的 allowed methods 中
		for (RequestMethod method : annotation.methods()) {
			config.addAllowedMethod(method.name());
		}

		// 遍历注解中的 allowedHeaders 属性，并将解析后的值添加到 CorsConfiguration 中的 allowed headers 中
		for (String header : annotation.allowedHeaders()) {
			config.addAllowedHeader(resolveCorsAnnotationValue(header));
		}

		// 遍历注解中的 exposedHeaders 属性，并将解析后的值添加到 CorsConfiguration 中的 exposed headers 中
		for (String header : annotation.exposedHeaders()) {
			config.addExposedHeader(resolveCorsAnnotationValue(header));
		}

		// 解析 allowCredentials 属性的值，并根据值设置 CorsConfiguration 中的 allow credentials 属性
		String allowCredentials = resolveCorsAnnotationValue(annotation.allowCredentials());
		if ("true".equalsIgnoreCase(allowCredentials)) {
			config.setAllowCredentials(true);
		} else if ("false".equalsIgnoreCase(allowCredentials)) {
			config.setAllowCredentials(false);
		} else if (!allowCredentials.isEmpty()) {
			// 如果 allowCredentials 的值不是 true、false 或空字符串，则抛出异常
			throw new IllegalStateException("@CrossOrigin's allowCredentials value must be \"true\", \"false\", " +
					"or an empty string (\"\"): current value is [" + allowCredentials + "]");
		}

		// 设置 CorsConfiguration 中的 max age 属性为注解中的 maxAge 值
		if (annotation.maxAge() >= 0) {
			config.setMaxAge(annotation.maxAge());
		}
	}


	/**
	 * 解析跨域注解值中的占位符。
	 *
	 * @param value 要解析的值
	 * @return 解析后的值，如果解析失败则返回空字符串
	 */
	private String resolveCorsAnnotationValue(String value) {
		if (this.embeddedValueResolver != null) {
			String resolved = this.embeddedValueResolver.resolveStringValue(value);
			return (resolved != null ? resolved : "");
		} else {
			return value;
		}
	}

}
