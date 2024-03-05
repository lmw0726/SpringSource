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

package org.springframework.context.support;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link AbstractRefreshableApplicationContext}的子类，添加了指定配置位置的常见处理。
 * 作为基类供基于XML的应用程序上下文实现使用，例如 {@link ClassPathXmlApplicationContext} 和
 * {@link FileSystemXmlApplicationContext}，以及 {@link org.springframework.web.context.support.XmlWebApplicationContext}。
 *
 * @author Juergen Hoeller
 * @see #setConfigLocation
 * @see #setConfigLocations
 * @see #getDefaultConfigLocations
 * @since 2.5.2
 */
public abstract class AbstractRefreshableConfigApplicationContext extends AbstractRefreshableApplicationContext
		implements BeanNameAware, InitializingBean {
	/**
	 * 配置的位置数组
	 */
	@Nullable
	private String[] configLocations;
	/**
	 * 是否设置Id调用
	 */
	private boolean setIdCalled = false;


	/**
	 * 创建一个没有父上下文的新的AbstractRefreshableConfigApplicationContext。
	 */
	public AbstractRefreshableConfigApplicationContext() {
	}

	/**
	 * 使用给定的父上下文创建一个新的AbstractRefreshableConfigApplicationContext。
	 *
	 * @param parent 父上下文
	 */
	public AbstractRefreshableConfigApplicationContext(@Nullable ApplicationContext parent) {
		super(parent);
	}


	/**
	 * 以init-param样式设置此应用程序上下文的配置位置，
	 * 即使用逗号、分号或空格分隔的不同位置。
	 * <p>如果未设置，实现可能会使用适当的默认值。
	 *
	 * @param location 配置位置
	 */
	public void setConfigLocation(String location) {
		setConfigLocations(StringUtils.tokenizeToStringArray(location, CONFIG_LOCATION_DELIMITERS));
	}

	/**
	 * 设置此应用程序上下文的配置位置。
	 * <p>如果未设置，实现可能会使用适当的默认值。
	 *
	 * @param locations 应用程序上下文的配置位置
	 */
	public void setConfigLocations(@Nullable String... locations) {
		if (locations != null) {
			Assert.noNullElements(locations, "Config locations must not be null");
			this.configLocations = new String[locations.length];
			for (int i = 0; i < locations.length; i++) {
				this.configLocations[i] = resolvePath(locations[i]).trim();
			}
		} else {
			this.configLocations = null;
		}
	}

	/**
	 * 返回一个资源位置数组，指示应使用哪些 XML bean 定义文件构建此上下文。还可以包含位置模式，这将通过 ResourcePatternResolver 解析。
	 * <p>默认实现返回 {@code null}。子类可以覆盖此方法，以提供一组从中加载 bean 定义的资源位置。
	 *
	 * @return 资源位置数组，如果没有则为 {@code null}
	 * @see #getResources
	 * @see #getResourcePatternResolver
	 */
	@Nullable
	protected String[] getConfigLocations() {
		return (this.configLocations != null ? this.configLocations : getDefaultConfigLocations());
	}

	/**
	 * 返回要在没有指定显式配置位置的情况下使用的默认配置位置。
	 * <p>默认实现返回 {@code null}，需要明确的配置位置。
	 *
	 * @return 默认配置位置数组，如果有的话
	 * @see #setConfigLocations
	 */
	@Nullable
	protected String[] getDefaultConfigLocations() {
		return null;
	}

	/**
	 * 解析给定的路径，在必要时用相应的环境属性值替换占位符。应用于配置位置。
	 *
	 * @param path 原始文件路径
	 * @return 已解析的文件路径
	 * @see org.springframework.core.env.Environment#resolveRequiredPlaceholders(String)
	 */
	protected String resolvePath(String path) {
		return getEnvironment().resolveRequiredPlaceholders(path);
	}


	@Override
	public void setId(String id) {
		super.setId(id);
		this.setIdCalled = true;
	}

	/**
	 * 默认情况下，将此上下文的ID设置为bean名称，用于上下文实例本身被定义为bean的情况。
	 */
	@Override
	public void setBeanName(String name) {
		if (!this.setIdCalled) {
			super.setId(name);
			setDisplayName("ApplicationContext '" + name + "'");
		}
	}

	/**
	 * 如果在具体上下文的构造函数中尚未刷新，则触发 {@link #refresh()}。
	 */
	@Override
	public void afterPropertiesSet() {
		if (!isActive()) {
			refresh();
		}
	}

}
