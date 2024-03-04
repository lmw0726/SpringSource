/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.StringValueResolver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * {@link AliasRegistry} 接口的简单实现。
 *
 * <p>作为 {@link org.springframework.beans.factory.support.BeanDefinitionRegistry} 实现的基类。
 *
 * @author Juergen Hoeller
 * @author Qimiao Chen
 * @since 2.5.2
 */
public class SimpleAliasRegistry implements AliasRegistry {

	/**
	 * 记录器可用于子类。
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * 别名-规范名称的Map
	 */
	private final Map<String, String> aliasMap = new ConcurrentHashMap<>(16);


	@Override
	public void registerAlias(String name, String alias) {
		Assert.hasText(name, "'name' must not be empty");
		Assert.hasText(alias, "'alias' must not be empty");
		synchronized (this.aliasMap) {
			if (alias.equals(name)) {
				//如果别名和名称相同，别名Map去除该别名
				this.aliasMap.remove(alias);
				if (logger.isDebugEnabled()) {
					logger.debug("Alias definition '" + alias + "' ignored since it points to same name");
				}
			} else {
				//获取已注册的的Bean名称
				String registeredName = this.aliasMap.get(alias);
				if (registeredName != null) {
					//如果已注册的的Bean名称不为空，且该名称和当前名称相同，结束
					if (registeredName.equals(name)) {
						// 现有别名-无需重新注册
						return;
					}
					if (!allowAliasOverriding()) {
						//如果不允许重写别名，抛出IllegalStateException
						throw new IllegalStateException("Cannot define alias '" + alias + "' for name '" + name + "': It is already registered for name '" + registeredName + "'.");
					}
					if (logger.isDebugEnabled()) {
						logger.debug("Overriding alias '" + alias + "' definition for registered name '" + registeredName + "' with new target name '" + name + "'");
					}
				}
				//检查别名是否循环引用
				checkForAliasCircle(name, alias);
				//将别名和名称添加到别名Map
				this.aliasMap.put(alias, name);
				if (logger.isTraceEnabled()) {
					logger.trace("Alias definition '" + alias + "' registered for name '" + name + "'");
				}
			}
		}
	}

	/**
	 * 确定是否允许别名重写。<p> 默认值为 {@code true}。
	 */
	protected boolean allowAliasOverriding() {
		return true;
	}

	/**
	 * 确定给定名称是否已注册给定别名。
	 *
	 * @param name  要检查的名称
	 * @param alias 要查找的别名
	 * @since 4.2.1
	 */
	public boolean hasAlias(String name, String alias) {
		//获取已注册的Bean名称
		String registeredName = this.aliasMap.get(alias);
		//如果已注册的bean名称和当前名称相同
		//或者已注册的Bean名称，且已注册的Bean名称也是其他bean的别名。
		//返回true
		return ObjectUtils.nullSafeEquals(registeredName, name) || (registeredName != null && hasAlias(name, registeredName));
	}

	@Override
	public void removeAlias(String alias) {
		synchronized (this.aliasMap) {
			String name = this.aliasMap.remove(alias);
			if (name == null) {
				throw new IllegalStateException("No alias '" + alias + "' registered");
			}
		}
	}

	@Override
	public boolean isAlias(String name) {
		return this.aliasMap.containsKey(name);
	}

	@Override
	public String[] getAliases(String name) {
		List<String> result = new ArrayList<>();
		synchronized (this.aliasMap) {
			retrieveAliases(name, result);
		}
		return StringUtils.toStringArray(result);
	}

	/**
	 * 传递检索给定名称的所有别名。
	 *
	 * @param name   查找别名的目标名称
	 * @param result 生成的别名列表
	 */
	private void retrieveAliases(String name, List<String> result) {
		this.aliasMap.forEach((alias, registeredName) -> {
			if (registeredName.equals(name)) {
				//如果别名Map中的bean名称与目标名称相同，添加进结果集
				result.add(alias);
				//更换为别名，递归检索别名的结果集
				retrieveAliases(alias, result);
			}
		});
	}

	/**
	 * 解析在此注册表中注册的所有别名目标名称和别名，并将给定的 {@link StringValueResolver} 应用于它们。
	 * <p> 例如，值解析器可以解析目标bean名称甚至别名中的占位符。
	 *
	 * @param valueResolver 要应用的StringValueResolver
	 */
	public void resolveAliases(StringValueResolver valueResolver) {
		Assert.notNull(valueResolver, "StringValueResolver must not be null");
		synchronized (this.aliasMap) {
			//复制别名-规范名称Map
			Map<String, String> aliasCopy = new HashMap<>(this.aliasMap);
			aliasCopy.forEach((alias, registeredName) -> {
				//解析别名中的占位符，将其解析为具体的别名
				String resolvedAlias = valueResolver.resolveStringValue(alias);
				//解析规范bean名称中的占位符，将其解析为具体的bean名称
				String resolvedName = valueResolver.resolveStringValue(registeredName);
				if (resolvedAlias == null || resolvedName == null || resolvedAlias.equals(resolvedName)) {
					//如果解析出来的别名、bean名称为空，或者解析出来的别名和bean名称相同，移除掉该别名
					this.aliasMap.remove(alias);
				} else if (!resolvedAlias.equals(alias)) {
					//如果解析出来的别名和原来的别名不同，先获取原来的规范bean名称
					String existingName = this.aliasMap.get(resolvedAlias);
					if (existingName != null) {
						if (existingName.equals(resolvedName)) {
							//如果原来的规范bean名称和解析出来的bean名称相同，移除掉该别名
							this.aliasMap.remove(alias);
							return;
						}
						throw new IllegalStateException("Cannot register resolved alias '" + resolvedAlias + "' (original: '" + alias + "') for name '" + resolvedName + "': It is already registered for name '" + registeredName + "'.");
					}
					//检查解析出来的别名和解析出来是否存在循环有引用
					checkForAliasCircle(resolvedName, resolvedAlias);
					//移除原来的别名，再添加上解析好的别名和解析出来的bean名称
					this.aliasMap.remove(alias);
					this.aliasMap.put(resolvedAlias, resolvedName);
				} else if (!registeredName.equals(resolvedName)) {
					//如果解析出来的别名和原来的别名不同，替换掉原来的规范bean名称
					this.aliasMap.put(alias, resolvedName);
				}
			});
		}
	}

	/**
	 * 检查给定的名称是否已经指向给定的别名，作为另一个方向的别名，捕获一个循环引用并引发相应的IllegalStateException。
	 *
	 * @param name  候选名称
	 * @param alias 候选别名
	 * @see #registerAlias
	 * @see #hasAlias
	 */
	protected void checkForAliasCircle(String name, String alias) {
		if (hasAlias(alias, name)) {
			//如果别名Map已存在名称-别名的键值对，抛出IllegalStateException
			throw new IllegalStateException("Cannot register alias '" + alias + "' for name '" + name + "': Circular reference - '" + name + "' is a direct or indirect alias for '" + alias + "' already");
		}
	}

	/**
	 * 确定原始名称，将别名解析为规范名称。
	 *
	 * @param name 用户指定的名称
	 * @return 转换后的名称
	 */
	public String canonicalName(String name) {
		String canonicalName = name;
		// 处理别名
		String resolvedName;
		do {
			//如果获取该名称对应的别名
			resolvedName = this.aliasMap.get(canonicalName);
			if (resolvedName != null) {
				//如果别名不为空，则返回该别名
				canonicalName = resolvedName;
			}
		} while (resolvedName != null);
		return canonicalName;
	}

}
