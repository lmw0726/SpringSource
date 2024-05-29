/*
 * Copyright 2002-2022 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.NestedIOException;
import org.springframework.lang.Nullable;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

/**
 * {@link Resource} 实现的便利基类，预先实现了典型的行为。
 *
 * <p>"exists" 方法将检查是否可以打开 File 或 InputStream；
 * "isOpen" 将始终返回 false；"getURL" 和 "getFile" 将抛出异常；
 * "toString" 将返回描述。
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 28.12.2003
 */
public abstract class AbstractResource implements Resource {

	/**
	 * 判断文件是否存在，若判断过程产生异常（因为会调用SecurityManager来判断），就关闭对应的流
	 *
	 * @return true表示资源不存在
	 */
	@Override
	public boolean exists() {
		// Try file existence: can we find the file in the file system?
		if (isFile()) {
			try {
				//基于File进行判断
				return getFile().exists();
			} catch (IOException ex) {
				Log logger = LogFactory.getLog(getClass());
				if (logger.isDebugEnabled()) {
					logger.debug("Could not retrieve File for existence check of " + getDescription(), ex);
				}
			}
		}
		// Fall back to stream existence: can we open the stream?
		try {
			//基于InputStream进行判断，并关闭流
			getInputStream().close();
			return true;
		} catch (Throwable ex) {
			Log logger = LogFactory.getLog(getClass());
			if (logger.isDebugEnabled()) {
				logger.debug("Could not retrieve InputStream for existence check of " + getDescription(), ex);
			}
			return false;
		}
	}

	/**
	 * 根据文件是否存在判断是否可读。
	 *
	 * @return true表示可读
	 */
	@Override
	public boolean isReadable() {
		return exists();
	}

	/**
	 * 返回false表示未被打开
	 *
	 * @return false表示未被打开
	 */
	@Override
	public boolean isOpen() {
		return false;
	}

	/**
	 * 直接返回false，表示该资源不是文件
	 *
	 * @return true表示资源是一个文件
	 */
	@Override
	public boolean isFile() {
		return false;
	}

	/**
	 * 抛出FileNotFoundException异常，交予子类实现
	 *
	 * @return 资源URL
	 * @throws IOException IO异常
	 */
	@Override
	public URL getURL() throws IOException {
		throw new FileNotFoundException(getDescription() + " cannot be resolved to URL");
	}

	/**
	 * 基于getURL()返回的URL构建URI
	 *
	 * @return URI资源定位符
	 * @throws IOException IO异常
	 */
	@Override
	public URI getURI() throws IOException {
		URL url = getURL();
		try {
			return ResourceUtils.toURI(url);
		} catch (URISyntaxException ex) {
			throw new NestedIOException("Invalid URI [" + url + "]", ex);
		}
	}

	/**
	 * 抛出FileNotFoundException异常，交由子类实现
	 *
	 * @return 文件对象
	 * @throws IOException IO异常
	 */
	@Override
	public File getFile() throws IOException {
		throw new FileNotFoundException(getDescription() + " cannot be resolved to absolute file path");
	}

	/**
	 * 根据getInputStream()的结果构建ReadableByteChannel
	 *
	 * @return 可读通道
	 * @throws IOException IO异常
	 */
	@Override
	public ReadableByteChannel readableChannel() throws IOException {
		return Channels.newChannel(getInputStream());
	}

	/**
	 * 获取资源的长度
	 * 这个资源内容铲毒实际就是资源的字节长度，通过全部读取一遍来判断
	 *
	 * @return
	 * @throws IOException
	 */
	@Override
	public long contentLength() throws IOException {
		InputStream is = getInputStream();
		try {
			long size = 0;
			//每次最多读取256字节
			byte[] buf = new byte[256];
			int read;
			while ((read = is.read(buf)) != -1) {
				size += read;
			}
			return size;
		} finally {
			try {
				is.close();
			} catch (IOException ex) {
				Log logger = LogFactory.getLog(getClass());
				if (logger.isDebugEnabled()) {
					logger.debug("Could not close content-length InputStream for " + getDescription(), ex);
				}
			}
		}
	}

	/**
	 * 返回资源最后修改时间
	 *
	 * @return 资源最后修改时间
	 * @throws IOException IO异常
	 */
	@Override
	public long lastModified() throws IOException {
		File fileToCheck = getFileForLastModifiedCheck();
		long lastModified = fileToCheck.lastModified();
		if (lastModified == 0L && !fileToCheck.exists()) {
			throw new FileNotFoundException(getDescription() +
					" cannot be resolved in the file system for checking its last-modified timestamp");
		}
		return lastModified;
	}

	/**
	 * 确定用于时间戳检查的文件。
	 * <p>默认实现委托给{@link #getFile()}。
	 *
	 * @return 用于时间戳检查的文件（永不为{@code null}）
	 * @throws FileNotFoundException 如果资源无法解析为绝对文件路径，即在文件系统中不可用
	 * @throws IOException           如果存在一般的解析/读取失败
	 */
	protected File getFileForLastModifiedCheck() throws IOException {
		return getFile();
	}

	/**
	 * 抛出FileNotFoundException异常，交由子类海鲜
	 *
	 * @param relativePath 相对路径
	 * @return 创建的相关资源
	 * @throws IOException IO异常
	 */
	@Override
	public Resource createRelative(String relativePath) throws IOException {
		throw new FileNotFoundException("Cannot create a relative resource for " + getDescription());
	}

	/**
	 * 获取资源名称，默认返回null，交由子类实现
	 *
	 * @return 文件名
	 */
	@Override
	@Nullable
	public String getFilename() {
		return null;
	}


	/**
	 * 此实现比较描述字符串。
	 *
	 * @see #getDescription()
	 */
	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof Resource &&
				((Resource) other).getDescription().equals(getDescription())));
	}

	/**
	 * 此实现返回描述的哈希码。
	 *
	 * @see #getDescription()
	 */
	@Override
	public int hashCode() {
		return getDescription().hashCode();
	}

	/**
	 * 返回资源的描述
	 *
	 * @return 资源的描述
	 */
	@Override
	public String toString() {
		return getDescription();
	}

}
