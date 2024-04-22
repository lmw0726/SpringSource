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

package org.springframework.web.servlet.handler;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.MethodIntrospector;
import org.springframework.lang.Nullable;
import org.springframework.util.*;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.pattern.PathPatternParser;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * {@link HandlerMethod} 的映射和请求之间定义映射关系的 {@link HandlerMapping} 实现的抽象基类。
 *
 * <p>对于每个注册的处理程序方法，使用子类定义映射类型 {@code <T>} 的详细信息。
 *
 * @param <T> 包含将处理程序方法匹配到传入请求所需条件的 {@link HandlerMethod} 的映射。
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 3.1
 */
public abstract class AbstractHandlerMethodMapping<T> extends AbstractHandlerMapping implements InitializingBean {

	/**
	 * 用于作用域代理后面的目标bean的Bean名称前缀。用于排除这些目标bean，而不是相应的代理，以便优先使用处理程序方法。
	 * <p>我们在这里不检查自动装配候选状态，这是处理自动装配级别上的代理目标过滤问题的方式，
	 * 因为自动装配候选状态可能已经被设置为{@code false}，但仍然希望该bean符合处理程序方法的条件。
	 * <p>最初在{@link org.springframework.aop.scope.ScopedProxyUtils}中定义，但在此处重复定义，以避免对spring-aop模块的硬依赖。
	 */
	private static final String SCOPED_TARGET_NAME_PREFIX = "scopedTarget.";

	private static final HandlerMethod PREFLIGHT_AMBIGUOUS_MATCH =
			new HandlerMethod(new EmptyHandler(), ClassUtils.getMethod(EmptyHandler.class, "handle"));

	/**
	 * 允许跨域配置
	 */
	private static final CorsConfiguration ALLOW_CORS_CONFIG = new CorsConfiguration();

	static {
		// 添加允许的跨域来源模式，允许所有来源
		ALLOW_CORS_CONFIG.addAllowedOriginPattern("*");
		// 添加允许的所有方法
		ALLOW_CORS_CONFIG.addAllowedMethod("*");
		// 添加允许的所有头部信息
		ALLOW_CORS_CONFIG.addAllowedHeader("*");
		// 设置是否允许发送身份凭证（例如，cookies）
		ALLOW_CORS_CONFIG.setAllowCredentials(true);
	}

	/**
	 * 是否在祖先ApplicationContext中检测bean中的处理程序方法
	 */
	private boolean detectHandlerMethodsInAncestorContexts = false;

	/**
	 * 命名策略
	 */
	@Nullable
	private HandlerMethodMappingNamingStrategy<T> namingStrategy;

	/**
	 * 映射器注册程序
	 */
	private final MappingRegistry mappingRegistry = new MappingRegistry();


	@Override
	public void setPatternParser(PathPatternParser patternParser) {
		Assert.state(this.mappingRegistry.getRegistrations().isEmpty(),
				"PathPatternParser must be set before the initialization of " +
						"request mappings through InitializingBean#afterPropertiesSet.");
		super.setPatternParser(patternParser);
	}

	/**
	 * 是否在祖先ApplicationContext中检测bean中的处理程序方法。
	 * <p>默认值为"false"：仅考虑当前ApplicationContext中的bean，即仅在定义此HandlerMapping的上下文中（通常是当前DispatcherServlet的上下文）。
	 * <p>打开此标志以在祖先上下文（通常是Spring根WebApplicationContext）中检测处理程序bean。
	 *
	 * @see #getCandidateBeanNames()
	 */
	public void setDetectHandlerMethodsInAncestorContexts(boolean detectHandlerMethodsInAncestorContexts) {
		this.detectHandlerMethodsInAncestorContexts = detectHandlerMethodsInAncestorContexts;
	}

	/**
	 * 配置用于为每个映射的处理程序方法分配默认名称的命名策略。
	 * <p>默认命名策略基于类名的大写字母，后跟"#"，然后是方法名，例如，对于名为TestController且方法名为getFoo的类，命名策略为"TC#getFoo"。
	 */
	public void setHandlerMethodMappingNamingStrategy(HandlerMethodMappingNamingStrategy<T> namingStrategy) {
		this.namingStrategy = namingStrategy;
	}

	/**
	 * 返回配置的命名策略，如果没有则返回{@code null}。
	 */
	@Nullable
	public HandlerMethodMappingNamingStrategy<T> getNamingStrategy() {
		return this.namingStrategy;
	}

	/**
	 * 返回一个（只读）映射，其中包含所有映射和HandlerMethod。
	 */
	public Map<T, HandlerMethod> getHandlerMethods() {
		// 获取映射注册表的读锁
		this.mappingRegistry.acquireReadLock();
		try {
			// 返回不可修改的映射，其中键是注册的 URL 路径，值是处理程序方法
			return Collections.unmodifiableMap(
					this.mappingRegistry.getRegistrations().entrySet().stream()
							.collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().handlerMethod)));
		} finally {
			// 释放映射注册表的读锁
			this.mappingRegistry.releaseReadLock();
		}
	}

	/**
	 * 返回给定映射名称的处理程序方法。
	 *
	 * @param mappingName 映射名称
	 * @return 与之匹配的HandlerMethod的列表，或{@code null}；返回的列表永远不会被修改，可以安全地迭代。
	 * @see #setHandlerMethodMappingNamingStrategy
	 */
	@Nullable
	public List<HandlerMethod> getHandlerMethodsForMappingName(String mappingName) {
		return this.mappingRegistry.getHandlerMethodsByMappingName(mappingName);
	}

	/**
	 * 返回内部映射注册表。为测试目的而提供。
	 */
	MappingRegistry getMappingRegistry() {
		return this.mappingRegistry;
	}

	/**
	 * 注册给定的映射。
	 * <p>此方法可以在初始化完成后在运行时调用。
	 *
	 * @param mapping 映射到处理程序方法的映射
	 * @param handler 处理程序
	 * @param method  方法
	 */
	public void registerMapping(T mapping, Object handler, Method method) {
		if (logger.isTraceEnabled()) {
			logger.trace("Register \"" + mapping + "\" to " + method.toGenericString());
		}
		this.mappingRegistry.register(mapping, handler, method);
	}

	/**
	 * 注销给定的映射。
	 * <p>此方法可以在初始化完成后在运行时调用。
	 *
	 * @param mapping 要注销的映射
	 */
	public void unregisterMapping(T mapping) {
		if (logger.isTraceEnabled()) {
			logger.trace("Unregister mapping \"" + mapping + "\"");
		}
		this.mappingRegistry.unregister(mapping);
	}


	// 处理程序方法检测

	/**
	 * 在初始化时检测处理程序方法。
	 *
	 * @see #initHandlerMethods
	 */
	@Override
	public void afterPropertiesSet() {
		initHandlerMethods();
	}

	/**
	 * 扫描ApplicationContext中的bean，检测并注册处理程序方法。
	 *
	 * @see #getCandidateBeanNames()
	 * @see #processCandidateBean
	 * @see #handlerMethodsInitialized
	 */
	protected void initHandlerMethods() {
		// 遍历候选的 Bean 名称列表
		for (String beanName : getCandidateBeanNames()) {
			// 如果 Bean 名称不以指定的前缀开头
			if (!beanName.startsWith(SCOPED_TARGET_NAME_PREFIX)) {
				// 处理候选的 Bean
				processCandidateBean(beanName);
			}
		}
		// 初始化处理程序方法
		handlerMethodsInitialized(getHandlerMethods());
	}

	/**
	 * 确定应用程序上下文中候选bean的名称。
	 *
	 * @see #setDetectHandlerMethodsInAncestorContexts
	 * @see BeanFactoryUtils#beanNamesForTypeIncludingAncestors
	 * @since 5.1
	 */
	protected String[] getCandidateBeanNames() {
		// 如果检测到在祖先上下文中的处理程序方法
		return (this.detectHandlerMethodsInAncestorContexts ?
				// 获取包括祖先在内的类型为 Object 的所有 Bean 的名称
				BeanFactoryUtils.beanNamesForTypeIncludingAncestors(obtainApplicationContext(), Object.class) :
				// 否则获取当前应用上下文中类型为 Object 的所有 Bean 的名称
				obtainApplicationContext().getBeanNamesForType(Object.class));
	}

	/**
	 * 确定指定候选bean的类型，并在识别为处理程序类型时调用{@link #detectHandlerMethods}。
	 * <p>此实现通过检查{@link org.springframework.beans.factory.BeanFactory＃getType}
	 * 避免了bean创建，并使用bean名称调用{@link #detectHandlerMethods}。
	 *
	 * @param beanName 候选bean的名称
	 * @see #isHandler
	 * @see #detectHandlerMethods
	 * @since 5.1
	 */
	protected void processCandidateBean(String beanName) {
		Class<?> beanType = null;
		try {
			// 获取bean名称对应的bean类型
			beanType = obtainApplicationContext().getType(beanName);
		} catch (Throwable ex) {
			// 无法解析的bean类型，可能来自延迟bean - 让我们忽略它。
			if (logger.isTraceEnabled()) {
				logger.trace("Could not resolve type for bean '" + beanName + "'", ex);
			}
		}
		if (beanType != null && isHandler(beanType)) {
			//如果存在bean类型，并且是一个处理器，则检测处理器方法
			detectHandlerMethods(beanName);
		}
	}

	/**
	 * 查找指定处理程序bean中的处理程序方法。
	 *
	 * @param handler 处理程序bean的名称或实际实例
	 * @see #getMappingForMethod
	 */
	protected void detectHandlerMethods(Object handler) {
		// 如果处理程序是一个字符串类型的 Bean 名称
		Class<?> handlerType = (handler instanceof String ?
				// 获取该 Bean 的类型
				obtainApplicationContext().getType((String) handler) : handler.getClass());

		// 如果处理程序的类型不为 null
		if (handlerType != null) {
			// 获取用户类，去除代理等封装层，得到真实的类
			Class<?> userType = ClassUtils.getUserClass(handlerType);
			// 选择处理程序类中的方法，并进行注册
			Map<Method, T> methods = MethodIntrospector.selectMethods(userType,
					// 方法信息查找器，查找方法的注解等信息
					(MethodIntrospector.MetadataLookup<T>) method -> {
						try {
							// 获取方法的映射信息
							return getMappingForMethod(method, userType);
						} catch (Throwable ex) {
							// 捕获异常，抛出状态异常，提供更详细的错误信息
							throw new IllegalStateException("Invalid mapping on handler class [" +
									userType.getName() + "]: " + method, ex);
						}
					});
			// 如果日志级别为 trace
			if (logger.isTraceEnabled()) {
				// 打印映射信息
				logger.trace(formatMappings(userType, methods));
				// 如果映射日志级别为 debug
			} else if (mappingsLogger.isDebugEnabled()) {
				// 打印映射信息
				mappingsLogger.debug(formatMappings(userType, methods));
			}
			// 遍历方法集合，注册处理程序方法
			methods.forEach((method, mapping) -> {
				// 选择可调用方法，处理 AOP 的影响
				Method invocableMethod = AopUtils.selectInvocableMethod(method, userType);
				// 注册处理程序方法
				registerHandlerMethod(handler, invocableMethod, mapping);
			});
		}
	}

	private String formatMappings(Class<?> userType, Map<Method, T> methods) {
		// 获取用户类的包名
		String packageName = ClassUtils.getPackageName(userType);
		// 格式化类型名称
		String formattedType = (StringUtils.hasText(packageName) ?
				// 如果包名不为空，则格式化成首字母缩写形式
				Arrays.stream(packageName.split("\\."))
						.map(packageSegment -> packageSegment.substring(0, 1))
						.collect(Collectors.joining(".", "", "." + userType.getSimpleName())) :
				// 否则直接使用类名
				userType.getSimpleName());
		// 方法格式化器，用于格式化方法的参数列表
		Function<Method, String> methodFormatter = method -> Arrays.stream(method.getParameterTypes())
				.map(Class::getSimpleName)
				.collect(Collectors.joining(",", "(", ")"));
		// 将方法映射转换成字符串形式
		return methods.entrySet().stream()
				.map(e -> {
					Method method = e.getKey();
					// 拼接方法名称和参数列表
					return e.getValue() + ": " + method.getName() + methodFormatter.apply(method);
				})
				.collect(Collectors.joining("\n\t", "\n\t" + formattedType + ":" + "\n\t", ""));
	}

	/**
	 * 注册处理程序方法及其唯一映射。在检测到每个处理程序方法时在启动时调用。
	 *
	 * @param handler 处理程序的bean名称或处理程序实例
	 * @param method  要注册的方法
	 * @param mapping 与处理程序方法关联的映射条件
	 * @throws IllegalStateException 如果另一个方法已经在相同的映射下注册
	 */
	protected void registerHandlerMethod(Object handler, Method method, T mapping) {
		this.mappingRegistry.register(mapping, handler, method);
	}

	/**
	 * 创建HandlerMethod实例。
	 *
	 * @param handler 处理程序的bean名称或处理程序实例
	 * @param method  目标方法
	 * @return 创建的HandlerMethod
	 */
	protected HandlerMethod createHandlerMethod(Object handler, Method method) {
		if (handler instanceof String) {
			// 如果处理程序是字符串类型（即处理程序是由bean名称引用的），则使用该名称获取相应的bean，并创建一个HandlerMethod对象
			return new HandlerMethod((String) handler,
					obtainApplicationContext().getAutowireCapableBeanFactory(),
					obtainApplicationContext(),
					method);
		}
		// 否则，直接使用处理程序对象和方法创建一个HandlerMethod对象
		return new HandlerMethod(handler, method);
	}

	/**
	 * 提取并返回映射的CORS配置。
	 */
	@Nullable
	protected CorsConfiguration initCorsConfiguration(Object handler, Method method, T mapping) {
		return null;
	}

	/**
	 * 在检测到所有处理程序方法后调用。
	 *
	 * @param handlerMethods 包含处理程序方法和映射的只读映射。
	 */
	protected void handlerMethodsInitialized(Map<T, HandlerMethod> handlerMethods) {
		// 总数包括检测到的映射数 + 通过registerMapping显式注册的映射数
		int total = handlerMethods.size();
		if ((logger.isTraceEnabled() && total == 0) || (logger.isDebugEnabled() && total > 0)) {
			logger.debug(total + " mappings in " + formatMappingName());
		}
	}


	/**
	 * 查找给定请求的处理程序方法。
	 */
	@Override
	@Nullable
	protected HandlerMethod getHandlerInternal(HttpServletRequest request) throws Exception {
		// 获取请求的查找路径
		String lookupPath = initLookupPath(request);

		// 获取映射注册表的读取锁
		this.mappingRegistry.acquireReadLock();
		try {
			// 在映射注册表中查找处理程序方法
			HandlerMethod handlerMethod = lookupHandlerMethod(lookupPath, request);

			// 如果找到了处理程序方法，则使用已解析的bean创建HandlerMethod对象；否则返回null
			return (handlerMethod != null ? handlerMethod.createWithResolvedBean() : null);
		} finally {
			// 释放映射注册表的读取锁
			this.mappingRegistry.releaseReadLock();
		}
	}

	/**
	 * 查找当前请求的最佳匹配处理程序方法。
	 * 如果找到多个匹配项，则选择最佳匹配项。
	 *
	 * @param lookupPath 当前servlet映射中的映射查找路径
	 * @param request    当前请求
	 * @return 最佳匹配的处理程序方法；如果没有匹配项，则返回{@code null}
	 * @see #handleMatch(Object, String, HttpServletRequest)
	 * @see #handleNoMatch(Set, String, HttpServletRequest)
	 */
	@Nullable
	protected HandlerMethod lookupHandlerMethod(String lookupPath, HttpServletRequest request) throws Exception {
		// 创建一个列表来存储匹配项
		List<Match> matches = new ArrayList<>();

		// 获取直接路径匹配项并添加到匹配列表中
		List<T> directPathMatches = this.mappingRegistry.getMappingsByDirectPath(lookupPath);
		if (directPathMatches != null) {
			addMatchingMappings(directPathMatches, matches, request);
		}

		// 如果匹配列表为空，则尝试匹配所有注册的映射
		if (matches.isEmpty()) {
			addMatchingMappings(this.mappingRegistry.getRegistrations().keySet(), matches, request);
		}

		// 如果匹配列表不为空，则选择最佳匹配项
		if (!matches.isEmpty()) {
			// 获取第一个匹配项
			Match bestMatch = matches.get(0);
			if (matches.size() > 1) {
				// 对匹配项进行排序并选择最佳匹配项
				Comparator<Match> comparator = new MatchComparator(getMappingComparator(request));
				matches.sort(comparator);
				// 获取第一个匹配项
				bestMatch = matches.get(0);

				// 如果是预检请求，则检查是否存在多个具有CORS配置的匹配项
				if (CorsUtils.isPreFlightRequest(request)) {
					for (Match match : matches) {
						if (match.hasCorsConfig()) {
							//如果有跨域配置，则返回不明确匹配的处理器方法
							return PREFLIGHT_AMBIGUOUS_MATCH;
						}
					}
				} else {
					// 否则，检查是否存在多个最佳匹配项
					// 获取第二个匹配项
					Match secondBestMatch = matches.get(1);
					if (comparator.compare(bestMatch, secondBestMatch) == 0) {
						// 如果第一个和第一个项排序相同，获取他们的方法
						Method m1 = bestMatch.getHandlerMethod().getMethod();
						Method m2 = secondBestMatch.getHandlerMethod().getMethod();
						String uri = request.getRequestURI();
						// 抛出异常
						throw new IllegalStateException(
								"Ambiguous handler methods mapped for '" + uri + "': {" + m1 + ", " + m2 + "}");
					}
				}
			}

			// 将最佳匹配项的处理程序方法设置为请求属性
			request.setAttribute(BEST_MATCHING_HANDLER_ATTRIBUTE, bestMatch.getHandlerMethod());
			// 处理匹配项
			handleMatch(bestMatch.mapping, lookupPath, request);
			// 返回处理方法
			return bestMatch.getHandlerMethod();
		} else {
			// 如果没有匹配项，则处理没有匹配的情况
			return handleNoMatch(this.mappingRegistry.getRegistrations().keySet(), lookupPath, request);
		}
	}

	private void addMatchingMappings(Collection<T> mappings, List<Match> matches, HttpServletRequest request) {
		for (T mapping : mappings) {
			// 获取匹配映射
			T match = getMatchingMapping(mapping, request);
			if (match != null) {
				// 如果添加进匹配列表中
				matches.add(new Match(match, this.mappingRegistry.getRegistrations().get(mapping)));
			}
		}
	}

	/**
	 * 当找到匹配的映射时调用。
	 *
	 * @param mapping    匹配的映射
	 * @param lookupPath 当前servlet映射中的映射查找路径
	 * @param request    当前请求
	 */
	protected void handleMatch(T mapping, String lookupPath, HttpServletRequest request) {
		request.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, lookupPath);
	}

	/**
	 * 当未找到匹配的映射时调用。
	 *
	 * @param mappings   所有已注册的映射
	 * @param lookupPath 当前servlet映射中的映射查找路径
	 * @param request    当前请求
	 * @throws ServletException 如果发生错误
	 */
	@Nullable
	protected HandlerMethod handleNoMatch(Set<T> mappings, String lookupPath, HttpServletRequest request)
			throws Exception {

		return null;
	}

	@Override
	protected boolean hasCorsConfigurationSource(Object handler) {
		// 检查是否存在全局的跨域配置源
		return super.hasCorsConfigurationSource(handler) ||
				// 或请求处理程序（HandlerMethod）是否具有显式的CORS配置
				(handler instanceof HandlerMethod &&
						this.mappingRegistry.getCorsConfiguration((HandlerMethod) handler) != null);
	}

	@Override
	protected CorsConfiguration getCorsConfiguration(Object handler, HttpServletRequest request) {
		// 获取处理程序的跨域配置
		CorsConfiguration corsConfig = super.getCorsConfiguration(handler, request);
		if (handler instanceof HandlerMethod) {
			HandlerMethod handlerMethod = (HandlerMethod) handler;
			// 检查处理程序是否为预检请求的模糊匹配
			if (handlerMethod.equals(PREFLIGHT_AMBIGUOUS_MATCH)) {
				// 如果是，返回允许任何跨域请求的默认跨域配置
				return AbstractHandlerMethodMapping.ALLOW_CORS_CONFIG;
			} else {
				// 合并处理程序方法的跨域配置和全局的跨域配置
				CorsConfiguration corsConfigFromMethod = this.mappingRegistry.getCorsConfiguration(handlerMethod);
				corsConfig = (corsConfig != null ? corsConfig.combine(corsConfigFromMethod) : corsConfigFromMethod);
			}
		}
		return corsConfig;
	}


	// 抽象模板方法

	/**
	 * 给定的类型是否是具有处理程序方法的处理程序。
	 *
	 * @param beanType 正在检查的bean的类型
	 * @return 如果这是一个处理程序类型，则为“true”，否则为“false”。
	 */
	protected abstract boolean isHandler(Class<?> beanType);

	/**
	 * 为处理程序方法提供映射。对于无法提供映射的方法，不是处理程序方法。
	 *
	 * @param method      要为其提供映射的方法
	 * @param handlerType 处理程序类型，可能是方法声明类的子类型
	 * @return 映射，如果方法没有映射，则返回{@code null}
	 */
	@Nullable
	protected abstract T getMappingForMethod(Method method, Class<?> handlerType);

	/**
	 * 提取并返回提供的映射中包含的URL路径。
	 *
	 * @deprecated 自5.3起，使用{@link #getDirectPaths(Object)}代替提供非模式映射
	 */
	@Deprecated
	protected Set<String> getMappingPathPatterns(T mapping) {
		return Collections.emptySet();
	}

	/**
	 * 返回不是模式的请求映射路径。
	 *
	 * @since 5.3
	 */
	protected Set<String> getDirectPaths(T mapping) {
		// 初始化一个空的URL集合
		Set<String> urls = Collections.emptySet();
		// 遍历映射路径模式
		for (String path : getMappingPathPatterns(mapping)) {
			// 检查路径是否是一个模式
			if (!getPathMatcher().isPattern(path)) {
				// 如果路径不是模式，则将其添加到URL集合中
				urls = (urls.isEmpty() ? new HashSet<>(1) : urls);
				urls.add(path);
			}
		}
		// 返回URL集合
		return urls;
	}

	/**
	 * 检查映射是否与当前请求匹配，并返回一个（可能是新的）映射，其中包含与当前请求相关的条件。
	 *
	 * @param mapping 要获取匹配项的映射
	 * @param request 当前的HTTP servlet请求
	 * @return 匹配项，如果映射不匹配则返回{@code null}
	 */
	@Nullable
	protected abstract T getMatchingMapping(T mapping, HttpServletRequest request);

	/**
	 * 返回用于对匹配的映射进行排序的比较器。
	 * 返回的比较器应将“更好的”匹配项排序得更高。
	 *
	 * @param request 当前请求
	 * @return 比较器（永远不会是{@code null}）
	 */
	protected abstract Comparator<T> getMappingComparator(HttpServletRequest request);


	/**
	 * 一个维护所有映射到处理程序方法的注册表，提供执行查找的方法并提供并发访问的注册表。
	 * <p>出于测试目的的包私有。
	 */
	class MappingRegistry {

		/**
		 * 映射和注册器映射
		 */
		private final Map<T, MappingRegistration<T>> registry = new HashMap<>();

		/**
		 * 查找到的路径和映射器映射
		 */
		private final MultiValueMap<String, T> pathLookup = new LinkedMultiValueMap<>();

		/**
		 * 查找到的名称和处理器方法映射
		 */
		private final Map<String, List<HandlerMethod>> nameLookup = new ConcurrentHashMap<>();

		/**
		 * 处理器方法和跨域配置映射
		 */
		private final Map<HandlerMethod, CorsConfiguration> corsLookup = new ConcurrentHashMap<>();

		/**
		 * 读写锁
		 */
		private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

		/**
		 * 返回所有注册。
		 *
		 * @since 5.3
		 */
		public Map<T, MappingRegistration<T>> getRegistrations() {
			return this.registry;
		}

		/**
		 * 根据给定的URL路径返回匹配项。不是线程安全的。
		 *
		 * @see #acquireReadLock()
		 */
		@Nullable
		public List<T> getMappingsByDirectPath(String urlPath) {
			return this.pathLookup.get(urlPath);
		}

		/**
		 * 根据映射名称返回处理程序方法。线程安全，可并发使用。
		 */
		public List<HandlerMethod> getHandlerMethodsByMappingName(String mappingName) {
			return this.nameLookup.get(mappingName);
		}

		/**
		 * 返回CORS配置。线程安全，可并发使用。
		 */
		@Nullable
		public CorsConfiguration getCorsConfiguration(HandlerMethod handlerMethod) {
			// 从处理程序方法获取已解析的处理程序方法
			HandlerMethod original = handlerMethod.getResolvedFromHandlerMethod();
			// 返回从跨域查找器中获取的跨域配置
			return this.corsLookup.get(original != null ? original : handlerMethod);
		}

		/**
		 * 使用getMappings和getMappingsByUrl时获取读锁。
		 */
		public void acquireReadLock() {
			this.readWriteLock.readLock().lock();
		}

		/**
		 * 在使用getMappings和getMappingsByUrl后释放读锁。
		 */
		public void releaseReadLock() {
			this.readWriteLock.readLock().unlock();
		}


		public void register(T mapping, Object handler, Method method) {
			// 获取写锁
			this.readWriteLock.writeLock().lock();
			try {
				// 创建处理程序方法
				HandlerMethod handlerMethod = createHandlerMethod(handler, method);
				// 验证方法映射
				validateMethodMapping(handlerMethod, mapping);

				// 获取直接路径
				Set<String> directPaths = AbstractHandlerMethodMapping.this.getDirectPaths(mapping);
				// 将路径映射添加到路径查找器中
				for (String path : directPaths) {
					this.pathLookup.add(path, mapping);
				}

				// 如果有命名策略，则获取名称并添加映射名称
				String name = null;
				if (getNamingStrategy() != null) {
					name = getNamingStrategy().getName(handlerMethod, mapping);
					addMappingName(name, handlerMethod);
				}

				// 初始化跨域配置
				CorsConfiguration corsConfig = initCorsConfiguration(handler, method, mapping);
				if (corsConfig != null) {
					// 如果跨域配置不为空，则验证允许凭据
					corsConfig.validateAllowCredentials();
					// 并将处理程序方法和跨域配置添加到跨域查找器中
					this.corsLookup.put(handlerMethod, corsConfig);
				}

				// 将映射注册到注册表中
				this.registry.put(mapping,
						new MappingRegistration<>(mapping, handlerMethod, directPaths, name, corsConfig != null));
			} finally {
				// 释放写锁
				this.readWriteLock.writeLock().unlock();
			}
		}

		private void validateMethodMapping(HandlerMethod handlerMethod, T mapping) {
			// 从注册表中获取映射注册信息
			MappingRegistration<T> registration = this.registry.get(mapping);
			// 如果注册信息不为空，则获取已存在的处理程序方法
			HandlerMethod existingHandlerMethod = (registration != null ? registration.getHandlerMethod() : null);
			if (existingHandlerMethod != null && !existingHandlerMethod.equals(handlerMethod)) {
				// 如果已存在的处理程序方法不为空且不等于当前处理程序方法，则抛出异常，表示映射不明确
				throw new IllegalStateException(
						"Ambiguous mapping. Cannot map '" + handlerMethod.getBean() + "' method \n" +
								handlerMethod + "\nto " + mapping + ": There is already '" +
								existingHandlerMethod.getBean() + "' bean method\n" + existingHandlerMethod + " mapped.");
			}
		}

		private void addMappingName(String name, HandlerMethod handlerMethod) {
			// 获取与名称关联的处理程序方法列表
			List<HandlerMethod> oldList = this.nameLookup.get(name);
			// 如果旧列表为空，则将其设置为空列表
			if (oldList == null) {
				oldList = Collections.emptyList();
			}

			// 检查当前处理程序方法是否已经存在于旧列表中，如果是，则直接返回
			for (HandlerMethod current : oldList) {
				if (handlerMethod.equals(current)) {
					return;
				}
			}

			// 创建一个新列表，将旧列表的元素复制到新列表中，然后将当前处理程序方法添加到新列表中
			List<HandlerMethod> newList = new ArrayList<>(oldList.size() + 1);
			newList.addAll(oldList);
			newList.add(handlerMethod);

			// 将新列表与名称关联存储在名称查找表中
			this.nameLookup.put(name, newList);
		}

		public void unregister(T mapping) {
			// 获取写锁，确保线程安全
			this.readWriteLock.writeLock().lock();
			try {
				// 从注册表中移除指定映射
				MappingRegistration<T> registration = this.registry.remove(mapping);
				if (registration == null) {
					// 如果注册表中没有找到对应的注册信息，直接返回
					return;
				}

				// 遍历注册信息中的直接路径
				for (String path : registration.getDirectPaths()) {
					// 根据路径获取映射列表
					List<T> mappings = this.pathLookup.get(path);
					if (mappings != null) {
						// 从路径查找表中移除指定的映射
						mappings.remove(registration.getMapping());
						if (mappings.isEmpty()) {
							// 如果移除后路径映射列表为空，则从路径查找表中移除该路径
							this.pathLookup.remove(path);
						}
					}
				}

				// 移除与注册信息相关联的映射名称
				removeMappingName(registration);

				// 从跨域查找表中移除与处理程序方法相关的跨域配置信息
				this.corsLookup.remove(registration.getHandlerMethod());
			} finally {
				// 释放写锁
				this.readWriteLock.writeLock().unlock();
			}
		}

		private void removeMappingName(MappingRegistration<T> definition) {
			String name = definition.getMappingName();
			if (name == null) {
				// 如果映射名称为空，直接返回
				return;
			}
			// 获取处理器方法
			HandlerMethod handlerMethod = definition.getHandlerMethod();
			// 获取旧的处理器方法列表
			List<HandlerMethod> oldList = this.nameLookup.get(name);
			if (oldList == null) {
				// 如果映射名称在名称查找表中不存在，直接返回
				return;
			}
			if (oldList.size() <= 1) {
				// 如果名称查找表中的列表大小小于等于1，直接移除该映射名称
				this.nameLookup.remove(name);
				return;
			}
			List<HandlerMethod> newList = new ArrayList<>(oldList.size() - 1);
			for (HandlerMethod current : oldList) {
				// 遍历旧列表，将不等于处理程序方法的映射添加到新列表中
				if (!current.equals(handlerMethod)) {
					newList.add(current);
				}
			}
			// 将新列表放回名称查找表中
			this.nameLookup.put(name, newList);
		}
	}


	static class MappingRegistration<T> {

		/**
		 * 映射
		 */
		private final T mapping;

		/**
		 * 处理器方法
		 */
		private final HandlerMethod handlerMethod;

		/**
		 * 直接路径集合
		 */
		private final Set<String> directPaths;

		/**
		 * 映射名称
		 */
		@Nullable
		private final String mappingName;

		/**
		 * 是否是跨域配置
		 */
		private final boolean corsConfig;

		public MappingRegistration(T mapping, HandlerMethod handlerMethod,
								   @Nullable Set<String> directPaths, @Nullable String mappingName, boolean corsConfig) {

			Assert.notNull(mapping, "Mapping must not be null");
			Assert.notNull(handlerMethod, "HandlerMethod must not be null");
			this.mapping = mapping;
			this.handlerMethod = handlerMethod;
			this.directPaths = (directPaths != null ? directPaths : Collections.emptySet());
			this.mappingName = mappingName;
			this.corsConfig = corsConfig;
		}

		public T getMapping() {
			return this.mapping;
		}

		public HandlerMethod getHandlerMethod() {
			return this.handlerMethod;
		}

		public Set<String> getDirectPaths() {
			return this.directPaths;
		}

		@Nullable
		public String getMappingName() {
			return this.mappingName;
		}

		public boolean hasCorsConfig() {
			return this.corsConfig;
		}
	}


	/**
	 * 匹配的HandlerMethod及其映射的轻量级封装，用于在当前请求上下文中使用比较器比较最佳匹配。
	 */
	private class Match {
		/**
		 * 映射
		 */
		private final T mapping;
		/**
		 * 映射注册器
		 */
		private final MappingRegistration<T> registration;

		public Match(T mapping, MappingRegistration<T> registration) {
			this.mapping = mapping;
			this.registration = registration;
		}

		public HandlerMethod getHandlerMethod() {
			return this.registration.getHandlerMethod();
		}

		public boolean hasCorsConfig() {
			return this.registration.hasCorsConfig();
		}

		@Override
		public String toString() {
			return this.mapping.toString();
		}
	}


	private class MatchComparator implements Comparator<Match> {
		/**
		 * 比较器
		 */
		private final Comparator<T> comparator;

		public MatchComparator(Comparator<T> comparator) {
			this.comparator = comparator;
		}

		@Override
		public int compare(Match match1, Match match2) {
			return this.comparator.compare(match1.mapping, match2.mapping);
		}
	}


	private static class EmptyHandler {

		@SuppressWarnings("unused")
		public void handle() {
			throw new UnsupportedOperationException("Not implemented");
		}
	}

}
