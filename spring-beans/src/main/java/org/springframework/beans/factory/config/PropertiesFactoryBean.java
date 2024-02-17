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

package org.springframework.beans.factory.config;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.support.PropertiesLoaderSupport;
import org.springframework.lang.Nullable;

import java.io.IOException;
import java.util.Properties;

/**
 * 允许将类路径位置的属性文件作为 Properties 实例在 bean 工厂中可用。可用于通过 bean 引用填充任何类型为 Properties 的 bean 属性。
 *
 * <p>支持从属性文件加载和/或在此 FactoryBean 上设置本地属性。创建的 Properties 实例将从加载的值和本地值合并。如果未设置位置或本地属性，则在初始化时将抛出异常。
 *
 * <p>可以创建一个单例或每次请求时都创建一个新对象。默认为单例。
 *
 * @author Juergen Hoeller
 * @see #setLocation
 * @see #setProperties
 * @see #setLocalOverride
 * @see java.util.Properties
 */
public class PropertiesFactoryBean extends PropertiesLoaderSupport
		implements FactoryBean<Properties>, InitializingBean {

	/**
	 * 是否是单例，默认为单例
	 */
	private boolean singleton = true;

	/**
	 * 单例实例属性
	 */
	@Nullable
	private Properties singletonInstance;


	/**
	 * 设置是否应创建共享的“单例”Properties实例，还是在每个请求上创建一个新的Properties实例。
	 * <p>默认为“true”（共享单例）。
	 */
	public final void setSingleton(boolean singleton) {
		this.singleton = singleton;
	}

	@Override
	public final boolean isSingleton() {
		return this.singleton;
	}


	@Override
	public final void afterPropertiesSet() throws IOException {
		if (this.singleton) {
			this.singletonInstance = createProperties();
		}
	}

	@Override
	@Nullable
	public final Properties getObject() throws IOException {
		if (this.singleton) {
			return this.singletonInstance;
		} else {
			return createProperties();
		}
	}

	@Override
	public Class<Properties> getObjectType() {
		return Properties.class;
	}


	/**
	 * 模板方法，子类可以重写以构造此工厂返回的对象。默认实现返回普通的合并Properties实例。
	 * <p>在共享单例的情况下，在此FactoryBean初始化期间调用；否则，在每次 {@link #getObject()} 调用时调用。
	 *
	 * @return 此工厂返回的对象
	 * @throws IOException 如果在加载属性时发生异常
	 * @see #mergeProperties()
	 */
	protected Properties createProperties() throws IOException {
		return mergeProperties();
	}

}
