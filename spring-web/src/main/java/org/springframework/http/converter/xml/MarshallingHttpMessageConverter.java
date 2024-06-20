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

package org.springframework.http.converter.xml;

import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;
import org.springframework.util.Assert;

import javax.xml.transform.Result;
import javax.xml.transform.Source;

/**
 * 实现了 {@link org.springframework.http.converter.HttpMessageConverter HttpMessageConverter}
 * 接口，可以使用 Spring 的 {@link Marshaller} 和 {@link Unmarshaller} 抽象来读取和写入 XML。
 *
 * <p>在使用之前，此转换器需要设置 {@code Marshaller} 和 {@code Unmarshaller}。可以通过
 * {@linkplain #MarshallingHttpMessageConverter(Marshaller) 构造函数}或者
 * {@linkplain #setMarshaller(Marshaller) bean 属性}进行注入。
 *
 * <p>默认情况下，此转换器支持 {@code text/xml} 和 {@code application/xml}。可以通过设置
 * {@link #setSupportedMediaTypes(java.util.List) supportedMediaTypes} 属性进行覆盖。
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
public class MarshallingHttpMessageConverter extends AbstractXmlHttpMessageConverter<Object> {

	/**
	 * 编组器
	 */
	@Nullable
	private Marshaller marshaller;

	/**
	 * 解组器
	 */
	@Nullable
	private Unmarshaller unmarshaller;


	/**
	 * 创建一个不带 {@link Marshaller} 或 {@link Unmarshaller} 的新 {@code MarshallingHttpMessageConverter}。
	 * 必须在构造后通过调用 {@link #setMarshaller(Marshaller)} 和 {@link #setUnmarshaller(Unmarshaller)} 方法设置 Marshaller 和 Unmarshaller。
	 */
	public MarshallingHttpMessageConverter() {
	}

	/**
	 * 使用给定的 {@link Marshaller} 创建一个新的 {@code MarshallingMessageConverter}。
	 * <p>如果给定的 {@link Marshaller} 同时实现了 {@link Unmarshaller} 接口，则用于编组和解组。
	 * 否则，将抛出异常。
	 * <p>注意，Spring 中的所有 {@code Marshaller} 实现也实现了 {@code Unmarshaller} 接口，
	 * 因此可以安全地使用此构造函数。
	 *
	 * @param marshaller 用作编组器和解组器的对象
	 */
	public MarshallingHttpMessageConverter(Marshaller marshaller) {
		Assert.notNull(marshaller, "Marshaller must not be null");
		this.marshaller = marshaller;
		if (marshaller instanceof Unmarshaller) {
			// 如果 Marshaller 实现了 Unmarshaller 接口，则设置 Unmarshaller
			this.unmarshaller = (Unmarshaller) marshaller;
		}
	}

	/**
	 * 使用给定的 {@code Marshaller} 和 {@code Unmarshaller} 创建一个新的 {@code MarshallingMessageConverter}。
	 *
	 * @param marshaller   要使用的 Marshaller
	 * @param unmarshaller 要使用的 Unmarshaller
	 */
	public MarshallingHttpMessageConverter(Marshaller marshaller, Unmarshaller unmarshaller) {
		Assert.notNull(marshaller, "Marshaller must not be null");
		Assert.notNull(unmarshaller, "Unmarshaller must not be null");
		this.marshaller = marshaller;
		this.unmarshaller = unmarshaller;
	}


	/**
	 * 设置此消息转换器要使用的 {@link Marshaller}。
	 */
	public void setMarshaller(Marshaller marshaller) {
		this.marshaller = marshaller;
	}

	/**
	 * 设置此消息转换器要使用的 {@link Unmarshaller}。
	 */
	public void setUnmarshaller(Unmarshaller unmarshaller) {
		this.unmarshaller = unmarshaller;
	}


	@Override
	public boolean canRead(Class<?> clazz, @Nullable MediaType mediaType) {
		return (canRead(mediaType) && this.unmarshaller != null && this.unmarshaller.supports(clazz));
	}

	@Override
	public boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType) {
		return (canWrite(mediaType) && this.marshaller != null && this.marshaller.supports(clazz));
	}

	@Override
	protected boolean supports(Class<?> clazz) {
		// 不应该被调用，因为我们重写了canRead()/canWrite()
		throw new UnsupportedOperationException();
	}

	@Override
	protected Object readFromSource(Class<?> clazz, HttpHeaders headers, Source source) throws Exception {
		Assert.notNull(this.unmarshaller, "Property 'unmarshaller' is required");
		// 对源数据进行解组操作，获取结果
		Object result = this.unmarshaller.unmarshal(source);
		// 如果结果不是 clazz 类型的实例
		if (!clazz.isInstance(result)) {
			// 抛出类型不匹配异常
			throw new TypeMismatchException(result, clazz);
		}
		// 返回结果
		return result;
	}

	@Override
	protected void writeToResult(Object o, HttpHeaders headers, Result result) throws Exception {
		Assert.notNull(this.marshaller, "Property 'marshaller' is required");
		this.marshaller.marshal(o, result);
	}

}
