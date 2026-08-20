/*
 * Copyright 2002-2018 the original author or authors.
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

import java.util.Hashtable;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.lang.Nullable;
import org.springframework.util.CollectionUtils;

/**
 * 简化 JNDI 操作的辅助类。它提供了查找和绑定对象的方法，
 * 并允许 {@link JndiCallback} 接口的实现使用提供的 JNDI 命名上下文执行任意操作。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see JndiCallback
 * @see #execute
 */
public class JndiTemplate {


	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private Properties environment;


	/**
	 * 创建一个新的 JndiTemplate 实例。
	 */
	public JndiTemplate() {
	}

	/**
	 * 使用给定的环境创建一个新的 JndiTemplate 实例。
	 */
	public JndiTemplate(@Nullable Properties environment) {
		this.environment = environment;
	}


	/**
	 * 设置 JNDI InitialContext 的环境。
	 */
	public void setEnvironment(@Nullable Properties environment) {
		this.environment = environment;
	}

	/**
	 * 返回 JNDI InitialContext 的环境（如果有）。
	 */
	@Nullable
	public Properties getEnvironment() {
		return this.environment;
	}


	/**
	 * 执行给定的 JNDI 上下文回调实现。
	 * @param contextCallback 要使用的 JndiCallback 实现
	 * @return 回调返回的结果对象，或 {@code null}
	 * @throws NamingException 由回调实现抛出
	 * @see #createInitialContext
	 */
	@Nullable
	public <T> T execute(JndiCallback<T> contextCallback) throws NamingException {
		Context ctx = getContext();
		try {
			return contextCallback.doInContext(ctx);
		}
		finally {
			releaseContext(ctx);
		}
	}

	/**
	 * 获取与当前模板配置对应的 JNDI 上下文。
	 * 由 {@link #execute} 调用；也可以直接调用。
	 * <p>默认实现委托给 {@link #createInitialContext()}。
	 * @return JNDI 上下文（不为 {@code null}）
	 * @throws NamingException 获取上下文失败时抛出
	 * @see #releaseContext
	 */
	public Context getContext() throws NamingException {
		return createInitialContext();
	}

	/**
	 * 释放通过 {@link #getContext()} 获取的 JNDI 上下文。
	 * @param ctx 要释放的 JNDI 上下文（可能为 {@code null}）
	 * @see #getContext
	 */
	public void releaseContext(@Nullable Context ctx) {
		if (ctx != null) {
			try {
				ctx.close();
			}
			catch (NamingException ex) {
				logger.debug("Could not close JNDI InitialContext", ex);
			}
		}
	}

	/**
	 * 创建一个新的 JNDI 初始上下文。由 {@link #getContext} 调用。
	 * <p>默认实现使用当前模板的环境设置。
	 * 可以被子类化以提供自定义上下文，例如用于测试。
	 * @return 初始上下文实例
	 * @throws NamingException 初始化错误时抛出
	 */
	protected Context createInitialContext() throws NamingException {
		Hashtable<?, ?> icEnv = null;
		Properties env = getEnvironment();
		if (env != null) {
			icEnv = new Hashtable<>(env.size());
			CollectionUtils.mergePropertiesIntoMap(env, icEnv);
		}
		return new InitialContext(icEnv);
	}


	/**
	 * 在当前 JNDI 上下文中查找指定名称的对象。
	 * @param name 对象的 JNDI 名称
	 * @return 找到的对象（不能为 {@code null}；如果行为不佳的
	 * JNDI 实现返回了 null，则会抛出 NamingException）
	 * @throws NamingException 如果 JNDI 中没有绑定指定名称的对象
	 */
	public Object lookup(final String name) throws NamingException {
		if (logger.isDebugEnabled()) {
			logger.debug("Looking up JNDI object with name [" + name + "]");
		}
		Object result = execute(ctx -> ctx.lookup(name));
		if (result == null) {
			throw new NameNotFoundException(
					"JNDI object with [" + name + "] not found: JNDI implementation returned null");
		}
		return result;
	}

	/**
	 * 在当前 JNDI 上下文中查找指定名称的对象。
	 * @param name 对象的 JNDI 名称
	 * @param requiredType JNDI 对象必须匹配的类型。可以是实际类的接口或
	 * 超类，或为 {@code null} 表示匹配任意类型。例如，
	 * 如果值为 {@code Object.class}，无论返回实例的类型是什么，
	 * 此方法都会成功。
	 * @return 找到的对象（不能为 {@code null}；如果行为不佳的
	 * JNDI 实现返回了 null，则会抛出 NamingException）
	 * @throws NamingException 如果 JNDI 中没有绑定指定名称的对象
	 */
	@SuppressWarnings("unchecked")
	public <T> T lookup(String name, @Nullable Class<T> requiredType) throws NamingException {
		Object jndiObject = lookup(name);
		if (requiredType != null && !requiredType.isInstance(jndiObject)) {
			throw new TypeMismatchNamingException(name, requiredType, jndiObject.getClass());
		}
		return (T) jndiObject;
	}

	/**
	 * 将给定对象绑定到当前 JNDI 上下文，使用指定的名称。
	 * @param name 对象的 JNDI 名称
	 * @param object 要绑定的对象
	 * @throws NamingException 由 JNDI 抛出，通常是名称已被绑定
	 */
	public void bind(final String name, final Object object) throws NamingException {
		if (logger.isDebugEnabled()) {
			logger.debug("Binding JNDI object with name [" + name + "]");
		}
		execute(ctx -> {
			ctx.bind(name, object);
			return null;
		});
	}

	/**
	 * 将给定对象重新绑定到当前 JNDI 上下文，使用指定的名称。
	 * 覆盖任何已存在的绑定。
	 * @param name 对象的 JNDI 名称
	 * @param object 要重新绑定的对象
	 * @throws NamingException 由 JNDI 抛出
	 */
	public void rebind(final String name, final Object object) throws NamingException {
		if (logger.isDebugEnabled()) {
			logger.debug("Rebinding JNDI object with name [" + name + "]");
		}
		execute(ctx -> {
			ctx.rebind(name, object);
			return null;
		});
	}

	/**
	 * 从当前 JNDI 上下文中移除指定名称的绑定。
	 * @param name 对象的 JNDI 名称
	 * @throws NamingException 由 JNDI 抛出，通常是名称未找到
	 */
	public void unbind(final String name) throws NamingException {
		if (logger.isDebugEnabled()) {
			logger.debug("Unbinding JNDI object with name [" + name + "]");
		}
		execute(ctx -> {
			ctx.unbind(name);
			return null;
		});
	}

}
