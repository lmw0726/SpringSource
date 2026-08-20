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

package org.springframework.cache.annotation;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.AdviceModeImportSelector;
import org.springframework.context.annotation.AutoProxyRegistrar;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * 根据导入的 {@code @Configuration} 类上 {@link EnableCaching#mode} 的值，
 * 选择应使用哪个 {@link AbstractCachingConfiguration} 实现。
 *
 * <p>检测 JSR-107 是否存在，并相应地启用 JCache 支持。
 *
 * @author Chris Beams
 * @author Stephane Nicoll
 * @since 3.1
 * @see EnableCaching
 * @see ProxyCachingConfiguration
 */
public class CachingConfigurationSelector extends AdviceModeImportSelector<EnableCaching> {

	private static final String PROXY_JCACHE_CONFIGURATION_CLASS =
			"org.springframework.cache.jcache.config.ProxyJCacheConfiguration";

	private static final String CACHE_ASPECT_CONFIGURATION_CLASS_NAME =
			"org.springframework.cache.aspectj.AspectJCachingConfiguration";

	private static final String JCACHE_ASPECT_CONFIGURATION_CLASS_NAME =
			"org.springframework.cache.aspectj.AspectJJCacheConfiguration";


	private static final boolean jsr107Present;

	private static final boolean jcacheImplPresent;

	static {
		ClassLoader classLoader = CachingConfigurationSelector.class.getClassLoader();
		jsr107Present = ClassUtils.isPresent("javax.cache.Cache", classLoader);
		jcacheImplPresent = ClassUtils.isPresent(PROXY_JCACHE_CONFIGURATION_CLASS, classLoader);
	}


	/**
	 * 分别针对 {@link EnableCaching#mode()} 的 {@code PROXY} 和 {@code ASPECTJ} 值，
	 * 返回 {@link ProxyCachingConfiguration} 或 {@code AspectJCachingConfiguration}。
	 * 也可能包含相应的 JCache 配置。
	 */
	@Override
	public String[] selectImports(AdviceMode adviceMode) {
		switch (adviceMode) {
			case PROXY:
				return getProxyImports();
			case ASPECTJ:
				return getAspectJImports();
			default:
				return null;
		}
	}

	/**
	 * 当 {@link AdviceMode} 设置为 {@link AdviceMode#PROXY} 时返回要使用的导入。
	 * <p>如果 JSR-107 可用，则负责添加必要的 JSR-107 导入。
	 */
	private String[] getProxyImports() {
		List<String> result = new ArrayList<>(3);
		result.add(AutoProxyRegistrar.class.getName());
		result.add(ProxyCachingConfiguration.class.getName());
		if (jsr107Present && jcacheImplPresent) {
			result.add(PROXY_JCACHE_CONFIGURATION_CLASS);
		}
		return StringUtils.toStringArray(result);
	}

	/**
	 * 当 {@link AdviceMode} 设置为 {@link AdviceMode#ASPECTJ} 时返回要使用的导入。
	 * <p>如果 JSR-107 可用，则负责添加必要的 JSR-107 导入。
	 */
	private String[] getAspectJImports() {
		List<String> result = new ArrayList<>(2);
		result.add(CACHE_ASPECT_CONFIGURATION_CLASS_NAME);
		if (jsr107Present && jcacheImplPresent) {
			result.add(JCACHE_ASPECT_CONFIGURATION_CLASS_NAME);
		}
		return StringUtils.toStringArray(result);
	}

}
