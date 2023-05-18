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

package org.springframework.web.context.support;

import org.springframework.core.io.AbstractFileResolvingResource;
import org.springframework.core.io.ContextResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.WebUtils;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * {@link org.springframework.core.io.Resource} implementation for
 * {@link javax.servlet.ServletContext} resources, interpreting
 * relative paths within the web application root directory.
 *
 * <p>Always supports stream access and URL access, but only allows
 * {@code java.io.File} access when the web application archive
 * is expanded.
 *
 * @author Juergen Hoeller
 * @see javax.servlet.ServletContext#getResourceAsStream
 * @see javax.servlet.ServletContext#getResource
 * @see javax.servlet.ServletContext#getRealPath
 * @since 28.12.2003
 */
public class ServletContextResource extends AbstractFileResolvingResource implements ContextResource {
	/**
	 * Servlet上下文
	 */
	private final ServletContext servletContext;

	/**
	 * 路径
	 */
	private final String path;


	public ServletContextResource(ServletContext servletContext, String path) {
		Assert.notNull(servletContext, "Cannot resolve ServletContextResource without ServletContext");
		this.servletContext = servletContext;

		Assert.notNull(path, "Path is required");
		String pathToUse = StringUtils.cleanPath(path);
		if (!pathToUse.startsWith("/")) {
			pathToUse = "/" + pathToUse;
		}
		this.path = pathToUse;
	}


	/**
	 * 获取Servlet上下文
	 *
	 * @return Servlet上下文
	 */
	public final ServletContext getServletContext() {
		return this.servletContext;
	}

	/**
	 * 返回此资源的路径
	 *
	 * @return 这个资源的路径
	 */
	public final String getPath() {
		return this.path;
	}

	/**
	 * 资源是否存在
	 *
	 * @return true表示资源存在
	 */
	@Override
	public boolean exists() {
		try {
			URL url = this.servletContext.getResource(this.path);
			return (url != null);
		} catch (MalformedURLException ex) {
			return false;
		}
	}

	/**
	 * 资源是否可读，获取它的文件输入流，看他是否为空
	 *
	 * @return true表示资源可读
	 */
	@Override
	public boolean isReadable() {
		InputStream is = this.servletContext.getResourceAsStream(this.path);
		if (is != null) {
			try {
				is.close();
			} catch (IOException ex) {

			}
			return true;
		} else {
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
			//获取URL，如果该URL不为空且URL是一个文件，返回true
			URL url = this.servletContext.getResource(this.path);
			if (url != null && ResourceUtils.isFileURL(url)) {
				return true;
			} else {
				//通过路径进行判断判断它的真实路径是否不为空
				return (this.servletContext.getRealPath(this.path) != null);
			}
		} catch (MalformedURLException ex) {
			return false;
		}
	}

	/**
	 * 获取文件输入流
	 *
	 * @return 文件输入流
	 * @throws IOException IO异常
	 */
	@Override
	public InputStream getInputStream() throws IOException {
		InputStream is = this.servletContext.getResourceAsStream(this.path);
		if (is == null) {
			throw new FileNotFoundException("Could not open " + getDescription());
		}
		return is;
	}

	/**
	 * 获取URL
	 *
	 * @return URL
	 * @throws IOException IO异常
	 */
	@Override
	public URL getURL() throws IOException {
		URL url = this.servletContext.getResource(this.path);
		if (url == null) {
			throw new FileNotFoundException(
					getDescription() + " cannot be resolved to URL because it does not exist");
		}
		return url;
	}

	/**
	 * 获取资源的文件表示形式
	 *
	 * @return 文件
	 * @throws IOException IO异常
	 */
	@Override
	public File getFile() throws IOException {
		//获取URL，如果URL不为空，且它是一个文件类型的URL
		URL url = this.servletContext.getResource(this.path);
		if (url != null && ResourceUtils.isFileURL(url)) {
			// 由文件系统进行处理，调用父类AbstractFileResolvingResource.getFile()进行处理
			return super.getFile();
		} else {
			//通过路径获取该文件
			String realPath = WebUtils.getRealPath(this.servletContext, this.path);
			return new File(realPath);
		}
	}

	/**
	 * 创建一个相关的资源
	 *
	 * @param relativePath 相对路径
	 * @return 相关的ServletResource
	 */
	@Override
	public Resource createRelative(String relativePath) {
		String pathToUse = StringUtils.applyRelativePath(this.path, relativePath);
		return new ServletContextResource(this.servletContext, pathToUse);
	}

	/**
	 * 获取文件名
	 *
	 * @return 文件名
	 */
	@Override
	@Nullable
	public String getFilename() {
		return StringUtils.getFilename(this.path);
	}

	/**
	 * 获取资源的描述
	 *
	 * @return 资源的描述
	 */
	@Override
	public String getDescription() {
		return "ServletContext resource [" + this.path + "]";
	}

	/**
	 * 获取上下文中的路径
	 *
	 * @return 上下文中的路径
	 */
	@Override
	public String getPathWithinContext() {
		return this.path;
	}

	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof ServletContextResource)) {
			return false;
		}
		ServletContextResource otherRes = (ServletContextResource) other;
		return (this.servletContext.equals(otherRes.servletContext) && this.path.equals(otherRes.path));
	}

	@Override
	public int hashCode() {
		return this.path.hashCode();
	}

}
