/*
 * Copyright 2002-2017 the original author or authors.
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

import com.google.gson.*;
import org.springframework.util.Base64Utils;

import java.lang.reflect.Type;

/**
 * 用于获取一个 Google Gson 2.x {@link GsonBuilder} 的简单实用工具类，
 * 当读取和写入 JSON 时，将 {@code byte[]} 属性进行 Base64 编码。
 *
 * <p>通过注册一个自定义的 {@link com.google.gson.TypeAdapter}，
 * 通过 {@link GsonBuilder#registerTypeHierarchyAdapter(Class, Object)} 注册，
 * 该适配器将 {@code byte[]} 属性序列化为和反序列化为 Base64 编码的字符串，
 * 而不是 JSON 数组。
 *
 * @author Juergen Hoeller
 * @author Roy Clarkson
 * @see GsonFactoryBean#setBase64EncodeByteArrays
 * @see org.springframework.util.Base64Utils
 * @since 4.1
 */
public abstract class GsonBuilderUtils {

	/**
	 * 获取一个 {@link GsonBuilder}，用于对读取和写入 JSON 时对 {@code byte[]} 属性进行 Base64 编码。
	 * <p>将通过 {@link GsonBuilder#registerTypeHierarchyAdapter(Class, Object)} 注册一个自定义的
	 * {@link com.google.gson.TypeAdapter}，该适配器将 {@code byte[]} 属性序列化为和反序列化为
	 * Base64 编码的字符串，而不是 JSON 数组。
	 *
	 * @return 配置了 Base64 编码 {@code byte[]} 属性的 GsonBuilder
	 */
	public static GsonBuilder gsonBuilderWithBase64EncodedByteArrays() {
		// 创建一个GsonBuilder实例
		GsonBuilder builder = new GsonBuilder();

		// 注册Base64TypeAdapter来处理 字节数组 类型的数据
		builder.registerTypeHierarchyAdapter(byte[].class, new Base64TypeAdapter());

		// 返回配置好的GsonBuilder实例
		return builder;
	}


	private static class Base64TypeAdapter implements JsonSerializer<byte[]>, JsonDeserializer<byte[]> {

		@Override
		public JsonElement serialize(byte[] src, Type typeOfSrc, JsonSerializationContext context) {
			return new JsonPrimitive(Base64Utils.encodeToString(src));
		}

		@Override
		public byte[] deserialize(JsonElement json, Type type, JsonDeserializationContext cxt) {
			return Base64Utils.decodeFromString(json.getAsString());
		}
	}

}
