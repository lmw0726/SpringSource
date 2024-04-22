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

package org.springframework.web.servlet.mvc.method.annotation;

import org.springframework.context.EmbeddedValueResolverAware;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringValueResolver;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.MatchableHandlerMapping;
import org.springframework.web.servlet.handler.RequestMatchResult;
import org.springframework.web.servlet.mvc.condition.AbstractRequestCondition;
import org.springframework.web.servlet.mvc.condition.CompositeRequestCondition;
import org.springframework.web.servlet.mvc.condition.ConsumesRequestCondition;
import org.springframework.web.servlet.mvc.condition.RequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.RequestMappingInfoHandlerMapping;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.pattern.PathPatternParser;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * 从 {@link Controller @Controller} 类中的类型和方法级别的 {@link RequestMapping @RequestMapping}
 * 注解创建 {@link RequestMappingInfo} 实例。
 *
 * <p><strong>弃用说明:</strong></p> 在 5.2.4 中，{@link #setUseSuffixPatternMatch(boolean) useSuffixPatternMatch} 和
 * {@link #setUseRegisteredSuffixPatternMatch(boolean) useRegisteredSuffixPatternMatch} 被弃用，
 * 以此阻止使用路径扩展来进行请求映射和内容协商
 * （与 {@link org.springframework.web.accept.ContentNegotiationManagerFactoryBean ContentNegotiationManagerFactoryBean} 中的类似弃用）。
 * 有关更多上下文，请阅读问题 <a href="https://github.com/spring-projects/spring-framework/issues/24179">#24179</a>。
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 3.1
 */
public class RequestMappingHandlerMapping extends RequestMappingInfoHandlerMapping
		implements MatchableHandlerMapping, EmbeddedValueResolverAware {

	/**
	 * 是否使用后缀匹配模式
	 */
	private boolean useSuffixPatternMatch = false;

	/**
	 * 是否注册后缀匹配模式
	 */
	private boolean useRegisteredSuffixPatternMatch = false;

	/**
	 * 设置是否匹配 URL，而不考虑尾部斜杠的存在。
	 */
	private boolean useTrailingSlashMatch = true;

	/**
	 * 路径前缀映射
	 */
	private Map<String, Predicate<Class<?>>> pathPrefixes = Collections.emptyMap();

	/**
	 * 内容协商管理器
	 */
	private ContentNegotiationManager contentNegotiationManager = new ContentNegotiationManager();

	/**
	 * 字符串值解析器
	 */
	@Nullable
	private StringValueResolver embeddedValueResolver;

	/**
	 * 请求映射信息配置
	 */
	private RequestMappingInfo.BuilderConfiguration config = new RequestMappingInfo.BuilderConfiguration();


	/**
	 * 设置是否在匹配模式和请求时使用后缀模式匹配（".*"）。如果启用，映射到 "/users" 的方法也将匹配到 "/users.*"。
	 * <p>默认情况下，此属性设置为 {@code false}。
	 * <p>还可以通过 {@link #setUseRegisteredSuffixPatternMatch(boolean)} 更精细地控制允许使用的特定后缀。
	 * <p><strong>注意:</strong> 当配置了 {@link #setPatternParser(PathPatternParser)} 时，此属性将被忽略。
	 *
	 * @deprecated 自 5.2.4 起。有关路径扩展配置选项弃用的类级别注释，请参见类级别的说明。由于此方法没有替代方法，在 5.2.x 中有必要将其设置为 {@code false}。
	 * 在 5.3 中，默认值更改为 {@code false}，并且不再需要使用此属性。
	 */
	@Deprecated
	public void setUseSuffixPatternMatch(boolean useSuffixPatternMatch) {
		this.useSuffixPatternMatch = useSuffixPatternMatch;
	}

	/**
	 * 设置后缀模式匹配是否仅适用于显式注册到 {@link ContentNegotiationManager} 的路径扩展名。
	 * 通常建议这样做以减少歧义，并避免出现由于其他原因路径中出现 "." 时的问题。
	 * <p>默认情况下，此属性设置为 "false"。
	 * <p><strong>注意:</strong> 当配置了 {@link #setPatternParser(PathPatternParser)} 时，此属性将被忽略。
	 *
	 * @deprecated 自 5.2.4 起。有关路径扩展配置选项弃用的类级别注释，请参见类级别的说明。
	 */
	@Deprecated
	public void setUseRegisteredSuffixPatternMatch(boolean useRegisteredSuffixPatternMatch) {
		this.useRegisteredSuffixPatternMatch = useRegisteredSuffixPatternMatch;
		this.useSuffixPatternMatch = (useRegisteredSuffixPatternMatch || this.useSuffixPatternMatch);
	}

	/**
	 * 设置是否匹配 URL，而不考虑尾部斜杠的存在。如果启用，映射到 "/users" 的方法也将匹配到 "/users/"。
	 * <p>默认值为 {@code true}。
	 */
	public void setUseTrailingSlashMatch(boolean useTrailingSlashMatch) {
		this.useTrailingSlashMatch = useTrailingSlashMatch;
		if (getPatternParser() != null) {
			getPatternParser().setMatchOptionalTrailingSeparator(useTrailingSlashMatch);
		}
	}

	/**
	 * 配置要应用于控制器方法的路径前缀。
	 * <p>前缀用于丰富每个 {@code @RequestMapping} 方法的映射，其控制器类型与相应的 {@code Predicate} 匹配。
	 * 使用第一个匹配谓词的前缀。
	 * <p>考虑使用 {@link org.springframework.web.method.HandlerTypePredicate HandlerTypePredicate} 来对控制器进行分组。
	 *
	 * @param prefixes 一个包含路径前缀的映射
	 * @since 5.1
	 */
	public void setPathPrefixes(Map<String, Predicate<Class<?>>> prefixes) {
		this.pathPrefixes = (!prefixes.isEmpty() ?
				Collections.unmodifiableMap(new LinkedHashMap<>(prefixes)) :
				Collections.emptyMap());
	}

	/**
	 * 配置的路径前缀，作为一个只读的、可能为空的映射。
	 *
	 * @since 5.1
	 */
	public Map<String, Predicate<Class<?>>> getPathPrefixes() {
		return this.pathPrefixes;
	}

	/**
	 * 设置要用于确定请求的媒体类型的 {@link ContentNegotiationManager}。
	 * 如果未设置，则使用默认构造函数。
	 */
	public void setContentNegotiationManager(ContentNegotiationManager contentNegotiationManager) {
		Assert.notNull(contentNegotiationManager, "ContentNegotiationManager must not be null");
		this.contentNegotiationManager = contentNegotiationManager;
	}

	/**
	 * 返回配置的 {@link ContentNegotiationManager}。
	 */
	public ContentNegotiationManager getContentNegotiationManager() {
		return this.contentNegotiationManager;
	}

	@Override
	public void setEmbeddedValueResolver(StringValueResolver resolver) {
		this.embeddedValueResolver = resolver;
	}

	@Override
	@SuppressWarnings("deprecation")
	public void afterPropertiesSet() {
		// 创建请求映射信息构建器配置对象
		this.config = new RequestMappingInfo.BuilderConfiguration();
		// 设置是否匹配结尾斜杠
		this.config.setTrailingSlashMatch(useTrailingSlashMatch());
		// 设置内容协商管理器
		this.config.setContentNegotiationManager(getContentNegotiationManager());

		// 如果模式解析器不为 null
		if (getPatternParser() != null) {
			// 设置模式解析器
			this.config.setPatternParser(getPatternParser());
			// 确保不同时使用后缀模式匹配和已注册的后缀模式匹配
			Assert.isTrue(!this.useSuffixPatternMatch && !this.useRegisteredSuffixPatternMatch,
					"Suffix pattern matching not supported with PathPatternParser.");
		} else {
			// 设置是否使用后缀模式匹配
			this.config.setSuffixPatternMatch(useSuffixPatternMatch());
			// 设置是否使用已注册的后缀模式匹配
			this.config.setRegisteredSuffixPatternMatch(useRegisteredSuffixPatternMatch());
			// 设置路径匹配器
			this.config.setPathMatcher(getPathMatcher());
		}

		// 调用父类的属性设置完成方法
		super.afterPropertiesSet();

	}


	/**
	 * 是否使用已注册的后缀进行模式匹配。
	 *
	 * @deprecated 自 5.2.4 起已弃用。请参阅 {@link #setUseSuffixPatternMatch(boolean)} 上的弃用通知。
	 */
	@Deprecated
	public boolean useSuffixPatternMatch() {
		return this.useSuffixPatternMatch;
	}

	/**
	 * 是否使用已注册的后缀进行模式匹配。
	 *
	 * @deprecated 自 5.2.4 起已弃用。请参阅 {@link #setUseRegisteredSuffixPatternMatch(boolean)} 上的弃用通知。
	 */
	@Deprecated
	public boolean useRegisteredSuffixPatternMatch() {
		return this.useRegisteredSuffixPatternMatch;
	}

	/**
	 * 是否匹配 URL，无论是否存在尾部斜杠。
	 */
	public boolean useTrailingSlashMatch() {
		return this.useTrailingSlashMatch;
	}

	/**
	 * 返回用于后缀模式匹配的文件扩展名。
	 *
	 * @deprecated 自 5.2.4 起已弃用。请参阅路径扩展配置选项的弃用类级别说明。
	 */
	@Nullable
	@Deprecated
	@SuppressWarnings("deprecation")
	public List<String> getFileExtensions() {
		return this.config.getFileExtensions();
	}

	/**
	 * 获取一个 {@link RequestMappingInfo.BuilderConfiguration}，反映了此 {@code HandlerMapping} 的内部配置，
	 * 可用于设置 {@link RequestMappingInfo.Builder#options(RequestMappingInfo.BuilderConfiguration)}。
	 * <p>这对于通过 {@link #registerHandlerMethod(Object, Method, RequestMappingInfo)} 进行程序注册请求映射很有用。
	 *
	 * @return 反映内部状态的生成器配置
	 * @since 5.3.14
	 */
	public RequestMappingInfo.BuilderConfiguration getBuilderConfiguration() {
		return this.config;
	}


	/**
	 * {@inheritDoc}
	 * <p>期望处理程序具有类型级别的 @{@link Controller} 注解或类型级别的 @{@link RequestMapping} 注解。
	 */
	@Override
	protected boolean isHandler(Class<?> beanType) {
		// 如果当前bean类型上含有@Controller注解或者@RequestMapping注解，则说明是处理器
		return (AnnotatedElementUtils.hasAnnotation(beanType, Controller.class) ||
				AnnotatedElementUtils.hasAnnotation(beanType, RequestMapping.class));
	}

	/**
	 * 使用方法和类型级别的 @{@link RequestMapping} 注解来创建 RequestMappingInfo。
	 *
	 * @return 创建的 RequestMappingInfo，如果方法没有 {@code @RequestMapping} 注解，则返回 {@code null}。
	 * @see #getCustomMethodCondition(Method)
	 * @see #getCustomTypeCondition(Class)
	 */
	@Override
	@Nullable
	protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {
		// 创建请求映射信息对象
		RequestMappingInfo info = createRequestMappingInfo(method);
		// 如果请求映射信息对象不为 null
		if (info != null) {
			// 创建处理器类型的请求映射信息对象
			RequestMappingInfo typeInfo = createRequestMappingInfo(handlerType);
			// 如果处理器类型的请求映射信息对象不为 null
			if (typeInfo != null) {
				// 合并处理器类型的请求映射信息到当前请求映射信息
				info = typeInfo.combine(info);
			}
			// 获取处理器类型的路径前缀
			String prefix = getPathPrefix(handlerType);
			// 如果路径前缀不为 null
			if (prefix != null) {
				// 创建包含路径前缀的请求映射信息对象，并将其与当前请求映射信息合并
				info = RequestMappingInfo.paths(prefix).options(this.config).build().combine(info);
			}
		}
		// 返回请求映射信息对象
		return info;
	}

	@Nullable
	String getPathPrefix(Class<?> handlerType) {
		// 遍历路径前缀映射表中的每个条目
		for (Map.Entry<String, Predicate<Class<?>>> entry : this.pathPrefixes.entrySet()) {
			// 如果当前处理器类型符合该条目对应的谓词条件
			if (entry.getValue().test(handlerType)) {
				// 获取路径前缀
				String prefix = entry.getKey();
				// 如果存在嵌入值解析器
				if (this.embeddedValueResolver != null) {
					// 解析路径前缀中的嵌入值
					prefix = this.embeddedValueResolver.resolveStringValue(prefix);
				}
				// 返回解析后的路径前缀
				return prefix;
			}
		}
		// 如果没有匹配的路径前缀，则返回 null
		return null;
	}

	/**
	 * 委托给 {@link #createRequestMappingInfo(RequestMapping, RequestCondition)}，
	 * 根据提供的 {@code annotatedElement} 是类还是方法，提供适当的自定义 {@link RequestCondition}。
	 *
	 * @see #getCustomTypeCondition(Class)
	 * @see #getCustomMethodCondition(Method)
	 */
	@Nullable
	private RequestMappingInfo createRequestMappingInfo(AnnotatedElement element) {
		// 查找注解元素上合并的 @RequestMapping 注解
		RequestMapping requestMapping = AnnotatedElementUtils.findMergedAnnotation(element, RequestMapping.class);
		// 获取自定义类型或方法条件
		RequestCondition<?> condition = (element instanceof Class ?
				getCustomTypeCondition((Class<?>) element) : getCustomMethodCondition((Method) element));
		// 如果存在 @RequestMapping 注解，则创建请求映射信息对象，否则返回 null
		return (requestMapping != null ? createRequestMappingInfo(requestMapping, condition) : null);
	}

	/**
	 * 提供自定义的类型级别请求条件。
	 * 自定义 {@link RequestCondition} 可以是任何类型，只要在所有调用此方法时返回相同的条件类型，
	 * 以确保可以组合和比较自定义请求条件。
	 * <p>考虑扩展 {@link AbstractRequestCondition} 用于自定义条件类型，
	 * 并使用 {@link CompositeRequestCondition} 提供多个自定义条件。
	 *
	 * @param handlerType 要为其创建条件的处理程序类型
	 * @return 条件，或 {@code null}
	 */
	@Nullable
	protected RequestCondition<?> getCustomTypeCondition(Class<?> handlerType) {
		return null;
	}

	/**
	 * 提供自定义的方法级别请求条件。
	 * 自定义 {@link RequestCondition} 可以是任何类型，只要在所有调用此方法时返回相同的条件类型，
	 * 以确保可以组合和比较自定义请求条件。
	 * <p>考虑扩展 {@link AbstractRequestCondition} 用于自定义条件类型，
	 * 并使用 {@link CompositeRequestCondition} 提供多个自定义条件。
	 *
	 * @param method 要为其创建条件的处理程序方法
	 * @return 条件，或 {@code null}
	 */
	@Nullable
	protected RequestCondition<?> getCustomMethodCondition(Method method) {
		return null;
	}

	/**
	 * 从提供的 {@link RequestMapping @RequestMapping} 注解创建一个 {@link RequestMappingInfo}，
	 * 它可以是直接声明的注解、元注解，或在注解层次结构中合并注解属性后合成的结果。
	 */
	protected RequestMappingInfo createRequestMappingInfo(
			RequestMapping requestMapping, @Nullable RequestCondition<?> customCondition) {
		// 解析@RequestMapping的各项属性，并添加到 请求映射信息 中。
		RequestMappingInfo.Builder builder = RequestMappingInfo
				// 解析路径中的占位符
				.paths(resolveEmbeddedValuesInPatterns(requestMapping.path()))
				.methods(requestMapping.method())
				.params(requestMapping.params())
				.headers(requestMapping.headers())
				.consumes(requestMapping.consumes())
				.produces(requestMapping.produces())
				.mappingName(requestMapping.name());
		if (customCondition != null) {
			// 如果自定义条件存在，则设置自定义条件
			builder.customCondition(customCondition);
		}
		return builder.options(this.config).build();
	}

	/**
	 * 解析模式数组中的占位符值。
	 *
	 * @return 包含更新后模式的新数组
	 */
	protected String[] resolveEmbeddedValuesInPatterns(String[] patterns) {
		if (this.embeddedValueResolver == null) {
			return patterns;
		} else {
			// 字符串值解析器存在
			String[] resolvedPatterns = new String[patterns.length];
			for (int i = 0; i < patterns.length; i++) {
				// 逐个解析占位符
				resolvedPatterns[i] = this.embeddedValueResolver.resolveStringValue(patterns[i]);
			}
			return resolvedPatterns;
		}
	}


	@Override
	public void registerMapping(RequestMappingInfo mapping, Object handler, Method method) {
		// 注册映射
		super.registerMapping(mapping, handler, method);
		// 更新消费条件
		updateConsumesCondition(mapping, method);
	}

	/**
	 * {@inheritDoc}
	 * <p><strong>注意:</strong> 要创建 {@link RequestMappingInfo}，
	 * 请使用 {@link #getBuilderConfiguration()} 并在
	 * {@link RequestMappingInfo.Builder#options(RequestMappingInfo.BuilderConfiguration)}
	 * 上设置选项，以匹配此 {@code HandlerMapping} 的配置方式。例如，这很重要，
	 * 可以确保使用基于 {@link org.springframework.web.util.pattern.PathPattern}
	 * 或 {@link org.springframework.util.PathMatcher} 的匹配。
	 *
	 * @param handler 处理器的 bean 名称或处理器实例
	 * @param method  要注册的方法
	 * @param mapping 与处理器方法关联的映射条件
	 */
	@Override
	protected void registerHandlerMethod(Object handler, Method method, RequestMappingInfo mapping) {
		// 注册处理器方法
		super.registerHandlerMethod(handler, method, mapping);
		// 更新消费条件
		updateConsumesCondition(mapping, method);
	}

	private void updateConsumesCondition(RequestMappingInfo info, Method method) {
		// 获取请求方法的 消费 条件
		ConsumesRequestCondition condition = info.getConsumesCondition();
		// 如果 消费 条件不为空
		if (!condition.isEmpty()) {
			// 遍历方法的参数
			for (Parameter parameter : method.getParameters()) {
				// 获取参数上的 @RequestBody 注解
				MergedAnnotation<RequestBody> annot = MergedAnnotations.from(parameter).get(RequestBody.class);
				// 如果参数上存在 RequestBody 注解
				if (annot.isPresent()) {
					// 设置请求体是否必须的标志
					condition.setBodyRequired(annot.getBoolean("required"));
					// 结束循环
					break;
				}
			}
		}
	}

	@Override
	public RequestMatchResult match(HttpServletRequest request, String pattern) {
		Assert.isNull(getPatternParser(), "This HandlerMapping requires a PathPattern");
		// 创建请求映射信息对象
		RequestMappingInfo info = RequestMappingInfo.paths(pattern).options(this.config).build();
		// 获取匹配的请求映射信息
		RequestMappingInfo match = info.getMatchingCondition(request);
		// 如果匹配的请求映射信息不为 null，并且其模式条件不为 null
		return (match != null && match.getPatternsCondition() != null ?
				// 创建请求匹配结果对象，并返回
				new RequestMatchResult(
						match.getPatternsCondition().getPatterns().iterator().next(),
						UrlPathHelper.getResolvedLookupPath(request),
						getPathMatcher()) : null);
	}

	@Override
	protected CorsConfiguration initCorsConfiguration(Object handler, Method method, RequestMappingInfo mappingInfo) {
		// 创建处理器方法对象
		HandlerMethod handlerMethod = createHandlerMethod(handler, method);
		// 获取处理器类的类型
		Class<?> beanType = handlerMethod.getBeanType();
		// 查找处理器类上的 @CrossOrigin 注解
		CrossOrigin typeAnnotation = AnnotatedElementUtils.findMergedAnnotation(beanType, CrossOrigin.class);
		// 查找方法上的 @CrossOrigin 注解
		CrossOrigin methodAnnotation = AnnotatedElementUtils.findMergedAnnotation(method, CrossOrigin.class);

		// 如果处理器类和方法上都没有 CrossOrigin 注解，则返回 null
		if (typeAnnotation == null && methodAnnotation == null) {
			return null;
		}

		// 创建跨域配置对象
		CorsConfiguration config = new CorsConfiguration();
		// 更新跨域类注解和方法注解上的配置对象
		updateCorsConfig(config, typeAnnotation);
		updateCorsConfig(config, methodAnnotation);

		// 如果允许的方法为空，则添加映射信息中定义的方法
		if (CollectionUtils.isEmpty(config.getAllowedMethods())) {
			for (RequestMethod allowedMethod : mappingInfo.getMethodsCondition().getMethods()) {
				config.addAllowedMethod(allowedMethod.name());
			}
		}

		// 应用默认的允许值，并返回配置对象
		return config.applyPermitDefaultValues();
	}

	private void updateCorsConfig(CorsConfiguration config, @Nullable CrossOrigin annotation) {
		// 如果注解为空，则直接返回
		if (annotation == null) {
			return;
		}
		// 遍历允许的来源
		for (String origin : annotation.origins()) {
			config.addAllowedOrigin(resolveCorsAnnotationValue(origin));
		}
		// 遍历允许的来源模式
		for (String patterns : annotation.originPatterns()) {
			config.addAllowedOriginPattern(resolveCorsAnnotationValue(patterns));
		}
		// 遍历允许的方法
		for (RequestMethod method : annotation.methods()) {
			config.addAllowedMethod(method.name());
		}
		// 遍历允许的头部
		for (String header : annotation.allowedHeaders()) {
			config.addAllowedHeader(resolveCorsAnnotationValue(header));
		}
		// 遍历暴露的头部
		for (String header : annotation.exposedHeaders()) {
			config.addExposedHeader(resolveCorsAnnotationValue(header));
		}

		// 解析 allowCredentials 值
		String allowCredentials = resolveCorsAnnotationValue(annotation.allowCredentials());
		if ("true".equalsIgnoreCase(allowCredentials)) {
			// 设置为支持用户凭证
			config.setAllowCredentials(true);
		} else if ("false".equalsIgnoreCase(allowCredentials)) {
			config.setAllowCredentials(false);
		} else if (!allowCredentials.isEmpty()) {
			// 如果 allowCredentials 值不为空且不是 true 或 false，则抛出异常
			throw new IllegalStateException("@CrossOrigin's allowCredentials value must be \"true\", \"false\", " +
					"or an empty string (\"\"): current value is [" + allowCredentials + "]");
		}

		// 设置预检请求结果的有效期
		if (annotation.maxAge() >= 0) {
			config.setMaxAge(annotation.maxAge());
		}
	}

	private String resolveCorsAnnotationValue(String value) {
		if (this.embeddedValueResolver != null) {
			// 如果字符串值解析器存在，则解析字符串
			String resolved = this.embeddedValueResolver.resolveStringValue(value);
			return (resolved != null ? resolved : "");
		} else {
			return value;
		}
	}

}
