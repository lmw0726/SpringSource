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

package org.springframework.scripting.support;

import java.io.IOException;
import java.io.Reader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.lang.Nullable;
import org.springframework.scripting.ScriptSource;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

/**
 * 基于 Spring 的 {@link org.springframework.core.io.Resource} 抽象的
 * {@link org.springframework.scripting.ScriptSource} 实现。
 * 从底层资源的 {@link org.springframework.core.io.Resource#getFile() File} 或
 * {@link org.springframework.core.io.Resource#getInputStream() InputStream}
 * 加载脚本文本，并跟踪文件的最后修改时间戳（如果可能）。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.springframework.core.io.Resource#getInputStream()
 * @see org.springframework.core.io.Resource#getFile()
 * @see org.springframework.core.io.ResourceLoader
 */
public class ResourceScriptSource implements ScriptSource {

	/** 子类可用的日志记录器。 */
	protected final Log logger = LogFactory.getLog(getClass());

	private EncodedResource resource;

	private long lastModified = -1;

	private final Object lastModifiedMonitor = new Object();


	/**
	 * 为给定资源创建新的 ResourceScriptSource。
	 * @param resource 从中加载脚本的 EncodedResource
	 */
	public ResourceScriptSource(EncodedResource resource) {
		Assert.notNull(resource, "Resource must not be null");
		this.resource = resource;
	}

	/**
	 * 为给定资源创建新的 ResourceScriptSource。
	 * @param resource 从中加载脚本的 Resource（使用 UTF-8 编码）
	 */
	public ResourceScriptSource(Resource resource) {
		Assert.notNull(resource, "Resource must not be null");
		this.resource = new EncodedResource(resource, "UTF-8");
	}


	/**
	 * 返回用于加载脚本的 {@link org.springframework.core.io.Resource}。
	 */
	public final Resource getResource() {
		return this.resource.getResource();
	}

	/**
	 * 设置用于读取脚本资源的编码。
	 * <p>对于普通资源，默认值为 "UTF-8"。
	 * {@code null} 值表示使用平台默认编码。
	 */
	public void setEncoding(@Nullable String encoding) {
		this.resource = new EncodedResource(this.resource.getResource(), encoding);
	}


	@Override
	public String getScriptAsString() throws IOException {
		synchronized (this.lastModifiedMonitor) {
			this.lastModified = retrieveLastModifiedTime();
		}
		Reader reader = this.resource.getReader();
		return FileCopyUtils.copyToString(reader);
	}

	@Override
	public boolean isModified() {
		synchronized (this.lastModifiedMonitor) {
			return (this.lastModified < 0 || retrieveLastModifiedTime() > this.lastModified);
		}
	}

	/**
	 * 获取底层资源的当前最后修改时间戳。
	 * @return 当前时间戳，如果无法确定则返回 0
	 */
	protected long retrieveLastModifiedTime() {
		try {
			return getResource().lastModified();
		}
		catch (IOException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug(getResource() + " could not be resolved in the file system - " +
						"current timestamp not available for script modification check", ex);
			}
			return 0;
		}
	}

	@Override
	@Nullable
	public String suggestedClassName() {
		String filename = getResource().getFilename();
		return (filename != null ? StringUtils.stripFilenameExtension(filename) : null);
	}

	@Override
	public String toString() {
		return this.resource.toString();
	}

}
