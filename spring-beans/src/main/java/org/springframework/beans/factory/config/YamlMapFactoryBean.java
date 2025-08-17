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
import org.springframework.lang.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 用于从 YAML 源读取的 {@code Map} 工厂，保留 YAML 声明的值类型及其结构。
 *
 * <p>YAML 是一种易于人类阅读的配置格式，具有层级结构的属性特性。
 * 它或多或少是 JSON 的超集，因此具备许多类似功能。
 *
 * <p>如果提供了多个资源，后面的资源会分层覆盖前面的资源；
 * 也就是说，任意深度的同名嵌套 {@code Map} 类型条目会被合并。例如：
 *
 * <pre class="code">
 * foo:
 *   bar:
 *    one: two
 * three: four
 * </pre>
 *
 * 加上（列表中较后的）
 *
 * <pre class="code">
 * foo:
 *   bar:
 *    one: 2
 * five: six
 * </pre>
 *
 * 最终结果是：
 *
 * <pre class="code">
 * foo:
 *   bar:
 *    one: 2
 * three: four
 * five: six
 * </pre>
 *
 * 注意，第一个文档中 "foo" 的值不会被第二个文档中的值简单替换，
 * 而是其嵌套值被合并。
 *
 * <p>从 Spring Framework 5.0.6 起，需要 SnakeYAML 1.18 或更高版本。
 *
 * @author Dave Syer
 * @author Juergen Hoeller
 * @since 4.1
 */
public class YamlMapFactoryBean extends YamlProcessor implements FactoryBean<Map<String, Object>>, InitializingBean {

	private boolean singleton = true;

	@Nullable
	private Map<String, Object> map;


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
			this.map = createMap();
		}
	}

	@Override
	@Nullable
	public Map<String, Object> getObject() {
		return (this.map != null ? this.map : createMap());
	}

	@Override
	public Class<?> getObjectType() {
		return Map.class;
	}


	/**
	 * 模板方法，子类可重写以构造此工厂返回的对象。
	 * <p>如果是共享单例，则首次调用 {@link #getObject()} 时延迟调用；
	 * 否则，每次调用 {@link #getObject()} 时都会调用。
	 * <p>默认实现返回合并后的 {@code Map} 实例。
	 * @return 此工厂返回的对象
	 * @see #process(MatchCallback)
	 */
	protected Map<String, Object> createMap() {
		Map<String, Object> result = new LinkedHashMap<>();
		process((properties, map) -> merge(result, map));
		return result;
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	private void merge(Map<String, Object> output, Map<String, Object> map) {
		map.forEach((key, value) -> {
			Object existing = output.get(key);
			if (value instanceof Map && existing instanceof Map) {
				// Eclipse IDE需要内部强制转换。
				Map<String, Object> result = new LinkedHashMap<>((Map<String, Object>) existing);
				merge(result, (Map) value);
				output.put(key, result);
			}
			else {
				output.put(key, value);
			}
		});
	}

}
