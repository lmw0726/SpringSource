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

package org.springframework.core.type;

import org.springframework.lang.Nullable;

/**
 * 定义特定类的抽象元数据接口，
 * 以一种不要求加载该类的形式进行访问。
 *
 * @author Juergen Hoeller
 * @since 2.5
 * @see StandardClassMetadata
 * @see org.springframework.core.type.classreading.MetadataReader#getClassMetadata()
 * @see AnnotationMetadata
 */
public interface ClassMetadata {

	/**
	 * 返回底层类的名称。
	 */
	String getClassName();

	/**
	 * 判断底层类是否表示一个接口。
	 */
	boolean isInterface();

	/**
	 * 判断底层类是否表示一个注解。
	 * @since 4.1
	 */
	boolean isAnnotation();

	/**
	 * 判断底层类是否被标记为抽象类。
	 */
	boolean isAbstract();

	/**
	 * 判断底层类是否是具体类，
	 * 即既不是接口也不是抽象类。
	 */
	default boolean isConcrete() {
		return !(isInterface() || isAbstract());
	}

	/**
	 * 判断底层类是否被标记为 'final'。
	 */
	boolean isFinal();

	/**
	 * 判断底层类是否是独立的，
	 * 即是否为顶级类或可独立构造的静态内部类。
	 */
	boolean isIndependent();

	/**
	 * 判断底层类是否声明在一个外部类中
	 * （即是否是内部类/嵌套类，或方法内的局部类）。
	 * <p>如果返回 {@code false}，则表示底层类是顶级类。
	 */
	default boolean hasEnclosingClass() {
		return (getEnclosingClassName() != null);
	}

	/**
	 * 返回底层类的外部类名，
	 * 如果底层类是顶级类，则返回 {@code null}。
	 */
	@Nullable
	String getEnclosingClassName();

	/**
	 * 判断底层类是否有父类。
	 */
	default boolean hasSuperClass() {
		return (getSuperClassName() != null);
	}

	/**
	 * 返回底层类的父类名，
	 * 如果没有父类则返回 {@code null}。
	 */
	@Nullable
	String getSuperClassName();

	/**
	 * 返回底层类实现的所有接口名，
	 * 如果没有接口则返回空数组。
	 */
	String[] getInterfaceNames();

	/**
	 * 返回该类中声明的所有成员类的名称。
	 * 包含 public、protected、默认（包）访问权限以及 private 的类和接口，
	 * 不包含继承来的类和接口。
	 * 如果不存在成员类或接口，则返回空数组。
	 * @since 3.1
	 */
	String[] getMemberClassNames();

}
