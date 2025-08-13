/*
 * Copyright 2003,2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cglib.proxy;

import org.springframework.cglib.core.*;
import org.springframework.cglib.reflect.FastClass;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 由 {@link Enhancer} 生成的类在调用被拦截方法时将此对象传递给
 * 注册的 {@link MethodInterceptor} 对象。它可用于调用原始方法，
 * 或在同一类型的不同对象上调用相同方法。
 * @version $Id: MethodProxy.java,v 1.16 2009/01/11 20:09:48 herbyderby Exp $
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class MethodProxy {

	private Signature sig1;

	private Signature sig2;

	private CreateInfo createInfo;

	private final Object initLock = new Object();

	private volatile FastClassInfo fastClassInfo;

	/**
	 * 仅供 {@link Enhancer} 内部使用；有关类似功能，请参阅 {@link org.springframework.cglib.reflect.FastMethod} 类。
	 */
	public static MethodProxy create(Class c1, Class c2, String desc, String name1, String name2) {
		MethodProxy proxy = new MethodProxy();
		proxy.sig1 = new Signature(name1, desc);
		proxy.sig2 = new Signature(name2, desc);
		proxy.createInfo = new CreateInfo(c1, c2);
		return proxy;
	}

	private void init() {
		/*
		 * 使用 volatile 不变式允许我们原子性地初始化 FastClass 和
		 * 方法索引对。
		 *
		 * 在 Java 5 中，双重检查锁定 (Double-checked locking) 与 volatile 结合使用是安全的。
		 * 在 1.5 之前，这段代码可能会导致 fastClassInfo 被多次实例化，这似乎是无害的。
		 */
		if (fastClassInfo == null) {
			synchronized (initLock) {
				if (fastClassInfo == null) {
					CreateInfo ci = createInfo;

					FastClassInfo fci = new FastClassInfo();
					fci.f1 = helper(ci, ci.c1);
					fci.f2 = helper(ci, ci.c2);
					fci.i1 = fci.f1.getIndex(sig1);
					fci.i2 = fci.f2.getIndex(sig2);
					fastClassInfo = fci;
					createInfo = null;
				}
			}
		}
	}


	private static class FastClassInfo {

		FastClass f1;

		FastClass f2;

		int i1;

		int i2;
	}


	private static class CreateInfo {

		Class c1;

		Class c2;

		NamingPolicy namingPolicy;

		GeneratorStrategy strategy;

		boolean attemptLoad;

		public CreateInfo(Class c1, Class c2) {
			this.c1 = c1;
			this.c2 = c2;
			AbstractClassGenerator fromEnhancer = AbstractClassGenerator.getCurrent();
			if (fromEnhancer != null) {
				namingPolicy = fromEnhancer.getNamingPolicy();
				strategy = fromEnhancer.getStrategy();
				attemptLoad = fromEnhancer.getAttemptLoad();
			}
		}
	}


	private static FastClass helper(CreateInfo ci, Class type) {
		FastClass.Generator g = new FastClass.Generator();
		g.setType(type);
		// SPRING补丁开始
		g.setContextClass(type);
		// SPRING补丁结束
		g.setClassLoader(ci.c2.getClassLoader());
		g.setNamingPolicy(ci.namingPolicy);
		g.setStrategy(ci.strategy);
		g.setAttemptLoad(ci.attemptLoad);
		return g.create();
	}

	private MethodProxy() {
	}

	/**
	 * 返回代理方法的签名。
	 */
	public Signature getSignature() {
		return sig1;
	}

	/**
	 * 返回 CGLIB 创建的合成方法的名称，
	 * 该方法由 {@link #invokeSuper} 用于调用超类（未拦截的）方法实现。
	 * 参数类型与代理方法相同。
	 */
	public String getSuperName() {
		return sig2.getName();
	}

	/**
	 * 返回 {@link org.springframework.cglib.reflect.FastClass} 方法索引，
	 * 用于 {@link #invokeSuper} 使用的方法。此索引唯一标识
	 * 生成的代理中的方法，因此可用于引用外部元数据。
	 * @see #getSuperName
	 */
	public int getSuperIndex() {
		init();
		return fastClassInfo.i2;
	}

	// 用于测试
	FastClass getFastClass() {
		init();
		return fastClassInfo.f1;
	}

	// 用于测试
	FastClass getSuperFastClass() {
		init();
		return fastClassInfo.f2;
	}

	/**
	 * 返回用于拦截与给定签名匹配的方法的 <code>MethodProxy</code>。
	 * @param type 由 Enhancer 生成的类
	 * @param sig 要匹配的签名
	 * @return MethodProxy 实例，如果未找到适用的匹配方法则返回 null
	 * @throws IllegalArgumentException 如果该类不是由 Enhancer 创建的，或者未使用 MethodInterceptor
	 */
	public static MethodProxy find(Class type, Signature sig) {
		try {
			Method m = type.getDeclaredMethod(MethodInterceptorGenerator.FIND_PROXY_NAME,
					MethodInterceptorGenerator.FIND_PROXY_TYPES);
			return (MethodProxy) m.invoke(null, new Object[]{sig});
		}
		catch (NoSuchMethodException ex) {
			throw new IllegalArgumentException("Class " + type + " does not use a MethodInterceptor");
		}
		catch (IllegalAccessException | InvocationTargetException ex) {
			throw new CodeGenerationException(ex);
		}
	}

	/**
	 * 在同一类型的不同对象上调用原始方法。
	 * @param obj 兼容的对象；如果您使用作为 MethodInterceptor 的第一个参数传递的对象（通常不是您想要的），
	 * 则会导致递归
	 * @param args 传递给被拦截方法的参数；您可以替换不同的参数数组，只要类型兼容
	 * @throws Throwable 被调用方法抛出的裸露异常会直接传递，而不会封装在 <code>InvocationTargetException</code> 中
	 * @see MethodInterceptor#intercept
	 */
	public Object invoke(Object obj, Object[] args) throws Throwable {
		try {
			init();
			FastClassInfo fci = fastClassInfo;
			return fci.f1.invoke(fci.i1, obj, args);
		}
		catch (InvocationTargetException ex) {
			throw ex.getTargetException();
		}
		catch (IllegalArgumentException ex) {
			if (fastClassInfo.i1 < 0)
				throw new IllegalArgumentException("Protected method: " + sig1);
			throw ex;
		}
	}

	/**
	 * 在指定对象上调用原始（父类）方法。
	 * @param obj 增强的对象，必须是作为 MethodInterceptor 的第一个参数传递的对象
	 * @param args 传递给被拦截方法的参数；您可以替换不同的参数数组，只要类型兼容
	 * @throws Throwable 被调用方法抛出的裸露异常会直接传递，而不会封装在 <code>InvocationTargetException</code> 中
	 * @see MethodInterceptor#intercept
	 */
	public Object invokeSuper(Object obj, Object[] args) throws Throwable {
		try {
			init();
			FastClassInfo fci = fastClassInfo;
			return fci.f2.invoke(fci.i2, obj, args);
		}
		catch (InvocationTargetException e) {
			throw e.getTargetException();
		}
	}

}
