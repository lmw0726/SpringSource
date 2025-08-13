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

package org.springframework.core.io;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.InputStream;

/**
 * 基于给定 {@link InputStream} 的 {@link Resource} 实现。
 * <p>仅当没有其他特定的 {@code Resource} 实现适用时使用。
 * 特别是在可能的情况下，应优先使用 {@link ByteArrayResource} 或任何基于文件的 {@code Resource} 实现。
 *
 * <p>与其他 {@code Resource} 实现不同，这是一个 <i>已打开</i> 资源的描述符，
 * 因此 {@link #isOpen()} 返回 {@code true}。
 * 如果需要在某处持有资源描述符，或需要多次读取流，请勿使用 {@code InputStreamResource}。
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @see ByteArrayResource
 * @see ClassPathResource
 * @see FileSystemResource
 * @see UrlResource
 * @since 28.12.2003
 */
public class InputStreamResource extends AbstractResource {

	private final InputStream inputStream;

	private final String description;

	private boolean read = false;


	public InputStreamResource(InputStream inputStream) {
		this(inputStream, "resource loaded through InputStream");
	}


	public InputStreamResource(InputStream inputStream, @Nullable String description) {
		Assert.notNull(inputStream, "InputStream must not be null");
		this.inputStream = inputStream;
		this.description = (description != null ? description : "");
	}


	/**
	 * 默认资源是存在的
	 *
	 * @return true表示资源存在
	 */
	@Override
	public boolean exists() {
		return true;
	}


	/**
	 * 资源是可打开的
	 *
	 * @return true表示资源可被打开
	 */
	@Override
	public boolean isOpen() {
		return true;
	}


	/**
	 * 获取输入流
	 *
	 * @return 输入流
	 * @throws IOException           IO异常
	 * @throws IllegalStateException 非法状态异常
	 */
	@Override
	public InputStream getInputStream() throws IOException, IllegalStateException {
		//保证流只能够读取一次
		if (this.read) {
			throw new IllegalStateException("InputStream has already been read - " +
					"do not use InputStreamResource if a stream needs to be read multiple times");
		}
		//一旦读取，流读取标志设置为true
		this.read = true;
		return this.inputStream;
	}

	/**
	 * 获取资源的描述
	 * @return 资源描述
	 */
	@Override
	public String getDescription() {
		return "InputStream resource [" + this.description + "]";
	}



	/**
	 * 两个对象是否相等
	 * @param other 另外的对象
	 * @return true表示相等
	 */
	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof InputStreamResource &&
				((InputStreamResource) other).inputStream.equals(this.inputStream)));
	}

	/**
	 * 获取哈希值
	 * @return 输入流的哈希值
	 */
	@Override
	public int hashCode() {
		return this.inputStream.hashCode();
	}

}
