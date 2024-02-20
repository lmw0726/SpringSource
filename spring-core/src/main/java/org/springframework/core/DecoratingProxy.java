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
 * 由装饰代理实现的接口，特别是Spring AOP代理，但也可能是具有装饰器语义的自定义代理。
 *
 * <p>请注意，仅当装饰的类不在代理类的层次结构中时，才应该实现此接口。特别是，
 * 类似于Spring AOP CGLIB代理的“目标类”代理不应该实现它，因为在那里可以在代理类上简单地执行对目标类的任何查找。
 *
 * <p>在核心模块中定义，以便允许{@link org.springframework.core.annotation.AnnotationAwareOrderComparator}
 * （以及潜在的其他不依赖spring-aop的候选项）用于内省目的，特别是注解查找。
 *
 * @author Juergen Hoeller
 * @since 4.3
 */
public interface DecoratingProxy {

	/**
	 * 返回此代理后面（最终）的装饰类。
	 * <p>在AOP代理的情况下，这将是最终的目标类，而不仅仅是立即目标（在多个嵌套代理的情况下）。
	 *
	 * @return 装饰的类（永远不会为{@code null}）
	 */
	Class<?> getDecoratedClass();

}
