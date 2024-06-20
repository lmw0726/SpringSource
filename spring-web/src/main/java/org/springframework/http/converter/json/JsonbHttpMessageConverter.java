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

package org.springframework.http.converter.json;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * 实现 {@link org.springframework.http.converter.HttpMessageConverter} 接口，
 * 使用 <a href="http://json-b.net/">JSON Binding API</a> 读写 JSON 的转换器。
 *
 * <p>此转换器可用于绑定到类型化的 bean 或未类型化的 {@code HashMap}。
 * 默认支持 {@code application/json} 和 {@code application/*+json}，使用 {@code UTF-8} 字符集。
 *
 * @author Juergen Hoeller
 * @see javax.json.bind.Jsonb
 * @see javax.json.bind.JsonbBuilder
 * @see #setJsonb
 * @since 5.0
 */
public class JsonbHttpMessageConverter extends AbstractJsonHttpMessageConverter {
	/**
	 * Jsonb 实例
	 */
	private Jsonb jsonb;


	/**
	 * 使用默认配置构造新的 {@code JsonbHttpMessageConverter}。
	 */
	public JsonbHttpMessageConverter() {
		this(JsonbBuilder.create());
	}

	/**
	 * 使用给定配置构造新的 {@code JsonbHttpMessageConverter}。
	 *
	 * @param config 底层代理的 {@code JsonbConfig}
	 */
	public JsonbHttpMessageConverter(JsonbConfig config) {
		this.jsonb = JsonbBuilder.create(config);
	}

	/**
	 * 使用给定的 Jsonb 实例构造新的 {@code JsonbHttpMessageConverter}。
	 *
	 * @param jsonb 要使用的 Jsonb 实例
	 */
	public JsonbHttpMessageConverter(Jsonb jsonb) {
		Assert.notNull(jsonb, "A Jsonb instance is required");
		this.jsonb = jsonb;
	}


	/**
	 * 设置要使用的 {@code Jsonb} 实例。
	 * 如果未设置，将创建一个默认的 {@code Jsonb} 实例。
	 * <p>设置自定义配置的 {@code Jsonb} 是控制 JSON 序列化过程的一种方式。
	 *
	 * @see #JsonbHttpMessageConverter(Jsonb)
	 * @see #JsonbHttpMessageConverter(JsonbConfig)
	 * @see JsonbBuilder
	 */
	public void setJsonb(Jsonb jsonb) {
		Assert.notNull(jsonb, "A Jsonb instance is required");
		this.jsonb = jsonb;
	}

	/**
	 * 返回此转换器配置的 {@code Jsonb} 实例。
	 */
	public Jsonb getJsonb() {
		return this.jsonb;
	}


	@Override
	protected Object readInternal(Type resolvedType, Reader reader) throws Exception {
		return getJsonb().fromJson(reader, resolvedType);
	}

	@Override
	protected void writeInternal(Object object, @Nullable Type type, Writer writer) throws Exception {
		if (type instanceof ParameterizedType) {
			// 如果类型是参数化类型，则调用带有类型的toJson方法
			getJsonb().toJson(object, type, writer);
		} else {
			// 否则调用不带类型的toJson方法
			getJsonb().toJson(object, writer);
		}
	}

}
