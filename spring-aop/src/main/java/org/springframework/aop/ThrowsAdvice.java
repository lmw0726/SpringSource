/*
 * Copyright 2002-2008 the original author or authors.
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
 * 异常通知的标记接口。
 *
 * <p>此接口上没有任何方法，因为方法通过反射调用。
 * 实现类必须实现如下形式的方法：
 *
 * <pre class="code">void afterThrowing([Method, args, target], ThrowableSubclass);</pre>
 *
 * <p>以下是一些有效方法的示例：
 *
 * <pre class="code">public void afterThrowing(Exception ex)</pre>
 * <pre class="code">public void afterThrowing(RemoteException)</pre>
 * <pre class="code">public void afterThrowing(Method method, Object[] args, Object target, Exception ex)</pre>
 * <pre class="code">public void afterThrowing(Method method, Object[] args, Object target, ServletException ex)</pre>
 *
 * 前三个参数是可选的，只有在我们希望获取有关连接点的更多信息时才有用，
 * 例如 AspectJ 的 <b>after-throwing</b> 通知。
 *
 * <p><b>注意：</b>如果异常通知方法本身抛出异常，
 * 它将覆盖原始异常（即改变抛给用户的异常）。
 * 覆盖异常通常是 RuntimeException；这与任何方法签名都兼容。
 * 然而，如果异常通知方法抛出 checked 异常，
 * 它必须与目标方法声明的异常匹配，因此在某种程度上会耦合到特定的目标方法签名。
 * <b>不要抛出与目标方法签名不兼容的未声明 checked 异常！</b>
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see AfterReturningAdvice
 * @see MethodBeforeAdvice
 */
public interface ThrowsAdvice extends AfterAdvice {

}
