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

package org.springframework.util;

import org.springframework.lang.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * 辅助类，允许以声明式方式指定要调用的方法，支持静态和非静态方法。
 *
 * <p>用法：指定 "targetClass"/"targetMethod" 或 "targetObject"/"targetMethod"，
 * 可选指定参数，调用 prepare 方法准备。之后可以多次调用 invoke 方法，获取调用结果。
 *
 * @author Colin Sampaleanu
 * @author Juergen Hoeller
 * @since 2004-02-19
 * @see #prepare
 * @see #invoke
 */
public class MethodInvoker {

	private static final Object[] EMPTY_ARGUMENTS = new Object[0];


	@Nullable
	protected Class<?> targetClass;

	@Nullable
	private Object targetObject;

	@Nullable
	private String targetMethod;

	@Nullable
	private String staticMethod;

	@Nullable
	private Object[] arguments;

	/** 将调用的方法对象。 */
	@Nullable
	private Method methodObject;


	/**
	 * 设置调用目标方法的目标类。
	 * 仅在目标方法为静态方法时需要设置；
	 * 否则需要指定目标对象。
	 * @see #setTargetObject
	 * @see #setTargetMethod
	 */
	public void setTargetClass(@Nullable Class<?> targetClass) {
		this.targetClass = targetClass;
	}

	/**
	 * 返回将要调用目标方法的目标类。
	 */
	@Nullable
	public Class<?> getTargetClass() {
		return this.targetClass;
	}

	/**
	 * 设置调用目标方法的目标对象。
	 * 仅在目标方法非静态时需要设置；
	 * 否则只需设置目标类即可。
	 * @see #setTargetClass
	 * @see #setTargetMethod
	 */
	public void setTargetObject(@Nullable Object targetObject) {
		this.targetObject = targetObject;
		if (targetObject != null) {
			this.targetClass = targetObject.getClass();
		}
	}

	/**
	 * 返回调用目标方法的目标对象。
	 */
	@Nullable
	public Object getTargetObject() {
		return this.targetObject;
	}

	/**
	 * 设置要调用的方法名。
	 * 根据是否设置了目标对象，可指代静态方法或非静态方法。
	 * @see #setTargetClass
	 * @see #setTargetObject
	 */
	public void setTargetMethod(@Nullable String targetMethod) {
		this.targetMethod = targetMethod;
	}

	/**
	 * 返回要调用的方法名。
	 */
	@Nullable
	public String getTargetMethod() {
		return this.targetMethod;
	}

	/**
	 * 设置要调用的完整静态方法名，
	 * 例如 "example.MyExampleClass.myExampleMethod"。
	 * 这是指定目标类和目标方法的便捷替代方式。
	 * @see #setTargetClass
	 * @see #setTargetMethod
	 */
	public void setStaticMethod(String staticMethod) {
		this.staticMethod = staticMethod;
	}

	/**
	 * 设置方法调用的参数。如果未设置该属性，
	 * 或参数数组长度为 0，则认为调用的是无参数的方法。
	 */
	public void setArguments(Object... arguments) {
		this.arguments = arguments;
	}

	/**
	 * 返回方法调用的参数。
	 */
	public Object[] getArguments() {
		return (this.arguments != null ? this.arguments : EMPTY_ARGUMENTS);
	}


	/**
	 * 准备指定的方法。
	 * 之后可以多次调用该方法。
	 * @see #getPreparedMethod
	 * @see #invoke
	 */
	public void prepare() throws ClassNotFoundException, NoSuchMethodException {
		if (this.staticMethod != null) {
			int lastDotIndex = this.staticMethod.lastIndexOf('.');
			if (lastDotIndex == -1 || lastDotIndex == this.staticMethod.length()) {
				throw new IllegalArgumentException(
						"staticMethod must be a fully qualified class plus method name: " +
						"e.g. 'example.MyExampleClass.myExampleMethod'");
			}
			String className = this.staticMethod.substring(0, lastDotIndex);
			String methodName = this.staticMethod.substring(lastDotIndex + 1);
			this.targetClass = resolveClassName(className);
			this.targetMethod = methodName;
		}

		Class<?> targetClass = getTargetClass();
		String targetMethod = getTargetMethod();
		Assert.notNull(targetClass, "Either 'targetClass' or 'targetObject' is required");
		Assert.notNull(targetMethod, "Property 'targetMethod' is required");

		Object[] arguments = getArguments();
		Class<?>[] argTypes = new Class<?>[arguments.length];
		for (int i = 0; i < arguments.length; ++i) {
			argTypes[i] = (arguments[i] != null ? arguments[i].getClass() : Object.class);
		}

		// 尝试先获取精确匹配的方法
		try {
			this.methodObject = targetClass.getMethod(targetMethod, argTypes);
		}
		catch (NoSuchMethodException ex) {
			// 找不到精确匹配方法时，尝试通过自定义查找匹配方法
			this.methodObject = findMatchingMethod();
			if (this.methodObject == null) {
				throw ex;
			}
		}
	}

	/**
	 * 将给定的类名解析为 Class。
	 * <p>默认实现使用 {@code ClassUtils.forName}，
	 * 并使用线程上下文类加载器。
	 * @param className 要解析的类名
	 * @return 解析得到的 Class 对象
	 * @throws ClassNotFoundException 如果类名无效
	 */
	protected Class<?> resolveClassName(String className) throws ClassNotFoundException {
		return ClassUtils.forName(className, ClassUtils.getDefaultClassLoader());
	}

	/**
	 * 查找指定名称且参数匹配的方法。
	 * @return 匹配的方法，若无匹配返回 {@code null}
	 * @see #getTargetClass()
	 * @see #getTargetMethod()
	 * @see #getArguments()
	 */
	@Nullable
	protected Method findMatchingMethod() {
		String targetMethod = getTargetMethod();
		Object[] arguments = getArguments();
		int argCount = arguments.length;

		Class<?> targetClass = getTargetClass();
		Assert.state(targetClass != null, "No target class set");
		Method[] candidates = ReflectionUtils.getAllDeclaredMethods(targetClass);
		int minTypeDiffWeight = Integer.MAX_VALUE;
		Method matchingMethod = null;

		for (Method candidate : candidates) {
			if (candidate.getName().equals(targetMethod)) {
				if (candidate.getParameterCount() == argCount) {
					Class<?>[] paramTypes = candidate.getParameterTypes();
					int typeDiffWeight = getTypeDifferenceWeight(paramTypes, arguments);
					if (typeDiffWeight < minTypeDiffWeight) {
						minTypeDiffWeight = typeDiffWeight;
						matchingMethod = candidate;
					}
				}
			}
		}

		return matchingMethod;
	}

	/**
	 * 返回准备好的将被调用的方法对象。
	 * <p>例如可用于确定返回类型。
	 * @return 准备好的方法对象（永不为 {@code null}）
	 * @throws IllegalStateException 如果调用器尚未准备好
	 * @see #prepare
	 * @see #invoke
	 */
	public Method getPreparedMethod() throws IllegalStateException {
		if (this.methodObject == null) {
			throw new IllegalStateException("prepare() must be called prior to invoke() on MethodInvoker");
		}
		return this.methodObject;
	}

	/**
	 * 判断此调用器是否已经准备好，
	 * 即是否允许访问 {@link #getPreparedMethod()}。
	 */
	public boolean isPrepared() {
		return (this.methodObject != null);
	}

	/**
	 * 调用指定的方法。
	 * <p>调用器需要事先准备好。
	 * @return 方法调用返回的对象（可能为 null），
	 * 或者如果方法返回 void 则为 {@code null}
	 * @throws InvocationTargetException 如果目标方法抛出异常
	 * @throws IllegalAccessException 如果无法访问目标方法
	 * @see #prepare
	 */
	@Nullable
	public Object invoke() throws InvocationTargetException, IllegalAccessException {
		// 静态方法时，target 将为 {@code null}。
		Object targetObject = getTargetObject();
		Method preparedMethod = getPreparedMethod();
		if (targetObject == null && !Modifier.isStatic(preparedMethod.getModifiers())) {
			throw new IllegalArgumentException("Target method must not be non-static without a target");
		}
		ReflectionUtils.makeAccessible(preparedMethod);
		return preparedMethod.invoke(targetObject, getArguments());
	}


	/**
	 * 评估候选方法的声明参数类型与实际传入参数列表的匹配程度的算法。
	 * <p>计算一个权重，表示类型与参数间类层次结构的差异。
	 * 直接匹配（例如 Integer 类型与 Integer 实例）不增加权重——所有直接匹配权重为 0。
	 * 如果类型是 Object，参数是 Integer，则因超类（Object，层级上升 2）仍匹配，权重加 2。
	 * 如果类型是 Number，参数是 Integer，则因超类（Number，层级上升 1）匹配，权重加 1。
	 * 例如，参数类型为 Integer 的构造函数优先于参数为 Number 的构造函数，
	 * 而后者优先于参数为 Object 的构造函数。
	 * 所有参数的权重将累计。
	 * <p>注意：该算法不仅被 MethodInvoker 使用，
	 * 也是 Spring 容器中构造函数和工厂方法选择的算法（适用于宽松构造函数解析）。
	 * @param paramTypes 要匹配的参数类型数组
	 * @param args 要匹配的参数对象数组
	 * @return 所有参数累计的权重值
	 */
	public static int getTypeDifferenceWeight(Class<?>[] paramTypes, Object[] args) {
		int result = 0;
		for (int i = 0; i < paramTypes.length; i++) {
			if (!ClassUtils.isAssignableValue(paramTypes[i], args[i])) {
				return Integer.MAX_VALUE;
			}
			if (args[i] != null) {
				Class<?> paramType = paramTypes[i];
				Class<?> superClass = args[i].getClass().getSuperclass();
				while (superClass != null) {
					if (paramType.equals(superClass)) {
						result = result + 2;
						superClass = null;
					}
					else if (ClassUtils.isAssignable(paramType, superClass)) {
						result = result + 2;
						superClass = superClass.getSuperclass();
					}
					else {
						superClass = null;
					}
				}
				if (paramType.isInterface()) {
					result = result + 1;
				}
			}
		}
		return result;
	}

}
