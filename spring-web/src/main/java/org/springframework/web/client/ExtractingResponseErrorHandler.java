/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.client;

import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link ResponseErrorHandler} 的实现，使用 {@link HttpMessageConverter HttpMessageConverters} 将 HTTP 错误响应转换为 {@link RestClientException RestClientExceptions}。
 *
 * <p>要使用此错误处理程序，必须指定 {@linkplain #setStatusMapping(Map) status mapping} 和/或 {@linkplain #setSeriesMapping(Map) series mapping}。
 * 如果这些映射中的任何一个与给定 {@code ClientHttpResponse} 的 {@linkplain ClientHttpResponse#getStatusCode() status code} 匹配，
 * {@link #hasError(ClientHttpResponse)} 将返回 {@code true}，并且 {@link #handleError(ClientHttpResponse)} 将尝试使用 {@linkplain #setMessageConverters(List) 配置的消息转换器}
 * 将响应转换为 {@link RestClientException} 的映射子类。请注意，{@linkplain #setStatusMapping(Map) status mapping} 优先于 {@linkplain #setSeriesMapping(Map) series mapping}。
 *
 * <p>如果没有匹配项，此错误处理程序将默认为 {@link DefaultResponseErrorHandler} 的行为。
 * 请注意，您可以通过将 {@linkplain #setSeriesMapping(Map) series mapping} 从 {@code HttpStatus.Series#CLIENT_ERROR} 和/或 {@code HttpStatus.Series#SERVER_ERROR}
 * 到 {@code null} 的映射来覆盖此默认行为。
 *
 * @author Simon Galperin
 * @author Arjen Poutsma
 * @see RestTemplate#setErrorHandler(ResponseErrorHandler)
 * @since 5.0
 */
public class ExtractingResponseErrorHandler extends DefaultResponseErrorHandler {

	/**
	 * 消息转换器列表。
	 */
	private List<HttpMessageConverter<?>> messageConverters = Collections.emptyList();

	/**
	 * 状态码映射，键为 HTTP状态码 ，值为 RestClientException 子类。
	 */
	private final Map<HttpStatus, Class<? extends RestClientException>> statusMapping = new LinkedHashMap<>();

	/**
	 * 状态码系列映射，键为 HTTP状态码系列，值为 RestClientException 子类。
	 */
	private final Map<HttpStatus.Series, Class<? extends RestClientException>> seriesMapping = new LinkedHashMap<>();


	/**
	 * 使用给定的 {@link HttpMessageConverter} 实例创建一个新的、空的 {@code ExtractingResponseErrorHandler}。
	 * <p>注意，当使用此构造函数时，必须调用 {@link #setMessageConverters(List)}。
	 */
	public ExtractingResponseErrorHandler() {
	}

	/**
	 * 使用给定的 {@link HttpMessageConverter} 实例创建一个新的 {@code ExtractingResponseErrorHandler}。
	 *
	 * @param messageConverters 要使用的消息转换器
	 */
	public ExtractingResponseErrorHandler(List<HttpMessageConverter<?>> messageConverters) {
		this.messageConverters = messageConverters;
	}


	/**
	 * 设置此提取器要使用的消息转换器。
	 */
	public void setMessageConverters(List<HttpMessageConverter<?>> messageConverters) {
		this.messageConverters = messageConverters;
	}

	/**
	 * 将 HTTP 状态码映射设置为 {@code RestClientException} 子类。
	 * 如果此映射中有一个匹配项与给定 {@code ClientHttpResponse} 的 {@linkplain ClientHttpResponse#getStatusCode() status code} 匹配，
	 * {@link #hasError(ClientHttpResponse)} 将返回 {@code true}，并且 {@link #handleError(ClientHttpResponse)} 将尝试使用
	 * {@linkplain #setMessageConverters(List) 配置的消息转换器} 将响应转换为 {@link RestClientException} 的映射子类。
	 */
	public void setStatusMapping(Map<HttpStatus, Class<? extends RestClientException>> statusMapping) {
		if (!CollectionUtils.isEmpty(statusMapping)) {
			// 如果状态映射不为空，则将新的状态映射添加到现有状态映射中
			this.statusMapping.putAll(statusMapping);
		}
	}

	/**
	 * 将 HTTP 状态系列映射设置为 {@code RestClientException} 子类。
	 * 如果此映射中有一个匹配项与给定 {@code ClientHttpResponse} 的 {@linkplain ClientHttpResponse#getStatusCode() status code} 匹配，
	 * {@link #hasError(ClientHttpResponse)} 将返回 {@code true}，并且 {@link #handleError(ClientHttpResponse)} 将尝试使用
	 * {@linkplain #setMessageConverters(List) 配置的消息转换器} 将响应转换为 {@link RestClientException} 的映射子类。
	 */
	public void setSeriesMapping(Map<HttpStatus.Series, Class<? extends RestClientException>> seriesMapping) {
		if (!CollectionUtils.isEmpty(seriesMapping)) {
			// 如果系列映射不为空，则将新的系列映射添加到现有系列映射中
			this.seriesMapping.putAll(seriesMapping);
		}
	}


	@Override
	protected boolean hasError(HttpStatus statusCode) {
		// 如果状态码存在于 状态映射 映射中
		if (this.statusMapping.containsKey(statusCode)) {
			// 检查映射值是否为非空
			return this.statusMapping.get(statusCode) != null;
		} else if (this.seriesMapping.containsKey(statusCode.series())) {
			// 如果状态码存在于 状态系列映射中
			// 检查映射值是否为非空
			return this.seriesMapping.get(statusCode.series()) != null;
		} else {
			// 否则调用超类的方法来判断是否为错误状态码
			return super.hasError(statusCode);
		}
	}

	@Override
	public void handleError(ClientHttpResponse response, HttpStatus statusCode) throws IOException {
		// 如果状态码存在于 状态映射中
		if (this.statusMapping.containsKey(statusCode)) {
			// 从映射中提取与状态码对应的值，并处理响应
			extract(this.statusMapping.get(statusCode), response);
		} else if (this.seriesMapping.containsKey(statusCode.series())) {
			// 如果状态码存在于 状态系列映射中
			// 从 状态系列中提取与状态码对应系列的值，并处理响应
			extract(this.seriesMapping.get(statusCode.series()), response);
		} else {
			// 否则调用超类的方法处理响应
			super.handleError(response, statusCode);
		}
	}

	private void extract(@Nullable Class<? extends RestClientException> exceptionClass,
						 ClientHttpResponse response) throws IOException {

		// 如果异常类为 null，则不执行任何操作，直接返回
		if (exceptionClass == null) {
			return;
		}

		// 创建一个 Http消息转换器提取器 对象
		HttpMessageConverterExtractor<? extends RestClientException> extractor =
				new HttpMessageConverterExtractor<>(exceptionClass, this.messageConverters);
		// 使用 提取器 从响应中提取异常信息
		RestClientException exception = extractor.extractData(response);
		// 如果成功提取到异常信息
		if (exception != null) {
			// 抛出提取到的异常
			throw exception;
		}
	}

}
