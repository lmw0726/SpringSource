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

package org.springframework.web.method.support;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;

/**
 * 一个支持异步类型的返回值处理器。此类返回值类型需要优先处理，以便可以“展开”异步值。
 *
 * <p><strong>注意：</strong> 不需要实现此协议，但应在需要将处理程序优先于其他处理程序的情况下实现它。
 * 例如，默认情况下在内置处理程序之后排序的自定义（异步）处理程序应优先于 {@code @ResponseBody} 或
 * {@code @ModelAttribute} 处理。这应该在异步值准备就绪后发生。相比之下，内置（异步）处理程序已经在同步处理程序之前排序。
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
public interface AsyncHandlerMethodReturnValueHandler extends HandlerMethodReturnValueHandler {

	/**
	 * 检查给定的返回值是否代表异步计算。
	 *
	 * @param returnValue 返回处理程序方法后的值
	 * @param returnType  返回类型
	 * @return 如果返回值类型表示异步值，则为 {@code true}
	 */
	boolean isAsyncReturnValue(@Nullable Object returnValue, MethodParameter returnType);

}
