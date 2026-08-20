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

package org.springframework.cache.concurrent;

import java.util.concurrent.ConcurrentMap;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * 在 Spring 容器中使用时，用于轻松配置 {@link ConcurrentMapCache} 的 {@link FactoryBean}。
 * 可以通过 bean 属性进行配置；默认使用分配给该 bean 的 Spring bean 名称作为缓存名称。
 *
 * <p>适用于测试或简单的缓存场景，通常与 {@link org.springframework.cache.support.SimpleCacheManager}
 * 配合使用，或通过 {@link ConcurrentMapCacheManager} 动态使用。
 *
 * @author Costin Leau
 * @author Juergen Hoeller
 * @since 3.1
 */
public class ConcurrentMapCacheFactoryBean
		implements FactoryBean<ConcurrentMapCache>, BeanNameAware, InitializingBean {

	private String name = "";

	@Nullable
	private ConcurrentMap<Object, Object> store;

	private boolean allowNullValues = true;

	@Nullable
	private ConcurrentMapCache cache;


	/**
	 * 指定缓存的名称。
	 * <p>默认为 ""（空字符串）。
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * 指定用作内部存储的 ConcurrentMap（可以预先填充数据）。
	 * <p>默认为标准的 {@link java.util.concurrent.ConcurrentHashMap}。
	 */
	public void setStore(ConcurrentMap<Object, Object> store) {
		this.store = store;
	}

	/**
	 * 设置是否允许 {@code null} 值（将其适配为内部的 null 占位值）。
	 * <p>默认为 "true"。
	 */
	public void setAllowNullValues(boolean allowNullValues) {
		this.allowNullValues = allowNullValues;
	}

	@Override
	public void setBeanName(String beanName) {
		if (!StringUtils.hasLength(this.name)) {
			setName(beanName);
		}
	}

	@Override
	public void afterPropertiesSet() {
		this.cache = (this.store != null ? new ConcurrentMapCache(this.name, this.store, this.allowNullValues) :
				new ConcurrentMapCache(this.name, this.allowNullValues));
	}


	@Override
	@Nullable
	public ConcurrentMapCache getObject() {
		return this.cache;
	}

	@Override
	public Class<?> getObjectType() {
		return ConcurrentMapCache.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
