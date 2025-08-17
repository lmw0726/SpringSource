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

package org.springframework.beans.propertyeditors;

import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.beans.PropertyEditorSupport;

/**
 * {@link Class java.lang.Class} 的属性编辑器，用于直接设置 Class 属性，
 * 无需通过 String 类型的类名作为桥梁。
 *
 * <p>同时支持类似 "java.lang.String[]"-风格的数组类名，这在标准
 * {@link Class#forName(String)} 方法中是无法直接支持的。
 *
 * @author Juergen Hoeller
 * @author Rick Evans
 * @since 13.05.2003
 * @see Class#forName
 * @see org.springframework.util.ClassUtils#forName(String, ClassLoader)
 */
public class ClassEditor extends PropertyEditorSupport {

	@Nullable
	private final ClassLoader classLoader;


	/**
	 * 创建默认的 ClassEditor，使用线程上下文 ClassLoader。
	 */
	public ClassEditor() {
		this(null);
	}

	/**
	 * 创建指定 ClassLoader 的 ClassEditor。
	 * @param classLoader 要使用的 ClassLoader
	 * (如果为 {@code null}，则使用线程上下文 ClassLoader)
	 */
	public ClassEditor(@Nullable ClassLoader classLoader) {
		this.classLoader = (classLoader != null ? classLoader : ClassUtils.getDefaultClassLoader());
	}


	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		if (StringUtils.hasText(text)) {
			setValue(ClassUtils.resolveClassName(text.trim(), this.classLoader));
		}
		else {
			setValue(null);
		}
	}

	@Override
	public String getAsText() {
		Class<?> clazz = (Class<?>) getValue();
		if (clazz != null) {
			return ClassUtils.getQualifiedName(clazz);
		}
		else {
			return "";
		}
	}

}
