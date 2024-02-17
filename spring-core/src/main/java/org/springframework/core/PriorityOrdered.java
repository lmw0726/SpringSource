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

package org.springframework.core;

/**
 * {@link Ordered}接口的扩展，表示<em>优先级</em>排序：
 * {@code PriorityOrdered}对象始终在<em>普通</em>{@link Ordered}对象之前应用，而不考虑它们的排序值。
 *
 * <p>在对一组{@code Ordered}对象进行排序时，{@code PriorityOrdered}对象和<em>普通</em>{@code Ordered}对象有效地被视为
 * 两个单独的子集，{@code PriorityOrdered}对象的集合位于<em>普通</em>{@code Ordered}对象的集合之前，并且在这些子集内部应用相对排序。
 *
 * <p>这主要是一个专用接口，在框架内部用于特别重要的对象，特别是要首先识别<em>优先</em>对象的情况下，可能甚至不需要获取剩余的对象。一个典型的例子：
 * Spring {@link org.springframework.context.ApplicationContext}中的优先级后处理器。
 *
 * <p>注意：{@code PriorityOrdered}后处理器bean在一个特殊阶段初始化，优先于其他后处理器bean。这在它们的自动装配行为上有微妙的影响：
 * 它们只会针对不需要对类型匹配进行急切初始化的bean进行自动装配。
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 2.5
 * @see org.springframework.beans.factory.config.PropertyOverrideConfigurer
 * @see org.springframework.beans.factory.config.PropertyPlaceholderConfigurer
 */
public interface PriorityOrdered extends Ordered {
}
