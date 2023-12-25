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

package org.springframework.web.reactive.result.method;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.MethodIntrospector;
import org.springframework.http.server.RequestPath;
import org.springframework.lang.Nullable;
import org.springframework.util.*;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.AbstractHandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * {@link HandlerMapping} 实现的抽象基类，定义请求和 {@link HandlerMethod} 之间的映射关系。
 *
 * <p>对于每个已注册的处理程序方法，都维护着一个唯一的映射，子类定义了映射类型 {@code <T>} 的细节。
 *
 * @param <T> 包含将处理程序方法与传入请求进行匹配所需条件的 {@link HandlerMethod} 的映射。
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Sam Brannen
 * @since 5.0
 */
public abstract class AbstractHandlerMethodMapping<T> extends AbstractHandlerMapping implements InitializingBean {

	/**
	 * 用于标识代理后的目标bean的Bean名称前缀。用于在处理程序方法检测中排除这些目标，而是使用相应的代理。
	 * <p>我们在这里不检查自动装配候选状态，这是处理代理目标过滤问题的方法，
	 * 因为自动装配候选可能已经被设置为{@code false}，但仍然期望该bean符合处理程序方法的条件。
	 * <p>最初定义在{@link org.springframework.aop.scope.ScopedProxyUtils}中，
	 * 但在这里重复定义以避免对spring-aop模块的硬依赖。
	 */
	private static final String SCOPED_TARGET_NAME_PREFIX = "scopedTarget.";

	/**
	 * 在请求映射比访问控制头更为微妙的情况下，在预检请求匹配时返回的HandlerMethod。
	 */
	private static final HandlerMethod PREFLIGHT_AMBIGUOUS_MATCH =
			new HandlerMethod(new PreFlightAmbiguousMatchHandler(),
					ClassUtils.getMethod(PreFlightAmbiguousMatchHandler.class, "handle"));

	/**
	 * 允许的CORS配置，用于在请求映射比访问控制头更为微妙的情况下进行预检请求匹配。
	 */
	private static final CorsConfiguration ALLOW_CORS_CONFIG = new CorsConfiguration();

	static {
		ALLOW_CORS_CONFIG.addAllowedOriginPattern("*");
		ALLOW_CORS_CONFIG.addAllowedMethod("*");
		ALLOW_CORS_CONFIG.addAllowedHeader("*");
		ALLOW_CORS_CONFIG.setAllowCredentials(true);
	}


	/**
	 * 用于管理处理程序方法的映射关系
	 */
	private final MappingRegistry mappingRegistry = new MappingRegistry();


	// TODO: handlerMethodMappingNamingStrategy

	/**
	 * 返回所有映射和 HandlerMethod 的（只读）映射。
	 */
	public Map<T, HandlerMethod> getHandlerMethods() {
		this.mappingRegistry.acquireReadLock();
		try {
			return Collections.unmodifiableMap(
					this.mappingRegistry.getRegistrations().entrySet().stream()
							.collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getHandlerMethod())));
		} finally {
			this.mappingRegistry.releaseReadLock();
		}
	}

	/**
	 * 返回内部映射注册表。供测试使用。
	 */
	MappingRegistry getMappingRegistry() {
		return this.mappingRegistry;
	}

	/**
	 * 注册给定的映射。
	 * <p>此方法可能在初始化完成后在运行时被调用。
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
	 * 取消注册给定的映射。
	 * <p>此方法可能在初始化完成后在运行时被调用。
	 *
	 * @param mapping 要取消注册的映射
	 */
	public void unregisterMapping(T mapping) {
		if (logger.isTraceEnabled()) {
			logger.trace("Unregister mapping \"" + mapping);
		}
		this.mappingRegistry.unregister(mapping);
	}


	// Handler method detection

	/**
	 * 在初始化时检测处理方法。
	 */
	@Override
	public void afterPropertiesSet() {
		initHandlerMethods();

		// 总数包括检测到的映射 + 通过 registerMapping 显式注册的映射
		int total = getHandlerMethods().size();
		if ((logger.isTraceEnabled() && total == 0) || (logger.isDebugEnabled() && total > 0)) {
			logger.debug(total + " mappings in " + formatMappingName());
		}
	}

	/**
	 * 扫描 ApplicationContext 中的 bean，检测并注册处理方法。
	 *
	 * @see #isHandler(Class)
	 * @see #getMappingForMethod(Method, Class)
	 * @see #handlerMethodsInitialized(Map)
	 */
	protected void initHandlerMethods() {
		// 获取应用程序上下文中所有注册的 Object 类型的 bean 名称
		String[] beanNames = obtainApplicationContext().getBeanNamesForType(Object.class);

		// 遍历所有 bean 名称
		for (String beanName : beanNames) {
			// 排除以 SCOPED_TARGET_NAME_PREFIX 开头的 bean（这些通常是 ScopedProxy 的目标 bean）
			if (!beanName.startsWith(SCOPED_TARGET_NAME_PREFIX)) {
				Class<?> beanType = null;
				try {
					// 尝试获取 bean 的类型，可能会由于延迟加载的 bean 而抛出异常
					beanType = obtainApplicationContext().getType(beanName);
				} catch (Throwable ex) {
					// 无法解析的 bean 类型，可能来自延迟加载的 bean - 我们忽略它。
					if (logger.isTraceEnabled()) {
						logger.trace("Could not resolve type for bean '" + beanName + "'", ex);
					}
				}

				// 如果成功获取 bean 的类型，并且该类型是处理程序对象
				if (beanType != null && isHandler(beanType)) {
					// 检测并初始化处理程序对象中的映射方法
					detectHandlerMethods(beanName);
				}
			}
		}

		// 在处理程序方法初始化完成后调用回调方法
		handlerMethodsInitialized(getHandlerMethods());

	}

	/**
	 * 在处理器中查找处理方法。
	 *
	 * @param handler 处理器的 bean 名称或处理器实例
	 */
	protected void detectHandlerMethods(final Object handler) {
		// 获取处理程序对象的类型，如果处理程序是字符串，则从应用程序上下文中获取其类型，否则直接获取其类
		Class<?> handlerType = (handler instanceof String ?
				obtainApplicationContext().getType((String) handler) : handler.getClass());

		// 如果处理程序对象的类型不为空
		if (handlerType != null) {
			// 获取处理程序对象的用户类（非代理类）
			final Class<?> userType = ClassUtils.getUserClass(handlerType);

			// 选择用户类中与映射相关的方法并存储在 Map 中
			Map<Method, T> methods = MethodIntrospector.selectMethods(userType,
					(MethodIntrospector.MetadataLookup<T>) method -> getMappingForMethod(method, userType));

			// 如果日志级别为 TRACE，则记录格式化后的映射信息
			if (logger.isTraceEnabled()) {
				logger.trace(formatMappings(userType, methods));
			}
			// 如果日志级别不是 TRACE 但是 DEBUG，则使用 mappingsLogger 记录格式化后的映射信息
			else if (mappingsLogger.isDebugEnabled()) {
				mappingsLogger.debug(formatMappings(userType, methods));
			}

			// 遍历获取到的方法和对应的映射信息，并注册处理程序方法
			methods.forEach((method, mapping) -> {
				// 选择可调用的方法，考虑到可能的代理情况
				Method invocableMethod = AopUtils.selectInvocableMethod(method, userType);
				// 注册处理程序方法
				registerHandlerMethod(handler, invocableMethod, mapping);
			});
		}
	}

	/**
	 * 格式化映射信息。
	 *
	 * @param userType 类型的 Class 对象
	 * @param methods  方法与其对应的映射关系
	 * @return 格式化后的映射信息
	 */
	private String formatMappings(Class<?> userType, Map<Method, T> methods) {
		// 获取给定类 userType 的包名
		String packageName = ClassUtils.getPackageName(userType);

		// 格式化类型名称
		String formattedType = (StringUtils.hasText(packageName) ?
				// 如果包名不为空，则进行处理
				Arrays.stream(packageName.split("\\."))
						.map(packageSegment -> packageSegment.substring(0, 1))
						.collect(Collectors.joining(".", "", "." + userType.getSimpleName())) :
				// 如果包名为空，则直接使用类的简单名称
				userType.getSimpleName());

		// 定义一个方法格式化器，用于获取方法的参数类型列表
		Function<Method, String> methodFormatter = method -> Arrays.stream(method.getParameterTypes())
				.map(Class::getSimpleName)
				.collect(Collectors.joining(",", "(", ")"));

		// 返回格式化后的方法列表字符串
		return methods.entrySet().stream()
				.map(e -> {
					Method method = e.getKey();
					// 格式化方法名和参数列表
					return e.getValue() + ": " + method.getName() + methodFormatter.apply(method);
				})
				// 使用换行符连接方法信息，并在起始处添加类信息的格式化字符串
				.collect(Collectors.joining("\n\t", "\n\t" + formattedType + ":" + "\n\t", ""));
	}

	/**
	 * 注册处理器方法及其唯一映射。在启动时为每个检测到的处理器方法调用。
	 *
	 * @param handler 处理器的 bean 名称或处理器实例
	 * @param method  要注册的方法
	 * @param mapping 与处理器方法关联的映射条件
	 * @throws IllegalStateException 如果另一个方法已在相同映射下注册
	 */
	protected void registerHandlerMethod(Object handler, Method method, T mapping) {
		this.mappingRegistry.register(mapping, handler, method);
	}

	/**
	 * 创建 HandlerMethod 实例。
	 *
	 * @param handler 可能是 bean 名称或实际处理器实例
	 * @param method  目标方法
	 * @return 创建的 HandlerMethod
	 */
	protected HandlerMethod createHandlerMethod(Object handler, Method method) {
		if (handler instanceof String) {
			return new HandlerMethod((String) handler,
					obtainApplicationContext().getAutowireCapableBeanFactory(),
					obtainApplicationContext(),
					method);
		}
		return new HandlerMethod(handler, method);
	}

	/**
	 * 提取并返回映射的 CORS 配置。
	 */
	@Nullable
	protected CorsConfiguration initCorsConfiguration(Object handler, Method method, T mapping) {
		return null;
	}

	/**
	 * 在检测到所有处理器方法之后调用。
	 *
	 * @param handlerMethods 包含处理器方法和映射的只读映射。
	 */
	protected void handlerMethodsInitialized(Map<T, HandlerMethod> handlerMethods) {
	}


	// Handler method lookup

	/**
	 * 查找给定请求的处理器方法。
	 *
	 * @param exchange 当前交换信息
	 */
	@Override
	public Mono<HandlerMethod> getHandlerInternal(ServerWebExchange exchange) {
		// 获取映射注册表的读取锁
		this.mappingRegistry.acquireReadLock();
		try {
			HandlerMethod handlerMethod;
			try {
				// 查找处理方法
				handlerMethod = lookupHandlerMethod(exchange);
			} catch (Exception ex) {
				// 如果查找过程中出现异常，则返回一个包含异常的 Mono
				return Mono.error(ex);
			}
			if (handlerMethod != null) {
				// 如果找到处理方法，则创建一个已解析 bean 的新处理方法
				handlerMethod = handlerMethod.createWithResolvedBean();
			}
			// 返回一个包含处理方法的 Mono 或者一个空的 Mono
			return Mono.justOrEmpty(handlerMethod);
		} finally {
			// 在完成操作后释放映射注册表的读取锁
			this.mappingRegistry.releaseReadLock();
		}
	}

	/**
	 * 查找当前请求的最佳匹配处理器方法。
	 * 如果找到多个匹配项，则选择最佳匹配项。
	 *
	 * @param exchange 当前交换信息
	 * @return 最佳匹配的处理器方法，如果没有匹配则为 {@code null}
	 * @throws Exception 可能抛出的异常
	 * @see #handleMatch
	 * @see #handleNoMatch
	 */
	@Nullable
	protected HandlerMethod lookupHandlerMethod(ServerWebExchange exchange) throws Exception {
		// 创建一个空的匹配列表
		List<Match> matches = new ArrayList<>();

		// 查找直接路径匹配的映射
		List<T> directPathMatches = this.mappingRegistry.getMappingsByDirectPath(exchange);
		if (directPathMatches != null) {
			// 将直接路径匹配的映射添加到匹配列表中
			addMatchingMappings(directPathMatches, matches, exchange);
		}

		// 如果匹配列表为空，则尝试匹配所有注册的映射
		if (matches.isEmpty()) {
			addMatchingMappings(this.mappingRegistry.getRegistrations().keySet(), matches, exchange);
		}

		// 如果有匹配的映射
		if (!matches.isEmpty()) {
			// 根据指定的比较器对匹配列表进行排序
			Comparator<Match> comparator = new MatchComparator(getMappingComparator(exchange));
			matches.sort(comparator);

			// 获取最佳匹配的映射
			Match bestMatch = matches.get(0);

			// 如果匹配列表中有多个匹配的映射
			if (matches.size() > 1) {
				// 如果是预检请求，检查是否有跨域配置
				if (CorsUtils.isPreFlightRequest(exchange.getRequest())) {
					for (Match match : matches) {
						if (match.hasCorsConfig()) {
							return PREFLIGHT_AMBIGUOUS_MATCH;
						}
					}
				} else {
					// 获取第二个最佳匹配的映射
					Match secondBestMatch = matches.get(1);
					// 如果第一和第二最佳匹配的映射相等，则抛出异常，表示模糊匹配
					if (comparator.compare(bestMatch, secondBestMatch) == 0) {
						Method m1 = bestMatch.getHandlerMethod().getMethod();
						Method m2 = secondBestMatch.getHandlerMethod().getMethod();
						RequestPath path = exchange.getRequest().getPath();
						throw new IllegalStateException(
								"Ambiguous handler methods mapped for '" + path + "': {" + m1 + ", " + m2 + "}");
					}
				}
			}

			// 处理最佳匹配的映射
			handleMatch(bestMatch.mapping, bestMatch.getHandlerMethod(), exchange);
			return bestMatch.getHandlerMethod();
		} else {
			// 如果没有匹配的映射，则处理没有匹配的情况
			return handleNoMatch(this.mappingRegistry.getRegistrations().keySet(), exchange);
		}
	}

	/**
	 * 将匹配的映射添加到列表中。
	 *
	 * @param mappings 待匹配的映射集合
	 * @param matches  匹配结果列表
	 * @param exchange 当前交换信息
	 */
	private void addMatchingMappings(Collection<T> mappings, List<Match> matches, ServerWebExchange exchange) {
		// 遍历给定的映射列表
		for (T mapping : mappings) {
			// 获取与当前请求匹配的映射
			T match = getMatchingMapping(mapping, exchange);
			// 如果找到匹配的映射，则添加到匹配列表中
			if (match != null) {
				// 创建匹配对象，并将匹配的映射和相应的注册信息添加到匹配列表
				matches.add(new Match(match, this.mappingRegistry.getRegistrations().get(mapping)));
			}
		}
	}

	/**
	 * 在找到匹配的映射时调用。
	 *
	 * @param mapping       匹配的映射
	 * @param handlerMethod 匹配的方法
	 * @param exchange      当前交换信息
	 */
	protected void handleMatch(T mapping, HandlerMethod handlerMethod, ServerWebExchange exchange) {
		//找到的路径
		String lookupPath = exchange.getRequest().getPath().pathWithinApplication().value();
		//将路径放入交换信息的属性值中
		exchange.getAttributes().put(PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, lookupPath);
	}

	/**
	 * 在未找到匹配的映射时调用。
	 *
	 * @param mappings 所有已注册的映射
	 * @param exchange 当前交换信息
	 * @return 一个备用的 HandlerMethod 或者 {@code null}
	 * @throws Exception 可以被转换为错误状态码的详细信息
	 */
	@Nullable
	protected HandlerMethod handleNoMatch(Set<T> mappings, ServerWebExchange exchange) throws Exception {
		return null;
	}


	/**
	 * 重写以确定处理器是否有 CORS 配置源。
	 *
	 * @param handler 处理器对象
	 * @return 若处理器有 CORS 配置源则返回 true，否则返回 false
	 */
	@Override
	protected boolean hasCorsConfigurationSource(Object handler) {
		// 调用父类方法检查处理器对象是否有默认的 CORS 配置源
		return super.hasCorsConfigurationSource(handler) ||
				// 如果处理器是 HandlerMethod 类型，并且当前映射注册器获取到的跨域配置存在，则返回true
				(handler instanceof HandlerMethod && this.mappingRegistry.getCorsConfiguration((HandlerMethod) handler) != null);
	}


	/**
	 * 获取处理器方法的 CORS 配置。
	 *
	 * @param handler  处理器对象
	 * @param exchange 当前交换
	 * @return CorsConfiguration 对象，包含处理器方法的 CORS 配置
	 */
	@Override
	protected CorsConfiguration getCorsConfiguration(Object handler, ServerWebExchange exchange) {
		// 调用父类方法获取处理器对象的默认 CORS 配置
		CorsConfiguration corsConfig = super.getCorsConfiguration(handler, exchange);

		// 如果处理器是 HandlerMethod 类型
		if (handler instanceof HandlerMethod) {
			HandlerMethod handlerMethod = (HandlerMethod) handler;

			// 如果处理器方法与预检请求不明确匹配，则返回允许的 CORS 配置
			if (handlerMethod.equals(PREFLIGHT_AMBIGUOUS_MATCH)) {
				return ALLOW_CORS_CONFIG;
			}

			// 获取处理器方法的 CORS 配置
			CorsConfiguration methodConfig = this.mappingRegistry.getCorsConfiguration(handlerMethod);

			// 合并默认配置和方法配置
			corsConfig = (corsConfig != null ? corsConfig.combine(methodConfig) : methodConfig);
		}
		return corsConfig;
	}


	// Abstract template methods

	/**
	 * 判断给定的类型是否是具有处理程序方法的处理程序。
	 *
	 * @param beanType 要检查的 bean 的类型
	 * @return 如果是处理程序类型则返回“true”，否则返回“false”。
	 */
	protected abstract boolean isHandler(Class<?> beanType);

	/**
	 * 为处理器方法提供映射。无法提供映射的方法不是处理器方法。
	 *
	 * @param method      要提供映射的方法
	 * @param handlerType 处理器类型，可能是方法声明类的子类型
	 * @return 映射，如果方法未映射则为{@code null}
	 */
	@Nullable
	protected abstract T getMappingForMethod(Method method, Class<?> handlerType);

	/**
	 * 返回不是模式的请求映射路径。
	 *
	 * @since 5.3
	 */
	protected Set<String> getDirectPaths(T mapping) {
		return Collections.emptySet();
	}

	/**
	 * 检查映射是否与当前请求匹配，并返回具有与当前请求相关的条件的（可能是新的）映射。
	 *
	 * @param mapping  要获取匹配项的映射
	 * @param exchange 当前交换
	 * @return 匹配项，如果映射不匹配则为{@code null}
	 */
	@Nullable
	protected abstract T getMatchingMapping(T mapping, ServerWebExchange exchange);

	/**
	 * 返回用于对匹配映射进行排序的比较器。
	 * 返回的比较器应该将“更好”的匹配项排在更高位置。
	 *
	 * @param exchange 当前交换
	 * @return 比较器（永远不为{@code null}）
	 */
	protected abstract Comparator<T> getMappingComparator(ServerWebExchange exchange);


	/**
	 * 一个管理所有映射到处理器方法的注册表，提供查找的方法并提供并发访问的方法。
	 *
	 * <p>为了测试目的而包私有。
	 */
	class MappingRegistry {

		/**
		 * 存储映射对象与注册信息之间的关联映射
		 */
		private final Map<T, MappingRegistration<T>> registry = new HashMap<>();

		/**
		 * 存储路径与映射对象之间的多对多关系，一个路径可能对应多个映射对象
		 */
		private final MultiValueMap<String, T> pathLookup = new LinkedMultiValueMap<>();

		/**
		 * 存储处理器方法与 CORS（跨域资源共享）配置之间的映射关系
		 */
		private final Map<HandlerMethod, CorsConfiguration> corsLookup = new ConcurrentHashMap<>();

		/**
		 * 用于支持并发读写的可重入读写锁
		 */
		private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();


		/**
		 * 返回所有注册信息。
		 *
		 * @since 5.3
		 */
		public Map<T, MappingRegistration<T>> getRegistrations() {
			return this.registry;
		}

		/**
		 * 根据给定的 URL 路径返回匹配项。非线程安全。
		 *
		 * @see #acquireReadLock()
		 * @since 5.3
		 */
		@Nullable
		public List<T> getMappingsByDirectPath(ServerWebExchange exchange) {
			String path = exchange.getRequest().getPath().pathWithinApplication().value();
			return this.pathLookup.get(path);
		}

		/**
		 * 返回 CORS 配置。线程安全，可并发使用。
		 */
		@Nullable
		public CorsConfiguration getCorsConfiguration(HandlerMethod handlerMethod) {
			// 获取源自处理器方法的原始处理器方法
			HandlerMethod original = handlerMethod.getResolvedFromHandlerMethod();

			// 检索原始处理器方法（如果存在）或者给定的处理器方法所关联的 CORS 配置
			return this.corsLookup.get(original != null ? original : handlerMethod);

		}

		/**
		 * 在使用 getMappings 和 getMappingsByUrl 时获取读锁。
		 */
		public void acquireReadLock() {
			this.readWriteLock.readLock().lock();
		}

		/**
		 * 在使用 getMappings 和 getMappingsByUrl 后释放读锁。
		 */
		public void releaseReadLock() {
			this.readWriteLock.readLock().unlock();
		}

		/**
		 * 注册映射到处理器方法的操作。确保并发安全操作。
		 *
		 * @param mapping 要注册的映射对象
		 * @param handler 处理映射的处理器对象
		 * @param method  处理映射的方法对象
		 */
		public void register(T mapping, Object handler, Method method) {
			// 获取写锁，以确保并发安全操作
			this.readWriteLock.writeLock().lock();
			try {
				// 创建处理器方法并验证方法映射
				HandlerMethod handlerMethod = createHandlerMethod(handler, method);
				validateMethodMapping(handlerMethod, mapping);

				// 获取直接路径并将路径与映射关联起来
				Set<String> directPaths = AbstractHandlerMethodMapping.this.getDirectPaths(mapping);
				for (String path : directPaths) {
					this.pathLookup.add(path, mapping);
				}

				// 初始化并验证 CORS 配置，并将其与处理器方法关联起来
				CorsConfiguration corsConfig = initCorsConfiguration(handler, method, mapping);
				if (corsConfig != null) {
					corsConfig.validateAllowCredentials();
					this.corsLookup.put(handlerMethod, corsConfig);
				}

				// 将映射对象及其注册信息存储起来
				this.registry.put(mapping, new MappingRegistration<>(mapping, handlerMethod, directPaths, corsConfig != null));
			} finally {
				// 释放写锁
				this.readWriteLock.writeLock().unlock();
			}

		}

		/**
		 * 验证方法映射是否存在重复。
		 *
		 * @param handlerMethod 待验证的处理器方法
		 * @param mapping       要验证的映射对象
		 * @throws IllegalStateException 如果存在重复映射则抛出异常
		 */
		private void validateMethodMapping(HandlerMethod handlerMethod, T mapping) {
			// 获取与映射相关的注册信息
			MappingRegistration<T> registration = this.registry.get(mapping);

			// 检查是否存在已注册的处理方法，若存在且与当前处理方法不同，抛出异常
			HandlerMethod existingHandlerMethod = (registration != null ? registration.getHandlerMethod() : null);
			if (existingHandlerMethod != null && !existingHandlerMethod.equals(handlerMethod)) {
				throw new IllegalStateException(
						"Ambiguous mapping. Cannot map '" + handlerMethod.getBean() + "' method \n" +
								handlerMethod + "\nto " + mapping + ": There is already '" +
								existingHandlerMethod.getBean() + "' bean method\n" + existingHandlerMethod + " mapped.");
			}

		}

		/**
		 * 取消注册给定映射的处理器方法。
		 *
		 * @param mapping 要取消注册的映射对象
		 */
		public void unregister(T mapping) {
			// 获取写入锁，执行移除操作
			this.readWriteLock.writeLock().lock();
			try {
				// 从注册中移除映射信息
				MappingRegistration<T> registration = this.registry.remove(mapping);
				if (registration == null) {
					// 如果未找到映射信息，直接返回
					return;
				}

				// 遍历映射路径列表，移除映射信息关联的所有路径
				for (String path : registration.getDirectPaths()) {
					List<T> mappings = this.pathLookup.get(path);
					if (mappings != null) {
						mappings.remove(registration.getMapping());
						if (mappings.isEmpty()) {
							this.pathLookup.remove(path);
						}
					}
				}

				// 移除 CORS 查找表中的对应处理方法的信息
				this.corsLookup.remove(registration.getHandlerMethod());
			} finally {
				// 解锁
				this.readWriteLock.writeLock().unlock();
			}
		}
	}


	static class MappingRegistration<T> {

		/**
		 * 存储映射对象的类型（泛型 T）
		 */
		private final T mapping;

		/**
		 * 存储处理器方法对象
		 */
		private final HandlerMethod handlerMethod;

		/**
		 * 存储直接路径集合
		 */
		private final Set<String> directPaths;

		/**
		 * 标识是否有 CORS（跨域资源共享）配置
		 */
		private final boolean corsConfig;


		/**
		 * 构造函数，使用给定的映射、处理器方法、直接路径集合和跨域配置信息。
		 *
		 * @param mapping       映射
		 * @param handlerMethod 处理器方法
		 * @param directPaths   直接路径集合（可为null）
		 * @param corsConfig    是否具有跨域配置
		 */
		public MappingRegistration(
				T mapping, HandlerMethod handlerMethod, @Nullable Set<String> directPaths, boolean corsConfig) {
			Assert.notNull(mapping, "Mapping must not be null");
			Assert.notNull(handlerMethod, "HandlerMethod must not be null");
			this.mapping = mapping;
			this.handlerMethod = handlerMethod;
			this.directPaths = (directPaths != null ? directPaths : Collections.emptySet());
			this.corsConfig = corsConfig;
		}

		/**
		 * 获取映射。
		 *
		 * @return 映射
		 */
		public T getMapping() {
			return this.mapping;
		}

		/**
		 * 获取处理器方法。
		 *
		 * @return 处理器方法
		 */
		public HandlerMethod getHandlerMethod() {
			return this.handlerMethod;
		}

		/**
		 * 获取直接路径集合。
		 *
		 * @return 直接路径集合
		 */
		public Set<String> getDirectPaths() {
			return this.directPaths;
		}

		/**
		 * 检查是否有跨域配置。
		 *
		 * @return 如果有跨域配置则返回 true，否则返回 false
		 */
		public boolean hasCorsConfig() {
			return this.corsConfig;
		}
	}


	/**
	 * 在当前请求的上下文中，对匹配的 HandlerMethod 及其映射进行比较的目的，提供了对匹配结果的简单包装。
	 */
	private class Match {

		/**
		 * 存储映射对象
		 */
		private final T mapping;

		/**
		 * 存储映射的注册信息
		 */
		private final MappingRegistration<T> registration;


		/**
		 * 构造函数，使用给定的映射和注册信息。
		 *
		 * @param mapping      匹配的映射
		 * @param registration 映射的注册信息
		 */
		public Match(T mapping, MappingRegistration<T> registration) {
			this.mapping = mapping;
			this.registration = registration;
		}

		/**
		 * 获取匹配的处理器方法。
		 *
		 * @return 匹配的处理器方法
		 */
		public HandlerMethod getHandlerMethod() {
			return this.registration.getHandlerMethod();
		}

		/**
		 * 检查是否有跨域配置。
		 *
		 * @return 如果有跨域配置则返回 true，否则返回 false
		 */
		public boolean hasCorsConfig() {
			return this.registration.hasCorsConfig();
		}

		/**
		 * 返回匹配对象的字符串表示形式。
		 *
		 * @return 匹配对象的字符串表示形式
		 */
		@Override
		public String toString() {
			return this.mapping.toString();
		}
	}


	/**
	 * 匹配比较器，实现了 {@code Comparator<Match>} 接口。
	 */
	private class MatchComparator implements Comparator<Match> {

		private final Comparator<T> comparator;

		/**
		 * 构造函数，使用给定的比较器。
		 *
		 * @param comparator 给定的比较器
		 */
		public MatchComparator(Comparator<T> comparator) {
			this.comparator = comparator;
		}

		/**
		 * 比较两个匹配对象。
		 *
		 * @param match1 第一个匹配对象
		 * @param match2 第二个匹配对象
		 * @return 比较结果
		 */
		@Override
		public int compare(Match match1, Match match2) {
			return this.comparator.compare(match1.mapping, match2.mapping);
		}
	}


	/**
	 * 预先处理模糊匹配的处理程序。
	 */
	private static class PreFlightAmbiguousMatchHandler {

		/**
		 * 处理方法。目前未实现。
		 */
		@SuppressWarnings("unused")
		public void handle() {
			throw new UnsupportedOperationException("Not implemented");
		}
	}

}
