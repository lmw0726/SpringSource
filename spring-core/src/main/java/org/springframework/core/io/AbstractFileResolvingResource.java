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

import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.NoSuchFileException;
import java.nio.file.StandardOpenOption;

/**
 * Abstract base class for resources which resolve URLs into File references,
 * such as {@link UrlResource} or {@link ClassPathResource}.
 *
 * <p>Detects the "file" protocol as well as the JBoss "vfs" protocol in URLs,
 * resolving file system references accordingly.
 *
 * @author Juergen Hoeller
 * @since 3.0
 */
public abstract class AbstractFileResolvingResource extends AbstractResource {
	/**
	 * 资源是否存在
	 *
	 * @return true表示资源存在
	 */
	@Override
	public boolean exists() {
		try {
			//获取URL
			URL url = getURL();
			//如果url是FileUrl
			if (ResourceUtils.isFileURL(url)) {
				//通过文件系统解析进行处理
				return getFile().exists();
			} else {
				//尝试URL连接内容长度的报头
				URLConnection con = url.openConnection();
				customizeConnection(con);
				HttpURLConnection httpCon =
						(con instanceof HttpURLConnection ? (HttpURLConnection) con : null);
				if (httpCon != null) {
					//获取连接返回值。
					int code = httpCon.getResponseCode();
					//如果返回值为OK,返回true,如果返回NOT_FOUND返回false
					if (code == HttpURLConnection.HTTP_OK) {
						return true;
					} else if (code == HttpURLConnection.HTTP_NOT_FOUND) {
						return false;
					}
				}
				//如果内容长度大于0返回true
				if (con.getContentLengthLong() > 0) {
					return true;
				}
				if (httpCon != null) {
					//即不返回OK，也不返回内容长度，返回false
					httpCon.disconnect();
					return false;
				} else {
					//连接为空，关闭流，返回true
					getInputStream().close();
					return true;
				}
			}
		} catch (IOException ex) {
			//获取不到连接，返回false
			return false;
		}
	}

	/**
	 * 资源是否可读
	 *
	 * @return true表示资源可读
	 */
	@Override
	public boolean isReadable() {
		try {
			return checkReadable(getURL());
		} catch (IOException ex) {
			return false;
		}
	}

	/**
	 * 检查URL是否可读
	 *
	 * @param url URL
	 * @return true表示可读
	 */
	boolean checkReadable(URL url) {
		try {
			//如果是一个文件URL
			if (ResourceUtils.isFileURL(url)) {
				//通过文件系统进行解析处理
				File file = getFile();
				//如果不是文件夹，并且文件是可读的，那么它是可读的。
				return (file.canRead() && !file.isDirectory());
			} else {
				//发起一个URL访问
				URLConnection con = url.openConnection();
				customizeConnection(con);
				if (con instanceof HttpURLConnection) {
					//如果是HttpURLConnection,发起调用后获取返回值
					HttpURLConnection httpCon = (HttpURLConnection) con;
					int code = httpCon.getResponseCode();
					//返回值不是OK，返回false
					if (code != HttpURLConnection.HTTP_OK) {
						httpCon.disconnect();
						return false;
					}
				}
				//获取内容长度，大于0返回true
				long contentLength = con.getContentLengthLong();
				if (contentLength > 0) {
					return true;
				} else if (contentLength == 0) {
					//长度为0，返回false。空文件或目录，认为是不可读的
					return false;
				} else {
					//否则，关闭流，返回true
					getInputStream().close();
					return true;
				}
			}
		} catch (IOException ex) {
			//发生异常，返回false
			return false;
		}
	}

	/**
	 * 是否是一个文件
	 *
	 * @return true表示是一个文件
	 */
	@Override
	public boolean isFile() {
		try {
			URL url = getURL();
			//协议头是vfs，使用VfsResourceDelegate进行解析
			if (url.getProtocol().startsWith(ResourceUtils.URL_PROTOCOL_VFS)) {
				return VfsResourceDelegate.getResource(url).isFile();
			}
			//否则就看其协议头是否以file开头
			return ResourceUtils.URL_PROTOCOL_FILE.equals(url.getProtocol());
		} catch (IOException ex) {
			return false;
		}
	}

	/**
	 * 获取文件
	 *
	 * @return 文件
	 * @throws IOException IO异常
	 */
	@Override
	public File getFile() throws IOException {
		//看其协议头是否是vfs开头，是则使用VfsResourceDelegate获取url对应的文件
		URL url = getURL();
		if (url.getProtocol().startsWith(ResourceUtils.URL_PROTOCOL_VFS)) {
			return VfsResourceDelegate.getResource(url).getFile();
		}
		//使用ResourceUtils获取该URL的文件
		return ResourceUtils.getFile(url, getDescription());
	}

	/**
	 * 获取最后一次确认修改的文件
	 *
	 * @return 文件
	 * @throws IOException IO异常
	 */
	@Override
	protected File getFileForLastModifiedCheck() throws IOException {
		URL url = getURL();
		//如果是一个JarUrl
		if (ResourceUtils.isJarURL(url)) {
			//获取实际URL
			URL actualUrl = ResourceUtils.extractArchiveURL(url);
			//如果该URL协议头为vfs，使用VfsResourceDelegate解析
			if (actualUrl.getProtocol().startsWith(ResourceUtils.URL_PROTOCOL_VFS)) {
				return VfsResourceDelegate.getResource(actualUrl).getFile();
			}
			//通过ResourceUtils获取它的文件
			return ResourceUtils.getFile(actualUrl, "Jar URL");
		} else {
			//返回文件
			return getFile();
		}
	}

	/**
	 * 是否是一个文件
	 *
	 * @param uri 统一资源标志符
	 * @return true表示是一个文件
	 */
	protected boolean isFile(URI uri) {
		try {
			//如果uri的协议头是以vfs开头的，使用VfsResourceDelegate解析，判断它是否是文件
			if (uri.getScheme().startsWith(ResourceUtils.URL_PROTOCOL_VFS)) {
				return VfsResourceDelegate.getResource(uri).isFile();
			}
			//否则判断uri的协议头是否是file协议头
			return ResourceUtils.URL_PROTOCOL_FILE.equals(uri.getScheme());
		} catch (IOException ex) {
			return false;
		}
	}


	/**
	 * 获取资源对应的文件形式
	 *
	 * @param uri 统一资源标志符
	 * @return 资源对应的文件
	 * @throws IOException IO异常
	 */
	protected File getFile(URI uri) throws IOException {
		//如果uri的协议头是以vfs开头，则使用VfsResourceDelegate解析成File的形式
		if (uri.getScheme().startsWith(ResourceUtils.URL_PROTOCOL_VFS)) {
			return VfsResourceDelegate.getResource(uri).getFile();
		}
		//否则调用ResourceUtils获取uri对应的文件形式
		return ResourceUtils.getFile(uri, getDescription());
	}

	/**
	 * 获取可读通道
	 *
	 * @return 可读字节通道
	 * @throws IOException IO异常
	 */
	@Override
	public ReadableByteChannel readableChannel() throws IOException {
		try {
			//调用文件系统获取对用的channel
			return FileChannel.open(getFile().toPath(), StandardOpenOption.READ);
		} catch (FileNotFoundException | NoSuchFileException ex) {
			//发生异常调用父类的readableChannel方法
			return super.readableChannel();
		}
	}

	/**
	 * 获取资源的内容长度
	 *
	 * @return 资源的内容长度
	 * @throws IOException IO异常
	 */
	@Override
	public long contentLength() throws IOException {
		//获取资源对应的URL
		URL url = getURL();
		//如果该URL是文件类型。
		if (ResourceUtils.isFileURL(url)) {
			//嗲用文件系统解析处理
			File file = getFile();
			long length = file.length();
			//如果文件长度为0，且文件不存在，抛出异常
			if (length == 0L && !file.exists()) {
				throw new FileNotFoundException(getDescription() +
						" cannot be resolved in the file system for checking its content length");
			}
			return length;
		} else {
			//进行URL请求，获取请求头中的content-length
			URLConnection con = url.openConnection();
			customizeConnection(con);
			return con.getContentLengthLong();
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
		//获取资源对应的URL
		URL url = getURL();
		boolean fileCheck = false;
		//如果该URL是一个文件类型或者Jar类型的URL
		if (ResourceUtils.isFileURL(url) || ResourceUtils.isJarURL(url)) {
			//调用文件系统解析处理
			fileCheck = true;
			try {
				//获取该URL对应File
				File fileToCheck = getFileForLastModifiedCheck();
				//通过File获取最后一次修改时间
				long lastModified = fileToCheck.lastModified();
				//如果最后一次修改时间戳大于0或者文件存在，返回该时间戳
				if (lastModified > 0L || fileToCheck.exists()) {
					return lastModified;
				}
			} catch (FileNotFoundException ex) {
				// Defensively fall back to URL connection check instead
			}
		}
		//通过URL连接，获取请求头中的last-modified
		URLConnection con = url.openConnection();
		customizeConnection(con);
		long lastModified = con.getLastModified();
		//如果它是一个文件夹、last-modified为0、内容长度为0，抛出异常
		if (fileCheck && lastModified == 0 && con.getContentLengthLong() <= 0) {
			throw new FileNotFoundException(getDescription() +
					" cannot be resolved in the file system for checking its last-modified timestamp");
		}
		return lastModified;
	}


	/**
	 * 定制化连接
	 *
	 * @param con URL连接
	 * @throws IOException IO异常
	 */
	protected void customizeConnection(URLConnection con) throws IOException {
		//在给定的连接上设置“useCaches”标志，最好设置为false，但对于基于JNLP的资源，将标志设置为true。
		ResourceUtils.useCachesIfNecessary(con);
		if (con instanceof HttpURLConnection) {
			//设置请求方法为HEAD方法
			customizeConnection((HttpURLConnection) con);
		}
	}

	/**
	 * 定制化连接，设置请求方法为HEAD方法
	 *
	 * @param con HttpUrl连接
	 * @throws IOException IO异常
	 */
	protected void customizeConnection(HttpURLConnection con) throws IOException {
		con.setRequestMethod("HEAD");
	}

	/**
	 * 内部的代理类，避免在运行时硬依赖JBoss VFS的API
	 */
	private static class VfsResourceDelegate {

		public static Resource getResource(URL url) throws IOException {
			return new VfsResource(VfsUtils.getRoot(url));
		}

		public static Resource getResource(URI uri) throws IOException {
			return new VfsResource(VfsUtils.getRoot(uri));
		}
	}

}
