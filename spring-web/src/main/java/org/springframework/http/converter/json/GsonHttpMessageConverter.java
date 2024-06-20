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

import com.google.gson.Gson;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * 实现 {@link org.springframework.http.converter.HttpMessageConverter} 接口，
 * 使用 <a href="https://code.google.com/p/google-gson/">Google Gson</a> 库读写 JSON 的转换器。
 *
 * <p>此转换器可用于绑定到类型化的 bean 或未类型化的 {@code HashMap}。
 * 默认支持 {@code application/json} 和 {@code application/*+json}，使用 {@code UTF-8} 字符集。
 *
 * <p>经过 Gson 2.8 测试；与 Gson 2.0 及更高版本兼容。
 *
 * @author Roy Clarkson
 * @author Juergen Hoeller
 * @see com.google.gson.Gson
 * @see com.google.gson.GsonBuilder
 * @see #setGson
 * @since 4.1
 */
public class GsonHttpMessageConverter extends AbstractJsonHttpMessageConverter {
	/**
	 * Gson 实例
	 */
	private Gson gson;


	/**
	 * 使用默认配置构造新的 {@code GsonHttpMessageConverter}。
	 */
	public GsonHttpMessageConverter() {
		this.gson = new Gson();
	}

	/**
	 * 使用给定的 Gson 实例构造新的 {@code GsonHttpMessageConverter}。
	 *
	 * @param gson 要使用的 Gson 实例
	 * @since 5.0
	 */
	public GsonHttpMessageConverter(Gson gson) {
		Assert.notNull(gson, "A Gson instance is required");
		this.gson = gson;
	}


	/**
	 * 设置要使用的 {@code Gson} 实例。
	 * 如果未设置，将使用默认的 {@link Gson#Gson() Gson} 实例。
	 * <p>设置自定义配置的 {@code Gson} 是控制 JSON 序列化过程的一种方式。
	 *
	 * @see #GsonHttpMessageConverter(Gson)
	 */
	public void setGson(Gson gson) {
		Assert.notNull(gson, "A Gson instance is required");
		this.gson = gson;
	}

	/**
	 * 返回此转换器配置的 {@code Gson} 实例。
	 */
	public Gson getGson() {
		return this.gson;
	}


	@Override
	protected Object readInternal(Type resolvedType, Reader reader) throws Exception {
		return getGson().fromJson(reader, resolvedType);
	}

	@Override
	protected void writeInternal(Object object, @Nullable Type type, Writer writer) throws Exception {
		// 在 Gson 中，使用类型参数的 toJson 将完全使用给定的类型，
		// 忽略对象的实际类型… 可能更具体的类型，例如指定类型的子类，包含额外字段。
		// 因此，我们只传递参数化类型声明，可能包含对象实例不保留的额外泛型。
		if (type instanceof ParameterizedType) {
			// 如果类型是参数化类型，则调用带有类型的toJson方法
			getGson().toJson(object, type, writer);
		} else {
			// 否则调用不带类型的toJson方法
			getGson().toJson(object, writer);
		}
	}

}
