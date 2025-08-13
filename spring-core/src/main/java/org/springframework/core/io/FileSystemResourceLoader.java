/*
 * Copyright 2002-2017 the original author or authors.
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

/**
 * {@link ResourceLoader} 的实现类，将普通路径解析为文件系统资源，
 * 而不是类路径资源（后者是 {@link DefaultResourceLoader} 的默认策略）。
 *
 * <p><b>注意：</b> 普通路径始终会被解释为相对于当前 VM 工作目录的路径，
 * 即使它们以斜杠开头也是如此。（这与 Servlet 容器中的语义一致。）
 * <b>使用显式的 "file:" 前缀可强制使用绝对文件路径。</b>
 *
 * <p>{@link org.springframework.context.support.FileSystemXmlApplicationContext}
 * 是一个功能完整的 ApplicationContext 实现，提供了相同的资源路径解析策略。
 *
 * @author Juergen Hoeller
 * @since 1.1.3
 * @see DefaultResourceLoader
 * @see org.springframework.context.support.FileSystemXmlApplicationContext
 */
public class FileSystemResourceLoader extends DefaultResourceLoader {

	/**
	 * 将资源路径解析为文件系统路径。
	 * <p>注意：即使给定路径以斜杠开头，
	 * 也会被解释为相对于当前 VM 工作目录的路径。
	 * @param path 资源路径
	 * @return 对应的 Resource 句柄
	 * @see FileSystemResource
	 * @see org.springframework.web.context.support.ServletContextResourceLoader#getResourceByPath
	 */
	@Override
	protected Resource getResourceByPath(String path) {
		if (path.startsWith("/")) {
			path = path.substring(1);
		}
		return new FileSystemContextResource(path);
	}


	/**
	 * 明确表达上下文相对路径的 FileSystemResource，
	 * 通过实现 ContextResource 接口来实现。
	 */
	private static class FileSystemContextResource extends FileSystemResource implements ContextResource {

		public FileSystemContextResource(String path) {
			super(path);
		}

		@Override
		public String getPathWithinContext() {
			return getPath();
		}
	}

}
