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

package org.springframework.http.converter;

import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * 实现了 {@code HttpMessageConverter} 接口，使用 {@link StringHttpMessageConverter} 用于读取和写入内容，
 * 并使用 {@link ConversionService} 将字符串内容转换为目标对象类型及其反向转换。
 *
 * <p>默认情况下，此转换器仅支持媒体类型 {@code text/plain}。可以通过 {@link #setSupportedMediaTypes supportedMediaTypes} 属性进行覆盖。
 *
 * <p>使用示例:
 *
 * <pre class="code">
 * &lt;bean class="org.springframework.http.converter.ObjectToStringHttpMessageConverter"&gt;
 *   &lt;constructor-arg&gt;
 *     &lt;bean class="org.springframework.context.support.ConversionServiceFactoryBean"/&gt;
 *   &lt;/constructor-arg&gt;
 * &lt;/bean&gt;
 * </pre>
 *
 * @author <a href="mailto:dmitry.katsubo@gmail.com">Dmitry Katsubo</a>
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public class ObjectToStringHttpMessageConverter extends AbstractHttpMessageConverter<Object> {
	/**
	 * 转换服务
	 */
	private final ConversionService conversionService;

	/**
	 * 字符串Http消息转换器
	 */
	private final StringHttpMessageConverter stringHttpMessageConverter;


	/**
	 * 构造函数，接受一个 {@code ConversionService} 用于将（String）消息主体转换为目标类类型，并使用 {@link StringHttpMessageConverter#DEFAULT_CHARSET} 作为默认字符集。
	 *
	 * @param conversionService 转换服务
	 */
	public ObjectToStringHttpMessageConverter(ConversionService conversionService) {
		this(conversionService, StringHttpMessageConverter.DEFAULT_CHARSET);
	}

	/**
	 * 构造函数，接受一个 {@code ConversionService} 和一个默认字符集。
	 *
	 * @param conversionService 转换服务
	 * @param defaultCharset    默认字符集
	 */
	public ObjectToStringHttpMessageConverter(ConversionService conversionService, Charset defaultCharset) {
		super(defaultCharset, MediaType.TEXT_PLAIN);

		Assert.notNull(conversionService, "ConversionService is required");
		this.conversionService = conversionService;
		this.stringHttpMessageConverter = new StringHttpMessageConverter(defaultCharset);
	}


	/**
	 * 委托给 {@link StringHttpMessageConverter#setWriteAcceptCharset(boolean)} 方法。
	 */
	public void setWriteAcceptCharset(boolean writeAcceptCharset) {
		this.stringHttpMessageConverter.setWriteAcceptCharset(writeAcceptCharset);
	}


	@Override
	public boolean canRead(Class<?> clazz, @Nullable MediaType mediaType) {
		return canRead(mediaType) && this.conversionService.canConvert(String.class, clazz);
	}

	@Override
	public boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType) {
		return canWrite(mediaType) && this.conversionService.canConvert(clazz, String.class);
	}

	@Override
	protected boolean supports(Class<?> clazz) {
		// 不应该被调用，因为我们重写了canRead/Write
		throw new UnsupportedOperationException();
	}

	@Override
	protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		// 使用 字符串Http消息转换器 读取输入消息并转换为 String 类型的 值
		String value = this.stringHttpMessageConverter.readInternal(String.class, inputMessage);
		// 使用 转换服务 将 值 转换为 对应类型的 结果
		Object result = this.conversionService.convert(value, clazz);
		// 如果转换结果为 null
		if (result == null) {
			// 抛出消息不可读异常，指示意外的 null 转换结果
			throw new HttpMessageNotReadableException(
					"Unexpected null conversion result for '" + value + "' to " + clazz,
					inputMessage);
		}
		// 返回转换后的结果
		return result;
	}

	@Override
	protected void writeInternal(Object obj, HttpOutputMessage outputMessage) throws IOException {
		// 使用 转换服务 将 对象 转换为 String 类型的 值
		String value = this.conversionService.convert(obj, String.class);
		// 如果 值 不为 null
		if (value != null) {
			// 使用 字符串Http消息转换器 将 值 写入输出消息
			this.stringHttpMessageConverter.writeInternal(value, outputMessage);
		}
	}

	@Override
	protected Long getContentLength(Object obj, @Nullable MediaType contentType) {
		// 使用 转换服务 将 对象 转换为 String 类型的 值
		String value = this.conversionService.convert(obj, String.class);
		// 如果 值 为 null
		if (value == null) {
			// 返回 0L
			return 0L;
		}
		// 否则，使用 字符串Http消息转换器 计算 值 的内容长度
		return this.stringHttpMessageConverter.getContentLength(value, contentType);
	}

}
