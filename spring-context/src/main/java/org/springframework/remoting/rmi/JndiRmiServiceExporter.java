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

package org.springframework.remoting.rmi;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Properties;

import javax.naming.NamingException;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jndi.JndiTemplate;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;

/**
 * 将 RMI 服务绑定到 JNDI 的服务导出器。
 * 通常用于 RMI-IIOP（CORBA）。
 *
 * <p>通过 {@link javax.rmi.PortableRemoteObject} 类导出服务。
 * 需要使用 "-iiop" 选项运行 "rmic" 来为每个导出的服务生成对应的存根和骨架。
 *
 * <p>也支持通过 RMI 调用器暴露任意非 RMI 服务，
 * 供 {@link JndiRmiClientInterceptor} / {@link JndiRmiProxyFactoryBean}
 * 自动检测并访问此类调用器。
 *
 * <p>使用 RMI 调用器时，RMI 通信工作在 {@link RmiInvocationHandler}
 * 层级，任何服务只需一个存根即可。服务接口不必继承
 * {@code java.rmi.Remote} 或在所有方法上抛出 {@code java.rmi.RemoteException}，
 * 但入参和出参必须可序列化。
 *
 * <p>JNDI 环境可以通过 "jndiEnvironment" bean 属性指定，
 * 也可以在 {@code jndi.properties} 文件中配置或作为系统属性设置。
 * 例如：
 *
 * <pre class="code">&lt;property name="jndiEnvironment"&gt;
 * 	 &lt;props&gt;
 *		 &lt;prop key="java.naming.factory.initial"&gt;com.sun.jndi.cosnaming.CNCtxFactory&lt;/prop&gt;
 *		 &lt;prop key="java.naming.provider.url"&gt;iiop://localhost:1050&lt;/prop&gt;
 *	 &lt;/props&gt;
 * &lt;/property&gt;</pre>
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see #setService
 * @see #setJndiTemplate
 * @see #setJndiEnvironment
 * @see #setJndiName
 * @see JndiRmiClientInterceptor
 * @see JndiRmiProxyFactoryBean
 * @see javax.rmi.PortableRemoteObject#exportObject
 * @deprecated 5.3 起弃用（逐步淘汰基于序列化的远程调用）
 */
@Deprecated
public class JndiRmiServiceExporter extends RmiBasedExporter implements InitializingBean, DisposableBean {

	@Nullable
	private static Method exportObject;

	@Nullable
	private static Method unexportObject;

	static {
		try {
			Class<?> portableRemoteObject =
					JndiRmiServiceExporter.class.getClassLoader().loadClass("javax.rmi.PortableRemoteObject");
			exportObject = portableRemoteObject.getMethod("exportObject", Remote.class);
			unexportObject = portableRemoteObject.getMethod("unexportObject", Remote.class);
		}
		catch (Throwable ex) {
			// JDK 9+ 中 java.corba 模块不可用
			exportObject = null;
			unexportObject = null;
		}
	}


	private JndiTemplate jndiTemplate = new JndiTemplate();

	private String jndiName;

	private Remote exportedObject;


	/**
	 * 设置用于 JNDI 查找的 JNDI 模板。
	 * 也可以通过 "jndiEnvironment" 指定 JNDI 环境设置。
	 * @see #setJndiEnvironment
	 */
	public void setJndiTemplate(JndiTemplate jndiTemplate) {
		this.jndiTemplate = (jndiTemplate != null ? jndiTemplate : new JndiTemplate());
	}

	/**
	 * 设置用于 JNDI 查找的 JNDI 环境。
	 * 使用给定的环境设置创建一个 JndiTemplate。
	 * @see #setJndiTemplate
	 */
	public void setJndiEnvironment(Properties jndiEnvironment) {
		this.jndiTemplate = new JndiTemplate(jndiEnvironment);
	}

	/**
	 * 设置导出的 RMI 服务的 JNDI 名称。
	 */
	public void setJndiName(String jndiName) {
		this.jndiName = jndiName;
	}


	@Override
	public void afterPropertiesSet() throws NamingException, RemoteException {
		prepare();
	}

	/**
	 * 初始化此服务导出器，将指定服务绑定到 JNDI。
	 * @throws NamingException 如果服务绑定失败
	 * @throws RemoteException 如果服务导出失败
	 */
	public void prepare() throws NamingException, RemoteException {
		if (this.jndiName == null) {
			throw new IllegalArgumentException("Property 'jndiName' is required");
		}

		// 初始化并缓存导出的对象。
		this.exportedObject = getObjectToExport();
		invokePortableRemoteObject(exportObject);

		rebind();
	}

	/**
	 * 将指定服务重新绑定到 JNDI，用于在目标注册中心重启后恢复连接。
	 * @throws NamingException 如果服务绑定失败
	 */
	public void rebind() throws NamingException {
		if (logger.isDebugEnabled()) {
			logger.debug("Binding RMI service to JNDI location [" + this.jndiName + "]");
		}
		this.jndiTemplate.rebind(this.jndiName, this.exportedObject);
	}

	/**
	 * 在 Bean 工厂关闭时从 JNDI 解除 RMI 服务的绑定。
	 */
	@Override
	public void destroy() throws NamingException, RemoteException {
		if (logger.isDebugEnabled()) {
			logger.debug("Unbinding RMI service from JNDI location [" + this.jndiName + "]");
		}
		this.jndiTemplate.unbind(this.jndiName);
		invokePortableRemoteObject(unexportObject);
	}


	private void invokePortableRemoteObject(@Nullable Method method) throws RemoteException {
		if (method != null) {
			try {
				method.invoke(null, this.exportedObject);
			}
			catch (InvocationTargetException ex) {
				Throwable targetEx = ex.getTargetException();
				if (targetEx instanceof RemoteException) {
					throw (RemoteException) targetEx;
				}
				ReflectionUtils.rethrowRuntimeException(targetEx);
			}
			catch (Throwable ex) {
				throw new IllegalStateException("PortableRemoteObject invocation failed", ex);
			}
		}
	}

}
