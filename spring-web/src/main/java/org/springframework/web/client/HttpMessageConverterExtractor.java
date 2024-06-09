/*
 * Copyright 2002-2020 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;

/**
 * 使用给定的{@linkplain HttpMessageConverter 实体转换器}将响应转换为类型{@code T}的响应提取器。
 *
 * @param <T> 数据类型
 * @author Arjen Poutsma
 * @author Sam Brannen
 * @see RestTemplate
 * @since 3.0
 */
public class HttpMessageConverterExtractor<T> implements ResponseExtractor<T> {
	/**
	 * 响应类型
	 */
	private final Type responseType;

	/**
	 * 响应类
	 */
	@Nullable
	private final Class<T> responseClass;

	/**
	 * 消息转换器列表
	 */
	private final List<HttpMessageConverter<?>> messageConverters;

	/**
	 * 日志记录器
	 */
	private final Log logger;


	/**
	 * 使用给定的响应类型和消息转换器创建{@code HttpMessageConverterExtractor}的新实例。给定的转换器必须支持响应类型。
	 */
	public HttpMessageConverterExtractor(Class<T> responseType, List<HttpMessageConverter<?>> messageConverters) {
		this((Type) responseType, messageConverters);
	}

	/**
	 * 使用给定的响应类型和消息转换器创建{@code HttpMessageConverterExtractor}的新实例。给定的转换器必须支持响应类型。
	 */
	public HttpMessageConverterExtractor(Type responseType, List<HttpMessageConverter<?>> messageConverters) {
		this(responseType, messageConverters, LogFactory.getLog(HttpMessageConverterExtractor.class));
	}

	@SuppressWarnings("unchecked")
	HttpMessageConverterExtractor(Type responseType, List<HttpMessageConverter<?>> messageConverters, Log logger) {
		Assert.notNull(responseType, "'responseType' must not be null");
		Assert.notEmpty(messageConverters, "'messageConverters' must not be empty");
		Assert.noNullElements(messageConverters, "'messageConverters' must not contain null elements");
		this.responseType = responseType;
		this.responseClass = (responseType instanceof Class ? (Class<T>) responseType : null);
		this.messageConverters = messageConverters;
		this.logger = logger;
	}


	@Override
	@SuppressWarnings({"unchecked", "rawtypes", "resource"})
	public T extractData(ClientHttpResponse response) throws IOException {
		// 包装响应
		MessageBodyClientHttpResponseWrapper responseWrapper = new MessageBodyClientHttpResponseWrapper(response);
		// 如果响应没有消息体或消息体为空，则返回空
		if (!responseWrapper.hasMessageBody() || responseWrapper.hasEmptyMessageBody()) {
			return null;
		}
		// 获取响应内容类型
		MediaType contentType = getContentType(responseWrapper);

		try {
			// 遍历消息转换器
			for (HttpMessageConverter<?> messageConverter : this.messageConverters) {
				if (messageConverter instanceof GenericHttpMessageConverter) {
					// 如果消息转换器是 通用Http消息转换器 类型
					GenericHttpMessageConverter<?> genericMessageConverter = (GenericHttpMessageConverter<?>) messageConverter;
					// 如果消息转换器能够读取指定类型的响应
					if (genericMessageConverter.canRead(this.responseType, null, contentType)) {
						if (logger.isDebugEnabled()) {
							ResolvableType resolvableType = ResolvableType.forType(this.responseType);
							logger.debug("Reading to [" + resolvableType + "]");
						}
						// 读取响应并返回
						return (T) genericMessageConverter.read(this.responseType, null, responseWrapper);
					}
				}
				if (this.responseClass != null) {
					// 如果消息转换器能够读取指定类的响应
					if (messageConverter.canRead(this.responseClass, contentType)) {
						if (logger.isDebugEnabled()) {
							String className = this.responseClass.getName();
							logger.debug("Reading to [" + className + "] as \"" + contentType + "\"");
						}
						// 读取响应并返回
						return (T) messageConverter.read((Class) this.responseClass, responseWrapper);
					}
				}
			}
		} catch (IOException | HttpMessageNotReadableException ex) {
			// 处理异常
			throw new RestClientException("Error while extracting response for type [" +
					this.responseType + "] and content type [" + contentType + "]", ex);
		}

		// 如果没有找到适合的消息转换器，则抛出未知内容类型异常
		throw new UnknownContentTypeException(this.responseType, contentType,
				responseWrapper.getRawStatusCode(), responseWrapper.getStatusText(),
				responseWrapper.getHeaders(), getResponseBody(responseWrapper));
	}

	/**
	 * 根据“Content-Type”头确定响应的内容类型，否则默认为{@link MediaType#APPLICATION_OCTET_STREAM}。
	 *
	 * @param response 响应
	 * @return 媒体类型，或者“application/octet-stream”
	 */
	protected MediaType getContentType(ClientHttpResponse response) {
		// 获取响应的内容类型
		MediaType contentType = response.getHeaders().getContentType();
		if (contentType == null) {
			// 如果没有指定内容类型
			if (logger.isTraceEnabled()) {
				logger.trace("No content-type, using 'application/octet-stream'");
			}
			// 使用默认的 'application/octet-stream'
			contentType = MediaType.APPLICATION_OCTET_STREAM;
		}
		return contentType;
	}

	private static byte[] getResponseBody(ClientHttpResponse response) {
		try {
			// 从响应体中读取数据并拷贝到字节数组中返回
			return FileCopyUtils.copyToByteArray(response.getBody());
		} catch (IOException ex) {
			// 发生 I/O 异常时，忽略异常
		}

		// 如果发生异常或者无法读取数据，则返回空字节数组
		return new byte[0];
	}
}
