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

package org.springframework.web.context.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * ServletContextResourcePatternResolver 是 {@link PathMatchingResourcePatternResolver} 的 ServletContext 感知子类，
 * 能够通过 {@link ServletContext#getResourcePaths} 查找位于 Web 应用程序根目录下的匹配资源。
 * 对于其他资源，会回退到超类的文件系统检查。
 *
 * @author Juergen Hoeller
 * @since 1.1.2
 */
public class ServletContextResourcePatternResolver extends PathMatchingResourcePatternResolver {
	/**
	 * 日志记录器
	 */
	private static final Log logger = LogFactory.getLog(ServletContextResourcePatternResolver.class);


	/**
	 * 创建一个新的 Servlet上下文资源模式解析器。
	 *
	 * @param servletContext 用于加载资源的 ServletContext
	 * @see ServletContextResourceLoader#ServletContextResourceLoader(javax.servlet.ServletContext)
	 */
	public ServletContextResourcePatternResolver(ServletContext servletContext) {
		super(new ServletContextResourceLoader(servletContext));
	}

	/**
	 * 创建一个新的 Servlet上下文资源模式解析器。
	 *
	 * @param resourceLoader 用于加载根目录和实际资源的 ResourceLoader
	 */
	public ServletContextResourcePatternResolver(ResourceLoader resourceLoader) {
		super(resourceLoader);
	}


	/**
	 * 重写的版本，检查 ServletContextResource 并使用 {@code ServletContext.getResourcePaths} 查找 Web 应用程序根目录下的匹配资源。
	 * 对于其他资源，委托给超类版本。
	 *
	 * @see #doRetrieveMatchingServletContextResources
	 * @see ServletContextResource
	 * @see javax.servlet.ServletContext#getResourcePaths
	 */
	@Override
	protected Set<Resource> doFindPathMatchingFileResources(Resource rootDirResource, String subPattern)
			throws IOException {

		// 如果根目录资源是 Servlet上下文资源 类型
		if (rootDirResource instanceof ServletContextResource) {
			// 将根目录资源转换为 Servlet上下文资源 类型
			ServletContextResource scResource = (ServletContextResource) rootDirResource;
			// 获取 Servlet上下文 对象
			ServletContext sc = scResource.getServletContext();
			// 构建完整的模式路径
			String fullPattern = scResource.getPath() + subPattern;
			// 创建结果集合
			Set<Resource> result = new LinkedHashSet<>(8);
			// 获取匹配的 Servlet上下文 资源
			doRetrieveMatchingServletContextResources(sc, fullPattern, scResource.getPath(), result);
			return result;
		} else {
			// 否则调用父类方法查找匹配的文件资源
			return super.doFindPathMatchingFileResources(rootDirResource, subPattern);
		}
	}

	/**
	 * 递归检索匹配给定模式的 Servlet上下文 资源，并将它们添加到给定的结果集中。
	 *
	 * @param servletContext 要处理的 ServletContext
	 * @param fullPattern    要与之匹配的模式，带有前置的根目录路径
	 * @param dir            当前目录
	 * @param result         要添加到的匹配资源的集合
	 * @throws IOException 如果无法检索目录内容
	 * @see ServletContextResource
	 * @see javax.servlet.ServletContext#getResourcePaths
	 */
	protected void doRetrieveMatchingServletContextResources(
			ServletContext servletContext, String fullPattern, String dir, Set<Resource> result)
			throws IOException {

		// 获取目录下的所有资源路径
		Set<String> candidates = servletContext.getResourcePaths(dir);
		if (candidates != null) {
			// 检查完整模式是否包含通配符"**"
			boolean dirDepthNotFixed = fullPattern.contains("**");
			// 查找完整模式中的JAR文件分隔符
			int jarFileSep = fullPattern.indexOf(ResourceUtils.JAR_URL_SEPARATOR);
			String jarFilePath = null;
			String pathInJarFile = null;
			// 检查模式中是否存在JAR文件分隔符
			if (jarFileSep > 0 && jarFileSep + ResourceUtils.JAR_URL_SEPARATOR.length() < fullPattern.length()) {
				// 截取JAR文件路径和JAR文件内路径
				jarFilePath = fullPattern.substring(0, jarFileSep);
				pathInJarFile = fullPattern.substring(jarFileSep + ResourceUtils.JAR_URL_SEPARATOR.length());
			}
			// 遍历资源候选列表
			for (String currPath : candidates) {
				if (!currPath.startsWith(dir)) {
					// 如果返回的资源路径不是以相对目录开头，则假设返回的是绝对路径 -> 去除绝对路径
					int dirIndex = currPath.indexOf(dir);
					if (dirIndex != -1) {
						currPath = currPath.substring(dirIndex);
					}
				}
				if (currPath.endsWith("/") && (dirDepthNotFixed || StringUtils.countOccurrencesOf(currPath, "/") <=
						StringUtils.countOccurrencesOf(fullPattern, "/"))) {
					// 如果资源路径是目录且目录深度未固定或者当前路径深度小于等于模式路径深度，则递归搜索子目录
					doRetrieveMatchingServletContextResources(servletContext, fullPattern, currPath, result);
				}
				if (jarFilePath != null && getPathMatcher().match(jarFilePath, currPath)) {
					// 如果基本模式匹配到一个JAR文件，则在其中搜索匹配的条目
					String absoluteJarPath = servletContext.getRealPath(currPath);
					if (absoluteJarPath != null) {
						doRetrieveMatchingJarEntries(absoluteJarPath, pathInJarFile, result);
					}
				}
				if (getPathMatcher().match(fullPattern, currPath)) {
					// 如果资源路径匹配到完整模式，则将其添加到结果集中
					result.add(new ServletContextResource(servletContext, currPath));
				}
			}
		}
	}

	/**
	 * 从给定的 jar 中提取与模式匹配的条目。
	 *
	 * @param jarFilePath  jar 文件的路径
	 * @param entryPattern 用于匹配 jar 条目的模式
	 * @param result       要添加到的匹配资源的集合
	 */
	private void doRetrieveMatchingJarEntries(String jarFilePath, String entryPattern, Set<Resource> result) {
		// 如果日志级别是调试
		if (logger.isDebugEnabled()) {
			// 输出正在搜索的 JAR 文件及匹配的条目
			logger.debug("Searching jar file [" + jarFilePath + "] for entries matching [" + entryPattern + "]");
		}
		try (JarFile jarFile = new JarFile(jarFilePath)) {
			// 遍历 JAR 文件中的条目
			for (Enumeration<JarEntry> entries = jarFile.entries(); entries.hasMoreElements(); ) {
				JarEntry entry = entries.nextElement();
				String entryPath = entry.getName();
				// 如果条目的路径与 匹配 jar 条目的模式 匹配
				if (getPathMatcher().match(entryPattern, entryPath)) {
					// 将匹配的资源添加到结果集中
					result.add(new UrlResource(
							ResourceUtils.URL_PROTOCOL_JAR,
							ResourceUtils.FILE_URL_PREFIX + jarFilePath + ResourceUtils.JAR_URL_SEPARATOR + entryPath));
				}
			}
		} catch (IOException ex) {
			// 如果无法打开 JAR 文件，记录警告信息
			if (logger.isWarnEnabled()) {
				logger.warn("Cannot search for matching resources in jar file [" + jarFilePath +
						"] because the jar cannot be opened through the file system", ex);
			}
		}
	}

}
