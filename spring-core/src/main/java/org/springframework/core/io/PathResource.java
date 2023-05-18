/*
 * Copyright 2002-2019 the original author or authors.
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

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.*;

/**
 * {@link Resource} implementation for {@link java.nio.file.Path} handles,
 * performing all operations and transformations via the {@code Path} API.
 * Supports resolution as a {@link File} and also as a {@link URL}.
 * Implements the extended {@link WritableResource} interface.
 *
 * <p>Note: As of 5.1, {@link java.nio.file.Path} support is also available
 * in {@link FileSystemResource#FileSystemResource(Path) FileSystemResource},
 * applying Spring's standard String-based path transformations but
 * performing all operations via the {@link java.nio.file.Files} API.
 * This {@code PathResource} is effectively a pure {@code java.nio.path.Path}
 * based alternative with different {@code createRelative} behavior.
 *
 * @author Philippe Marschall
 * @author Juergen Hoeller
 * @see java.nio.file.Path
 * @see java.nio.file.Files
 * @see FileSystemResource
 * @since 4.0
 */
public class PathResource extends AbstractResource implements WritableResource {

	private final Path path;



	public PathResource(Path path) {
		Assert.notNull(path, "Path must not be null");
		this.path = path.normalize();
	}


	public PathResource(String path) {
		Assert.notNull(path, "Path must not be null");
		this.path = Paths.get(path).normalize();
	}


	public PathResource(URI uri) {
		Assert.notNull(uri, "URI must not be null");
		this.path = Paths.get(uri).normalize();
	}


	/**
	 * 获取该路径
	 *
	 * @return 返回这个路径
	 */
	public final String getPath() {
		return this.path.toString();
	}


	/**
	 * 资源是否存在
	 *
	 * @return true表示资源存在
	 */
	@Override
	public boolean exists() {
		//调用Files API确定该路径是否存在
		return Files.exists(this.path);
	}


	/**
	 * 资源是否可读
	 *
	 * @return true表示资源可读
	 */
	@Override
	public boolean isReadable() {
		//如果该路径不是一个文件夹，通过Files API确定该路径是否可读的。
		//文件夹是可读的
		return (Files.isReadable(this.path) && !Files.isDirectory(this.path));
	}

	/**
	 * 获取输入流
	 *
	 * @return 输入流
	 * @throws IOException IO异常
	 */
	@Override
	public InputStream getInputStream() throws IOException {
		//如果该资源不存在，则抛出文件未找到异常
		if (!exists()) {
			throw new FileNotFoundException(getPath() + " (no such file or directory)");
		}
		//如果该路径是一个文件夹，也抛出一个文件未找到异常
		if (Files.isDirectory(this.path)) {
			throw new FileNotFoundException(getPath() + " (is a directory)");
		}
		//通过调用Files.newInputStream获取该路径的输入流
		return Files.newInputStream(this.path);
	}


	/**
	 * 资源是否可写
	 *
	 * @return true表示资源可写
	 */
	@Override
	public boolean isWritable() {
		//如果是文件，则由Files API决定该路径是否可写
		//如果是文件夹，则是不可写的。
		return (Files.isWritable(this.path) && !Files.isDirectory(this.path));
	}


	/**
	 * 获取输出流
	 *
	 * @return 输出流
	 * @throws IOException IO异常
	 */
	@Override
	public OutputStream getOutputStream() throws IOException {
		//如果是一个文件夹，抛出异常
		if (Files.isDirectory(this.path)) {
			throw new FileNotFoundException(getPath() + " (is a directory)");
		}
		//调用Files.newOutputStream获取该路径的输出流
		return Files.newOutputStream(this.path);
	}

	/**
	 * 获取URL，统一资源定位符
	 *
	 * @return URL，统一资源定位符
	 * @throws IOException IO异常
	 */
	@Override
	public URL getURL() throws IOException {
		//将该路径转为URI后再转为URL。
		return this.path.toUri().toURL();
	}


	/**
	 * 获取URI，统一资源标识符
	 *
	 * @return URI，统一资源标识符
	 * @throws IOException IO异常
	 */
	@Override
	public URI getURI() throws IOException {
		//将该路径直转为URI
		return this.path.toUri();
	}

	/**
	 * 是否是文件
	 *
	 * @return true表示是一个文件
	 */
	@Override
	public boolean isFile() {
		return true;
	}

	/**
	 * 获取该文件
	 *
	 * @return 该文件
	 * @throws IOException IO异常
	 */
	@Override
	public File getFile() throws IOException {
		try {
			//将路径转为File返回
			return this.path.toFile();
		} catch (UnsupportedOperationException ex) {
			//只有默认文件系统上的路径可以转换为文件：
			//对无法转换的情况进行异常翻译。
			throw new FileNotFoundException(this.path + " cannot be resolved to absolute file path");
		}
	}

	/**
	 * 获取可读字节通道
	 *
	 * @return 可读字节通道
	 * @throws IOException IO异常
	 */
	@Override
	public ReadableByteChannel readableChannel() throws IOException {
		try {
			//通过Files API读取该路径的可读字节通道
			return Files.newByteChannel(this.path, StandardOpenOption.READ);
		} catch (NoSuchFileException ex) {
			//否则抛出异常
			throw new FileNotFoundException(ex.getMessage());
		}
	}

	/**
	 * 获取可写字节通道
	 *
	 * @return 可写字节通道
	 * @throws IOException IO异常
	 */
	@Override
	public WritableByteChannel writableChannel() throws IOException {
		return Files.newByteChannel(this.path, StandardOpenOption.WRITE);
	}

	/**
	 * 获取资源的内容长度
	 *
	 * @return 内容长度
	 * @throws IOException IO异常
	 */
	@Override
	public long contentLength() throws IOException {
		return Files.size(this.path);
	}


	/**
	 * 获取最后一次修改的时间
	 *
	 * @return 最后一次修改的时间
	 * @throws IOException IO异常
	 */
	@Override
	public long lastModified() throws IOException {
		//不能使用超类的方法，因为他会转为File，并且只有在默认的文件系统上路径才能转换为File
		return Files.getLastModifiedTime(this.path).toMillis();
	}

	/**
	 * 创建相关的资源
	 *
	 * @param relativePath 相对路径
	 * @return 相关的资源
	 */
	@Override
	public Resource createRelative(String relativePath) {
		return new PathResource(this.path.resolve(relativePath));
	}


	/**
	 * 获取资源的文件名
	 *
	 * @return 资源的文件名
	 */
	@Override
	public String getFilename() {
		return this.path.getFileName().toString();
	}

	/**
	 * 获取资源的描述
	 *
	 * @return 资源的描述
	 */
	@Override
	public String getDescription() {
		return "path [" + this.path.toAbsolutePath() + "]";
	}

	/**
	 * 两个对象是否相等
	 *
	 * @param other 另外的对象
	 * @return true表示两个对象相等
	 */
	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof PathResource &&
				this.path.equals(((PathResource) other).path)));
	}

	/**
	 * 获取哈希值
	 *
	 * @return 哈希值
	 */
	@Override
	public int hashCode() {
		return this.path.hashCode();
	}

}
