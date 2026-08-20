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

package org.springframework.jmx.export.assembler;

/**
 * 扩展 {@code MBeanInfoAssembler} 以添加自动检测逻辑。
 * {@code MBeanExporter} 会给予此接口的实现类一个机会，使其能够
 * 在注册过程中包含额外的 bean。
 *
 * <p>决定包含哪些 bean 的具体机制交由实现类自行决定。
 *
 * @author Rob Harrop
 * @since 1.2
 * @see org.springframework.jmx.export.MBeanExporter
 */
public interface AutodetectCapableMBeanInfoAssembler extends MBeanInfoAssembler {


	/**
	 * 指示某个特定的 bean 是否应该被包含在注册过程中，
	 * 即使该 bean 未在 {@code MBeanExporter} 的 {@code beans} 映射中指定。
	 * @param beanClass bean 的类（可能是代理类）
	 * @param beanName bean 在 bean 工厂中的名称
	 */
	boolean includeBean(Class<?> beanClass, String beanName);

}
