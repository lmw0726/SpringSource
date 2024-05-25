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

package org.springframework.web.util;

import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;

import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * 以构建器风格的方法来准备和扩展带有变量的 URI 模板。
 *
 * <p>实际上是 {@link UriComponentsBuilder} 的泛化，但可以直接扩展为 {@link URI} 而不是
 * {@link UriComponents}，同时将编码偏好、基础 URI 等常见问题留作实现考虑。
 *
 * <p>通常通过 {@link UriBuilderFactory} 获取，它作为一个中央组件，配置一次后用于创建许多 URL。
 *
 * @author Rossen Stoyanchev
 * @see UriBuilderFactory
 * @see UriComponentsBuilder
 * @since 5.0
 */
public interface UriBuilder {

	/**
	 * 设置 URI 方案，可以包含 URI 模板变量，也可以为 {@code null} 以清除此构建器的方案。
	 *
	 * @param scheme URI 方案
	 */
	UriBuilder scheme(@Nullable String scheme);

	/**
	 * 设置 URI 用户信息，可以包含 URI 模板变量，也可以为 {@code null} 以清除此构建器的用户信息。
	 *
	 * @param userInfo URI 用户信息
	 */
	UriBuilder userInfo(@Nullable String userInfo);

	/**
	 * 设置 URI 主机，可以包含 URI 模板变量，也可以为 {@code null} 以清除此构建器的主机。
	 *
	 * @param host URI 主机
	 */
	UriBuilder host(@Nullable String host);

	/**
	 * 设置 URI 端口。传递 {@code -1} 将清除此构建器的端口。
	 *
	 * @param port URI 端口
	 */
	UriBuilder port(int port);

	/**
	 * 设置 URI 端口。仅在端口需要用 URI 变量参数化时使用此方法。否则请使用 {@link #port(int)}。
	 * 传递 {@code null} 将清除此构建器的端口。
	 *
	 * @param port URI 端口
	 */
	UriBuilder port(@Nullable String port);

	/**
	 * 向此构建器的路径追加内容。
	 * <p>给定的值按原样追加到之前的 {@link #path(String) path} 值后面，不插入任何额外的斜杠。例如：
	 * <pre class="code">
	 *
	 * builder.path("/first-").path("value/").path("/{id}").build("123")
	 *
	 * // 结果是 "/first-value/123"
	 * </pre>
	 * <p>相比之下，{@link #pathSegment(String...) pathSegment} 会在各个路径段之间插入斜杠。例如：
	 * <pre class="code">
	 *
	 * builder.pathSegment("first-value", "second-value").path("/")
	 *
	 * // 结果是 "/first-value/second-value/"
	 * </pre>
	 * <p>生成的完整路径将被规范化以消除重复的斜杠。
	 * <p><strong>注意：</strong> 在 {@link #path(String) path} 中插入包含斜杠的 URI 变量值时，这些斜杠是否被编码取决于配置的编码模式。有关详细信息，请参阅
	 * {@link UriComponentsBuilder#encode()}，或如果通过 {@code WebClient} 或 {@code RestTemplate} 间接构建 URI，请参阅其
	 * {@link DefaultUriBuilderFactory#setEncodingMode encodingMode}。还可以参阅参考文档的
	 * <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/web.html#web-uri-encoding">URI 编码</a> 部分。
	 *
	 * @param path URI 路径
	 */
	UriBuilder path(String path);

	/**
	 * 覆盖当前路径。
	 *
	 * @param path URI 路径，或 {@code null} 表示空路径
	 */
	UriBuilder replacePath(@Nullable String path);

	/**
	 * 使用路径段追加到路径。例如：
	 * <pre class="code">
	 *
	 * builder.pathSegment("first-value", "second-value", "{id}").build("123")
	 *
	 * // 结果是 "/first-value/second-value/123"
	 * </pre>
	 * <p>如果路径段中存在斜杠，则会进行编码：
	 * <pre class="code">
	 *
	 * builder.pathSegment("ba/z", "{id}").build("a/b")
	 *
	 * // 结果是 "/ba%2Fz/a%2Fb"
	 * </pre>
	 * 若要插入尾部斜杠，请使用 {@link #path} 构建方法：
	 * <pre class="code">
	 *
	 * builder.pathSegment("first-value", "second-value").path("/")
	 *
	 * // 结果是 "/first-value/second-value/"
	 * </pre>
	 * <p>空路径段将被忽略，因此结果完整路径中不会出现重复的斜杠。
	 *
	 * @param pathSegments URI 路径段
	 */
	UriBuilder pathSegment(String... pathSegments) throws IllegalArgumentException;

	/**
	 * 将给定的查询字符串解析为查询参数，其中参数由 {@code '&'} 分隔，其值（如果有）由 {@code '='} 分隔。
	 * 查询字符串可以包含 URI 模板变量。
	 * <p><strong>注意：</strong> 请查看 {@link #queryParam(String, Object...)} 的 Javadoc，以了解有关单个查询参数的处理和编码的更多说明。
	 *
	 * @param query 查询字符串
	 */
	UriBuilder query(String query);

	/**
	 * 清除现有的查询参数，然后委托给 {@link #query(String)}。
	 * <p><strong>注意：</strong> 请查看 {@link #queryParam(String, Object...)} 的 Javadoc，以了解有关单个查询参数的处理和编码的更多说明。
	 *
	 * @param query 查询字符串；{@code null} 值将移除所有查询参数。
	 */
	UriBuilder replaceQuery(@Nullable String query);

	/**
	 * 追加给定的查询参数。参数名称和值都可以包含 URI 模板变量，以便稍后从值中扩展。如果没有给定值，生成的 URI 将仅包含查询参数名称，
	 * 例如 {@code "?foo"} 而不是 {@code "?foo=bar"}。
	 * <p><strong>注意：</strong> 编码（如果应用的话）只会编码在查询参数名称或值中非法的字符，如 {@code "="} 或 {@code "&"}。所有其他符号根据
	 * <a href="https://tools.ietf.org/html/rfc3986">RFC 3986</a> 的语法规则是合法的，不会被编码。这包括 {@code "+"}，有时需要对其进行编码以避免
	 * 其被解释为编码的空格。可以通过使用 URI 模板变量和对变量值进行更严格的编码来应用更严格的编码。有关更多详细信息，请阅读 Spring Framework 参考文档的
	 * <a href="https://docs.spring.io/spring/docs/current/spring-framework-reference/web.html#web-uri-encoding">"URI 编码"</a> 部分。
	 *
	 * @param name   查询参数名称
	 * @param values 查询参数值
	 * @see #queryParam(String, Collection)
	 */
	UriBuilder queryParam(String name, Object... values);

	/**
	 * {@link #queryParam(String, Object...)} 的变体，使用集合。
	 * <p><strong>注意：</strong> 请查看 {@link #queryParam(String, Object...)} 的 Javadoc，以了解有关单个查询参数的处理和编码的更多说明。
	 *
	 * @param name   查询参数名称
	 * @param values 查询参数值
	 * @see #queryParam(String, Object...)
	 * @since 5.2
	 */
	UriBuilder queryParam(String name, @Nullable Collection<?> values);

	/**
	 * 委托给 {@link #queryParam(String, Object...)} 或 {@link #queryParam(String, Collection)}，
	 * 如果给定的 {@link Optional} 有值，则添加查询参数，否则如果它为空，则不添加任何查询参数。
	 *
	 * @param name  查询参数名称
	 * @param value 一个 Optional，可以为空或持有查询参数值。
	 * @since 5.3
	 */
	UriBuilder queryParamIfPresent(String name, Optional<?> value);

	/**
	 * 添加多个查询参数和值。
	 * <p><strong>注意：</strong> 请查看 {@link #queryParam(String, Object...)} 的 Javadoc，以了解有关单个查询参数的处理和编码的更多说明。
	 *
	 * @param params 查询参数
	 */
	UriBuilder queryParams(MultiValueMap<String, String> params);

	/**
	 * 设置查询参数值，替换现有值，如果没有给定值，则删除查询参数。
	 * <p><strong>注意：</strong> 请查看 {@link #queryParam(String, Object...)} 的 Javadoc，以了解有关单个查询参数的处理和编码的更多说明。
	 *
	 * @param name   查询参数名称
	 * @param values 查询参数值
	 * @see #replaceQueryParam(String, Collection)
	 */
	UriBuilder replaceQueryParam(String name, Object... values);

	/**
	 * {@link #replaceQueryParam(String, Object...)} 的变体，使用集合。
	 * <p><strong>注意：</strong> 请查看 {@link #queryParam(String, Object...)} 的 Javadoc，以了解有关单个查询参数的处理和编码的更多说明。
	 *
	 * @param name   查询参数名称
	 * @param values 查询参数值
	 * @since 5.2
	 * @see #replaceQueryParam(String, Object...)
	 */
	UriBuilder replaceQueryParam(String name, @Nullable Collection<?> values);

	/**
	 * 在删除所有现有查询参数后设置查询参数值。
	 * <p><strong>注意：</strong> 请查看 {@link #queryParam(String, Object...)} 的 Javadoc，以了解有关单个查询参数的处理和编码的更多说明。
	 *
	 * @param params 查询参数
	 */
	UriBuilder replaceQueryParams(MultiValueMap<String, String> params);

	/**
	 * 设置 URI 片段。给定的片段可以包含 URI 模板变量，也可以为 {@code null} 以清除此构建器的片段。
	 *
	 * @param fragment URI 片段
	 */
	UriBuilder fragment(@Nullable String fragment);

	/**
	 * 构建一个 {@link URI} 实例，并使用数组中的值替换 URI 模板变量。
	 *
	 * @param uriVariables URI 变量的数组
	 * @return URI 实例
	 */
	URI build(Object... uriVariables);

	/**
	 * 构建一个 {@link URI} 实例，并使用映射中的值替换 URI 模板变量。
	 *
	 * @param uriVariables URI 变量的映射
	 * @return URI 实例
	 */
	URI build(Map<String, ?> uriVariables);

}
