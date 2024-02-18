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

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.lang.Nullable;

import java.util.Properties;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * 支持 JDK 1.4 的首选项 API（{@code java.util.prefs}）的 PropertyPlaceholderConfigurer 的子类。
 *
 * <p>首先尝试在用户首选项中解析占位符作为键，然后在系统首选项中解析，最后在此配置器的属性中解析。因此，如果没有对应的首选项定义，则行为类似于 PropertyPlaceholderConfigurer。
 *
 * <p>支持自定义系统和用户首选项树的路径。还支持在占位符中指定的自定义路径（“myPath/myPlaceholderKey”）。如果未指定，则使用相应的根节点。
 *
 * @author Juergen Hoeller
 * @see #setSystemTreePath
 * @see #setUserTreePath
 * @see java.util.prefs.Preferences
 * @since 16.02.2004
 * @deprecated 自 5.2 起，与 {@link PropertyPlaceholderConfigurer} 一起不推荐使用
 */
@Deprecated
public class PreferencesPlaceholderConfigurer extends PropertyPlaceholderConfigurer implements InitializingBean {

	/**
	 * 系统首选项树的路径。
	 */
	@Nullable
	private String systemTreePath;

	/**
	 * 用户首选项树的路径。
	 */
	@Nullable
	private String userTreePath;

	/**
	 * 系统首选项。
	 */
	private Preferences systemPrefs = Preferences.systemRoot();

	/**
	 * 用户首选项。
	 */
	private Preferences userPrefs = Preferences.userRoot();


	/**
	 * 设置在系统首选项树中用于解析占位符的路径。默认为根节点。
	 */
	public void setSystemTreePath(String systemTreePath) {
		this.systemTreePath = systemTreePath;
	}

	/**
	 * 设置在用户首选项树中用于解析占位符的路径。默认为根节点。
	 */
	public void setUserTreePath(String userTreePath) {
		this.userTreePath = userTreePath;
	}


	/**
	 * 此实现会提前获取所需的系统和用户树节点的首选项实例。
	 */
	@Override
	public void afterPropertiesSet() {
		// 如果指定了系统树路径，则使用系统偏好节点创建指定路径的节点。
		if (this.systemTreePath != null) {
			this.systemPrefs = this.systemPrefs.node(this.systemTreePath);
		}
		// 如果指定了用户树路径，则使用用户偏好节点创建指定路径的节点。
		if (this.userTreePath != null) {
			this.userPrefs = this.userPrefs.node(this.userTreePath);
		}
	}

	/**
	 * 此实现会首先尝试在用户首选项中以占位符作为键解析，然后在系统首选项中，最后在传入的属性中解析。
	 */
	@Override
	protected String resolvePlaceholder(String placeholder, Properties props) {
		// 初始化路径和键变量
		String path = null;
		String key = placeholder;
		// 获取路径的末尾索引
		int endOfPath = placeholder.lastIndexOf('/');
		// 如果路径存在
		if (endOfPath != -1) {
			// 获取路径
			path = placeholder.substring(0, endOfPath);
			// 获取键
			key = placeholder.substring(endOfPath + 1);
		}
		// 从用户偏好节点解析占位符
		String value = resolvePlaceholder(path, key, this.userPrefs);
		// 如果用户偏好节点中未找到值，则从系统偏好节点解析
		if (value == null) {
			value = resolvePlaceholder(path, key, this.systemPrefs);
			// 如果系统偏好节点中未找到值，则从属性文件中查找
			if (value == null) {
				value = props.getProperty(placeholder);
			}
		}
		return value;
	}

	/**
	 * 解析给定路径和键相对于给定的首选项。
	 *
	 * @param path        首选项路径（斜杠 '/' 前面的占位符部分）
	 * @param key         首选项键（斜杠 '/' 后面的占位符部分）
	 * @param preferences 要解析的首选项
	 * @return 占位符的值，如果找不到则返回 {@code null}
	 */
	@Nullable
	protected String resolvePlaceholder(@Nullable String path, String key, Preferences preferences) {
		if (path != null) {
			// 如果节点不存在，则不要创建该节点...
			try {
				if (preferences.nodeExists(path)) {
					return preferences.node(path).get(key, null);
				} else {
					return null;
				}
			} catch (BackingStoreException ex) {
				throw new BeanDefinitionStoreException("Cannot access specified node path [" + path + "]", ex);
			}
		} else {
			return preferences.get(key, null);
		}
	}

}
