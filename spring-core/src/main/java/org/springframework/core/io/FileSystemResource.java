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
import org.springframework.util.StringUtils;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.FileSystem;
import java.nio.file.*;

/**
 * {@link Resource} implementation for {@code java.io.File} and
 * {@code java.nio.file.Path} handles with a file system target.
 * Supports resolution as a {@code File} and also as a {@code URL}.
 * Implements the extended {@link WritableResource} interface.
 *
 * <p>Note: As of Spring Framework 5.0, this {@link Resource} implementation uses
 * NIO.2 API for read/write interactions. As of 5.1, it may be constructed with a
 * {@link java.nio.file.Path} handle in which case it will perform all file system
 * interactions via NIO.2, only resorting to {@link File} on {@link #getFile()}.
 *
 * @author Juergen Hoeller
 * @see #FileSystemResource(String)
 * @see #FileSystemResource(File)
 * @see #FileSystemResource(Path)
 * @see java.io.File
 * @see java.nio.file.Files
 * @since 28.12.2003
 */
public class FileSystemResource extends AbstractResource implements WritableResource {

	private final String path;

	@Nullable
	private final File file;

	private final Path filePath;



	public FileSystemResource(String path) {
		Assert.notNull(path, "Path must not be null");
		this.path = StringUtils.cleanPath(path);
		this.file = new File(path);
		this.filePath = this.file.toPath();
	}


	public FileSystemResource(File file) {
		Assert.notNull(file, "File must not be null");
		this.path = StringUtils.cleanPath(file.getPath());
		this.file = file;
		this.filePath = file.toPath();
	}


	public FileSystemResource(Path filePath) {
		Assert.notNull(filePath, "Path must not be null");
		this.path = StringUtils.cleanPath(filePath.toString());
		this.file = null;
		this.filePath = filePath;
	}


	public FileSystemResource(FileSystem fileSystem, String path) {
		Assert.notNull(fileSystem, "FileSystem must not be null");
		Assert.notNull(path, "Path must not be null");
		this.path = StringUtils.cleanPath(path);
		this.file = null;
		this.filePath = fileSystem.getPath(this.path).normalize();
	}


	/**
	 * 返回该资源的文件路径
	 *
	 * @return 文件路径
	 */
	public final String getPath() {
		return this.path;
	}

	/**
	 * 如果文件存在，则调用File.exists（）方法，否则就调用Files.exists方法确认filePath是否存在
	 *
	 * @return true表示资源存在
	 */
	@Override
	public boolean exists() {
		return (this.file != null ? this.file.exists() : Files.exists(this.filePath));
	}

	/**
	 * 资源是否可读
	 *
	 * @return true表示资源可读
	 */
	@Override
	public boolean isReadable() {
		//如果文件不为空，且它不是文件夹，资源是否可读由该文件是否可读决定。
		//如果是一个文件夹，它是可读的。否则调用Files API看该路径是否可读。
		return (this.file != null ? this.file.canRead() && !this.file.isDirectory() :
				Files.isReadable(this.filePath) && !Files.isDirectory(this.filePath));
	}


	/**
	 * 获取文件输入流
	 *
	 * @return 文件流
	 * @throws IOException IO异常
	 */
	@Override
	public InputStream getInputStream() throws IOException {
		try {
			//调用Files API获取该路径的输入流
			return Files.newInputStream(this.filePath);
		} catch (NoSuchFileException ex) {
			//获取不到就抛出文件未找到异常。
			throw new FileNotFoundException(ex.getMessage());
		}
	}

	/**
	 * 文件是否可写
	 *
	 * @return true表示文件可写
	 */
	@Override
	public boolean isWritable() {
		//如果文件不为空，且它不是文件夹，资源是否可写由该文件可写属性决定。
		//否则如果该路径不是文件夹，调用Files API检查该路径是否可写。
		return (this.file != null ? this.file.canWrite() && !this.file.isDirectory() :
				Files.isWritable(this.filePath) && !Files.isDirectory(this.filePath));
	}

	/**
	 * 返回输出流
	 *
	 * @return 输出流，实际上是FileOutputStream
	 * @throws IOException IO异常
	 */
	@Override
	public OutputStream getOutputStream() throws IOException {
		//调用Files API获取该路径的文件输出流
		return Files.newOutputStream(this.filePath);
	}


	/**
	 * 获取该文件的URL，统一资源定位符
	 *
	 * @return 该文件的URL
	 * @throws IOException IO异常
	 */
	@Override
	public URL getURL() throws IOException {
		//如果文件为空，将filePath转为URI后，获取其URL返回。
		//否则，通过文件转为URI，再转为URL返回。
		return (this.file != null ? this.file.toURI().toURL() : this.filePath.toUri().toURL());
	}

	/**
	 * 获取文件的URI，统一资源标识符
	 *
	 * @return 该文件的URI
	 * @throws IOException IO异常
	 */
	@Override
	public URI getURI() throws IOException {
		//如果该文件为空，获取该路径的URI；否则获取该文件的URI。
		return (this.file != null ? this.file.toURI() : this.filePath.toUri());
	}

	/**
	 * 是否是一个文件，默认为true
	 *
	 * @return 是一个文件
	 */
	@Override
	public boolean isFile() {
		return true;
	}

	/**
	 * 获取该文件
	 *
	 * @return 该文件
	 */
	@Override
	public File getFile() {
		//该文件为空，获取该路径转换后的文件。否则就获取这个文件。
		return (this.file != null ? this.file : this.filePath.toFile());
	}

	/**
	 * 获取该文件可读字节通道
	 *
	 * @return 可读字节通道
	 * @throws IOException IO异常
	 */
	@Override
	public ReadableByteChannel readableChannel() throws IOException {
		try {
			//通过FileChannel.open获取该路径的标准可读通道。
			return FileChannel.open(this.filePath, StandardOpenOption.READ);
		} catch (NoSuchFileException ex) {
			//该路径不存在就抛出文件未找到异常
			throw new FileNotFoundException(ex.getMessage());
		}
	}


	/**
	 * 获取该文件的可写数组通道。
	 *
	 * @return 可写数组通道
	 * @throws IOException IO异常
	 */
	@Override
	public WritableByteChannel writableChannel() throws IOException {
		//通过FileChannel获取该路径的可写通道。
		return FileChannel.open(this.filePath, StandardOpenOption.WRITE);
	}

	/**
	 * 获取该文件内容的长度
	 *
	 * @return 文件内容的长度
	 * @throws IOException IO异常
	 */
	@Override
	public long contentLength() throws IOException {
		if (this.file != null) {
			//获取File.length()
			long length = this.file.length();
			//如果length为0并且该文件不存在，则抛出文件未找到异常
			if (length == 0L && !this.file.exists()) {
				throw new FileNotFoundException(getDescription() +
						" cannot be resolved in the file system for checking its content length");
			}
			return length;
		} else {
			try {
				//文件为空，通过调用Files.size获取该路径的文件大小。
				return Files.size(this.filePath);
			} catch (NoSuchFileException ex) {
				//获取不到则抛出文件未找到异常
				throw new FileNotFoundException(ex.getMessage());
			}
		}
	}

	/**
	 * 获取最后一次修改的时间
	 *
	 * @return 最后一次修改的时间
	 * @throws IOException IO异常
	 */
	@Override
	public long lastModified() throws IOException {
		if (this.file != null) {
			//文件不为空，通过File.lastModified()获取。
			return super.lastModified();
		} else {
			try {
				//否则通过Files API获取该路径的最后一次修改的时间。
				return Files.getLastModifiedTime(this.filePath).toMillis();
			} catch (NoSuchFileException ex) {
				//获取不到就抛出文件未找到异常
				throw new FileNotFoundException(ex.getMessage());
			}
		}
	}


	/**
	 * 根据相对路径创建一个相关的资源
	 *
	 * @param relativePath 相对路径
	 * @return 相关的资源
	 */
	@Override
	public Resource createRelative(String relativePath) {
		//获取一个相对路径
		String pathToUse = StringUtils.applyRelativePath(this.path, relativePath);
		//如果文件存在，就根据这个路径创建一个FileSystemResource。
		//否则就根据this.filePath.getFileSystem()和该相对路径创建FileSystemResource
		return (this.file != null ? new FileSystemResource(pathToUse) :
				new FileSystemResource(this.filePath.getFileSystem(), pathToUse));
	}


	/**
	 * 获取该资源的文件名
	 *
	 * @return 该资源的文件名
	 */
	@Override
	public String getFilename() {
		//如果该文件不为空，则通过File获取文件名。否则通过路径获取它的文件名。
		return (this.file != null ? this.file.getName() : this.filePath.getFileName().toString());
	}


	/**
	 * 获取资源描述
	 *
	 * @return 资源描述
	 */
	@Override
	public String getDescription() {
		return "file [" + (this.file != null ? this.file.getAbsolutePath() : this.filePath.toAbsolutePath()) + "]";
	}


	/**
	 * 两个文件资源是否相同
	 *
	 * @param other 另外的资源
	 * @return true表示该资源相同
	 */
	@Override
	public boolean equals(@Nullable Object other) {
		//如果它们的地址相同，则相等。
		//如果另外的对象是FileSystemResource的子类型，则比较它们的path是否相同。
		return (this == other || (other instanceof FileSystemResource &&
				this.path.equals(((FileSystemResource) other).path)));
	}


	/**
	 * 获取HashCode
	 *
	 * @return 哈希值
	 */
	@Override
	public int hashCode() {
		//根据零字符串获取它的哈希值
		return this.path.hashCode();
	}

}
