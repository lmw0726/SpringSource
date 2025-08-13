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

package org.springframework.core.serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * 对象流式写入输出流的策略接口。
 *
 * @author Gary Russell
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @since 3.0.5
 * @param <T> 对象类型
 * @see Deserializer
 */
@FunctionalInterface
public interface Serializer<T> {

	/**
	 * 将类型为 T 的对象写入给定的 OutputStream。
	 * <p>注意：实现类不应关闭给定的 OutputStream（或其任何装饰者），
	 * 关闭流的责任应由调用方负责。
	 * @param object 要序列化的对象
	 * @param outputStream 输出流
	 * @throws IOException 写入流时发生错误
	 */
	void serialize(T object, OutputStream outputStream) throws IOException;

	/**
	 * 将类型为 T 的对象序列化成字节数组。
	 * @param object 要序列化的对象
	 * @return 序列化得到的字节数组
	 * @throws IOException 序列化失败时抛出
	 * @since 5.2.7
	 */
	default byte[] serializeToByteArray(T object) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
		serialize(object, out);
		return out.toByteArray();
	}

}
