/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.context.index;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.SpringProperties;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.ConcurrentReferenceHashMap;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentMap;

/**
 * 候选组件索引加载机制，仅供框架内部使用。
 *
 * @author Stephane Nicoll
 * @since 5.0
 */
public final class CandidateComponentsIndexLoader {

	/**
	 * 查找组件的位置。
	 * <p>可以存在于多个 JAR 文件中。
	 */
	public static final String COMPONENTS_RESOURCE_LOCATION = "META-INF/spring.components";

	/**
	 * 系统属性，用于指示 Spring 忽略组件索引，即始终从 {@link #loadIndex(ClassLoader)} 返回 {@code null}。
	 * <p>默认值为 "false"，允许正常使用索引。将此标志切换为 {@code true} 可满足边界情况，
	 * 即索引仅部分可用于某些库（或用例），但无法为整个应用程序构建索引。
	 * 在这种情况下，应用程序上下文将回退到常规的类路径安排（即就好像根本没有索引一样）。
	 */
	public static final String IGNORE_INDEX = "spring.index.ignore";


	private static final boolean shouldIgnoreIndex = SpringProperties.getFlag(IGNORE_INDEX);

	private static final Log logger = LogFactory.getLog(CandidateComponentsIndexLoader.class);

	private static final ConcurrentMap<ClassLoader, CandidateComponentsIndex> cache =
			new ConcurrentReferenceHashMap<>();


	private CandidateComponentsIndexLoader() {
	}


	/**
	 * 使用给定的类加载器从 {@value #COMPONENTS_RESOURCE_LOCATION} 加载并实例化 {@link CandidateComponentsIndex}。
	 * 如果没有可用的索引，则返回 {@code null}。
	 *
	 * @param classLoader 用于加载的类加载器（可以为 {@code null} 以使用默认的类加载器）
	 * @return 要使用的索引，如果未找到索引则返回 {@code null}
	 * @throws IllegalArgumentException 如果无法加载任何模块索引或在创建 {@link CandidateComponentsIndex} 时发生错误
	 */
	@Nullable
	public static CandidateComponentsIndex loadIndex(@Nullable ClassLoader classLoader) {
		ClassLoader classLoaderToUse = classLoader;
		if (classLoaderToUse == null) {
			classLoaderToUse = CandidateComponentsIndexLoader.class.getClassLoader();
		}
		//将候选索引信息加载进缓存中。
		return cache.computeIfAbsent(classLoaderToUse, CandidateComponentsIndexLoader::doLoadIndex);
	}

	@Nullable
	private static CandidateComponentsIndex doLoadIndex(ClassLoader classLoader) {
		if (shouldIgnoreIndex) {
			return null;
		}

		try {
			//从META-INF/spring.components加载Spring组件
			Enumeration<URL> urls = classLoader.getResources(COMPONENTS_RESOURCE_LOCATION);
			if (!urls.hasMoreElements()) {
				return null;
			}
			List<Properties> result = new ArrayList<>();
			while (urls.hasMoreElements()) {
				URL url = urls.nextElement();
				Properties properties = PropertiesLoaderUtils.loadProperties(new UrlResource(url));
				result.add(properties);
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Loaded " + result.size() + " index(es)");
			}
			int totalCount = result.stream().mapToInt(Properties::size).sum();
			return (totalCount > 0 ? new CandidateComponentsIndex(result) : null);
		} catch (IOException ex) {
			throw new IllegalStateException("Unable to load indexes from location [" +
					COMPONENTS_RESOURCE_LOCATION + "]", ex);
		}
	}

}
