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

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;

/**
 * {@link Resource} implementation for {@code java.net.URL} locators.
 * Supports resolution as a {@code URL} and also as a {@code File} in
 * case of the {@code "file:"} protocol.
 *
 * @author Juergen Hoeller
 * @see java.net.URL
 * @since 28.12.2003
 */
public class UrlResource extends AbstractFileResolvingResource {

	@Nullable
	/**
	 * 统一资源标识符，用于URI和访问文件
	 */
	private final URI uri;

	/**
	 * 统一资源定位符，用于实际访问
	 */
	private final URL url;

	/**
	 * 干净的URL（规范化的路径），用于比较
	 */
	@Nullable
	private volatile URL cleanedUrl;


	public UrlResource(URL url) {
		Assert.notNull(url, "URL must not be null");
		this.uri = null;
		this.url = url;
	}


	public UrlResource(URI uri) throws MalformedURLException {
		Assert.notNull(uri, "URI must not be null");
		this.uri = uri;
		this.url = uri.toURL();
	}


	public UrlResource(String path) throws MalformedURLException {
		Assert.notNull(path, "Path must not be null");
		this.uri = null;
		this.url = new URL(path);
		this.cleanedUrl = getCleanedUrl(this.url, path);
	}


	public UrlResource(String protocol, String location) throws MalformedURLException {
		this(protocol, location, null);
	}


	public UrlResource(String protocol, String location, @Nullable String fragment) throws MalformedURLException {
		try {
			this.uri = new URI(protocol, location, fragment);
			this.url = this.uri.toURL();
		} catch (URISyntaxException ex) {
			MalformedURLException exToThrow = new MalformedURLException(ex.getMessage());
			exToThrow.initCause(ex);
			throw exToThrow;
		}
	}


	/**
	 * 获取原始URL的清理后的URL
	 *
	 * @param originalUrl  原始URL
	 * @param originalPath 目标路径
	 * @return 清理后的URL
	 */
	private static URL getCleanedUrl(URL originalUrl, String originalPath) {
		//清理后的路径
		String cleanedPath = StringUtils.cleanPath(originalPath);
		//如果清理后的路径不是给定的原始路径
		if (!cleanedPath.equals(originalPath)) {
			try {
				//以清理后的路径构建一个URL
				return new URL(cleanedPath);
			} catch (MalformedURLException ex) {
			}
		}
		//否则原始URL
		return originalUrl;
	}

	/**
	 * 懒加载一个清理后的URL
	 *
	 * @return 清理后的URL
	 */
	private URL getCleanedUrl() {
		URL cleanedUrl = this.cleanedUrl;
		if (cleanedUrl != null) {
			return cleanedUrl;
		}
		cleanedUrl = getCleanedUrl(this.url, (this.uri != null ? this.uri : this.url).toString());
		this.cleanedUrl = cleanedUrl;
		return cleanedUrl;
	}


	/**
	 * 获取输入流
	 *
	 * @return 输入流
	 * @throws IOException IO异常
	 */
	@Override
	public InputStream getInputStream() throws IOException {
		//建立URL连接，并获取其输入流
		URLConnection con = this.url.openConnection();
		ResourceUtils.useCachesIfNecessary(con);
		try {
			return con.getInputStream();
		} catch (IOException ex) {
			//关闭HTTP连接
			if (con instanceof HttpURLConnection) {
				((HttpURLConnection) con).disconnect();
			}
			throw ex;
		}
	}

	/**
	 * 获取URL
	 */
	@Override
	public URL getURL() {
		return this.url;
	}

	/**
	 * 获取URI
	 *
	 * @return 统一资源标识符
	 * @throws IOException IO异常
	 */
	@Override
	public URI getURI() throws IOException {
		if (this.uri != null) {
			return this.uri;
		} else {
			return super.getURI();
		}
	}

	@Override
	public boolean isFile() {
		if (this.uri != null) {
			return super.isFile(this.uri);
		} else {
			return super.isFile();
		}
	}


	/**
	 * 获取文件，调用父类（AbstractFileResolvingResource的getFile方法）
	 *
	 * @return 资源的文件表现形式
	 * @throws IOException IO异常
	 */
	@Override
	public File getFile() throws IOException {
		if (this.uri != null) {
			return super.getFile(this.uri);
		} else {
			return super.getFile();
		}
	}

	/**
	 * 创建一个相关的资源
	 *
	 * @param relativePath 相对路径
	 * @return 相关的路径
	 * @throws MalformedURLException 畸形的URL异常
	 */
	@Override
	public Resource createRelative(String relativePath) throws MalformedURLException {
		return new UrlResource(createRelativeURL(relativePath));
	}

	/**
	 * 创建一个相关的URL
	 *
	 * @param relativePath 相对路径
	 * @return 相关的URL
	 * @throws MalformedURLException 畸形的URL异常
	 */
	protected URL createRelativeURL(String relativePath) throws MalformedURLException {
		//相关路径以/开头，截取/后的部分。
		if (relativePath.startsWith("/")) {
			relativePath = relativePath.substring(1);
		}
		//将相关路径中的#替换为%23
		relativePath = StringUtils.replace(relativePath, "#", "%23");
		//构建URL
		return new URL(this.url, relativePath);
	}

	/**
	 * 获取文件名
	 *
	 * @return 文件名
	 */
	@Override
	public String getFilename() {
		return StringUtils.getFilename(getCleanedUrl().getPath());
	}

	/**
	 * 获取描述
	 *
	 * @return 资源描述
	 */
	@Override
	public String getDescription() {
		return "URL [" + this.url + "]";
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof UrlResource &&
				getCleanedUrl().equals(((UrlResource) other).getCleanedUrl())));
	}

	@Override
	public int hashCode() {
		return getCleanedUrl().hashCode();
	}

}
