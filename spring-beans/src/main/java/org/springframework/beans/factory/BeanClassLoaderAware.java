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

package org.springframework.beans.factory;

/**
 * 回调，允许bean了解bean的 {@link ClassLoader 类加载器}；即，由当前bean工厂用于加载bean类的类加载器。
 *
 * <p>这主要用于由框架类实现，这些类必须通过名称获取应用程序类，尽管它们自己可能是从共享类加载器加载的。
 *
 * <p>有关所有bean生命周期方法的列表，请参见
 * {@link BeanFactory BeanFactory javadocs}。
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @see BeanNameAware
 * @see BeanFactoryAware
 * @see InitializingBean
 * @since 2.0
 */
public interface BeanClassLoaderAware extends Aware {

	/**
	 * 向bean实例提供bean {@link ClassLoader 类加载器} 的回调。
	 * <p> 在 <i> 正常bean属性的填充之后 </i> 调用，但在 <i> 初始化回调之前 </i> 调用，例如
	 * {@link InitializingBean InitializingBean's}
	 * {@link InitializingBean#afterPropertiesSet()}
	 * 方法 或者自定义初始化方法。
	 *
	 * @param classLoader 拥有类加载器
	 */
	void setBeanClassLoader(ClassLoader classLoader);

}
