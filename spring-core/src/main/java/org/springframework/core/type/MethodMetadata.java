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

package org.springframework.core.type;

/**
 * 定义对特定方法注解的抽象访问接口，
 * 以一种不要求加载该方法所属类的形式进行访问。
 *
 * @author Juergen Hoeller
 * @author Mark Pollack
 * @author Chris Beams
 * @author Phillip Webb
 * @since 3.0
 * @see StandardMethodMetadata
 * @see AnnotationMetadata#getAnnotatedMethods
 * @see AnnotatedTypeMetadata
 */
public interface MethodMetadata extends AnnotatedTypeMetadata {

	/**
	 * 获取底层方法的名称。
	 */
	String getMethodName();

	/**
	 * 获取声明底层方法的类的全限定名。
	 */
	String getDeclaringClassName();

	/**
	 * 获取底层方法声明的返回类型的全限定名。
	 * @since 4.2
	 */
	String getReturnTypeName();

	/**
	 * 判断底层方法是否为有效的抽象方法：
	 * 即在类中被标记为 abstract，或在接口中声明为普通的非默认方法。
	 * @since 4.2
	 */
	boolean isAbstract();

	/**
	 * 判断底层方法是否声明为 'static'。
	 */
	boolean isStatic();

	/**
	 * 判断底层方法是否被标记为 'final'。
	 */
	boolean isFinal();

	/**
	 * 判断底层方法是否可被重写，
	 * 即未被标记为 static、final 或 private。
	 */
	boolean isOverridable();

}
