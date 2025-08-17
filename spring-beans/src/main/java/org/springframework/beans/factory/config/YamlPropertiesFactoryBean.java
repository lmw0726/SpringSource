/*
 * Copyright 2002-2019 the original author or authors.
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
import org.springframework.core.CollectionFactory;
import org.springframework.lang.Nullable;

import java.util.Properties;

/**
 * 用于从 YAML 源读取的 {@link java.util.Properties} 工厂，
 * 以扁平化的字符串属性值结构暴露数据。
 *
 * <p>YAML 是一种易于人类阅读的配置格式，具有层级结构的属性特性。
 * 它或多或少是 JSON 的超集，因此具备许多类似功能。
 *
 * <p><b>注意：所有暴露的值均为 {@code String} 类型，方便通过
 * 通用的 {@link Properties#getProperty} 方法访问
 * （例如通过 {@link PropertyResourceConfigurer#setProperties(Properties)} 进行配置属性解析）。
 * 如果不需要此行为，请改用 {@link YamlMapFactoryBean}。</b>
 *
 * <p>该工厂创建的 Properties 会对层级对象使用嵌套路径，
 * 例如如下 YAML：
 *
 * <pre class="code">
 * environments:
 *   dev:
 *     url: https://dev.bar.com
 *     name: Developer Setup
 *   prod:
 *     url: https://foo.bar.com
 *     name: My Cool App
 * </pre>
 *
 * 会被转换成如下 Properties：
 *
 * <pre class="code">
 * environments.dev.url=https://dev.bar.com
 * environments.dev.name=Developer Setup
 * environments.prod.url=https://foo.bar.com
 * environments.prod.name=My Cool App
 * </pre>
 *
 * 列表会被拆分成带有 <code>[]</code> 访问符的属性键，例如：
 *
 * <pre class="code">
 * servers:
 * - dev.bar.com
 * - foo.bar.com
 * </pre>
 *
 * 会变成如下 Properties：
 *
 * <pre class="code">
 * servers[0]=dev.bar.com
 * servers[1]=foo.bar.com
 * </pre>
 *
 * <p>从 Spring Framework 5.0.6 起，需要 SnakeYAML 1.18 或更高版本。
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @since 4.1
 */
public class YamlPropertiesFactoryBean extends YamlProcessor implements FactoryBean<Properties>, InitializingBean {

	private boolean singleton = true;

	@Nullable
	private Properties properties;


	/**
	 * 设置是否创建单例对象，否则每次请求创建一个新对象。
	 * 默认值为 {@code true}（单例）。
	 */
	public void setSingleton(boolean singleton) {
		this.singleton = singleton;
	}

	@Override
	public boolean isSingleton() {
		return this.singleton;
	}

	@Override
	public void afterPropertiesSet() {
		if (isSingleton()) {
			this.properties = createProperties();
		}
	}

	@Override
	@Nullable
	public Properties getObject() {
		return (this.properties != null ? this.properties : createProperties());
	}

	@Override
	public Class<?> getObjectType() {
		return Properties.class;
	}


	/**
	 * 模板方法，子类可重写以构造此工厂返回的对象。
	 * 默认实现返回包含所有资源内容的 Properties。
	 * <p>如果是共享单例，则首次调用 {@link #getObject()} 时延迟调用；
	 * 否则，每次调用 {@link #getObject()} 时都会调用。
	 * @return 此工厂返回的对象
	 * @see #process(MatchCallback)
	 */
	protected Properties createProperties() {
		Properties result = CollectionFactory.createStringAdaptingProperties();
		process((properties, map) -> result.putAll(properties));
		return result;
	}

}
