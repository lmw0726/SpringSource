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

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.rmi.RemoteException;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationBasedExporter;
import org.springframework.remoting.support.RemoteInvocationResult;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * 远程服务导出器的抽象基类，用于显式反序列化
 * {@link org.springframework.remoting.support.RemoteInvocation} 对象并序列化
 * {@link org.springframework.remoting.support.RemoteInvocationResult} 对象，
 * 例如 Spring 的 HTTP 调用器。
 *
 * <p>为 {@code ObjectInputStream} 和
 * {@code ObjectOutputStream} 处理提供模板方法。
 *
 * @author Juergen Hoeller
 * @since 2.5.1
 * @see java.io.ObjectInputStream
 * @see java.io.ObjectOutputStream
 * @see #doReadRemoteInvocation
 * @see #doWriteRemoteInvocationResult
 * @deprecated 从 5.3 版本开始弃用（逐步淘汰基于序列化的远程调用）
 */
@Deprecated
public abstract class RemoteInvocationSerializingExporter extends RemoteInvocationBasedExporter
		implements InitializingBean {


	/**
	 * 默认内容类型："application/x-java-serialized-object"。
	 */
	public static final String CONTENT_TYPE_SERIALIZED_OBJECT = "application/x-java-serialized-object";


	private String contentType = CONTENT_TYPE_SERIALIZED_OBJECT;

	private boolean acceptProxyClasses = true;

	private Object proxy;


	/**
	 * 设置用于发送远程调用响应的内容类型。
	 * <p>默认值为 "application/x-java-serialized-object"。
	 */
	public void setContentType(String contentType) {
		Assert.notNull(contentType, "'contentType' must not be null");
		this.contentType = contentType;
	}

	/**
	 * 返回用于发送远程调用响应的内容类型。
	 */
	public String getContentType() {
		return this.contentType;
	}

	/**
	 * 设置是否接受代理类的反序列化。
	 * <p>默认值为 "true"。可出于安全考虑禁用此选项。
	 */
	public void setAcceptProxyClasses(boolean acceptProxyClasses) {
		this.acceptProxyClasses = acceptProxyClasses;
	}

	/**
	 * 返回是否接受代理类的反序列化。
	 */
	public boolean isAcceptProxyClasses() {
		return this.acceptProxyClasses;
	}


	@Override
	public void afterPropertiesSet() {
		prepare();
	}

	/**
	 * 初始化此服务导出器。
	 */
	public void prepare() {
		this.proxy = getProxyForService();
	}

	protected final Object getProxy() {
		if (this.proxy == null) {
			throw new IllegalStateException(ClassUtils.getShortName(getClass()) + " has not been initialized");
		}
		return this.proxy;
	}


	/**
	 * 为给定的 InputStream 创建一个 ObjectInputStream。
	 * <p>默认实现会创建一个 Spring {@link CodebaseAwareObjectInputStream}。
	 * @param is 要读取的 InputStream
	 * @return 要使用的新 ObjectInputStream 实例
	 * @throws java.io.IOException 如果创建 ObjectInputStream 失败
	 */
	protected ObjectInputStream createObjectInputStream(InputStream is) throws IOException {
		return new CodebaseAwareObjectInputStream(is, getBeanClassLoader(), isAcceptProxyClasses());
	}

	/**
	 * 从给定的 ObjectInputStream 中执行实际的调用结果对象读取操作。
	 * <p>默认实现直接调用
	 * {@link java.io.ObjectInputStream#readObject()}。
	 * 可重写此方法以反序列化自定义包装对象（例如加密感知的持有者），
	 * 而非直接反序列化普通的调用对象。
	 * @param ois 要读取的 ObjectInputStream
	 * @return RemoteInvocationResult 对象
	 * @throws java.io.IOException 发生 I/O 故障时
	 * @throws ClassNotFoundException 如果在本地 ClassLoader 中找不到传输的类
	 */
	protected RemoteInvocation doReadRemoteInvocation(ObjectInputStream ois)
			throws IOException, ClassNotFoundException {

		Object obj = ois.readObject();
		if (!(obj instanceof RemoteInvocation)) {
			throw new RemoteException("Deserialized object needs to be assignable to type [" +
					RemoteInvocation.class.getName() + "]: " + ClassUtils.getDescriptiveType(obj));
		}
		return (RemoteInvocation) obj;
	}

	/**
	 * 为给定的 OutputStream 创建一个 ObjectOutputStream。
	 * <p>默认实现创建一个普通的
	 * {@link java.io.ObjectOutputStream}。
	 * @param os 要写入的 OutputStream
	 * @return 要使用的新 ObjectOutputStream 实例
	 * @throws java.io.IOException 如果创建 ObjectOutputStream 失败
	 */
	protected ObjectOutputStream createObjectOutputStream(OutputStream os) throws IOException {
		return new ObjectOutputStream(os);
	}

	/**
	 * 将给定的调用结果对象写入给定的 ObjectOutputStream。
	 * <p>默认实现直接调用
	 * {@link java.io.ObjectOutputStream#writeObject}。
	 * 可重写此方法以序列化自定义包装对象（例如加密感知的持有者），
	 * 而非直接序列化普通的调用结果对象。
	 * @param result RemoteInvocationResult 对象
	 * @param oos 要写入的 ObjectOutputStream
	 * @throws java.io.IOException 如果 I/O 方法抛出异常
	 */
	protected void doWriteRemoteInvocationResult(RemoteInvocationResult result, ObjectOutputStream oos)
			throws IOException {

		oos.writeObject(result);
	}

}
