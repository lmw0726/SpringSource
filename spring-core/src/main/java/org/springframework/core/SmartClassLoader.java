/*
 * Copyright 2002-2021 the original author or authors.
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

import org.springframework.lang.Nullable;

import java.security.ProtectionDomain;

/**
 * 由支持重新加载的 ClassLoader（例如基于 Groovy 的 ClassLoader）实现的接口。
 * 例如，Spring 的 CGLIB 代理工厂会检测此接口以决定是否进行缓存。
 *
 * <p>如果某个 ClassLoader <i>没有</i> 实现此接口，
 * 那么从该 ClassLoader 获取的所有类都应被视为不可重新加载（即可缓存）。
 *
 * @author Juergen Hoeller
 * @since 2.5.1
 */
public interface SmartClassLoader {

	/**
	 * 判断给定的类在此 ClassLoader 中是否可重新加载。
	 * <p>通常用于判断结果是否可以缓存（对于此 ClassLoader 而言），
	 * 或是每次都需要重新获取。
	 * 默认实现始终返回 {@code false}。
	 * @param clazz 要检查的类（通常从该 ClassLoader 加载）
	 * @return 是否预期该类稍后会以重新加载的版本（不同的 {@code Class} 对象）出现
	 */
	default boolean isClassReloadable(Class<?> clazz) {
		return false;
	}

	/**
	 * 返回此 SmartClassLoader 的原始 ClassLoader，
	 * 或者如果它是自给自足的，则可能返回自身。
	 * <p>默认实现按原样返回本地 ClassLoader 引用。
	 * 对于可重新加载或有选择性覆盖的 ClassLoader，
	 * 如果通常需要处理来自基础应用类加载器的未受影响的类，
	 * 则应实现该方法以返回此加载器派生自的原始 ClassLoader
	 * （例如 {@code return getParent();}）。
	 * <p>该方法会在 Spring 的 AOP 框架中被特别使用，
	 * 以便在目标类未在当前类加载器中定义时，
	 * 决定特定代理应使用的类加载器。
	 * 对于可重新加载的类加载器，
	 * 我们更倾向于使用基础应用类加载器来代理那些不在可重新加载类加载器中定义的普通类。
	 * @return 原始 ClassLoader（默认情况下为同一引用）
	 * @since 5.3.5
	 * @see ClassLoader#getParent()
	 * @see org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator
	 */
	default ClassLoader getOriginalClassLoader() {
		return (ClassLoader) this;
	}

	/**
	 * 在此类加载器中定义自定义类（通常是 CGLIB 代理类）。
	 * <p>这是 {@link ClassLoader} 中受保护方法
	 * {@code defineClass(String, byte[], int, int, ProtectionDomain)}
	 * 的公共等价方法，该方法传统上通过反射调用。
	 * 自定义类加载器的具体实现应简单地委托给该受保护方法，
	 * 以便在 JDK 9+ 中公开类加载器特定的定义，
	 * 而不会出现“非法访问”警告：
	 * {@code return defineClass(name, b, 0, b.length, protectionDomain)}。
	 * 请注意，JDK 9+ 的 {@code Lookup#defineClass} 方法
	 * 不支持为新定义指定自定义目标类加载器；
	 * 它始终会在与查找上下文类相同的类加载器中定义类。
	 * @param name 类的名称
	 * @param b 定义类的字节数组
	 * @param protectionDomain 类的保护域（如果有）
	 * @return 新创建的类
	 * @throws LinkageError 如果类定义有问题
	 * @throws SecurityException 如果定义尝试无效
	 * @throws UnsupportedOperationException 如果无法进行自定义定义尝试
	 * （此接口中的默认实现会抛出该异常）
	 * @since 5.3.4
	 * @see ClassLoader#defineClass(String, byte[], int, int, ProtectionDomain)
	 */
	default Class<?> publicDefineClass(String name, byte[] b, @Nullable ProtectionDomain protectionDomain) {
		throw new UnsupportedOperationException();
	}

}
