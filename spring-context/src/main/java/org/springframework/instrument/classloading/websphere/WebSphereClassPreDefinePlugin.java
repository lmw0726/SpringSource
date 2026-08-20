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

package org.springframework.instrument.classloading.websphere;

import java.lang.instrument.ClassFileTransformer;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.security.CodeSource;

import org.springframework.util.FileCopyUtils;

/**
 * 实现 WebSphere 7.0 ClassPreProcessPlugin 接口的适配器，
 * 内部委托给标准的 JDK {@link ClassFileTransformer}。
 *
 * <p>为避免对供应商 API 的编译时检查，使用了动态代理。
 *
 * @author Costin Leau
 * @since 3.1
 */
class WebSphereClassPreDefinePlugin implements InvocationHandler {

	private final ClassFileTransformer transformer;


	/**
	 * 创建一个新的 {@link WebSphereClassPreDefinePlugin}。
	 * @param transformer 要适配的 {@link ClassFileTransformer}（不能为 {@code null}）
	 */
	public WebSphereClassPreDefinePlugin(ClassFileTransformer transformer) {
		this.transformer = transformer;
		ClassLoader classLoader = transformer.getClass().getClassLoader();

		// 首先通过在虚拟类上调用转换来强制织入器的完整类加载
		try {
			String dummyClass = Dummy.class.getName().replace('.', '/');
			byte[] bytes = FileCopyUtils.copyToByteArray(classLoader.getResourceAsStream(dummyClass + ".class"));
			transformer.transform(classLoader, dummyClass, null, null, bytes);
		}
		catch (Throwable ex) {
			throw new IllegalArgumentException("Cannot load transformer", ex);
		}
	}


	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		switch (method.getName()) {
			case "equals":
				return (proxy == args[0]);
			case "hashCode":
				return hashCode();
			case "toString":
				return toString();
			case "transformClass":
				return transform((String) args[0], (byte[]) args[1], (CodeSource) args[2], (ClassLoader) args[3]);
			default:
				throw new IllegalArgumentException("Unknown method: " + method);
		}
	}

	protected byte[] transform(String className, byte[] classfileBuffer, CodeSource codeSource, ClassLoader classLoader)
			throws Exception {

		// 注意：WebSphere 传递的 className 是不带类的 "."，而转换器期望的是 VM 的 "/" 格式
		byte[] result = this.transformer.transform(classLoader, className.replace('.', '/'), null, null, classfileBuffer);
		return (result != null ? result : classfileBuffer);
	}

	@Override
	public String toString() {
		return getClass().getName() + " for transformer: " + this.transformer;
	}


	private static class Dummy {
	}

}
