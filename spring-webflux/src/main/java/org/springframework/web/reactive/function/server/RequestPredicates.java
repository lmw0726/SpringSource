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

package org.springframework.web.reactive.function.server;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.multipart.Part;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.web.cors.reactive.CorsUtils;
import org.springframework.web.reactive.function.BodyExtractor;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebSession;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriUtils;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 实现了{@link RequestPredicate}接口，实现了各种有用的请求匹配操作，比如基于路径、HTTP方法等的匹配。
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
public abstract class RequestPredicates {

	private static final Log logger = LogFactory.getLog(RequestPredicates.class);

	/**
	 * 返回一个总是匹配的{@code RequestPredicate}。
	 *
	 * @return 总是匹配的谓词
	 */
	public static RequestPredicate all() {
		return request -> true;
	}

	/**
	 * 返回一个{@code RequestPredicate}，如果请求的HTTP方法等于给定的方法，则匹配成功。
	 *
	 * @param httpMethod 要匹配的HTTP方法
	 * @return 一个测试给定HTTP方法的谓词
	 */
	public static RequestPredicate method(HttpMethod httpMethod) {
		return new HttpMethodPredicate(httpMethod);
	}

	/**
	 * 返回一个{@code RequestPredicate}，如果请求的HTTP方法等于给定方法之一，则匹配成功。
	 *
	 * @param httpMethods 要匹配的HTTP方法
	 * @return 一个测试给定HTTP方法的谓词
	 * @since 5.1
	 */
	public static RequestPredicate methods(HttpMethod... httpMethods) {
		return new HttpMethodPredicate(httpMethods);
	}

	/**
	 * 返回一个{@code RequestPredicate}，测试请求路径与给定的路径模式匹配。
	 *
	 * @param pattern 要匹配的路径模式
	 * @return 一个测试给定路径模式的谓词
	 */
	public static RequestPredicate path(String pattern) {
		Assert.notNull(pattern, "'pattern' must not be null");
		if (!pattern.isEmpty() && !pattern.startsWith("/")) {
			pattern = "/" + pattern;
		}
		return pathPredicates(PathPatternParser.defaultInstance).apply(pattern);
	}

	/**
	 * 返回一个函数，使用给定的{@link PathPatternParser}从模式字符串创建新的路径匹配{@code RequestPredicates}。
	 * <p>当解析路径模式时，此方法可用于指定非默认的自定义{@code PathPatternParser}。
	 *
	 * @param patternParser 用于解析模式的解析器
	 * @return 一个将模式字符串解析为路径匹配{@code RequestPredicates}实例的函数
	 */
	public static Function<String, RequestPredicate> pathPredicates(PathPatternParser patternParser) {
		Assert.notNull(patternParser, "PathPatternParser must not be null");
		return pattern -> new PathPatternPredicate(patternParser.parse(pattern));
	}

	/**
	 * 返回一个{@code RequestPredicate}，用于测试请求的头部信息是否符合给定的头部信息谓词。
	 *
	 * @param headersPredicate 测试请求头部信息的谓词
	 * @return 一个测试给定头部信息谓词的谓词
	 */
	public static RequestPredicate headers(Predicate<ServerRequest.Headers> headersPredicate) {
		return new HeadersPredicate(headersPredicate);
	}

	/**
	 * 返回一个{@code RequestPredicate}，用于检查请求的{@linkplain ServerRequest.Headers#contentType() 内容类型}
	 * 是否被给定的媒体类型之一所包含。
	 *
	 * @param mediaTypes 用于匹配请求内容类型的媒体类型
	 * @return 一个测试请求内容类型是否匹配给定媒体类型的谓词
	 */
	public static RequestPredicate contentType(MediaType... mediaTypes) {
		Assert.notEmpty(mediaTypes, "'mediaTypes' must not be empty");
		return new ContentTypePredicate(mediaTypes);
	}

	/**
	 * 返回一个{@code RequestPredicate}，用于测试请求的{@linkplain ServerRequest.Headers#accept() accept}头部信息是否与给定的媒体类型之一{@linkplain MediaType#isCompatibleWith(MediaType)兼容}。
	 *
	 * @param mediaTypes 用于匹配请求的accept头部信息的媒体类型
	 * @return 一个测试请求的accept头部信息是否与给定媒体类型兼容的谓词
	 */
	public static RequestPredicate accept(MediaType... mediaTypes) {
		Assert.notEmpty(mediaTypes, "'mediaTypes' must not be empty");
		return new AcceptPredicate(mediaTypes);
	}

	/**
	 * 返回一个{@code RequestPredicate}，如果请求的HTTP方法为{@code GET}且给定的{@code pattern}与请求路径匹配，则匹配。
	 *
	 * @param pattern 要与之匹配的路径模式
	 * @return 如果请求方法为GET并且给定模式与请求路径匹配，则匹配的谓词
	 */
	public static RequestPredicate GET(String pattern) {
		return method(HttpMethod.GET).and(path(pattern));
	}

	/**
	 * 返回一个{@code RequestPredicate}，如果请求的HTTP方法为{@code HEAD}且给定的{@code pattern}与请求路径匹配，则匹配。
	 *
	 * @param pattern 要与之匹配的路径模式
	 * @return 如果请求方法为HEAD并且给定模式与请求路径匹配，则匹配的谓词
	 */
	public static RequestPredicate HEAD(String pattern) {
		return method(HttpMethod.HEAD).and(path(pattern));
	}

	/**
	 * 返回一个{@code RequestPredicate}，如果请求的HTTP方法为{@code POST}且给定的{@code pattern}与请求路径匹配，则匹配。
	 *
	 * @param pattern 要与之匹配的路径模式
	 * @return 如果请求方法为POST并且给定模式与请求路径匹配，则匹配的谓词
	 */
	public static RequestPredicate POST(String pattern) {
		return method(HttpMethod.POST).and(path(pattern));
	}

	/**
	 * 返回一个{@code RequestPredicate}，如果请求的HTTP方法为{@code PUT}且给定的{@code pattern}与请求路径匹配，则匹配。
	 *
	 * @param pattern 要与之匹配的路径模式
	 * @return 如果请求方法为PUT并且给定模式与请求路径匹配，则匹配的谓词
	 */
	public static RequestPredicate PUT(String pattern) {
		return method(HttpMethod.PUT).and(path(pattern));
	}

	/**
	 * 返回一个{@code RequestPredicate}，如果请求的HTTP方法为{@code PATCH}且给定的{@code pattern}与请求路径匹配，则匹配。
	 *
	 * @param pattern 要与之匹配的路径模式
	 * @return 如果请求方法为PATCH并且给定模式与请求路径匹配，则匹配的谓词
	 */
	public static RequestPredicate PATCH(String pattern) {
		return method(HttpMethod.PATCH).and(path(pattern));
	}

	/**
	 * 返回一个{@code RequestPredicate}，如果请求的HTTP方法为{@code DELETE}且给定的{@code pattern}与请求路径匹配，则匹配。
	 *
	 * @param pattern 要与之匹配的路径模式
	 * @return 如果请求方法为DELETE并且给定模式与请求路径匹配，则匹配的谓词
	 */
	public static RequestPredicate DELETE(String pattern) {
		return method(HttpMethod.DELETE).and(path(pattern));
	}

	/**
	 * 返回一个{@code RequestPredicate}，如果请求的HTTP方法为{@code OPTIONS}且给定的{@code pattern}与请求路径匹配，则匹配。
	 *
	 * @param pattern 要与之匹配的路径模式
	 * @return 如果请求方法为OPTIONS并且给定模式与请求路径匹配，则匹配的谓词
	 */
	public static RequestPredicate OPTIONS(String pattern) {
		return method(HttpMethod.OPTIONS).and(path(pattern));
	}

	/**
	 * 返回一个{@code RequestPredicate}，如果请求的路径具有给定的扩展名，则匹配。
	 *
	 * @param extension 要与之匹配的路径扩展名（忽略大小写）
	 * @return 如果请求的路径具有给定的文件扩展名，则匹配的谓词
	 */
	public static RequestPredicate pathExtension(String extension) {
		Assert.notNull(extension, "'extension' must not be null");
		return new PathExtensionPredicate(extension);
	}

	/**
	 * 返回一个{@code RequestPredicate}，如果请求的路径与给定的谓词匹配，则匹配。
	 *
	 * @param extensionPredicate 要针对请求路径扩展名测试的谓词
	 * @return 如果给定的谓词与请求的路径文件扩展名匹配，则匹配的谓词
	 */
	public static RequestPredicate pathExtension(Predicate<String> extensionPredicate) {
		return new PathExtensionPredicate(extensionPredicate);
	}

	/**
	 * 返回一个{@code RequestPredicate}，如果请求的给定名称的查询参数具有给定值，则匹配。
	 *
	 * @param name  要与之匹配的查询参数的名称
	 * @param value 要与查询参数匹配的值
	 * @return 如果查询参数具有给定值，则匹配的谓词
	 * @see ServerRequest#queryParam(String)
	 * @since 5.0.7
	 */
	public static RequestPredicate queryParam(String name, String value) {
		return new QueryParamPredicate(name, value);
	}

	/**
	 * 返回一个{@code RequestPredicate}，用于测试请求的给定名称的查询参数是否与给定的谓词匹配。
	 *
	 * @param name      要测试的查询参数的名称
	 * @param predicate 要针对查询参数值进行测试的谓词
	 * @return 匹配给定谓词与给定名称的查询参数的谓词
	 * @see ServerRequest#queryParam(String)
	 */
	public static RequestPredicate queryParam(String name, Predicate<String> predicate) {
		return new QueryParamPredicate(name, predicate);
	}


	/**
	 * 跟踪匹配结果的辅助方法。
	 *
	 * @param prefix  前缀字符串，用于标识是哪种类型的匹配
	 * @param desired 期望的值
	 * @param actual  实际值
	 * @param match   匹配结果，是否匹配
	 */
	private static void traceMatch(String prefix, Object desired, @Nullable Object actual, boolean match) {
		if (logger.isTraceEnabled()) {
			logger.trace(String.format("%s \"%s\" %s against value \"%s\"",
					prefix, desired, match ? "matches" : "does not match", actual));
		}
	}

	/**
	 * 恢复 ServerRequest 的属性。
	 *
	 * @param request    ServerRequest 请求对象
	 * @param attributes 要恢复的属性映射
	 */
	private static void restoreAttributes(ServerRequest request, Map<String, Object> attributes) {
		// 清空请求的属性
		request.attributes().clear();
		// 将给定的属性映射放入请求的属性中
		request.attributes().putAll(attributes);
	}

	/**
	 * 合并两个路径变量的映射。
	 *
	 * @param oldVariables 原始的路径变量映射
	 * @param newVariables 新的路径变量映射
	 * @return 合并后的路径变量映射
	 */
	private static Map<String, String> mergePathVariables(Map<String, String> oldVariables,
														  Map<String, String> newVariables) {
		// 如果新的路径变量映射不为空
		if (!newVariables.isEmpty()) {
			// 创建一个新的 LinkedHashMap，将原始路径变量映射放入其中
			Map<String, String> mergedVariables = new LinkedHashMap<>(oldVariables);
			// 将新的路径变量映射放入合并后的映射中
			mergedVariables.putAll(newVariables);
			// 返回合并后的路径变量映射
			return mergedVariables;
		} else {
			// 如果新的路径变量映射为空，则直接返回原始路径变量映射
			return oldVariables;
		}
	}

	/**
	 * 合并路径模式。
	 *
	 * @param oldPattern 原始路径模式（可为空）
	 * @param newPattern 新的路径模式
	 * @return 合并后的路径模式
	 */
	private static PathPattern mergePatterns(@Nullable PathPattern oldPattern, PathPattern newPattern) {
		// 如果原始路径模式不为空
		if (oldPattern != null) {
			// 将新的路径模式合并到原始路径模式中，并返回合并后的路径模式
			return oldPattern.combine(newPattern);
		} else {
			// 如果原始路径模式为空，则直接返回新的路径模式
			return newPattern;
		}

	}


	/**
	 * 接收来自请求谓词逻辑结构的通知。
	 */
	public interface Visitor {

		/**
		 * 接收 HTTP 方法谓词的通知。
		 *
		 * @param methods 组成谓词的 HTTP 方法集合
		 * @see RequestPredicates#method(HttpMethod)
		 */
		void method(Set<HttpMethod> methods);

		/**
		 * 接收路径谓词的通知。
		 *
		 * @param pattern 组成谓词的路径模式
		 * @see RequestPredicates#path(String)
		 */
		void path(String pattern);

		/**
		 * 接收路径扩展谓词的通知。
		 *
		 * @param extension 组成谓词的路径扩展
		 * @see RequestPredicates#pathExtension(String)
		 */
		void pathExtension(String extension);

		/**
		 * 接收 HTTP 头部谓词的通知。
		 *
		 * @param name  要检查的 HTTP 头部名称
		 * @param value HTTP 头部的期望值
		 * @see RequestPredicates#headers(Predicate)
		 * @see RequestPredicates#contentType(MediaType...)
		 * @see RequestPredicates#accept(MediaType...)
		 */
		void header(String name, String value);

		/**
		 * 接收查询参数谓词的通知。
		 *
		 * @param name  查询参数的名称
		 * @param value 参数的期望值
		 * @see RequestPredicates#queryParam(String, String)
		 */
		void queryParam(String name, String value);

		/**
		 * 接收逻辑 AND 谓词的首次通知。
		 * 下一个通知将包含 AND 谓词的左侧；
		 * 接着是 {@link #and()}，然后是右侧，最后是 {@link #endAnd()}。
		 *
		 * @see RequestPredicate#and(RequestPredicate)
		 */
		void startAnd();

		/**
		 * 接收逻辑 AND 谓词的“中间”通知。
		 * 下一个通知将包含右侧，然后是 {@link #endAnd()}。
		 *
		 * @see RequestPredicate#and(RequestPredicate)
		 */
		void and();

		/**
		 * 接收逻辑 AND 谓词的最后通知。
		 *
		 * @see RequestPredicate#and(RequestPredicate)
		 */
		void endAnd();

		/**
		 * 接收逻辑 OR 谓词的首次通知。
		 * 下一个通知将包含 OR 谓词的左侧；
		 * 第二个通知包含右侧，然后是 {@link #endOr()}。
		 *
		 * @see RequestPredicate#or(RequestPredicate)
		 */
		void startOr();

		/**
		 * 接收逻辑 OR 谓词的“中间”通知。
		 * 下一个通知将包含右侧，然后是 {@link #endOr()}。
		 *
		 * @see RequestPredicate#or(RequestPredicate)
		 */
		void or();

		/**
		 * 接收逻辑 OR 谓词的最后通知。
		 *
		 * @see RequestPredicate#or(RequestPredicate)
		 */
		void endOr();

		/**
		 * 接收否定谓词的首次通知。
		 * 下一个通知将包含被否定的谓词，然后是 {@link #endNegate()}。
		 *
		 * @see RequestPredicate#negate()
		 */
		void startNegate();

		/**
		 * 接收否定谓词的最后通知。
		 *
		 * @see RequestPredicate#negate()
		 */
		void endNegate();

		/**
		 * 接收未知谓词的首次通知。
		 */
		void unknown(RequestPredicate predicate);
	}


	private static class HttpMethodPredicate implements RequestPredicate {

		/**
		 * HTTP方法集合
		 */
		private final Set<HttpMethod> httpMethods;

		public HttpMethodPredicate(HttpMethod httpMethod) {
			Assert.notNull(httpMethod, "HttpMethod must not be null");
			this.httpMethods = EnumSet.of(httpMethod);
		}

		public HttpMethodPredicate(HttpMethod... httpMethods) {
			Assert.notEmpty(httpMethods, "HttpMethods must not be empty");
			this.httpMethods = EnumSet.copyOf(Arrays.asList(httpMethods));
		}

		/**
		 * 测试请求的 HTTP 方法是否匹配给定的 HTTP 方法。
		 *
		 * @param request 请求对象
		 * @return 如果请求的方法匹配谓词中定义的方法，则为 true；否则为 false
		 */
		@Override
		public boolean test(ServerRequest request) {
			// 获取请求的 HTTP 方法
			HttpMethod method = method(request);

			// 检查请求的方法是否包含在预期的方法集合中
			boolean match = this.httpMethods.contains(method);

			// 输出匹配信息，用于追踪调试
			traceMatch("Method", this.httpMethods, method, match);

			// 返回是否匹配的结果
			return match;
		}

		/**
		 * 从 ServerRequest 对象中获取 HTTP 方法。
		 *
		 * @param request ServerRequest 对象
		 * @return 如果是预检请求（preflight request），则返回预检请求的控制方法（Access-Control-Request-Method）对应的 HTTP 方法；
		 * 否则返回实际请求的 HTTP 方法
		 */
		@Nullable
		private static HttpMethod method(ServerRequest request) {
			// 检查当前请求是否是预检请求（Preflight Request）
			if (CorsUtils.isPreFlightRequest(request.exchange().getRequest())) {
				// 获取 Access-Control-Request-Method 请求头的值
				String accessControlRequestMethod =
						request.headers().firstHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD);

				// 解析 Access-Control-Request-Method 为对应的 HttpMethod
				return HttpMethod.resolve(accessControlRequestMethod);
			} else {
				// 如果不是预检请求，则直接返回当前请求的 HttpMethod
				return request.method();
			}
		}

		@Override
		public void accept(Visitor visitor) {
			visitor.method(Collections.unmodifiableSet(this.httpMethods));
		}

		@Override
		public String toString() {
			if (this.httpMethods.size() == 1) {
				return this.httpMethods.iterator().next().toString();
			} else {
				return this.httpMethods.toString();
			}
		}
	}


	/**
	 * 路径模式断言
	 */
	private static class PathPatternPredicate implements RequestPredicate, ChangePathPatternParserVisitor.Target {

		/**
		 * 路径模式
		 */
		private PathPattern pattern;

		public PathPatternPredicate(PathPattern pattern) {
			Assert.notNull(pattern, "'pattern' must not be null");
			this.pattern = pattern;
		}

		/**
		 * 测试给定的 ServerRequest 是否符合特定的路径模式。
		 *
		 * @param request ServerRequest 对象
		 * @return 如果路径匹配成功，则返回 true；否则返回 false
		 */
		@Override
		public boolean test(ServerRequest request) {
			// 从 ServerRequest 中获取请求路径容器
			PathContainer pathContainer = request.requestPath().pathWithinApplication();

			// 使用路径模式匹配并提取信息
			PathPattern.PathMatchInfo info = this.pattern.matchAndExtract(pathContainer);

			// 记录匹配日志
			traceMatch("Pattern", this.pattern.getPatternString(), request.path(), info != null);

			if (info != null) {
				// 如果匹配成功，则合并请求属性
				mergeAttributes(request, info.getUriVariables(), this.pattern);
				return true;
			} else {
				// 如果匹配失败，则返回 false
				return false;
			}
		}

		/**
		 * 合并请求中的属性信息，包括路径变量和匹配模式。
		 *
		 * @param request   ServerRequest 对象
		 * @param variables 路径变量的键值对
		 * @param pattern   匹配模式
		 */
		private static void mergeAttributes(ServerRequest request, Map<String, String> variables, PathPattern pattern) {
			// 合并路径变量
			Map<String, String> pathVariables = mergePathVariables(request.pathVariables(), variables);
			request.attributes().put(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE,
					Collections.unmodifiableMap(pathVariables));

			// 合并匹配模式
			pattern = mergePatterns(
					(PathPattern) request.attributes().get(RouterFunctions.MATCHING_PATTERN_ATTRIBUTE),
					pattern);
			request.attributes().put(RouterFunctions.MATCHING_PATTERN_ATTRIBUTE, pattern);
		}

		/**
		 * 将当前的请求谓词嵌套到新的服务器请求中，主要是匹配路径的起始部分。
		 *
		 * @param request ServerRequest 对象
		 * @return 包含匹配信息的新 ServerRequest，如果没有匹配则返回 Optional.empty()
		 */
		@Override
		public Optional<ServerRequest> nest(ServerRequest request) {
			return Optional.ofNullable(this.pattern.matchStartOfPath(request.requestPath().pathWithinApplication()))
					.map(info -> new SubPathServerRequestWrapper(request, info, this.pattern));
		}

		@Override
		public void accept(Visitor visitor) {
			visitor.path(this.pattern.getPatternString());
		}

		/**
		 * 更改当前的路径模式解析器，并重新解析已保存的模式字符串。
		 *
		 * @param parser 要设置的新 PathPatternParser
		 */
		@Override
		public void changeParser(PathPatternParser parser) {
			String patternString = this.pattern.getPatternString();
			this.pattern = parser.parse(patternString);
		}

		@Override
		public String toString() {
			return this.pattern.getPatternString();
		}

	}


	/**
	 * 请求头断言
	 */
	private static class HeadersPredicate implements RequestPredicate {

		/**
		 * 请求头断言
		 */
		private final Predicate<ServerRequest.Headers> headersPredicate;

		public HeadersPredicate(Predicate<ServerRequest.Headers> headersPredicate) {
			Assert.notNull(headersPredicate, "Predicate must not be null");
			this.headersPredicate = headersPredicate;
		}

		/**
		 * 检查服务器请求是否符合条件。
		 *
		 * @param request 服务器请求对象
		 * @return 如果请求符合条件返回 true，否则返回 false
		 */
		@Override
		public boolean test(ServerRequest request) {
			// 检查是否为预检请求
			if (CorsUtils.isPreFlightRequest(request.exchange().getRequest())) {
				return true;
			} else {
				// 使用头部断言检查请求的头部信息
				return this.headersPredicate.test(request.headers());
			}
		}

		@Override
		public String toString() {
			return this.headersPredicate.toString();
		}
	}

	/**
	 * 内容类型断言
	 */
	private static class ContentTypePredicate extends HeadersPredicate {

		/**
		 * 媒体类型集合
		 */
		private final Set<MediaType> mediaTypes;

		public ContentTypePredicate(MediaType... mediaTypes) {
			this(new HashSet<>(Arrays.asList(mediaTypes)));
		}

		/**
		 * 私有构造函数，用于创建 ContentTypePredicate 对象。
		 *
		 * @param mediaTypes 包含的媒体类型的集合
		 */
		private ContentTypePredicate(Set<MediaType> mediaTypes) {
			super(headers -> {
				// 获取请求头中的 Content-Type，默认为 APPLICATION_OCTET_STREAM 类型
				MediaType contentType =
						headers.contentType().orElse(MediaType.APPLICATION_OCTET_STREAM);

				// 检查请求头中的 Content-Type 是否匹配给定的媒体类型集合中的任何一种类型
				boolean match = mediaTypes.stream()
						.anyMatch(mediaType -> mediaType.includes(contentType));

				// 跟踪匹配结果
				traceMatch("Content-Type", mediaTypes, contentType, match);

				return match;
			});
			this.mediaTypes = mediaTypes;
		}

		/**
		 * 接受一个访问者，并将内容类型信息传递给访问者。
		 *
		 * @param visitor 访问者对象
		 */
		@Override
		public void accept(Visitor visitor) {
			// 将内容类型信息传递给访问者
			visitor.header(HttpHeaders.CONTENT_TYPE,
					// 如果媒体类型集合大小为1，则将该类型作为内容类型；否则，将整个媒体类型集合作为内容类型
					(this.mediaTypes.size() == 1) ?
							this.mediaTypes.iterator().next().toString() :
							this.mediaTypes.toString());
		}

		@Override
		public String toString() {
			return String.format("Content-Type: %s",
					(this.mediaTypes.size() == 1) ?
							this.mediaTypes.iterator().next().toString() :
							this.mediaTypes.toString());
		}
	}

	/**
	 * 接受类型断言
	 */
	private static class AcceptPredicate extends HeadersPredicate {

		/**
		 * 媒体类型
		 */
		private final Set<MediaType> mediaTypes;

		public AcceptPredicate(MediaType... mediaTypes) {
			this(new HashSet<>(Arrays.asList(mediaTypes)));
		}

		/**
		 * 私有构造函数，创建 AcceptPredicate 对象。
		 *
		 * @param mediaTypes 接受的媒体类型集合
		 */
		private AcceptPredicate(Set<MediaType> mediaTypes) {
			super(headers -> {
				// 获取请求中可接受的媒体类型列表
				List<MediaType> acceptedMediaTypes = acceptedMediaTypes(headers);

				// 检查可接受的媒体类型是否与给定的媒体类型集合中的任何一种兼容
				boolean match = acceptedMediaTypes.stream()
						.anyMatch(acceptedMediaType -> mediaTypes.stream()
								.anyMatch(acceptedMediaType::isCompatibleWith));

				// 跟踪匹配结果
				traceMatch("Accept", mediaTypes, acceptedMediaTypes, match);

				return match;
			});
			this.mediaTypes = mediaTypes;
		}

		/**
		 * 获取请求头中可接受的媒体类型列表。
		 *
		 * @param headers 服务器请求的头部信息
		 * @return 可接受的媒体类型列表
		 */
		@NonNull
		private static List<MediaType> acceptedMediaTypes(ServerRequest.Headers headers) {
			// 获取请求头中可接受的媒体类型列表
			List<MediaType> acceptedMediaTypes = headers.accept();

			// 如果可接受的媒体类型列表为空，则默认为接受所有媒体类型
			if (acceptedMediaTypes.isEmpty()) {
				acceptedMediaTypes = Collections.singletonList(MediaType.ALL);
			} else {
				// 按照特异性和质量对媒体类型列表进行排序
				MediaType.sortBySpecificityAndQuality(acceptedMediaTypes);
			}
			return acceptedMediaTypes;
		}

		/**
		 * 接受一个访问者，并将可接受的内容类型信息传递给访问者。
		 *
		 * @param visitor 访问者对象
		 */
		@Override
		public void accept(Visitor visitor) {
			// 将可接受的内容类型信息传递给访问者
			visitor.header(HttpHeaders.ACCEPT,
					// 如果媒体类型集合大小为1，则将该类型作为接受的内容类型；否则，将整个媒体类型集合作为接受的内容类型
					(this.mediaTypes.size() == 1) ?
							this.mediaTypes.iterator().next().toString() :
							this.mediaTypes.toString());
		}

		@Override
		public String toString() {
			return String.format("Accept: %s",
					(this.mediaTypes.size() == 1) ?
							this.mediaTypes.iterator().next().toString() :
							this.mediaTypes.toString());
		}
	}

	/**
	 * 路径扩展后缀断言
	 */
	private static class PathExtensionPredicate implements RequestPredicate {

		/**
		 * 扩展名断言
		 */
		private final Predicate<String> extensionPredicate;

		/**
		 * 扩展名
		 */
		@Nullable
		private final String extension;

		public PathExtensionPredicate(Predicate<String> extensionPredicate) {
			Assert.notNull(extensionPredicate, "Predicate must not be null");
			this.extensionPredicate = extensionPredicate;
			this.extension = null;
		}

		public PathExtensionPredicate(String extension) {
			Assert.notNull(extension, "Extension must not be null");

			this.extensionPredicate = s -> {
				boolean match = extension.equalsIgnoreCase(s);
				traceMatch("Extension", extension, s, match);
				return match;
			};
			this.extension = extension;
		}

		/**
		 * 检查服务器请求的路径是否符合条件。
		 *
		 * @param request 服务器请求对象
		 * @return 如果路径符合条件返回 true，否则返回 false
		 */
		@Override
		public boolean test(ServerRequest request) {
			// 提取请求路径的文件扩展名
			String pathExtension = UriUtils.extractFileExtension(request.path());

			// 使用扩展名断言检查路径的扩展名是否符合条件
			return this.extensionPredicate.test(pathExtension);
		}

		/**
		 * 接受一个访问者，并将路径扩展信息传递给访问者。
		 *
		 * @param visitor 访问者对象
		 */
		@Override
		public void accept(Visitor visitor) {
			// 将路径扩展信息传递给访问者
			visitor.pathExtension(
					// 如果扩展名不为空，则将该扩展名作为路径扩展信息；否则，将扩展名断言的字符串表示作为路径扩展信息
					(this.extension != null) ?
							this.extension :
							this.extensionPredicate.toString());
		}

		@Override
		public String toString() {
			return String.format("*.%s",
					(this.extension != null) ?
							this.extension :
							this.extensionPredicate);
		}

	}

	/**
	 * 查询参数断言
	 */
	private static class QueryParamPredicate implements RequestPredicate {
		/**
		 * 参数名称
		 */
		private final String name;

		/**
		 * 值断言
		 */
		private final Predicate<String> valuePredicate;

		/**
		 * 参数值
		 */
		@Nullable
		private final String value;

		public QueryParamPredicate(String name, Predicate<String> valuePredicate) {
			Assert.notNull(name, "Name must not be null");
			Assert.notNull(valuePredicate, "Predicate must not be null");
			this.name = name;
			this.valuePredicate = valuePredicate;
			this.value = null;
		}

		public QueryParamPredicate(String name, String value) {
			Assert.notNull(name, "Name must not be null");
			Assert.notNull(value, "Value must not be null");
			this.name = name;
			this.valuePredicate = value::equals;
			this.value = value;
		}

		/**
		 * 检查服务器请求中指定名称的查询参数是否符合条件。
		 *
		 * @param request 服务器请求对象
		 * @return 如果查询参数符合条件返回 true，否则返回 false
		 */
		@Override
		public boolean test(ServerRequest request) {
			// 获取指定名称的查询参数值
			Optional<String> s = request.queryParam(this.name);

			// 使用值断言检查查询参数值是否符合条件
			return s.filter(this.valuePredicate).isPresent();
		}

		/**
		 * 接受一个访问者，并将查询参数信息传递给访问者。
		 *
		 * @param visitor 访问者对象
		 */
		@Override
		public void accept(Visitor visitor) {
			// 将查询参数信息传递给访问者
			visitor.queryParam(
					// 如果值不为空，则将该值作为查询参数值；否则，将值断言的字符串表示作为查询参数值
					this.name,
					(this.value != null) ?
							this.value :
							this.valuePredicate.toString());
		}

		@Override
		public String toString() {
			return String.format("?%s %s", this.name,
					(this.value != null) ?
							this.value :
							this.valuePredicate);
		}
	}


	/**
	 * {@link RequestPredicate}，要求同时满足 {@code left} 和 {@code right} 断言。
	 */
	static class AndRequestPredicate implements RequestPredicate, ChangePathPatternParserVisitor.Target {

		/**
		 * 左侧断言
		 */
		private final RequestPredicate left;

		/**
		 * 右侧断言
		 */
		private final RequestPredicate right;

		public AndRequestPredicate(RequestPredicate left, RequestPredicate right) {
			Assert.notNull(left, "Left RequestPredicate must not be null");
			Assert.notNull(right, "Right RequestPredicate must not be null");
			this.left = left;
			this.right = right;
		}

		/**
		 * 检查服务器请求是否符合左侧和右侧断言的条件。
		 *
		 * @param request 服务器请求对象
		 * @return 如果请求同时符合左侧和右侧断言的条件返回 true，否则返回 false
		 */
		@Override
		public boolean test(ServerRequest request) {
			// 保存旧的属性值
			Map<String, Object> oldAttributes = new HashMap<>(request.attributes());

			// 如果左侧和右侧断言都满足条件，则返回 true
			if (this.left.test(request) && this.right.test(request)) {
				return true;
			}

			// 恢复属性值
			restoreAttributes(request, oldAttributes);
			return false;
		}

		/**
		 * 对服务器请求进行嵌套，根据左侧和右侧断言进行嵌套处理。
		 *
		 * @param request 服务器请求对象
		 * @return 包含左侧和右侧断言嵌套处理后的可选服务器请求
		 */
		@Override
		public Optional<ServerRequest> nest(ServerRequest request) {
			// 对左侧断言进行嵌套处理，然后根据右侧断言进行进一步的嵌套处理
			return this.left.nest(request).flatMap(this.right::nest);
		}

		/**
		 * 接受一个访问者，并按照特定的顺序将左侧和右侧断言传递给访问者。
		 *
		 * @param visitor 访问者对象
		 */
		@Override
		public void accept(Visitor visitor) {
			// 开始 AND 条件组合
			visitor.startAnd();

			// 接受并传递左侧断言给访问者
			this.left.accept(visitor);

			// 添加 AND 操作符
			visitor.and();

			// 接受并传递右侧断言给访问者
			this.right.accept(visitor);

			// 结束 AND 条件组合
			visitor.endAnd();
		}

		/**
		 * 更改解析器，根据左侧和右侧断言进行解析器的更改操作。
		 *
		 * @param parser 要应用的路径模式解析器
		 */
		@Override
		public void changeParser(PathPatternParser parser) {
			// 如果左侧实现了 ChangePathPatternParserVisitor.Target 接口，则对左侧进行解析器更改操作
			if (this.left instanceof ChangePathPatternParserVisitor.Target) {
				((ChangePathPatternParserVisitor.Target) this.left).changeParser(parser);
			}

			// 如果右侧实现了 ChangePathPatternParserVisitor.Target 接口，则对右侧进行解析器更改操作
			if (this.right instanceof ChangePathPatternParserVisitor.Target) {
				((ChangePathPatternParserVisitor.Target) this.right).changeParser(parser);
			}
		}

		@Override
		public String toString() {
			return String.format("(%s && %s)", this.left, this.right);
		}
	}

	/**
	 * {@link RequestPredicate}，用于对委托的断言进行取反。
	 */
	static class NegateRequestPredicate implements RequestPredicate, ChangePathPatternParserVisitor.Target {

		/**
		 * 要取反的委托断言
		 */
		private final RequestPredicate delegate;

		/**
		 * 构造函数，传入要取反的委托断言。
		 *
		 * @param delegate 要取反的委托断言
		 */
		public NegateRequestPredicate(RequestPredicate delegate) {
			Assert.notNull(delegate, "Delegate must not be null");
			this.delegate = delegate;
		}

		/**
		 * 检查服务器请求是否不符合委托断言的条件。
		 *
		 * @param request 服务器请求对象
		 * @return 如果请求不符合委托断言的条件返回 true，否则返回 false
		 */
		@Override
		public boolean test(ServerRequest request) {
			// 保存旧的属性值
			Map<String, Object> oldAttributes = new HashMap<>(request.attributes());

			// 对委托断言取反
			boolean result = !this.delegate.test(request);

			// 如果取反结果为 false，则恢复属性值
			if (!result) {
				restoreAttributes(request, oldAttributes);
			}
			return result;
		}

		/**
		 * 接受一个访问者，并在委托断言上开始取反操作。
		 *
		 * @param visitor 访问者对象
		 */
		@Override
		public void accept(Visitor visitor) {
			visitor.startNegate();
			this.delegate.accept(visitor);
			visitor.endNegate();
		}

		/**
		 * 更改解析器，根据委托断言进行解析器的更改操作。
		 *
		 * @param parser 要应用的路径模式解析器
		 */
		@Override
		public void changeParser(PathPatternParser parser) {
			if (this.delegate instanceof ChangePathPatternParserVisitor.Target) {
				((ChangePathPatternParserVisitor.Target) this.delegate).changeParser(parser);
			}
		}

		/**
		 * 返回取反字符串表示形式。
		 *
		 * @return 取反字符串表示形式
		 */
		@Override
		public String toString() {
			return "!" + this.delegate.toString();
		}
	}

	/**
	 * {@link RequestPredicate}，允许 {@code left} 或 {@code right} 断言匹配。
	 */
	static class OrRequestPredicate implements RequestPredicate, ChangePathPatternParserVisitor.Target {

		/**
		 * 左侧断言
		 */
		private final RequestPredicate left;

		/**
		 * 右侧断言
		 */
		private final RequestPredicate right;

		/**
		 * 构造函数，传入左侧和右侧断言。
		 *
		 * @param left  左侧断言
		 * @param right 右侧断言
		 */
		public OrRequestPredicate(RequestPredicate left, RequestPredicate right) {
			Assert.notNull(left, "Left RequestPredicate must not be null");
			Assert.notNull(right, "Right RequestPredicate must not be null");
			this.left = left;
			this.right = right;
		}

		/**
		 * 检查服务器请求是否左侧或右侧断言匹配。
		 *
		 * @param request 服务器请求对象
		 * @return 如果左侧或右侧断言匹配返回 true，否则返回 false
		 */
		@Override
		public boolean test(ServerRequest request) {
			// 保存旧的属性值
			Map<String, Object> oldAttributes = new HashMap<>(request.attributes());

			// 如果左侧断言匹配，则返回 true
			if (this.left.test(request)) {
				return true;
			} else {
				// 恢复属性值
				restoreAttributes(request, oldAttributes);

				// 如果右侧断言匹配，则返回 true
				if (this.right.test(request)) {
					return true;
				}
			}

			// 恢复属性值
			restoreAttributes(request, oldAttributes);
			return false;
		}

		/**
		 * 对服务器请求进行嵌套，根据左侧和右侧断言进行嵌套处理。
		 *
		 * @param request 服务器请求对象
		 * @return 包含左侧和右侧断言嵌套处理后的可选服务器请求
		 */
		@Override
		public Optional<ServerRequest> nest(ServerRequest request) {
			Optional<ServerRequest> leftResult = this.left.nest(request);
			if (leftResult.isPresent()) {
				return leftResult;
			} else {
				return this.right.nest(request);
			}
		}

		/**
		 * 接受一个访问者，并按照特定的顺序将左侧和右侧断言传递给访问者。
		 *
		 * @param visitor 访问者对象
		 */
		@Override
		public void accept(Visitor visitor) {
			visitor.startOr();
			this.left.accept(visitor);
			visitor.or();
			this.right.accept(visitor);
			visitor.endOr();
		}

		/**
		 * 更改解析器，根据左侧和右侧断言进行解析器的更改操作。
		 *
		 * @param parser 要应用的路径模式解析器
		 */
		@Override
		public void changeParser(PathPatternParser parser) {
			if (this.left instanceof ChangePathPatternParserVisitor.Target) {
				((ChangePathPatternParserVisitor.Target) this.left).changeParser(parser);
			}
			if (this.right instanceof ChangePathPatternParserVisitor.Target) {
				((ChangePathPatternParserVisitor.Target) this.right).changeParser(parser);
			}
		}

		/**
		 * 返回逻辑或的字符串表示形式。
		 *
		 * @return 逻辑或的字符串表示形式
		 */
		@Override
		public String toString() {
			return String.format("(%s || %s)", this.left, this.right);
		}
	}


	private static class SubPathServerRequestWrapper implements ServerRequest {
		/**
		 * 封装了服务器请求相关信息的类。
		 */
		private final ServerRequest request;

		/**
		 * 表示请求路径的对象。
		 */
		private final RequestPath requestPath;

		/**
		 * 存储请求属性的映射。
		 */
		private final Map<String, Object> attributes;

		public SubPathServerRequestWrapper(ServerRequest request,
										   PathPattern.PathRemainingMatchInfo info, PathPattern pattern) {
			this.request = request;
			this.requestPath = requestPath(request.requestPath(), info);
			this.attributes = mergeAttributes(request, info.getUriVariables(), pattern);
		}

		/**
		 * 根据给定信息更新请求路径。
		 *
		 * @param original 原始请求路径
		 * @param info     路径匹配信息
		 * @return 更新后的请求路径
		 */
		private static RequestPath requestPath(RequestPath original, PathPattern.PathRemainingMatchInfo info) {
			// 创建 StringBuilder 对象，用于构建上下文路径
			StringBuilder contextPath = new StringBuilder(original.contextPath().value());

			// 将路径匹配的值追加到上下文路径中
			contextPath.append(info.getPathMatched().value());

			// 获取上下文路径的长度
			int length = contextPath.length();

			// 如果路径长度大于 0，并且最后一个字符是斜杠，则移除末尾的斜杠
			if (length > 0 && contextPath.charAt(length - 1) == '/') {
				contextPath.setLength(length - 1);
			}

			// 返回修改后的原始上下文路径
			return original.modifyContextPath(contextPath.toString());
		}

		/**
		 * 合并请求的属性信息，包括路径变量和路径模式。
		 *
		 * @param request       服务器请求
		 * @param pathVariables 路径变量映射
		 * @param pattern       路径模式
		 * @return 合并后的属性映射
		 */
		private static Map<String, Object> mergeAttributes(ServerRequest request,
														   Map<String, String> pathVariables,
														   PathPattern pattern) {
			Map<String, Object> result = new ConcurrentHashMap<>(request.attributes());

			// 合并路径变量
			result.put(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE,
					mergePathVariables(request.pathVariables(), pathVariables));

			// 合并路径模式
			pattern = mergePatterns((PathPattern) request.attributes().get(RouterFunctions.MATCHING_PATTERN_ATTRIBUTE),
					pattern);
			result.put(RouterFunctions.MATCHING_PATTERN_ATTRIBUTE, pattern);
			return result;
		}

		@Override
		public HttpMethod method() {
			return this.request.method();
		}

		@Override
		public String methodName() {
			return this.request.methodName();
		}

		@Override
		public URI uri() {
			return this.request.uri();
		}

		@Override
		public UriBuilder uriBuilder() {
			return this.request.uriBuilder();
		}

		@Override
		public RequestPath requestPath() {
			return this.requestPath;
		}

		@Override
		public Headers headers() {
			return this.request.headers();
		}

		@Override
		public MultiValueMap<String, HttpCookie> cookies() {
			return this.request.cookies();
		}

		@Override
		public Optional<InetSocketAddress> remoteAddress() {
			return this.request.remoteAddress();
		}

		@Override
		public Optional<InetSocketAddress> localAddress() {
			return this.request.localAddress();
		}

		@Override
		public List<HttpMessageReader<?>> messageReaders() {
			return this.request.messageReaders();
		}

		@Override
		public <T> T body(BodyExtractor<T, ? super ServerHttpRequest> extractor) {
			return this.request.body(extractor);
		}

		@Override
		public <T> T body(BodyExtractor<T, ? super ServerHttpRequest> extractor, Map<String, Object> hints) {
			return this.request.body(extractor, hints);
		}

		@Override
		public <T> Mono<T> bodyToMono(Class<? extends T> elementClass) {
			return this.request.bodyToMono(elementClass);
		}

		@Override
		public <T> Mono<T> bodyToMono(ParameterizedTypeReference<T> typeReference) {
			return this.request.bodyToMono(typeReference);
		}

		@Override
		public <T> Flux<T> bodyToFlux(Class<? extends T> elementClass) {
			return this.request.bodyToFlux(elementClass);
		}

		@Override
		public <T> Flux<T> bodyToFlux(ParameterizedTypeReference<T> typeReference) {
			return this.request.bodyToFlux(typeReference);
		}

		@Override
		public Map<String, Object> attributes() {
			return this.attributes;
		}

		@Override
		public Optional<String> queryParam(String name) {
			return this.request.queryParam(name);
		}

		@Override
		public MultiValueMap<String, String> queryParams() {
			return this.request.queryParams();
		}

		@Override
		@SuppressWarnings("unchecked")
		public Map<String, String> pathVariables() {
			return (Map<String, String>) this.attributes.getOrDefault(
					RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE, Collections.emptyMap());

		}

		@Override
		public Mono<WebSession> session() {
			return this.request.session();
		}

		@Override
		public Mono<? extends Principal> principal() {
			return this.request.principal();
		}

		@Override
		public Mono<MultiValueMap<String, String>> formData() {
			return this.request.formData();
		}

		@Override
		public Mono<MultiValueMap<String, Part>> multipartData() {
			return this.request.multipartData();
		}

		@Override
		public ServerWebExchange exchange() {
			return this.request.exchange();
		}

		@Override
		public String toString() {
			return method() + " " + path();
		}

	}

}
