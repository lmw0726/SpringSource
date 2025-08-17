/*
 * Copyright 2002-2020 the original author or authors.
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

/**
 * 一个适配器类，实现了 {@link SmartInstantiationAwareBeanPostProcessor} 的所有方法，
 * 并将其作为空操作（no-op），因此不会改变容器中每个 bean 的正常实例化处理。
 * 子类只需重写自己真正关心的方法即可。
 *
 * <p>注意：只有在确实需要 {@link InstantiationAwareBeanPostProcessor} 功能时，
 * 才推荐使用此基类。如果只需要最基本的 {@link BeanPostProcessor} 功能，
 * 建议直接实现该（更简单的）接口。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 * @deprecated 自 5.3 起已废弃，推荐直接实现 {@link InstantiationAwareBeanPostProcessor}
 * 或 {@link SmartInstantiationAwareBeanPostProcessor}。
 */
@Deprecated
public abstract class InstantiationAwareBeanPostProcessorAdapter implements SmartInstantiationAwareBeanPostProcessor {

}
