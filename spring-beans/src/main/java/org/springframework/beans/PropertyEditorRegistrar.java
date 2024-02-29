/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.beans;

/**
 * 用于向 PropertyEditorRegistry 注册自定义属性编辑器的策略的接口。
 *
 * <p>当您需要在多种不同情况下使用相同的属性编辑器集时，编写相应的注册器并在每种情况下重用它非常有用。
 *
 * @author Juergen Hoeller
 * @see PropertyEditorRegistry
 * @see java.beans.PropertyEditor
 * @since 1.2.6
 */
public interface PropertyEditorRegistrar {

	/**
	 * 使用给定的 {@code PropertyEditorRegistry} 注册自定义 {@link java.beans.PropertyEditor}。
	 * <p> 传入的注册表通常是 {@link BeanWrapper} 或 {@link org.springframework.validation.DataBinder DataBinder}。
	 * <p> 预计实现将为此方法的每次调用创建全新的 {@code PropertyEditors} 实例 (因为 {@code PropertyEditors} 不是线程安全的)。
	 *
	 * @param registry 用于注册自定义 {@code PropertyEditorRegistry} 的 {@code PropertyEditors}
	 */
	void registerCustomEditors(PropertyEditorRegistry registry);

}
