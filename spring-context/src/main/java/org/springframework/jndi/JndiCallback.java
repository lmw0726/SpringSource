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

package org.springframework.jndi;

import javax.naming.Context;
import javax.naming.NamingException;

import org.springframework.lang.Nullable;

/**
 * 需要在 JNDI 上下文中执行操作（如查找）的类应实现的回调接口。
 * 这种回调方式有助于简化错误处理，由 JndiTemplate 类来完成。
 * 这与 JdbcTemplate 的处理方式类似。
 *
 * <p>注意，几乎不需要实现此回调接口，因为 JndiTemplate 已通过
 * 便捷方法提供了所有常用的 JNDI 操作。
 *
 * @author Rod Johnson
 * @param <T> 返回结果的对象类型
 * @see JndiTemplate
 * @see org.springframework.jdbc.core.JdbcTemplate
 */
@FunctionalInterface
public interface JndiCallback<T> {

	/**
	 * 在给定的 JNDI 上下文中执行某些操作。
	 * <p>实现类无需担心错误处理或资源清理，JndiTemplate 类会处理这些事项。
	 * @param ctx 当前 JNDI 上下文
	 * @return 结果对象，或 {@code null}
	 * @throws NamingException 若 JNDI 方法抛出异常
	 */
	@Nullable
	T doInContext(Context ctx) throws NamingException;

}

