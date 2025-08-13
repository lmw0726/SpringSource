/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.core.env;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.Map;

/**
 * {@link MapPropertySource} 的一个特化实现，设计用于
 * {@linkplain AbstractEnvironment#getSystemEnvironment() 系统环境变量}。
 * 该类解决了 Bash 及其他 shell 对变量名中不允许包含点号（.）和/或连字符（-）的限制；
 * 同时允许属性名称使用大写字母，以符合更惯用的 shell 习惯。
 *
 * <p>例如，调用 {@code getProperty("foo.bar")} 会尝试查找原始属性名或任何“等效”的属性，
 * 并返回第一个找到的结果：
 * <ul>
 * <li>{@code foo.bar} - 原始名称</li>
 * <li>{@code foo_bar} - 将点号替换为下划线后的名称（如果有）</li>
 * <li>{@code FOO.BAR} - 原始名称的大写形式</li>
 * <li>{@code FOO_BAR} - 使用下划线和大写的名称</li>
 * </ul>
 * 上述名称的任何连字符变体也同样有效，甚至可以混合点号和连字符变体。
 *
 * <p>{@link #containsProperty(String)} 方法同样适用上述规则，
 * 只要存在上述任一属性名就返回 {@code true}，否则返回 {@code false}。
 *
 * <p>该特性在通过环境变量指定激活或默认配置文件时尤其有用。Bash 中以下写法是不允许的：
 *
 * <pre class="code">spring.profiles.active=p1 java -classpath ... MyApp</pre>
 *
 * 而下面这种写法是允许的，也更符合惯例：
 *
 * <pre class="code">SPRING_PROFILES_ACTIVE=p1 java -classpath ... MyApp</pre>
 *
 * <p>开启该类（或其包）的调试或追踪日志级别，可以看到何时发生这些“属性名解析”的消息。
 *
 * <p>该属性源默认包含在 {@link StandardEnvironment} 及其所有子类中。
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.1
 * @see StandardEnvironment
 * @see AbstractEnvironment#getSystemEnvironment()
 * @see AbstractEnvironment#ACTIVE_PROFILES_PROPERTY_NAME
 */
public class SystemEnvironmentPropertySource extends MapPropertySource {

	/**
	 * 使用给定名称创建一个新的 {@code SystemEnvironmentPropertySource}，并代理给定的 {@code MapPropertySource}。
	 */
	public SystemEnvironmentPropertySource(String name, Map<String, Object> source) {
		super(name, source);
	}


	/**
	 * 如果此属性源包含给定名称的属性，或其任何下划线/大写变体，则返回 {@code true}。
	 */
	@Override
	public boolean containsProperty(String name) {
		return (getProperty(name) != null);
	}

	/**
	 * 实现中，如果此属性源包含给定名称的属性，或其任何下划线/大写变体，则返回 {@code true}。
	 */
	@Override
	@Nullable
	public Object getProperty(String name) {
		String actualName = resolvePropertyName(name);
		if (logger.isDebugEnabled() && !name.equals(actualName)) {
			logger.debug("PropertySource '" + getName() + "' does not contain property '" + name +
					"', but found equivalent '" + actualName + "'");
		}
		return super.getProperty(actualName);
	}

	/**
	 * 检查此属性源是否包含给定名称的属性，或其任何下划线/大写变体。
	 * 如果找到，则返回解析后的名称；否则返回原始名称。永远不返回 {@code null}。
	 */
	protected final String resolvePropertyName(String name) {
		Assert.notNull(name, "Property name must not be null");
		String resolvedName = checkPropertyName(name);
		if (resolvedName != null) {
			return resolvedName;
		}
		String uppercasedName = name.toUpperCase();
		if (!name.equals(uppercasedName)) {
			resolvedName = checkPropertyName(uppercasedName);
			if (resolvedName != null) {
				return resolvedName;
			}
		}
		return name;
	}

	@Nullable
	private String checkPropertyName(String name) {
		// 按原样检查名称
		if (containsKey(name)) {
			return name;
		}
		// 仅将点替换为下划线后检查名称
		String noDotName = name.replace('.', '_');
		if (!name.equals(noDotName) && containsKey(noDotName)) {
			return noDotName;
		}
		// 仅将连字符替换为下划线后检查名称
		String noHyphenName = name.replace('-', '_');
		if (!name.equals(noHyphenName) && containsKey(noHyphenName)) {
			return noHyphenName;
		}
		// 将点和连字符都替换为下划线后检查名称
		String noDotNoHyphenName = noDotName.replace('-', '_');
		if (!noDotName.equals(noDotNoHyphenName) && containsKey(noDotNoHyphenName)) {
			return noDotNoHyphenName;
		}
		// 放弃检查，返回空
		return null;
	}

	private boolean containsKey(String name) {
		return (isSecurityManagerPresent() ? this.source.keySet().contains(name) : this.source.containsKey(name));
	}

	protected boolean isSecurityManagerPresent() {
		return (System.getSecurityManager() != null);
	}

}
