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

package org.springframework.web.servlet.function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.RequestPath;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriUtils;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * {@link RequestPredicate} 的实现，实现了各种有用的请求匹配操作，
 * 例如基于路径、HTTP 方法等进行匹配。
 *
 * @author Arjen Poutsma
 * @since 5.2
 */
public abstract class RequestPredicates {
	/**
	 * 日志记录器
	 */
	private static final Log logger = LogFactory.getLog(RequestPredicates.class);

	/**
	 * 返回一个总是匹配的 {@code RequestPredicate}。
	 *
	 * @return 一个总是匹配的断言
	 */
	public static RequestPredicate all() {
		return request -> true;
	}

	/**
	 * 返回一个 {@code RequestPredicate}，如果请求的 HTTP 方法等于给定的方法，则匹配。
	 *
	 * @param httpMethod 要匹配的 HTTP 方法
	 * @return 一个针对给定 HTTP 方法进行测试的断言
	 */
	public static RequestPredicate method(HttpMethod httpMethod) {
		return new HttpMethodPredicate(httpMethod);
	}

	/**
	 * 返回一个 {@code RequestPredicate}，如果请求的 HTTP 方法等于给定的方法之一，则匹配。
	 *
	 * @param httpMethods 要匹配的 HTTP 方法
	 * @return 一个针对给定 HTTP 方法进行测试的断言
	 */
	public static RequestPredicate methods(HttpMethod... httpMethods) {
		return new HttpMethodPredicate(httpMethods);
	}

	/**
	 * 返回一个 {@code RequestPredicate}，测试请求路径是否与给定的路径模式匹配。
	 *
	 * @param pattern 要匹配的模式
	 * @return 一个针对给定路径模式进行测试的断言
	 */
	public static RequestPredicate path(String pattern) {
		Assert.notNull(pattern, "'pattern' must not be null");
		if (!pattern.isEmpty() && !pattern.startsWith("/")) {
			pattern = "/" + pattern;
		}
		return pathPredicates(PathPatternParser.defaultInstance).apply(pattern);
	}

	/**
	 * 返回一个函数，该函数使用给定的 {@link PathPatternParser} 从模式字符串创建新的路径匹配 {@code RequestPredicates}。
	 * <p>当解析路径模式时，此方法可用于指定非默认的定制 {@code PathPatternParser}。
	 *
	 * @param patternParser 用于解析给定模式的解析器
	 * @return 一个函数，将模式字符串解析为路径匹配的 {@code RequestPredicates} 实例
	 */
	public static Function<String, RequestPredicate> pathPredicates(PathPatternParser patternParser) {
		Assert.notNull(patternParser, "PathPatternParser must not be null");
		return pattern -> new PathPatternPredicate(patternParser.parse(pattern));
	}

	/**
	 * 返回一个 {@code RequestPredicate}，测试请求的头是否与给定的头断言匹配。
	 *
	 * @param headersPredicate 用于测试请求头的断言
	 * @return 一个针对给定头断言进行测试的断言
	 */
	public static RequestPredicate headers(Predicate<ServerRequest.Headers> headersPredicate) {
		return new HeadersPredicate(headersPredicate);
	}

	/**
	 * 返回一个 {@code RequestPredicate}，测试请求的{@linkplain ServerRequest.Headers#contentType() 内容类型}是否与给定的媒体类型之一{@linkplain MediaType#includes(MediaType) 匹配}。
	 *
	 * @param mediaTypes 要将请求的内容类型与之匹配的媒体类型
	 * @return 一个测试请求的内容类型是否与给定媒体类型匹配的断言
	 */
	public static RequestPredicate contentType(MediaType... mediaTypes) {
		Assert.notEmpty(mediaTypes, "'mediaTypes' must not be empty");
		return new ContentTypePredicate(mediaTypes);
	}

	/**
	 * 返回一个 {@code RequestPredicate}，测试请求的{@linkplain ServerRequest.Headers#accept() 接受}头是否与给定的媒体类型之一{@linkplain MediaType#isCompatibleWith(MediaType) 兼容}。
	 *
	 * @param mediaTypes 要将请求的接受头与之匹配的媒体类型
	 * @return 一个测试请求的接受头是否与给定媒体类型匹配的断言
	 */
	public static RequestPredicate accept(MediaType... mediaTypes) {
		Assert.notEmpty(mediaTypes, "'mediaTypes' must not be empty");
		return new AcceptPredicate(mediaTypes);
	}

	/**
	 * 返回一个 {@code RequestPredicate}，如果请求的 HTTP 方法为 {@code GET}，并且给定的 {@code pattern} 与请求路径匹配，则匹配。
	 *
	 * @param pattern 要匹配的路径模式
	 * @return 如果请求方法为 GET，并且给定的模式与请求路径匹配，则匹配的断言
	 */
	public static RequestPredicate GET(String pattern) {
		return method(HttpMethod.GET).and(path(pattern));
	}

	/**
	 * 返回一个 {@code RequestPredicate}，如果请求的 HTTP 方法为 {@code HEAD}，并且给定的 {@code pattern} 与请求路径匹配，则匹配。
	 *
	 * @param pattern 要匹配的路径模式
	 * @return 如果请求方法为 HEAD，并且给定的模式与请求路径匹配，则匹配的断言
	 */
	public static RequestPredicate HEAD(String pattern) {
		return method(HttpMethod.HEAD).and(path(pattern));
	}

	/**
	 * 返回一个 {@code RequestPredicate}，如果请求的 HTTP 方法为 {@code POST}，并且给定的 {@code pattern} 与请求路径匹配，则匹配。
	 *
	 * @param pattern 要匹配的路径模式
	 * @return 如果请求方法为 POST，并且给定的模式与请求路径匹配，则匹配的断言
	 */
	public static RequestPredicate POST(String pattern) {
		return method(HttpMethod.POST).and(path(pattern));
	}

	/**
	 * 返回一个 {@code RequestPredicate}，如果请求的 HTTP 方法为 {@code PUT}，并且给定的 {@code pattern} 与请求路径匹配，则匹配。
	 *
	 * @param pattern 要匹配的路径模式
	 * @return 如果请求方法为 PUT，并且给定的模式与请求路径匹配，则匹配的断言
	 */
	public static RequestPredicate PUT(String pattern) {
		return method(HttpMethod.PUT).and(path(pattern));
	}

	/**
	 * 返回一个 {@code RequestPredicate}，如果请求的 HTTP 方法为 {@code PATCH}，并且给定的 {@code pattern} 与请求路径匹配，则匹配。
	 *
	 * @param pattern 要匹配的路径模式
	 * @return 如果请求方法为 PATCH，并且给定的模式与请求路径匹配，则匹配的断言
	 */
	public static RequestPredicate PATCH(String pattern) {
		return method(HttpMethod.PATCH).and(path(pattern));
	}

	/**
	 * 返回一个 {@code RequestPredicate}，如果请求的 HTTP 方法为 {@code DELETE}，并且给定的 {@code pattern} 与请求路径匹配，则匹配。
	 *
	 * @param pattern 要匹配的路径模式
	 * @return 如果请求方法为 DELETE，并且给定的模式与请求路径匹配，则匹配的断言
	 */
	public static RequestPredicate DELETE(String pattern) {
		return method(HttpMethod.DELETE).and(path(pattern));
	}

	/**
	 * 返回一个 {@code RequestPredicate}，如果请求的 HTTP 方法为 {@code OPTIONS}，并且给定的 {@code pattern} 与请求路径匹配，则匹配。
	 *
	 * @param pattern 要匹配的路径模式
	 * @return 如果请求方法为 OPTIONS，并且给定的模式与请求路径匹配，则匹配的断言
	 */
	public static RequestPredicate OPTIONS(String pattern) {
		return method(HttpMethod.OPTIONS).and(path(pattern));
	}

	/**
	 * 返回一个 {@code RequestPredicate}，如果请求的路径具有给定的扩展名，则匹配。
	 *
	 * @param extension 要匹配的路径扩展名，不区分大小写
	 * @return 如果请求的路径具有给定的文件扩展名，则匹配的断言
	 */
	public static RequestPredicate pathExtension(String extension) {
		Assert.notNull(extension, "'extension' must not be null");
		return new PathExtensionPredicate(extension);
	}

	/**
	 * 返回一个 {@code RequestPredicate}，如果请求的路径与给定的谓词匹配，则匹配。
	 *
	 * @param extensionPredicate 用于针对请求路径扩展名进行测试的谓词
	 * @return 如果给定的谓词针对请求的路径扩展名匹配，则匹配的断言
	 */
	public static RequestPredicate pathExtension(Predicate<String> extensionPredicate) {
		return new PathExtensionPredicate(extensionPredicate);
	}

	/**
	 * 返回一个 {@code RequestPredicate}，如果请求的具有给定名称的参数具有给定值，则匹配。
	 *
	 * @param name  参数的名称，用于测试
	 * @param value 要测试的参数的值
	 * @return 如果参数具有给定值，则匹配的断言
	 * @see ServerRequest#param(String)
	 */
	public static RequestPredicate param(String name, String value) {
		return new ParamPredicate(name, value);
	}

	/**
	 * 返回一个 {@code RequestPredicate}，用于针对给定名称的请求参数测试给定谓词。
	 *
	 * @param name      参数的名称，用于测试
	 * @param predicate 用于针对参数值进行测试的谓词
	 * @return 匹配给定名称参数的参数值的给定谓词的断言
	 * @see ServerRequest#param(String)
	 */
	public static RequestPredicate param(String name, Predicate<String> predicate) {
		return new ParamPredicate(name, predicate);
	}


	private static void traceMatch(String prefix, Object desired, @Nullable Object actual, boolean match) {
		if (logger.isTraceEnabled()) {
			logger.trace(String.format("%s \"%s\" %s against value \"%s\"",
					prefix, desired, match ? "matches" : "does not match", actual));
		}
	}

	private static void restoreAttributes(ServerRequest request, Map<String, Object> attributes) {
		// 清空原来的属性值
		request.attributes().clear();
		// 将新的属性值添加进去
		request.attributes().putAll(attributes);
	}

	private static Map<String, String> mergePathVariables(Map<String, String> oldVariables,
														  Map<String, String> newVariables) {

		// 如果新变量集合不为空，则合并新旧变量
		if (!newVariables.isEmpty()) {
			Map<String, String> mergedVariables = new LinkedHashMap<>(oldVariables);
			mergedVariables.putAll(newVariables);
			return mergedVariables;
		} else {
			// 否则返回旧变量
			return oldVariables;
		}
	}

	private static PathPattern mergePatterns(@Nullable PathPattern oldPattern, PathPattern newPattern) {
		// 如果旧的模式不为空，则合并新旧模式
		if (oldPattern != null) {
			return oldPattern.combine(newPattern);
		} else {
			// 否则返回新模式
			return newPattern;
		}

	}


	/**
	 * 接收来自请求谓词的逻辑结构的通知。
	 */
	public interface Visitor {

		/**
		 * 接收 HTTP 方法谓词的通知。
		 *
		 * @param methods 组成谓词的 HTTP 方法
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
		 * 接收 HTTP 标头谓词的通知。
		 *
		 * @param name  要检查的 HTTP 标头的名称
		 * @param value HTTP 标头的期望值
		 * @see RequestPredicates#headers(Predicate)
		 * @see RequestPredicates#contentType(MediaType...)
		 * @see RequestPredicates#accept(MediaType...)
		 */
		void header(String name, String value);

		/**
		 * 接收参数谓词的通知。
		 *
		 * @param name  参数的名称
		 * @param value 参数的期望值
		 * @see RequestPredicates#param(String, String)
		 */
		void param(String name, String value);

		/**
		 * 接收逻辑 AND 谓词的第一个通知。
		 * 随后的通知将包含 AND 谓词的左侧;
		 * 接着是 {@link #and()}，然后是右侧，最后是 {@link #endAnd()}。
		 *
		 * @see RequestPredicate#and(RequestPredicate)
		 */
		void startAnd();

		/**
		 * 接收逻辑 AND 谓词的“中间”通知。
		 * 接下来的通知包含右侧，然后是 {@link #endAnd()}。
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
		 * 接收逻辑 OR 谓词的第一个通知。
		 * 随后的通知将包含 OR 谓词的左侧;
		 * 第二个通知包含右侧，然后是 {@link #endOr()}。
		 *
		 * @see RequestPredicate#or(RequestPredicate)
		 */
		void startOr();

		/**
		 * 接收逻辑 OR 谓词的“中间”通知。
		 * 接下来的通知包含右侧，然后是 {@link #endOr()}。
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
		 * 接收否定谓词的第一个通知。
		 * 随后的通知将包含否定谓词，然后是 {@link #endNegate()}。
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
		 * 接收未知谓词的第一个通知。
		 */
		void unknown(RequestPredicate predicate);
	}


	private static class HttpMethodPredicate implements RequestPredicate {
		/**
		 * Http方法集合
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

		@Override
		public boolean test(ServerRequest request) {
			// 获取请求方法
			HttpMethod method = method(request);
			// 检查请求方法是否与已配置的方法匹配
			boolean match = this.httpMethods.contains(method);
			// 输出方法匹配的跟踪信息
			traceMatch("Method", this.httpMethods, method, match);
			// 返回匹配结果
			return match;
		}

		@Nullable
		private static HttpMethod method(ServerRequest request) {
			// 检查是否为预检请求
			if (CorsUtils.isPreFlightRequest(request.servletRequest())) {
				// 如果是预检请求，获取访问控制请求方法
				String accessControlRequestMethod = request.headers().firstHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD);
				// 解析并返回访问控制请求方法
				return HttpMethod.resolve(accessControlRequestMethod);
			} else {
				// 如果不是预检请求，返回请求方法
				return request.method();
			}
		}

		@Override
		public void accept(Visitor visitor) {
			visitor.method(Collections.unmodifiableSet(this.httpMethods));
		}

		@Override
		public String toString() {
			// 如果只有一个HTTP方法，则返回该方法的字符串表示形式
			if (this.httpMethods.size() == 1) {
				return this.httpMethods.iterator().next().toString();
			} else {
				// 如果有多个HTTP方法，则返回所有方法的字符串表示形式
				return this.httpMethods.toString();
			}
		}
	}


	private static class PathPatternPredicate implements RequestPredicate, ChangePathPatternParserVisitor.Target {
		/**
		 * 路径模式
		 */
		private PathPattern pattern;

		public PathPatternPredicate(PathPattern pattern) {
			Assert.notNull(pattern, "'pattern' must not be null");
			this.pattern = pattern;
		}

		@Override
		public boolean test(ServerRequest request) {
			// 从请求中获取路径容器
			PathContainer pathContainer = request.requestPath().pathWithinApplication();
			// 尝试使用路径模式匹配并提取路径信息
			PathPattern.PathMatchInfo info = this.pattern.matchAndExtract(pathContainer);
			// 跟踪模式匹配情况并输出日志
			traceMatch("Pattern", this.pattern.getPatternString(), request.path(), info != null);
			if (info != null) {
				// 如果匹配成功，则合并路径变量到请求属性中
				mergeAttributes(request, info.getUriVariables(), this.pattern);
				return true;
			} else {
				// 如果匹配失败，则返回false
				return false;
			}
		}

		private static void mergeAttributes(ServerRequest request, Map<String, String> variables,
											PathPattern pattern) {
			// 合并现有路径变量和新的变量
			Map<String, String> pathVariables = mergePathVariables(request.pathVariables(), variables);
			// 将合并后的路径变量设置到请求属性中
			request.attributes().put(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE,
					Collections.unmodifiableMap(pathVariables));

			// 合并现有的路径模式和新的模式
			pattern = mergePatterns(
					(PathPattern) request.attributes().get(RouterFunctions.MATCHING_PATTERN_ATTRIBUTE),
					pattern);
			// 将合并后的模式设置到请求属性中
			request.attributes().put(RouterFunctions.MATCHING_PATTERN_ATTRIBUTE, pattern);
		}

		@Override
		public Optional<ServerRequest> nest(ServerRequest request) {
			// 尝试匹配请求路径的起始部分与路由模式
			return Optional.ofNullable(this.pattern.matchStartOfPath(request.requestPath().pathWithinApplication()))
					// 如果匹配成功，则创建一个子路径服务器请求包装器
					.map(info -> new SubPathServerRequestWrapper(request, info, this.pattern));
		}

		@Override
		public void accept(Visitor visitor) {
			visitor.path(this.pattern.getPatternString());
		}

		@Override
		public void changeParser(PathPatternParser parser) {
			// 获取当前路由模式的字符串表示
			String patternString = this.pattern.getPatternString();
			// 使用路径解析器解析模式字符串并重新设置路由模式
			this.pattern = parser.parse(patternString);
		}

		@Override
		public String toString() {
			return this.pattern.getPatternString();
		}

	}


	private static class HeadersPredicate implements RequestPredicate {
		/**
		 * 请求头谓词
		 */
		private final Predicate<ServerRequest.Headers> headersPredicate;

		public HeadersPredicate(Predicate<ServerRequest.Headers> headersPredicate) {
			Assert.notNull(headersPredicate, "Predicate must not be null");
			this.headersPredicate = headersPredicate;
		}

		@Override
		public boolean test(ServerRequest request) {
			// 如果是预检请求则返回true
			if (CorsUtils.isPreFlightRequest(request.servletRequest())) {
				return true;
			} else {
				// 否则通过头部谓词测试请求头部
				return this.headersPredicate.test(request.headers());
			}
		}

		@Override
		public String toString() {
			return this.headersPredicate.toString();
		}
	}

	private static class ContentTypePredicate extends HeadersPredicate {
		/**
		 * 媒体类型集合
		 */
		private final Set<MediaType> mediaTypes;

		public ContentTypePredicate(MediaType... mediaTypes) {
			this(new HashSet<>(Arrays.asList(mediaTypes)));
		}

		private ContentTypePredicate(Set<MediaType> mediaTypes) {
			// 调用父类构造函数，传入一个谓词，用于测试请求的Content-Type是否与指定的媒体类型列表之一匹配
			super(headers -> {
				// 获取请求的Content-Type，默认为APPLICATION_OCTET_STREAM
				MediaType contentType = headers.contentType().orElse(MediaType.APPLICATION_OCTET_STREAM);
				// 检查请求的Content-Type是否与指定的媒体类型列表中的任何一个匹配
				boolean match = mediaTypes.stream()
						.anyMatch(mediaType -> mediaType.includes(contentType));
				// 打印匹配信息
				traceMatch("Content-Type", mediaTypes, contentType, match);
				// 返回匹配结果
				return match;
			});
			// 将传入的媒体类型列表赋值给成员变量
			this.mediaTypes = mediaTypes;
		}

		@Override
		public void accept(Visitor visitor) {
			visitor.header(HttpHeaders.CONTENT_TYPE,
					(this.mediaTypes.size() == 1) ?
							// 如果媒体类型列表中只有一个媒体类型，则直接取该媒体类型；
							this.mediaTypes.iterator().next().toString() :
							// 否则，将媒体类型列表转换为字符串形式
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

	private static class AcceptPredicate extends HeadersPredicate {
		/**
		 * 媒体类型集合
		 */
		private final Set<MediaType> mediaTypes;

		public AcceptPredicate(MediaType... mediaTypes) {
			this(new HashSet<>(Arrays.asList(mediaTypes)));
		}

		private AcceptPredicate(Set<MediaType> mediaTypes) {
			super(headers -> {
				// 获取请求头中的接受的媒体类型列表
				List<MediaType> acceptedMediaTypes = acceptedMediaTypes(headers);
				// 检查请求头中的接受的媒体类型是否与当前条件中的媒体类型匹配
				boolean match = acceptedMediaTypes.stream()
						.anyMatch(acceptedMediaType -> mediaTypes.stream()
								.anyMatch(acceptedMediaType::isCompatibleWith));
				// 打印匹配信息
				traceMatch("Accept", mediaTypes, acceptedMediaTypes, match);
				return match;
			});
			this.mediaTypes = mediaTypes;
		}

		@NonNull
		private static List<MediaType> acceptedMediaTypes(ServerRequest.Headers headers) {
			// 从请求头中获取接受的媒体类型列表
			List<MediaType> acceptedMediaTypes = headers.accept();
			// 如果接受的媒体类型列表为空，则将其替换为包含“全部”媒体类型的列表
			if (acceptedMediaTypes.isEmpty()) {
				acceptedMediaTypes = Collections.singletonList(MediaType.ALL);
			} else {
				// 否则，按特异性和质量对接受的媒体类型列表进行排序
				MediaType.sortBySpecificityAndQuality(acceptedMediaTypes);
			}
			return acceptedMediaTypes;
		}

		@Override
		public void accept(Visitor visitor) {
			visitor.header(HttpHeaders.ACCEPT,
					(this.mediaTypes.size() == 1) ?
							// 添加"Accept"请求头，如果媒体类型集合大小为1，则使用集合中的媒体类型；
							this.mediaTypes.iterator().next().toString() :
							// 否则使用集合的字符串表示形式
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


	private static class PathExtensionPredicate implements RequestPredicate {
		/**
		 * 扩展名谓词
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

			// 创建一个用于匹配扩展名的断言
			this.extensionPredicate = s -> {
				// 比较忽略大小写，如果请求的扩展名与给定的扩展名相同，则返回true
				boolean match = extension.equalsIgnoreCase(s);
				traceMatch("Extension", extension, s, match);
				return match;
			};
			this.extension = extension;
		}

		@Override
		public boolean test(ServerRequest request) {
			// 从请求路径中提取文件扩展名
			String pathExtension = UriUtils.extractFileExtension(request.path());
			// 使用扩展名断言来测试提取的扩展名是否匹配
			return this.extensionPredicate.test(pathExtension);
		}

		@Override
		public void accept(Visitor visitor) {
			visitor.pathExtension(
					(this.extension != null) ?
							// 如果有明确的扩展名，则使用该扩展名；
							this.extension :
							// 否则，使用扩展名断言的字符串表示形式
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


	private static class ParamPredicate implements RequestPredicate {
		/**
		 * 参数名称
		 */
		private final String name;

		/**
		 * 参数值谓词
		 */
		private final Predicate<String> valuePredicate;

		/**
		 * 参数值
		 */
		@Nullable
		private final String value;

		public ParamPredicate(String name, Predicate<String> valuePredicate) {
			Assert.notNull(name, "Name must not be null");
			Assert.notNull(valuePredicate, "Predicate must not be null");
			this.name = name;
			this.valuePredicate = valuePredicate;
			this.value = null;
		}

		public ParamPredicate(String name, String value) {
			Assert.notNull(name, "Name must not be null");
			Assert.notNull(value, "Value must not be null");
			this.name = name;
			this.valuePredicate = value::equals;
			this.value = value;
		}

		@Override
		public boolean test(ServerRequest request) {
			// 从请求参数中提取参数值
			Optional<String> s = request.param(this.name);
			// 使用参数值断言来测试提取的参数值是否匹配
			return s.filter(this.valuePredicate).isPresent();
		}

		@Override
		public void accept(Visitor visitor) {
			visitor.param(this.name,
					(this.value != null) ?
							// 如果有明确的值，则使用该值；
							this.value :
							// 否则，使用值断言的字符串表示形式
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
	 * {@link RequestPredicate} 用于左侧和右侧谓词都必须匹配的情况。
	 */
	static class AndRequestPredicate implements RequestPredicate, ChangePathPatternParserVisitor.Target {
		/**
		 * 左边请求谓词
		 */
		private final RequestPredicate left;

		/**
		 * 右边请求谓词
		 */
		private final RequestPredicate right;

		public AndRequestPredicate(RequestPredicate left, RequestPredicate right) {
			Assert.notNull(left, "Left RequestPredicate must not be null");
			Assert.notNull(right, "Right RequestPredicate must not be null");
			this.left = left;
			this.right = right;
		}

		@Override
		public boolean test(ServerRequest request) {
			// 备份原始请求属性
			Map<String, Object> oldAttributes = new HashMap<>(request.attributes());

			// 如果左右断言都通过，则返回true；
			if (this.left.test(request) && this.right.test(request)) {
				return true;
			}
			// 否则，恢复原始请求属性并返回false
			restoreAttributes(request, oldAttributes);
			return false;
		}

		@Override
		public Optional<ServerRequest> nest(ServerRequest request) {
			return this.left.nest(request).flatMap(this.right::nest);
		}

		@Override
		public void accept(Visitor visitor) {
			// 开始AND组合断言
			visitor.startAnd();
			// 将左侧断言添加到访问者中
			this.left.accept(visitor);
			// 添加AND连接符
			visitor.and();
			// 将右侧断言添加到访问者中
			this.right.accept(visitor);
			// 结束AND组合断言
			visitor.endAnd();
		}

		@Override
		public void changeParser(PathPatternParser parser) {
			// 如果左侧断言是ChangePathPatternParserVisitor.Target类型，则更改其解析器
			if (this.left instanceof ChangePathPatternParserVisitor.Target) {
				((ChangePathPatternParserVisitor.Target) this.left).changeParser(parser);
			}
			// 如果右侧断言是ChangePathPatternParserVisitor.Target类型，则更改其解析器
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
	 * {@link RequestPredicate} 反转委托谓词的谓词。
	 */
	static class NegateRequestPredicate implements RequestPredicate, ChangePathPatternParserVisitor.Target {
		/**
		 * 要代理的请求谓词
		 */
		private final RequestPredicate delegate;

		public NegateRequestPredicate(RequestPredicate delegate) {
			Assert.notNull(delegate, "Delegate must not be null");
			this.delegate = delegate;
		}

		@Override
		public boolean test(ServerRequest request) {
			// 备份旧的请求属性
			Map<String, Object> oldAttributes = new HashMap<>(request.attributes());
			// 调用委托断言并取反
			boolean result = !this.delegate.test(request);
			if (!result) {
				// 如果结果为false，即委托断言未通过，则恢复旧的请求属性
				restoreAttributes(request, oldAttributes);
			}
			return result;
		}

		@Override
		public void accept(Visitor visitor) {
			// 开始处理否定逻辑
			visitor.startNegate();
			// 接受委托断言的访问
			this.delegate.accept(visitor);
			// 结束否定逻辑
			visitor.endNegate();
		}

		@Override
		public void changeParser(PathPatternParser parser) {
			// 如果委托是 ChangePathPatternParserVisitor.Target 的实例
			if (this.delegate instanceof ChangePathPatternParserVisitor.Target) {
				// 将委托转换为 ChangePathPatternParserVisitor.Target 并更改解析器
				((ChangePathPatternParserVisitor.Target) this.delegate).changeParser(parser);
			}
		}

		@Override
		public String toString() {
			return "!" + this.delegate.toString();
		}
	}

	/**
	 * {@link RequestPredicate}，其中可以匹配{@code left}或{@code right}谓词。
	 */
	static class OrRequestPredicate implements RequestPredicate, ChangePathPatternParserVisitor.Target {
		/**
		 * 左侧请求谓词
		 */
		private final RequestPredicate left;

		/**
		 * 右侧请求谓词
		 */
		private final RequestPredicate right;

		public OrRequestPredicate(RequestPredicate left, RequestPredicate right) {
			Assert.notNull(left, "Left RequestPredicate must not be null");
			Assert.notNull(right, "Right RequestPredicate must not be null");
			this.left = left;
			this.right = right;
		}

		@Override
		public boolean test(ServerRequest request) {
			// 创建一个旧属性的副本
			Map<String, Object> oldAttributes = new HashMap<>(request.attributes());

			// 如果左侧条件测试通过，则返回 true
			if (this.left.test(request)) {
				return true;
			} else {
				// 如果左侧条件测试失败，则恢复属性并检查右侧条件
				restoreAttributes(request, oldAttributes);
				if (this.right.test(request)) {
					return true;
				}
			}

			// 如果左右条件都不满足，则恢复属性并返回 false
			restoreAttributes(request, oldAttributes);
			return false;
		}

		@Override
		public Optional<ServerRequest> nest(ServerRequest request) {
			// 尝试对左侧条件进行嵌套操作
			Optional<ServerRequest> leftResult = this.left.nest(request);
			// 如果左侧条件的嵌套操作成功，则返回结果
			if (leftResult.isPresent()) {
				return leftResult;
			} else {
				// 如果左侧条件的嵌套操作失败，则尝试对右侧条件进行嵌套操作
				return this.right.nest(request);
			}
		}

		@Override
		public void accept(Visitor visitor) {
			// 开始一个逻辑或操作
			visitor.startOr();
			// 访问左侧条件
			this.left.accept(visitor);
			// 添加逻辑或符号
			visitor.or();
			// 访问右侧条件
			this.right.accept(visitor);
			// 结束逻辑或操作
			visitor.endOr();
		}

		@Override
		public void changeParser(PathPatternParser parser) {
			// 如果左侧条件是目标，更改其解析器
			if (this.left instanceof ChangePathPatternParserVisitor.Target) {
				((ChangePathPatternParserVisitor.Target) this.left).changeParser(parser);
			}
			// 如果右侧条件是目标，更改其解析器
			if (this.right instanceof ChangePathPatternParserVisitor.Target) {
				((ChangePathPatternParserVisitor.Target) this.right).changeParser(parser);
			}
		}

		@Override
		public String toString() {
			return String.format("(%s || %s)", this.left, this.right);
		}
	}


	private static class SubPathServerRequestWrapper implements ServerRequest {
		/**
		 * 服务端请求
		 */
		private final ServerRequest request;

		/**
		 * 请求路径
		 */
		private RequestPath requestPath;

		/**
		 * 属性值
		 */
		private final Map<String, Object> attributes;

		public SubPathServerRequestWrapper(ServerRequest request,
										   PathPattern.PathRemainingMatchInfo info, PathPattern pattern) {
			this.request = request;
			this.requestPath = requestPath(request.requestPath(), info);
			this.attributes = mergeAttributes(request, info.getUriVariables(), pattern);
		}

		private static RequestPath requestPath(RequestPath original, PathPattern.PathRemainingMatchInfo info) {
			// 创建一个 StringBuilder 对象，用于构建上下文路径
			StringBuilder contextPath = new StringBuilder(original.contextPath().value());

			// 将路径匹配的值附加到上下文路径
			contextPath.append(info.getPathMatched().value());

			// 获取上下文路径的长度
			int length = contextPath.length();

			// 如果上下文路径不为空且最后一个字符是斜杠，则删除末尾的斜杠
			if (length > 0 && contextPath.charAt(length - 1) == '/') {
				contextPath.setLength(length - 1);
			}

			// 将修改后的上下文路径应用到原始的 ServerHttpRequest 对象中
			return original.modifyContextPath(contextPath.toString());
		}

		private static Map<String, Object> mergeAttributes(ServerRequest request,
														   Map<String, String> pathVariables, PathPattern pattern) {
			// 使用请求中的属性初始化一个线程安全的 ConcurrentHashMap
			Map<String, Object> result = new ConcurrentHashMap<>(request.attributes());

			// 合并 URI 模板变量并将结果放入属性集合中
			result.put(RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE, mergePathVariables(request.pathVariables(), pathVariables));

			// 合并路径模式并将结果放入属性集合中
			pattern = mergePatterns(
					(PathPattern) request.attributes().get(RouterFunctions.MATCHING_PATTERN_ATTRIBUTE),
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
		public MultiValueMap<String, Cookie> cookies() {
			return this.request.cookies();
		}

		@Override
		public Optional<InetSocketAddress> remoteAddress() {
			return this.request.remoteAddress();
		}

		@Override
		public List<HttpMessageConverter<?>> messageConverters() {
			return this.request.messageConverters();
		}

		@Override
		public <T> T body(Class<T> bodyType) throws ServletException, IOException {
			return this.request.body(bodyType);
		}

		@Override
		public <T> T body(ParameterizedTypeReference<T> bodyType)
				throws ServletException, IOException {
			return this.request.body(bodyType);
		}

		@Override
		public Optional<Object> attribute(String name) {
			return this.request.attribute(name);
		}

		@Override
		public Map<String, Object> attributes() {
			return this.attributes;
		}

		@Override
		public Optional<String> param(String name) {
			return this.request.param(name);
		}

		@Override
		public MultiValueMap<String, String> params() {
			return this.request.params();
		}

		@Override
		public MultiValueMap<String, Part> multipartData() throws IOException, ServletException {
			return this.request.multipartData();
		}

		@Override
		@SuppressWarnings("unchecked")
		public Map<String, String> pathVariables() {
			return (Map<String, String>) this.attributes.getOrDefault(
					RouterFunctions.URI_TEMPLATE_VARIABLES_ATTRIBUTE, Collections.emptyMap());
		}

		@Override
		public HttpSession session() {
			return this.request.session();
		}


		@Override
		public Optional<Principal> principal() {
			return this.request.principal();
		}

		@Override
		public HttpServletRequest servletRequest() {
			return this.request.servletRequest();
		}

		@Override
		public Optional<ServerResponse> checkNotModified(Instant lastModified) {
			return this.request.checkNotModified(lastModified);
		}

		@Override
		public Optional<ServerResponse> checkNotModified(String etag) {
			return this.request.checkNotModified(etag);
		}

		@Override
		public Optional<ServerResponse> checkNotModified(Instant lastModified, String etag) {
			return this.request.checkNotModified(lastModified, etag);
		}

		@Override
		public String toString() {
			return method() + " " + path();
		}

	}

}
