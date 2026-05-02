/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.aop;

/**
 * AOP 代理接口（特别是引介接口）的标记接口，
 * 表示它们明确打算返回原始目标对象（该对象通常会在方法调用返回时
 * 被代理对象替换）。
 *
 * <p>注意，这是一个类似 {@link java.io.Serializable} 风格的标记接口，
 * 从语义上应用于已声明的接口，而不是具体对象的完整类。
 * 换言之，此标记仅应用于某个特定接口（通常是一个不作为 AOP 代理
 * 主接口的引介接口），因此不会影响具体 AOP 代理可能实现的其他接口。
 *
 * @author Juergen Hoeller
 * @since 2.0.5
 * @see org.springframework.aop.scope.ScopedObject
 */
public interface RawTargetAccess {

}
