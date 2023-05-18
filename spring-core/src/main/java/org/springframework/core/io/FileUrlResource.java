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

package org.springframework.core.io;

import org.springframework.lang.Nullable;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

/**
 * Subclass of {@link UrlResource} which assumes file resolution, to the degree
 * of implementing the {@link WritableResource} interface for it. This resource
 * variant also caches resolved {@link File} handles from {@link #getFile()}.
 *
 * <p>This is the class resolved by {@link DefaultResourceLoader} for a "file:..."
 * URL location, allowing a downcast to {@link WritableResource} for it.
 *
 * <p>Alternatively, for direct construction from a {@link java.io.File} handle
 * or NIO {@link java.nio.file.Path}, consider using {@link FileSystemResource}.
 *
 * @author Juergen Hoeller
 * @since 5.0.2
 */
public class FileUrlResource extends UrlResource implements WritableResource {

	@Nullable
	private volatile File file;


	public FileUrlResource(URL url) {
		super(url);
	}


	public FileUrlResource(String location) throws MalformedURLException {
		super(ResourceUtils.URL_PROTOCOL_FILE, location);
	}

	/**
	 * 获取文件
	 *
	 * @return 文件
	 * @throws IOException IO异常
	 */
	@Override
	public File getFile() throws IOException {
		File file = this.file;
		if (file != null) {
			return file;
		}
		file = super.getFile();
		this.file = file;
		return file;
	}

	/**
	 * 资源是否可写
	 *
	 * @return true表示资源可写
	 */
	@Override
	public boolean isWritable() {
		try {
			File file = getFile();
			//文件可写并且不是文件夹，返回true
			return (file.canWrite() && !file.isDirectory());
		} catch (IOException ex) {
			return false;
		}
	}

	/**
	 * 获取输出流
	 *
	 * @return 输出流
	 * @throws IOException IO异常
	 */
	@Override
	public OutputStream getOutputStream() throws IOException {
		return Files.newOutputStream(getFile().toPath());
	}

	/**
	 * 获取可写通道
	 *
	 * @return 可写通道
	 * @throws IOException IO异常
	 */
	@Override
	public WritableByteChannel writableChannel() throws IOException {
		return FileChannel.open(getFile().toPath(), StandardOpenOption.WRITE);
	}

	/**
	 * 创建相关的资源
	 *
	 * @param relativePath 相对路径
	 * @return 相关的资源
	 * @throws MalformedURLException 畸形URL异常
	 */
	@Override
	public Resource createRelative(String relativePath) throws MalformedURLException {
		return new FileUrlResource(createRelativeURL(relativePath));
	}

}
