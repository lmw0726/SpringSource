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

package org.springframework.web.reactive.function.client;

import org.reactivestreams.Publisher;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.http.*;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ClientHttpRequest;
import org.springframework.http.client.reactive.ClientHttpResponse;
import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyExtractor;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.util.DefaultUriBuilderFactory;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriBuilderFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.net.URI;
import java.nio.charset.Charset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

/**
 * 非阻塞、响应式客户端，用于执行HTTP请求，通过底层HTTP客户端库（例如Reactor Netty）提供流畅、响应式的API。
 *
 * <p>使用静态工厂方法{@link #create()}、{@link #create(String)}或{@link WebClient#builder()}来准备一个实例。
 *
 * <p>有关带有响应体的示例，请参阅：
 * <ul>
 * <li>{@link RequestHeadersSpec#retrieve() retrieve()}
 * <li>{@link RequestHeadersSpec#exchangeToMono(Function) exchangeToMono()}
 * <li>{@link RequestHeadersSpec#exchangeToFlux(Function) exchangeToFlux()}
 * </ul>
 * <p>有关带有请求体的示例，请参阅：
 * <ul>
 * <li>{@link RequestBodySpec#bodyValue(Object) bodyValue(Object)}
 * <li>{@link RequestBodySpec#body(Publisher, Class) body(Publisher,Class)}
 * </ul>
 *
 * @author Rossen Stoyanchev
 * @author Arjen Poutsma
 * @author Sebastien Deleuze
 * @author Brian Clozel
 * @since 5.0
 */
public interface WebClient {

	/**
	 * 开始构建一个HTTP GET请求。
	 *
	 * @return 用于指定目标URL的规范
	 */
	RequestHeadersUriSpec<?> get();

	/**
	 * 开始构建一个HTTP HEAD请求。
	 *
	 * @return 用于指定目标URL的规范
	 */
	RequestHeadersUriSpec<?> head();

	/**
	 * 开始构建一个HTTP POST请求。
	 *
	 * @return 用于指定目标URL的规范
	 */
	RequestBodyUriSpec post();

	/**
	 * 开始构建一个HTTP PUT请求。
	 *
	 * @return 用于指定目标URL的规范
	 */
	RequestBodyUriSpec put();

	/**
	 * 开始构建一个HTTP PATCH请求。
	 *
	 * @return 用于指定目标URL的规范
	 */
	RequestBodyUriSpec patch();

	/**
	 * 开始构建一个HTTP DELETE请求。
	 *
	 * @return 用于指定目标URL的规范
	 */
	RequestHeadersUriSpec<?> delete();

	/**
	 * 开始构建一个HTTP OPTIONS请求。
	 *
	 * @return 用于指定目标URL的规范
	 */
	RequestHeadersUriSpec<?> options();

	/**
	 * 开始构建给定{@code HttpMethod}的请求。
	 *
	 * @return 用于指定目标URL的规范
	 */
	RequestBodyUriSpec method(HttpMethod method);


	/**
	 * 返回一个构建器以创建一个新的{@code WebClient}，其设置从当前{@code WebClient}复制。
	 */
	Builder mutate();


	// 静态，工厂方法

	/**
	 * 使用Reactor Netty默认创建一个新的{@code WebClient}。
	 *
	 * @see #create(String)
	 * @see #builder()
	 */
	static WebClient create() {
		return new DefaultWebClientBuilder().build();
	}

	/**
	 * {@link #create()}的变体，接受一个默认的基本URL。更多细节见{@link Builder#baseUrl(String) Builder.baseUrl(String)}。
	 *
	 * @param baseUrl 所有请求的基本URI
	 * @see #builder()
	 */
	static WebClient create(String baseUrl) {
		return new DefaultWebClientBuilder().baseUrl(baseUrl).build();
	}

	/**
	 * 获取一个{@code WebClient}构建器。
	 */
	static WebClient.Builder builder() {
		return new DefaultWebClientBuilder();
	}


	/**
	 * 用于创建{@link WebClient}的可变构建器。
	 */
	interface Builder {

		/**
		 * 配置请求的基本URL。实际上相当于：
		 * <pre class="code">
		 * String baseUrl = "https://abc.go.com/v1";
		 * DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory(baseUrl);
		 * WebClient client = WebClient.builder().uriBuilderFactory(factory).build();
		 * </pre>
		 * {@code DefaultUriBuilderFactory}用于准备每个请求的URL，
		 * 并使用给定的基本URL，除非给定URL请求为绝对URL，在这种情况下基本URL将被忽略。
		 * <p><strong>注意：</strong>此方法与{@link #uriBuilderFactory(UriBuilderFactory)}互斥。
		 * 如果两者都使用，则此处提供的baseUrl值将被忽略。
		 *
		 * @see DefaultUriBuilderFactory#DefaultUriBuilderFactory(String)
		 * @see #uriBuilderFactory(UriBuilderFactory)
		 */
		Builder baseUrl(String baseUrl);

		/**
		 * 配置在使用{@link Map}扩展URI模板时使用的默认URL变量值。实际上相当于：
		 * <pre class="code">
		 * Map&lt;String, ?&gt; defaultVars = ...;
		 * DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory();
		 * factory.setDefaultVariables(defaultVars);
		 * WebClient client = WebClient.builder().uriBuilderFactory(factory).build();
		 * </pre>
		 * <p><strong>注意：</strong>此方法与{@link #uriBuilderFactory(UriBuilderFactory)}互斥。
		 * 如果两者都使用，则此处提供的defaultUriVariables值将被忽略。
		 *
		 * @see DefaultUriBuilderFactory#setDefaultUriVariables(Map)
		 * @see #uriBuilderFactory(UriBuilderFactory)
		 */
		Builder defaultUriVariables(Map<String, ?> defaultUriVariables);

		/**
		 * 提供预配置的{@link UriBuilderFactory}实例。这是以下快捷属性的替代方式，并有效地覆盖了以下快捷属性：
		 * <ul>
		 * <li>{@link #baseUrl(String)}
		 * <li>{@link #defaultUriVariables(Map)}。
		 * </ul>
		 *
		 * @param uriBuilderFactory 要使用的URI构建器工厂
		 * @see #baseUrl(String)
		 * @see #defaultUriVariables(Map)
		 */
		Builder uriBuilderFactory(UriBuilderFactory uriBuilderFactory);

		/**
		 * 全局选项，指定要添加到每个请求的头信息，如果请求尚未包含此类头信息。
		 *
		 * @param header 头部名称
		 * @param values 头部值
		 */
		Builder defaultHeader(String header, String... values);

		/**
		 * 提供访问到目前为止声明的每个{@link #defaultHeader(String, String...)}，
		 * 并有可能添加、替换或移除。
		 *
		 * @param headersConsumer 消费者
		 */
		Builder defaultHeaders(Consumer<HttpHeaders> headersConsumer);

		/**
		 * 全局选项，指定要添加到每个请求的Cookie，如果请求尚未包含此类Cookie。
		 *
		 * @param cookie Cookie名称
		 * @param values Cookie值
		 */
		Builder defaultCookie(String cookie, String... values);

		/**
		 * 提供访问到目前为止声明的每个{@link #defaultCookie(String, String...)}，
		 * 并有可能添加、替换或移除。
		 *
		 * @param cookiesConsumer 消费者
		 */
		Builder defaultCookies(Consumer<MultiValueMap<String, String>> cookiesConsumer);

		/**
		 * 提供消费者以自定义正在构建的每个请求。
		 *
		 * @param defaultRequest 用于修改请求的消费者
		 * @since 5.1
		 */
		Builder defaultRequest(Consumer<RequestHeadersSpec<?>> defaultRequest);

		/**
		 * 将给定过滤器添加到过滤器链的末尾。
		 *
		 * @param filter 要添加到链中的过滤器
		 */
		Builder filter(ExchangeFilterFunction filter);

		/**
		 * 使用给定消费者操作过滤器。提供给消费者的列表是“活动”的，
		 * 因此消费者可以用于删除过滤器、更改排序等。
		 *
		 * @param filtersConsumer 消费过滤器列表的函数
		 * @return 此构建器
		 */
		Builder filters(Consumer<List<ExchangeFilterFunction>> filtersConsumer);

		/**
		 * 配置要使用的{@link ClientHttpConnector}。这对于插入和/或自定义底层HTTP客户端库（例如SSL）的选项非常有用。
		 * <p>默认情况下，此设置为{@link org.springframework.http.client.reactive.ReactorClientHttpConnector ReactorClientHttpConnector}。
		 *
		 * @param connector 要使用的连接器
		 */
		Builder clientConnector(ClientHttpConnector connector);

		/**
		 * 配置{@code WebClient}中的编解码器在{@link #exchangeStrategies(ExchangeStrategies)底层} {@code ExchangeStrategies}中使用。
		 *
		 * @param configurer 要应用的配置器
		 * @since 5.1.13
		 */
		Builder codecs(Consumer<ClientCodecConfigurer> configurer);

		/**
		 * 配置要使用的{@link ExchangeStrategies}。
		 * <p>对于大多数情况，请优先使用{@link #codecs(Consumer)}，
		 * 该方法允许在{@code ExchangeStrategies}中自定义编解码器，而不是替换它们。
		 * 这确保多方可以为编解码器配置做出贡献。
		 * <p>默认情况下，此设置为{@link ExchangeStrategies#withDefaults()}。
		 *
		 * @param strategies 要使用的策略
		 */
		Builder exchangeStrategies(ExchangeStrategies strategies);

		/**
		 * 自定义通过{@link #exchangeStrategies(ExchangeStrategies)}配置的策略。
		 * 此方法设计用于多方希望更新{@code ExchangeStrategies}的情况。
		 *
		 * @deprecated 自5.1.13起弃用，建议使用{@link #codecs(Consumer)}
		 */
		@Deprecated
		Builder exchangeStrategies(Consumer<ExchangeStrategies.Builder> configurer);

		/**
		 * 提供一个使用{@link ClientHttpConnector}和{@link ExchangeStrategies}预配置的{@link ExchangeFunction}。
		 * <p>这是{@link #clientConnector}和{@link #exchangeStrategies(ExchangeStrategies)}的替代方式，并有效地覆盖了它们。
		 *
		 * @param exchangeFunction 要使用的交换函数
		 */
		Builder exchangeFunction(ExchangeFunction exchangeFunction);

		/**
		 * 将给定的{@code Consumer}应用于此构建器实例。
		 * <p>这对于应用预打包的自定义很有用。
		 *
		 * @param builderConsumer 要应用的消费者
		 */
		Builder apply(Consumer<Builder> builderConsumer);

		/**
		 * 克隆此{@code WebClient.Builder}。
		 */
		Builder clone();

		/**
		 * 构建{@link WebClient}实例。
		 */
		WebClient build();
	}


	/**
	 * 用于指定请求的URI的合同。
	 *
	 * @param <S> 规范类型的自引用
	 */
	interface UriSpec<S extends RequestHeadersSpec<?>> {

		/**
		 * 使用绝对完整的{@link URI}指定URI。
		 */
		S uri(URI uri);

		/**
		 * 使用URI模板和URI变量指定请求的URI。
		 * 如果为客户端配置了{@link UriBuilderFactory}（例如，使用基本URI），则将使用它来展开URI模板。
		 */
		S uri(String uri, Object... uriVariables);

		/**
		 * 使用URI模板和URI变量指定请求的URI。
		 * 如果为客户端配置了{@link UriBuilderFactory}（例如，使用基本URI），则将使用它来展开URI模板。
		 */
		S uri(String uri, Map<String, ?> uriVariables);

		/**
		 * 通过URI模板开始，并通过从模板创建的{@link UriBuilder}结束指定URI。
		 *
		 * @since 5.2
		 */
		S uri(String uri, Function<UriBuilder, URI> uriFunction);

		/**
		 * 通过{@link UriBuilder}指定URI。
		 *
		 * @see #uri(String, Function)
		 */
		S uri(Function<UriBuilder, URI> uriFunction);
	}


	/**
	 * 用于在交换前指定请求头的合同。
	 *
	 * @param <S> 规范类型的自引用
	 */
	interface RequestHeadersSpec<S extends RequestHeadersSpec<S>> {

		/**
		 * 设置{@code Accept}标头指定的可接受{@linkplain MediaType 媒体类型}列表。
		 *
		 * @param acceptableMediaTypes 可接受的媒体类型
		 * @return 此构建器
		 */
		S accept(MediaType... acceptableMediaTypes);

		/**
		 * 设置{@code Accept-Charset}标头指定的可接受{@linkplain Charset 字符集}列表。
		 *
		 * @param acceptableCharsets 可接受的字符集
		 * @return 此构建器
		 */
		S acceptCharset(Charset... acceptableCharsets);

		/**
		 * 使用给定的名称和值添加一个Cookie。
		 *
		 * @param name  cookie的名称
		 * @param value cookie的值
		 * @return 此构建器
		 */
		S cookie(String name, String value);

		/**
		 * 提供对迄今为止声明的每个Cookie的访问，可以添加、替换或删除值。
		 *
		 * @param cookiesConsumer 提供访问权限的消费者
		 * @return 此构建器
		 */
		S cookies(Consumer<MultiValueMap<String, String>> cookiesConsumer);

		/**
		 * 设置{@code If-Modified-Since}标头的值。
		 * <p>日期应指定为自1970年1月1日GMT以来的毫秒数。
		 *
		 * @param ifModifiedSince 标头的新值
		 * @return 此构建器
		 */
		S ifModifiedSince(ZonedDateTime ifModifiedSince);

		/**
		 * 设置{@code If-None-Match}标头的值。
		 *
		 * @param ifNoneMatches 标头的新值
		 * @return 此构建器
		 */
		S ifNoneMatch(String... ifNoneMatches);

		/**
		 * 在给定名称下添加给定的单个标头值。
		 *
		 * @param headerName   标头名称
		 * @param headerValues 标头值
		 * @return 此构建器
		 */
		S header(String headerName, String... headerValues);

		/**
		 * 提供对迄今为止声明的每个标头的访问，可以添加、替换或删除值。
		 *
		 * @param headersConsumer 提供访问权限的消费者
		 * @return 此构建器
		 */
		S headers(Consumer<HttpHeaders> headersConsumer);

		/**
		 * 将给定名称的属性设置为给定值。
		 *
		 * @param name  要添加的属性名称
		 * @param value 要添加的属性值
		 * @return 此构建器
		 */
		S attribute(String name, Object value);

		/**
		 * 提供对迄今为止声明的每个属性的访问，可以添加、替换或删除值。
		 *
		 * @param attributesConsumer 提供访问权限的消费者
		 * @return 此构建器
		 */
		S attributes(Consumer<Map<String, Object>> attributesConsumer);

		/**
		 * 提供一个函数以填充Reactor {@code Context}。
		 *
		 * @param contextModifier 用于修改上下文的函数
		 * @since 5.3.1
		 * @deprecated 自5.3.2起将很快删除；此方法无法为下游（嵌套或后续）请求提供上下文，并且价值有限。
		 */
		@Deprecated
		S context(Function<Context, Context> contextModifier);

		/**
		 * 回调以访问{@link ClientHttpRequest}，从而访问底层HTTP库的本机请求。
		 * 这对于设置由底层库公开的高级每个请求选项可能很有用。
		 *
		 * @param requestConsumer 使用{@code ClientHttpRequest}访问的消费者
		 * @return 用于指定如何解码主体的{@code ResponseSpec}
		 * @since 5.3
		 */
		S httpRequest(Consumer<ClientHttpRequest> requestConsumer);

		/**
		 * 继续声明如何提取响应。例如，提取具有状态、标头和主体的{@link ResponseEntity}：
		 * <p><pre>
		 * Mono&lt;ResponseEntity&lt;Person&gt;&gt; entityMono = client.get()
		 *     .uri("/persons/1")
		 *     .accept(MediaType.APPLICATION_JSON)
		 *     .retrieve()
		 *     .toEntity(Person.class);
		 * </pre>
		 * <p>或者，如果仅对主体感兴趣：
		 * <p><pre>
		 * Mono&lt;Person&gt; entityMono = client.get()
		 *     .uri("/persons/1")
		 *     .accept(MediaType.APPLICATION_JSON)
		 *     .retrieve()
		 *     .bodyToMono(Person.class);
		 * </pre>
		 * <p>默认情况下，4xx和5xx响应将导致{@link WebClientResponseException}。要自定义错误处理，请使用{@link ResponseSpec#onStatus(Predicate, Function) onStatus}处理程序。
		 */
		ResponseSpec retrieve();

		/**
		 * {@link #retrieve()}的替代方法，通过访问{@link ClientResponse}提供更多控制。
		 * 这可以对于高级场景很有用，例如根据响应状态不同地解码响应：
		 * <p><pre>
		 * Mono&lt;Person&gt; entityMono = client.get()
		 *     .uri("/persons/1")
		 *     .accept(MediaType.APPLICATION_JSON)
		 *     .exchangeToMono(response -&gt; {
		 *         if (response.statusCode().equals(HttpStatus.OK)) {
		 *             return response.bodyToMono(Person.class);
		 *         }
		 *         else {
		 *             return response.createException().flatMap(Mono::error);
		 *         }
		 *     });
		 * </pre>
		 * <p><strong>注意：</strong>在返回的{@code Mono}完成后，如果尚未使用响应体，则响应体将自动释放。
		 * 如果需要响应内容，则所提供的函数必须声明如何解码它。
		 *
		 * @param responseHandler 处理响应的函数
		 * @param <V>             响应将转换为的对象类型
		 * @return 从响应生成的{@code Mono}
		 * @since 5.3
		 */
		<V> Mono<V> exchangeToMono(Function<ClientResponse, ? extends Mono<V>> responseHandler);

		/**
		 * {@link #retrieve()}的替代方法，通过访问{@link ClientResponse}提供更多控制。
		 * 这可以对于高级场景很有用，例如根据响应状态不同地解码响应：
		 * <p><pre>
		 * Flux&lt;Person&gt; entityMono = client.get()
		 *     .uri("/persons")
		 *     .accept(MediaType.APPLICATION_JSON)
		 *     .exchangeToFlux(response -&gt; {
		 *         if (response.statusCode().equals(HttpStatus.OK)) {
		 *             return response.bodyToFlux(Person.class);
		 *         }
		 *         else {
		 *             return response.createException().flatMapMany(Mono::error);
		 *         }
		 *     });
		 * </pre>
		 * <p><strong>注意：</strong>在返回的{@code Flux}完成后，如果尚未使用响应体，则响应体将自动释放。
		 * 如果需要响应内容，则所提供的函数必须声明如何解码它。
		 *
		 * @param responseHandler 处理响应的函数
		 * @param <V>             响应将转换为的对象类型
		 * @return 从响应生成的{@code Flux}对象
		 * @since 5.3
		 */
		<V> Flux<V> exchangeToFlux(Function<ClientResponse, ? extends Flux<V>> responseHandler);

		/**
		 * 执行HTTP请求并返回带有响应状态和标头的{@link ClientResponse}。然后可以使用响应的方法消费主体：
		 * <p><pre>
		 * Mono&lt;Person&gt; mono = client.get()
		 *     .uri("/persons/1")
		 *     .accept(MediaType.APPLICATION_JSON)
		 *     .exchange()
		 *     .flatMap(response -&gt; response.bodyToMono(Person.class));
		 *
		 * Flux&lt;Person&gt; flux = client.get()
		 *     .uri("/persons")
		 *     .accept(MediaType.APPLICATION_STREAM_JSON)
		 *     .exchange()
		 *     .flatMapMany(response -&gt; response.bodyToFlux(Person.class));
		 * </pre>
		 * <p><strong>注意：</strong>与{@link #retrieve()}不同，使用{@code exchange()}时，
		 * 应用程序有责任消耗任何响应内容，无论场景如何（成功、错误、意外数据等）。
		 * 不这样做可能会导致内存泄漏。请参阅{@link ClientResponse}以获取所有可用于消耗主体的选项列表。
		 * 通常优先使用{@link #retrieve()}，除非有充分理由使用{@code exchange()}，
		 * 后者允许在决定如何或是否消耗响应之前检查响应状态和标头。
		 *
		 * @return 响应的{@code Mono}
		 * @see #retrieve()
		 * @deprecated 自5.3起由于可能会泄露内存和/或连接的可能性；请使用{@link #exchangeToMono(Function)}，
		 * {@link #exchangeToFlux(Function)}；还考虑使用{@link #retrieve()}，
		 * 它通过{@link ResponseEntity}提供了对响应状态和标头的访问以及错误状态处理。
		 */
		@Deprecated
		Mono<ClientResponse> exchange();
	}


	/**
	 * 用于指定引导到交换的请求头和主体的合同。
	 */
	interface RequestBodySpec extends RequestHeadersSpec<RequestBodySpec> {

		/**
		 * 设置主体的字节长度，由{@code Content-Length}标头指定。
		 *
		 * @param contentLength 内容长度
		 * @return 此构建器
		 * @see HttpHeaders#setContentLength(long)
		 */
		RequestBodySpec contentLength(long contentLength);

		/**
		 * 设置主体的{@linkplain MediaType 媒体类型}，由{@code Content-Type}标头指定。
		 *
		 * @param contentType 内容类型
		 * @return 此构建器
		 * @see HttpHeaders#setContentType(MediaType)
		 */
		RequestBodySpec contentType(MediaType contentType);

		/**
		 * 使用{@linkplain BodyInserters#fromValue 值插入器}的快捷方式设置请求主体。
		 * 例如：
		 * <pre class="code">
		 * Person person = ... ;
		 *
		 * Mono&lt;Void&gt; result = client.post()
		 *     .uri("/persons/{id}", id)
		 *     .contentType(MediaType.APPLICATION_JSON)
		 *     .bodyValue(person)
		 *     .retrieve()
		 *     .bodyToMono(Void.class);
		 * </pre>
		 *
		 * @param body 写入请求主体的值
		 * @return 此构建器
		 * @throws IllegalArgumentException 如果{@code body}是已知的{@link Publisher}或生产者{@link ReactiveAdapterRegistry}
		 * @since 5.2
		 */
		RequestHeadersSpec<?> bodyValue(Object body);

		/**
		 * 使用{@linkplain BodyInserters#fromPublisher 发布者插入器}的快捷方式设置请求主体。
		 * 例如：
		 * <pre>
		 * Mono&lt;Person&gt; personMono = ... ;
		 *
		 * Mono&lt;Void&gt; result = client.post()
		 *     .uri("/persons/{id}", id)
		 *     .contentType(MediaType.APPLICATION_JSON)
		 *     .body(personMono, Person.class)
		 *     .retrieve()
		 *     .bodyToMono(Void.class);
		 * </pre>
		 *
		 * @param publisher    要写入请求的{@code Publisher}
		 * @param elementClass 发布的元素类型
		 * @param <T>          发布的元素类型
		 * @param <P>          {@code Publisher}的类型
		 * @return 此构建器
		 */
		<T, P extends Publisher<T>> RequestHeadersSpec<?> body(P publisher, Class<T> elementClass);

		/**
		 * 允许提供泛型信息的{@link #body(Publisher, Class)}的变体。
		 *
		 * @param publisher      要写入请求的{@code Publisher}
		 * @param elementTypeRef 发布的元素类型
		 * @param <T>            发布的元素类型
		 * @param <P>            {@code Publisher}的类型
		 * @return 此构建器
		 */
		<T, P extends Publisher<T>> RequestHeadersSpec<?> body(P publisher, ParameterizedTypeReference<T> elementTypeRef);

		/**
		 * 允许使用可以通过{@link ReactiveAdapterRegistry}解析为{@link Publisher}的任何生产者的{@link #body(Publisher, Class)}的变体。
		 *
		 * @param producer     要写入请求的生产者
		 * @param elementClass 生成的元素的类型
		 * @return 此构建器
		 * @since 5.2
		 */
		RequestHeadersSpec<?> body(Object producer, Class<?> elementClass);

		/**
		 * 允许使用可以通过{@link ReactiveAdapterRegistry}解析为{@link Publisher}的任何生产者的{@link #body(Publisher, ParameterizedTypeReference)}的变体。
		 *
		 * @param producer       要写入请求的生产者
		 * @param elementTypeRef 生成的元素的类型
		 * @return 此构建器
		 * @since 5.2
		 */
		RequestHeadersSpec<?> body(Object producer, ParameterizedTypeReference<?> elementTypeRef);

		/**
		 * 使用给定的主体插入器设置请求的主体。
		 * 请参见{@link BodyInserters}了解内置的{@link BodyInserter}实现。
		 *
		 * @param inserter 用于请求主体的主体插入器
		 * @return 此构建器
		 * @see org.springframework.web.reactive.function.BodyInserters
		 */
		RequestHeadersSpec<?> body(BodyInserter<?, ? super ClientHttpRequest> inserter);

		/**
		 * 使用{@linkplain BodyInserters#fromValue 值插入器}的快捷方式设置请求主体。
		 * 自5.2起，此方法委托给{@link #bodyValue(Object)}。
		 *
		 * @deprecated 自Spring Framework 5.2起，建议使用{@link #bodyValue(Object)}
		 */
		@Deprecated
		RequestHeadersSpec<?> syncBody(Object body);
	}


	/**
	 * 用于在交换之后指定响应操作的合同。
	 */
	interface ResponseSpec {

		/**
		 * 提供一个函数，将特定的错误状态代码映射到要向下游传播的错误信号，而不是响应。
		 * <p>默认情况下，如果没有匹配的状态处理程序，则状态码 &gt;= 400 的响应被映射为
		 * {@link WebClientResponseException}，该异常是通过 {@link ClientResponse#createException()} 创建的。
		 * <p>要将状态代码视为错误并将其作为正常响应处理，请从函数返回 {@code Mono.empty()}。
		 * 然后响应将向下游传播以进行处理。
		 * <p>要完全忽略错误响应，并且既不传播响应也不传播错误，请使用 {@link ExchangeFilterFunction filter}，
		 * 或在下游添加 {@code onErrorResume}，例如：
		 * <pre class="code">
		 * webClient.get()
		 *     .uri("https://abc.com/account/123")
		 *     .retrieve()
		 *     .bodyToMono(Account.class)
		 *     .onErrorResume(WebClientResponseException.class,
		 *          ex -&gt; ex.getRawStatusCode() == 404 ? Mono.empty() : Mono.error(ex));
		 * </pre>
		 *
		 * @param statusPredicate   要匹配响应的谓词
		 * @param exceptionFunction 将响应映射到错误信号的函数
		 * @return 此构建器
		 * @see ClientResponse#createException()
		 */
		ResponseSpec onStatus(Predicate<HttpStatus> statusPredicate,
							  Function<ClientResponse, Mono<? extends Throwable>> exceptionFunction);

		/**
		 * {@link #onStatus(Predicate, Function)} 的变体，适用于原始状态代码值。这对于自定义状态代码很有用。
		 *
		 * @param statusCodePredicate 要匹配响应的状态码谓词
		 * @param exceptionFunction   将响应映射到错误信号的函数
		 * @return 此构建器
		 * @since 5.1.9
		 */
		ResponseSpec onRawStatus(IntPredicate statusCodePredicate,
								 Function<ClientResponse, Mono<? extends Throwable>> exceptionFunction);

		/**
		 * 将响应体解码为给定目标类型。对于错误响应（状态码为 4xx 或 5xx），{@code Mono} 发出 {@link WebClientException}。
		 * 使用 {@link #onStatus(Predicate, Function)} 自定义错误响应处理。
		 *
		 * @param elementClass 要解码为的类型
		 * @param <T>          目标体类型
		 * @return 解码后的响应体
		 */
		<T> Mono<T> bodyToMono(Class<T> elementClass);

		/**
		 * {@link #bodyToMono(Class)} 的变体，使用 {@link ParameterizedTypeReference}。
		 *
		 * @param elementTypeRef 要解码为的类型
		 * @param <T>            目标体类型
		 * @return 解码后的响应体
		 */
		<T> Mono<T> bodyToMono(ParameterizedTypeReference<T> elementTypeRef);

		/**
		 * 将响应体解码为给定类型的 {@link Flux}。对于错误响应（状态码为 4xx 或 5xx），{@code Mono} 发出 {@link WebClientException}。
		 * 使用 {@link #onStatus(Predicate, Function)} 自定义错误响应处理。
		 *
		 * @param elementClass 要解码为的元素类型
		 * @param <T>          体元素类型
		 * @return 解码后的响应体
		 */
		<T> Flux<T> bodyToFlux(Class<T> elementClass);

		/**
		 * {@link #bodyToMono(Class)} 的变体，使用 {@link ParameterizedTypeReference}。
		 *
		 * @param elementTypeRef 要解码为的元素类型
		 * @param <T>            体元素类型
		 * @return 解码后的响应体
		 */
		<T> Flux<T> bodyToFlux(ParameterizedTypeReference<T> elementTypeRef);

		/**
		 * 返回一个 {@code ResponseEntity}，其中的主体已解码为给定类型的对象。对于错误响应（状态码为 4xx 或 5xx），
		 * {@code Mono} 发出 {@link WebClientException}。使用 {@link #onStatus(Predicate, Function)} 自定义错误响应处理。
		 *
		 * @param bodyClass 期望的响应主体类型
		 * @param <T>       响应主体类型
		 * @return {@code ResponseEntity} 与已解码主体
		 * @since 5.2
		 */
		<T> Mono<ResponseEntity<T>> toEntity(Class<T> bodyClass);

		/**
		 * {@link #bodyToMono(Class)} 的变体，使用 {@link ParameterizedTypeReference}。
		 *
		 * @param bodyTypeReference 期望的响应主体类型
		 * @param <T>               响应主体类型
		 * @return {@code ResponseEntity} 与已解码主体
		 * @since 5.2
		 */
		<T> Mono<ResponseEntity<T>> toEntity(ParameterizedTypeReference<T> bodyTypeReference);

		/**
		 * 返回一个 {@code ResponseEntity}，其中的主体已解码为给定类型的 {@code List}。对于错误响应（状态码为 4xx 或 5xx），
		 * {@code Mono} 发出 {@link WebClientException}。使用 {@link #onStatus(Predicate, Function)} 自定义错误响应处理。
		 *
		 * @param elementClass 要解码目标 Flux 的元素类型
		 * @param <T>          体元素类型
		 * @return {@code ResponseEntity}
		 * @since 5.2
		 */
		<T> Mono<ResponseEntity<List<T>>> toEntityList(Class<T> elementClass);

		/**
		 * {@link #toEntity(Class)} 的变体，使用 {@link ParameterizedTypeReference}。
		 *
		 * @param elementTypeRef 要解码目标 Flux 的元素类型
		 * @param <T>            体元素类型
		 * @return {@code ResponseEntity}
		 * @since 5.2
		 */
		<T> Mono<ResponseEntity<List<T>>> toEntityList(ParameterizedTypeReference<T> elementTypeRef);

		/**
		 * 返回一个 {@code ResponseEntity}，其中的主体已解码为给定类型的 {@code Flux} 元素。对于错误响应（状态码为 4xx 或 5xx），
		 * {@code Mono} 发出 {@link WebClientException}。使用 {@link #onStatus(Predicate, Function)} 自定义错误响应处理。
		 * <p><strong>注意：</strong>代表主体的 {@code Flux} 必须被订阅，否则相关资源将不会被释放。
		 *
		 * @param elementType 要解码目标 Flux 的元素类型
		 * @param <T>         体元素类型
		 * @return {@code ResponseEntity}
		 * @since 5.3.1
		 */
		<T> Mono<ResponseEntity<Flux<T>>> toEntityFlux(Class<T> elementType);

		/**
		 * {@link #toEntityFlux(Class)} 的变体，使用 {@link ParameterizedTypeReference}。
		 *
		 * @param elementTypeReference 要解码目标 Flux 的元素类型
		 * @param <T>                  体元素类型
		 * @return {@code ResponseEntity}
		 * @since 5.3.1
		 */
		<T> Mono<ResponseEntity<Flux<T>>> toEntityFlux(ParameterizedTypeReference<T> elementTypeReference);

		/**
		 * {@link #toEntityFlux(Class)} 的变体，使用 {@link BodyExtractor}。
		 *
		 * @param bodyExtractor 从响应中读取的 {@code BodyExtractor}
		 * @param <T>           体元素类型
		 * @return {@code ResponseEntity}
		 * @since 5.3.2
		 */
		<T> Mono<ResponseEntity<Flux<T>>> toEntityFlux(BodyExtractor<Flux<T>, ? super ClientHttpResponse> bodyExtractor);

		/**
		 * 返回一个没有主体的 {@code ResponseEntity}。对于错误响应（状态码为 4xx 或 5xx），
		 * {@code Mono} 发出 {@link WebClientException}。使用 {@link #onStatus(Predicate, Function)}
		 * 自定义错误响应处理。
		 *
		 * @return {@code ResponseEntity}
		 * @since 5.2
		 */
		Mono<ResponseEntity<Void>> toBodilessEntity();
	}


	/**
	 * 用于指定请求的请求头和 URI 的合同。
	 *
	 * @param <S> 指定规范类型的自引用
	 */
	interface RequestHeadersUriSpec<S extends RequestHeadersSpec<S>>
			extends UriSpec<S>, RequestHeadersSpec<S> {
	}


	/**
	 * 用于指定请求的请求头、主体和 URI 的合同。
	 */
	interface RequestBodyUriSpec extends RequestBodySpec, RequestHeadersUriSpec<RequestBodySpec> {
	}


}
