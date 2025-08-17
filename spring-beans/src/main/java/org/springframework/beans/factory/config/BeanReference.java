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

package org.springframework.beans.factory.config;

import org.springframework.beans.BeanMetadataElement;

/**
 * 以抽象方式暴露对 bean 名称的引用的接口。
 * 该接口不一定意味着对实际 bean 实例的引用；它只是表示对 bean 名称的逻辑引用。
 *
 * <p>作为所有类型 bean 引用持有者的公共接口，例如 {@link RuntimeBeanReference RuntimeBeanReference} 和
 * {@link RuntimeBeanNameReference RuntimeBeanNameReference}。
 *
 * @author Juergen Hoeller
 * @since 2.0
 */
public interface BeanReference extends BeanMetadataElement {

	/**
	 * 返回此引用指向的目标bean名称 (从不 {@code null})。
	 */
	String getBeanName();

}
