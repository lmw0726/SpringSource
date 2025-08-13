/*
 * Copyright 2002,2003,2004 The Apache Software Foundation
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

import org.springframework.asm.ClassVisitor;
import org.springframework.asm.Label;
import org.springframework.asm.Type;
import org.springframework.cglib.core.*;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.*;

/**
 * 生成动态子类以实现方法拦截。
 * 该类最初是 JDK 1.3 标准动态代理支持的替代方案，
 * 允许代理类继承具体的基类，除了实现接口之外。
 * 动态生成的子类会重写超类中非 final 的方法，
 * 并通过钩子回调用户定义的拦截器实现。
 *
 * <p>最原始也是最通用的回调类型是 {@link MethodInterceptor}，
 * 在 AOP 术语中，支持“环绕通知”，即你可以在调用“超类”方法之前和之后执行自定义代码。
 * 此外，还可以在调用超类方法之前修改参数，或者根本不调用它。
 *
 * <p>虽然 {@code MethodInterceptor} 足够通用以满足各种拦截需求，
 * 但它通常功能过于强大，可能导致复杂度增加。
 * 为简化和提升性能，还提供了如 {@link LazyLoader} 等专用回调类型。
 * 通常每个增强类使用单一回调，但可以通过 {@link CallbackFilter} 按方法控制使用哪种回调。
 *
 * <p>本类的最常见用途体现在静态辅助方法中。
 * 对于高级需求，例如自定义使用的 {@code ClassLoader}，
 * 应当创建新的 {@code Enhancer} 实例。CGLIB 其他类也采用类似设计模式。
 *
 * <p>除非显式调用 {@link #setUseFactory} 禁用此功能，
 * 所有增强对象均实现 {@link Factory} 接口。
 * {@code Factory} 接口提供了修改现有对象回调以及快速创建相同类型新实例的 API。
 *
 * <p>如果需要几乎无缝替代 {@code java.lang.reflect.Proxy}，
 * 可使用 {@link Proxy} 类。
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class Enhancer extends AbstractClassGenerator {

	private static final CallbackFilter ALL_ZERO = new CallbackFilter() {
		public int accept(Method method) {
			return 0;
		}
	};

	private static final Source SOURCE = new Source(Enhancer.class.getName());

	private static final EnhancerKey KEY_FACTORY =
			(EnhancerKey) KeyFactory.create(EnhancerKey.class, KeyFactory.HASH_ASM_TYPE, null);

	private static final String BOUND_FIELD = "CGLIB$BOUND";

	private static final String FACTORY_DATA_FIELD = "CGLIB$FACTORY_DATA";

	private static final String THREAD_CALLBACKS_FIELD = "CGLIB$THREAD_CALLBACKS";

	private static final String STATIC_CALLBACKS_FIELD = "CGLIB$STATIC_CALLBACKS";

	private static final String SET_THREAD_CALLBACKS_NAME = "CGLIB$SET_THREAD_CALLBACKS";

	private static final String SET_STATIC_CALLBACKS_NAME = "CGLIB$SET_STATIC_CALLBACKS";

	private static final String CONSTRUCTED_FIELD = "CGLIB$CONSTRUCTED";

	/**
	 * {@link org.springframework.cglib.core.AbstractClassGenerator.ClassLoaderData#generatedClasses} 需要保持缓存键的良好状态
	 * （当代理类存活时，这些键应保持有效），而其中一个缓存键是 {@link CallbackFilter}。
	 * 因此，生成的类包含一个静态字段，该字段持有对 {@link #filter} 的强引用。
	 * <p>这种设计达成了两个目的：确保生成的类可重用并可通过 generatedClasses 缓存访问，
	 * 同时允许在用户不再需要时卸载类加载器及相关的 {@link CallbackFilter}。</p>
	 */
	private static final String CALLBACK_FILTER_FIELD = "CGLIB$CALLBACK_FILTER";

	private static final Type OBJECT_TYPE =
			TypeUtils.parseType("Object");

	private static final Type FACTORY =
			TypeUtils.parseType("org.springframework.cglib.proxy.Factory");

	private static final Type ILLEGAL_STATE_EXCEPTION =
			TypeUtils.parseType("IllegalStateException");

	private static final Type ILLEGAL_ARGUMENT_EXCEPTION =
			TypeUtils.parseType("IllegalArgumentException");

	private static final Type THREAD_LOCAL =
			TypeUtils.parseType("ThreadLocal");

	private static final Type CALLBACK =
			TypeUtils.parseType("org.springframework.cglib.proxy.Callback");

	private static final Type CALLBACK_ARRAY =
			Type.getType(Callback[].class);

	private static final Signature CSTRUCT_NULL =
			TypeUtils.parseConstructor("");

	private static final Signature SET_THREAD_CALLBACKS =
			new Signature(SET_THREAD_CALLBACKS_NAME, Type.VOID_TYPE, new Type[]{CALLBACK_ARRAY});

	private static final Signature SET_STATIC_CALLBACKS =
			new Signature(SET_STATIC_CALLBACKS_NAME, Type.VOID_TYPE, new Type[]{CALLBACK_ARRAY});

	private static final Signature NEW_INSTANCE =
			new Signature("newInstance", Constants.TYPE_OBJECT, new Type[]{CALLBACK_ARRAY});

	private static final Signature MULTIARG_NEW_INSTANCE =
			new Signature("newInstance", Constants.TYPE_OBJECT, new Type[]{
					Constants.TYPE_CLASS_ARRAY,
					Constants.TYPE_OBJECT_ARRAY,
					CALLBACK_ARRAY,
			});

	private static final Signature SINGLE_NEW_INSTANCE =
			new Signature("newInstance", Constants.TYPE_OBJECT, new Type[]{CALLBACK});

	private static final Signature SET_CALLBACK =
			new Signature("setCallback", Type.VOID_TYPE, new Type[]{Type.INT_TYPE, CALLBACK});

	private static final Signature GET_CALLBACK =
			new Signature("getCallback", CALLBACK, new Type[]{Type.INT_TYPE});

	private static final Signature SET_CALLBACKS =
			new Signature("setCallbacks", Type.VOID_TYPE, new Type[]{CALLBACK_ARRAY});

	private static final Signature GET_CALLBACKS =
			new Signature("getCallbacks", CALLBACK_ARRAY, new Type[0]);

	private static final Signature THREAD_LOCAL_GET =
			TypeUtils.parseSignature("Object get()");

	private static final Signature THREAD_LOCAL_SET =
			TypeUtils.parseSignature("void set(Object)");

	private static final Signature BIND_CALLBACKS =
			TypeUtils.parseSignature("void CGLIB$BIND_CALLBACKS(Object)");

	private EnhancerFactoryData currentData;

	private Object currentKey;


	/**
	 * 内部接口，仅因 ClassLoader 问题而公开。
	 */
	public interface EnhancerKey {

		public Object newInstance(String type,
				String[] interfaces,
				WeakCacheKey<CallbackFilter> filter,
				Type[] callbackTypes,
				boolean useFactory,
				boolean interceptDuringConstruction,
				Long serialVersionUID);
	}


	private Class[] interfaces;

	private CallbackFilter filter;

	private Callback[] callbacks;

	private Type[] callbackTypes;

	private boolean validateCallbackTypes;

	private boolean classOnly;

	private Class superclass;

	private Class[] argumentTypes;

	private Object[] arguments;

	private boolean useFactory = true;

	private Long serialVersionUID;

	private boolean interceptDuringConstruction = true;

	/**
	 * 创建一个新的 `Enhancer`。每个生成的对象都应使用一个新的 `Enhancer` 对象，并且不应在线程之间共享。
	 * 要创建生成类的其他实例，请使用 `Factory` 接口。
	 * @see Factory
	 */
	public Enhancer() {
		super(SOURCE);
	}

	/**
	 * 设置生成的类将要继承的类。为方便起见，如果提供的父类实际上是一个接口，
	 * 将调用 `setInterfaces` 并传入适当的参数。非接口参数不能被声明为 final，并且必须有一个可访问的构造函数。
	 * @param superclass 要继承的类或要实现的接口。
	 * @see #setInterfaces(Class[])
	 */
	public void setSuperclass(Class superclass) {
		if (superclass != null && superclass.isInterface()) {
			setInterfaces(new Class[]{superclass});
			// SPRING补丁开始
			setContextClass(superclass);
			// SPRING补丁结束
		}
		else if (superclass != null && superclass.equals(Object.class)) {
			// 影响类加载器的选择
			this.superclass = null;
		}
		else {
			this.superclass = superclass;
			// SPRING补丁开始
			setContextClass(superclass);
			// SPRING补丁结束
		}
	}

	/**
	 * 设置要实现的接口。无论在此处指定什么，`Factory` 接口都将始终被实现。
	 * @param interfaces 要实现的接口数组，或为 null。
	 * @see Factory
	 */
	public void setInterfaces(Class[] interfaces) {
		this.interfaces = interfaces;
	}

	/**
	 * 设置用于将生成类的**方法**映射到特定**回调索引**的 {@link CallbackFilter}。
	 * 新的对象实例将始终使用相同的映射，但可能会使用不同的实际回调对象。
	 * @param filter 生成新类时使用的回调过滤器。
	 * @see #setCallbacks
	 */
	public void setCallbackFilter(CallbackFilter filter) {
		this.filter = filter;
	}


	/**
	 * 设置要使用的单个 {@link Callback}。
	 * 如果您使用 {@link #createClass}，则此方法将被忽略。
	 * @param callback 用于所有方法的回调。
	 * @see #setCallbacks
	 */
	public void setCallback(final Callback callback) {
		setCallbacks(new Callback[]{callback});
	}

	/**
	 * 设置要使用的回调数组。
	 * 如果您使用 {@link #createClass}，则此方法将被忽略。
	 * 您必须使用 {@link CallbackFilter} 为代理类中的每个方法指定此数组的索引。
	 * @param callbacks 回调数组。
	 * @see #setCallbackFilter
	 * @see #setCallback
	 */
	public void setCallbacks(Callback[] callbacks) {
		if (callbacks != null && callbacks.length == 0) {
			throw new IllegalArgumentException("Array cannot be empty");
		}
		this.callbacks = callbacks;
	}

	/**
	 * 设置增强的对象实例是否应该实现 {@link Factory} 接口。
	 * 添加此功能是为了方便那些需要代理对象与其目标对象更难区分的工具。
	 * 此外，在某些情况下，可能需要禁用 <code>Factory</code> 接口以防止代码更改底层回调。
	 * @param useFactory 是否实现 <code>Factory</code>；默认为 <code>true</code>
	 */
	public void setUseFactory(boolean useFactory) {
		this.useFactory = useFactory;
	}

	/**
	 * 设置从代理的构造函数内部调用的方法是否会被拦截。默认值为 true。
	 * 未被拦截的方法（如果存在）将调用代理基类的方法。
	 * @param interceptDuringConstruction 是否在构造期间拦截方法
	 */
	public void setInterceptDuringConstruction(boolean interceptDuringConstruction) {
		this.interceptDuringConstruction = interceptDuringConstruction;
	}

	/**
	 * 设置要使用的单一类型 {@link Callback}。
	 * 在调用 {@link #createClass} 时，可以使用此方法代替 {@link #setCallback}，
	 * 因为此时可能无法拥有实际的回调实例数组。
	 * @param callbackType 用于所有方法的回调类型
	 * @see #setCallbackTypes
	 */
	public void setCallbackType(Class callbackType) {
		setCallbackTypes(new Class[]{callbackType});
	}

	/**
	 * 设置要使用的回调类型数组。
	 * 在调用 {@link #createClass} 时，可以使用此方法代替 {@link #setCallbacks}，
	 * 因为此时可能无法拥有实际的回调实例数组。
	 * 您必须使用 {@link CallbackFilter} 为代理类中的每个方法指定此数组的索引。
	 * @param callbackTypes 回调类型数组
	 */
	public void setCallbackTypes(Class[] callbackTypes) {
		if (callbackTypes != null && callbackTypes.length == 0) {
			throw new IllegalArgumentException("Array cannot be empty");
		}
		this.callbackTypes = CallbackInfo.determineTypes(callbackTypes);
	}

	/**
	 * 如果有必要，生成一个新类，并使用指定的回调（如果有）创建一个新的对象实例。
	 * 使用超类的无参构造函数。
	 * @return 一个新实例
	 */
	public Object create() {
		classOnly = false;
		argumentTypes = null;
		return createHelper();
	}

	/**
	 * 如果有必要，生成一个新类，并使用指定的回调（如果有）创建一个新的对象实例。
	 * 使用与 <code>argumentTypes</code> 参数匹配的超类构造函数，并传入给定的参数。
	 * @param argumentTypes 构造函数签名
	 * @param arguments 传递给构造函数的兼容包装参数
	 * @return 一个新实例
	 */
	public Object create(Class[] argumentTypes, Object[] arguments) {
		classOnly = false;
		if (argumentTypes == null || arguments == null || argumentTypes.length != arguments.length) {
			throw new IllegalArgumentException("Arguments must be non-null and of equal length");
		}
		this.argumentTypes = argumentTypes;
		this.arguments = arguments;
		return createHelper();
	}

	/**
	 * 如果有必要，生成一个新类并返回它，而不创建新实例。
	 * 这会忽略所有已设置的回调。
	 * 要创建新实例，您将不得不使用反射，并且在构造函数期间调用的方法将不会被拦截。
	 * 为了避免这个问题，请使用多参数的 <code>create</code> 方法。
	 * @see #create(Class[], Object[])
	 */
	public Class createClass() {
		classOnly = true;
		return (Class) createHelper();
	}

	/**
	 * 在生成的类中插入一个静态的 serialVersionUID 字段。
	 * @param sUID 字段值，如果为 null 则不生成该字段。
	 */
	public void setSerialVersionUID(Long sUID) {
		serialVersionUID = sUID;
	}

	private void preValidate() {
		if (callbackTypes == null) {
			callbackTypes = CallbackInfo.determineTypes(callbacks, false);
			validateCallbackTypes = true;
		}
		if (filter == null) {
			if (callbackTypes.length > 1) {
				throw new IllegalStateException("Multiple callback types possible but no filter specified");
			}
			filter = ALL_ZERO;
		}
	}

	private void validate() {
		if (classOnly ^ (callbacks == null)) {
			if (classOnly) {
				throw new IllegalStateException("createClass does not accept callbacks");
			}
			else {
				throw new IllegalStateException("Callbacks are required");
			}
		}
		if (classOnly && (callbackTypes == null)) {
			throw new IllegalStateException("Callback types are required");
		}
		if (validateCallbackTypes) {
			callbackTypes = null;
		}
		if (callbacks != null && callbackTypes != null) {
			if (callbacks.length != callbackTypes.length) {
				throw new IllegalStateException("Lengths of callback and callback types array must be the same");
			}
			Type[] check = CallbackInfo.determineTypes(callbacks);
			for (int i = 0; i < check.length; i++) {
				if (!check[i].equals(callbackTypes[i])) {
					throw new IllegalStateException("Callback " + check[i] + " is not assignable to " + callbackTypes[i]);
				}
			}
		}
		else if (callbacks != null) {
			callbackTypes = CallbackInfo.determineTypes(callbacks);
		}
		if (interfaces != null) {
			for (int i = 0; i < interfaces.length; i++) {
				if (interfaces[i] == null) {
					throw new IllegalStateException("Interfaces cannot be null");
				}
				if (!interfaces[i].isInterface()) {
					throw new IllegalStateException(interfaces[i] + " is not an interface");
				}
			}
		}
	}

	/**
	 * 此类的目的是缓存相关的 java.lang.reflect 实例，以便
	 * 代理类可以比使用 {@link ReflectUtils#newInstance(Class, Class[], Object[])}
	 * 和 {@link Enhancer#setThreadCallbacks(Class, Callback[])} 时更快地实例化。
	 */
	static class EnhancerFactoryData {

		public final Class generatedClass;

		private final Method setThreadCallbacks;

		private final Class[] primaryConstructorArgTypes;

		private final Constructor primaryConstructor;

		public EnhancerFactoryData(Class generatedClass, Class[] primaryConstructorArgTypes, boolean classOnly) {
			this.generatedClass = generatedClass;
			try {
				setThreadCallbacks = getCallbacksSetter(generatedClass, SET_THREAD_CALLBACKS_NAME);
				if (classOnly) {
					this.primaryConstructorArgTypes = null;
					this.primaryConstructor = null;
				}
				else {
					this.primaryConstructorArgTypes = primaryConstructorArgTypes;
					this.primaryConstructor = ReflectUtils.getConstructor(generatedClass, primaryConstructorArgTypes);
				}
			}
			catch (NoSuchMethodException e) {
				throw new CodeGenerationException(e);
			}
		}

		/**
		 * 为给定的参数类型创建代理实例，并分配回调。
		 * 理想情况下，对于每个代理类，应该只使用一组参数类型，
		 * 否则它将不得不花费时间进行构造函数查找。
		 * 从技术上讲，它是 {@link Enhancer#createUsingReflection(Class)} 的重新实现，
		 * 带有“缓存 {@link #setThreadCallbacks} 和 {@link #primaryConstructor}”。
		 * @param argumentTypes 构造函数参数类型
		 * @param arguments 构造函数参数
		 * @param callbacks 为新实例设置的回调
		 * @return 新创建的代理
		 * @see #createUsingReflection(Class)
		 */
		public Object newInstance(Class[] argumentTypes, Object[] arguments, Callback[] callbacks) {
			setThreadCallbacks(callbacks);
			try {
				// 这里添加显式引用相等检查以防 Arrays.equals 没有
				if (primaryConstructorArgTypes == argumentTypes ||
						Arrays.equals(primaryConstructorArgTypes, argumentTypes)) {
					// 如果我们手头有相关的 Constructor 实例，直接调用它
					// 这跳过了“获取构造函数”的机制
					return ReflectUtils.newInstance(primaryConstructor, arguments);
				}
				// 如果观察到意外的参数类型，则走慢路径
				return ReflectUtils.newInstance(generatedClass, argumentTypes, arguments);
			}
			finally {
				// 清除线程回调，以便它们可以被垃圾回收
				setThreadCallbacks(null);
			}

		}

		private void setThreadCallbacks(Callback[] callbacks) {
			try {
				setThreadCallbacks.invoke(generatedClass, (Object) callbacks);
			}
			catch (IllegalAccessException e) {
				throw new CodeGenerationException(e);
			}
			catch (InvocationTargetException e) {
				throw new CodeGenerationException(e.getTargetException());
			}
		}
	}

	private Object createHelper() {
		preValidate();
		Object key = KEY_FACTORY.newInstance((superclass != null) ? superclass.getName() : null,
				ReflectUtils.getNames(interfaces),
				filter == ALL_ZERO ? null : new WeakCacheKey<CallbackFilter>(filter),
				callbackTypes,
				useFactory,
				interceptDuringConstruction,
				serialVersionUID);
		this.currentKey = key;
		Object result = super.create(key);
		return result;
	}

	@Override
	protected Class generate(ClassLoaderData data) {
		validate();
		if (superclass != null) {
			setNamePrefix(superclass.getName());
		}
		else if (interfaces != null) {
			setNamePrefix(interfaces[ReflectUtils.findPackageProtected(interfaces)].getName());
		}
		return super.generate(data);
	}

	protected ClassLoader getDefaultClassLoader() {
		if (superclass != null) {
			return superclass.getClassLoader();
		}
		else if (interfaces != null) {
			return interfaces[0].getClassLoader();
		}
		else {
			return null;
		}
	}

	protected ProtectionDomain getProtectionDomain() {
		if (superclass != null) {
			return ReflectUtils.getProtectionDomain(superclass);
		}
		else if (interfaces != null) {
			return ReflectUtils.getProtectionDomain(interfaces[0]);
		}
		else {
			return null;
		}
	}

	private Signature rename(Signature sig, int index) {
		return new Signature("CGLIB$" + sig.getName() + "$" + index,
				sig.getDescriptor());
	}

	/**
	 * 查找将由 Enhancer 生成的类使用指定超类和接口扩展的所有方法。
	 * 这对于构建 Callback 对象列表很有用。方法将添加到给定列表的末尾。
	 * 由于 Enhancer 生成的类的子类化特性，这些方法保证是非静态、非 final 和非私有的。
	 * 每个方法签名只会出现一次，即使它出现在多个类中。
	 * @param superclass 将被扩展的类，如果为 null 则表示没有超类
	 * @param interfaces 将被实现的接口数组，如果为 null 则表示没有接口
	 * @param methods 用于复制适用方法的列表
	 */
	public static void getMethods(Class superclass, Class[] interfaces, List methods) {
		getMethods(superclass, interfaces, methods, null, null);
	}

	private static void getMethods(Class superclass, Class[] interfaces, List methods, List interfaceMethods, Set forcePublic) {
		ReflectUtils.addAllMethods(superclass, methods);
		List target = (interfaceMethods != null) ? interfaceMethods : methods;
		if (interfaces != null) {
			for (int i = 0; i < interfaces.length; i++) {
				if (interfaces[i] != Factory.class) {
					ReflectUtils.addAllMethods(interfaces[i], target);
				}
			}
		}
		if (interfaceMethods != null) {
			if (forcePublic != null) {
				forcePublic.addAll(MethodWrapper.createSet(interfaceMethods));
			}
			methods.addAll(interfaceMethods);
		}
		CollectionUtils.filter(methods, new RejectModifierPredicate(Constants.ACC_STATIC));
		CollectionUtils.filter(methods, new VisibilityPredicate(superclass, true));
		CollectionUtils.filter(methods, new DuplicatesPredicate());
		CollectionUtils.filter(methods, new RejectModifierPredicate(Constants.ACC_FINAL));
	}

	public void generateClass(ClassVisitor v) throws Exception {
		Class sc = (superclass == null) ? Object.class : superclass;

		if (TypeUtils.isFinal(sc.getModifiers()))
			throw new IllegalArgumentException("Cannot subclass final class " + sc.getName());
		List constructors = new ArrayList(Arrays.asList(sc.getDeclaredConstructors()));
		filterConstructors(sc, constructors);

		// 顺序非常重要：必须先添加超类，然后是
		// 它的超类链，然后是每个接口及其父接口。
		List actualMethods = new ArrayList();
		List interfaceMethods = new ArrayList();
		final Set forcePublic = new HashSet();
		getMethods(sc, interfaces, actualMethods, interfaceMethods, forcePublic);

		List methods = CollectionUtils.transform(actualMethods, new Transformer() {
			public Object transform(Object value) {
				Method method = (Method) value;
				int modifiers = Constants.ACC_FINAL
						| (method.getModifiers()
						& ~Constants.ACC_ABSTRACT
						& ~Constants.ACC_NATIVE
						& ~Constants.ACC_SYNCHRONIZED);
				if (forcePublic.contains(MethodWrapper.create(method))) {
					modifiers = (modifiers & ~Constants.ACC_PROTECTED) | Constants.ACC_PUBLIC;
				}
				return ReflectUtils.getMethodInfo(method, modifiers);
			}
		});

		ClassEmitter e = new ClassEmitter(v);
		if (currentData == null) {
			e.begin_class(Constants.V1_8,
					Constants.ACC_PUBLIC,
					getClassName(),
					Type.getType(sc),
					(useFactory ?
							TypeUtils.add(TypeUtils.getTypes(interfaces), FACTORY) :
							TypeUtils.getTypes(interfaces)),
					Constants.SOURCE_FILE);
		}
		else {
			e.begin_class(Constants.V1_8,
					Constants.ACC_PUBLIC,
					getClassName(),
					null,
					new Type[]{FACTORY},
					Constants.SOURCE_FILE);
		}
		List constructorInfo = CollectionUtils.transform(constructors, MethodInfoTransformer.getInstance());

		e.declare_field(Constants.ACC_PRIVATE, BOUND_FIELD, Type.BOOLEAN_TYPE, null);
		e.declare_field(Constants.ACC_PUBLIC | Constants.ACC_STATIC, FACTORY_DATA_FIELD, OBJECT_TYPE, null);
		if (!interceptDuringConstruction) {
			e.declare_field(Constants.ACC_PRIVATE, CONSTRUCTED_FIELD, Type.BOOLEAN_TYPE, null);
		}
		e.declare_field(Constants.PRIVATE_FINAL_STATIC, THREAD_CALLBACKS_FIELD, THREAD_LOCAL, null);
		e.declare_field(Constants.PRIVATE_FINAL_STATIC, STATIC_CALLBACKS_FIELD, CALLBACK_ARRAY, null);
		if (serialVersionUID != null) {
			e.declare_field(Constants.PRIVATE_FINAL_STATIC, Constants.SUID_FIELD_NAME, Type.LONG_TYPE, serialVersionUID);
		}

		for (int i = 0; i < callbackTypes.length; i++) {
			e.declare_field(Constants.ACC_PRIVATE, getCallbackField(i), callbackTypes[i], null);
		}
		// 这被声明为私有，以避免“公共字段”污染
		e.declare_field(Constants.ACC_PRIVATE | Constants.ACC_STATIC, CALLBACK_FILTER_FIELD, OBJECT_TYPE, null);

		if (currentData == null) {
			emitMethods(e, methods, actualMethods);
			emitConstructors(e, constructorInfo);
		}
		else {
			emitDefaultConstructor(e);
		}
		emitSetThreadCallbacks(e);
		emitSetStaticCallbacks(e);
		emitBindCallbacks(e);

		if (useFactory || currentData != null) {
			int[] keys = getCallbackKeys();
			emitNewInstanceCallbacks(e);
			emitNewInstanceCallback(e);
			emitNewInstanceMultiarg(e, constructorInfo);
			emitGetCallback(e, keys);
			emitSetCallback(e, keys);
			emitGetCallbacks(e);
			emitSetCallbacks(e);
		}

		e.end_class();
	}

	/**
	 * 过滤超类的构造函数列表。
	 * 剩余的构造函数将包含在生成的类中。
	 * 默认实现是过滤掉所有私有构造函数，但子类可以通过扩展 Enhancer 来覆盖此行为。
	 * @param sc 超类
	 * @param constructors 超类中所有声明的构造函数列表
	 * @throws IllegalArgumentException 如果没有非私有构造函数
	 */
	protected void filterConstructors(Class sc, List constructors) {
		CollectionUtils.filter(constructors, new VisibilityPredicate(sc, true));
		if (constructors.size() == 0)
			throw new IllegalArgumentException("No visible constructors in " + sc);
	}

	/**
	 * 此方法不应在常规流程中调用。
	 * 从技术上讲，{@link #wrapCachedClass(Class)} 使用 {@link Enhancer.EnhancerFactoryData} 作为缓存值，
	 * 后者比普通的反射查找和调用能够更快地实例化。
	 * 保留此方法是为了向后兼容：以防万一它被使用过。
	 * @param type 要实例化的类
	 * @return 新创建的代理实例
	 * @throws Exception 如果出现问题
	 */
	protected Object firstInstance(Class type) throws Exception {
		if (classOnly) {
			return type;
		}
		else {
			return createUsingReflection(type);
		}
	}

	protected Object nextInstance(Object instance) {
		EnhancerFactoryData data = (EnhancerFactoryData) instance;

		if (classOnly) {
			return data.generatedClass;
		}

		Class[] argumentTypes = this.argumentTypes;
		Object[] arguments = this.arguments;
		if (argumentTypes == null) {
			argumentTypes = Constants.EMPTY_CLASS_ARRAY;
			arguments = null;
		}
		return data.newInstance(argumentTypes, arguments, callbacks);
	}

	@Override
	protected Object wrapCachedClass(Class klass) {
		Class[] argumentTypes = this.argumentTypes;
		if (argumentTypes == null) {
			argumentTypes = Constants.EMPTY_CLASS_ARRAY;
		}
		EnhancerFactoryData factoryData = new EnhancerFactoryData(klass, argumentTypes, classOnly);
		Field factoryDataField = null;
		try {
			// 随后的“操作”对于每个类只执行一次，
			// 所以它的速度快慢并不重要。
			factoryDataField = klass.getField(FACTORY_DATA_FIELD);
			factoryDataField.set(null, factoryData);
			Field callbackFilterField = klass.getDeclaredField(CALLBACK_FILTER_FIELD);
			callbackFilterField.setAccessible(true);
			callbackFilterField.set(null, this.filter);
		}
		catch (NoSuchFieldException e) {
			throw new CodeGenerationException(e);
		}
		catch (IllegalAccessException e) {
			throw new CodeGenerationException(e);
		}
		return new WeakReference<EnhancerFactoryData>(factoryData);
	}

	@Override
	protected Object unwrapCachedValue(Object cached) {
		if (currentKey instanceof EnhancerKey) {
			EnhancerFactoryData data = ((WeakReference<EnhancerFactoryData>) cached).get();
			return data;
		}
		return super.unwrapCachedValue(cached);
	}

	/**
	 * 调用此方法以在通过反射创建生成的类的新实例之前，注册要使用的 {@link Callback} 数组。
	 * 如果您正在使用 <code>Enhancer</code> 实例或 {@link Factory} 接口来创建新实例，则此方法是不必要的。
	 * 它的主要用途是当您希望自己缓存和重用生成的类，并且生成的类
	 * <i>不</i> 实现 {@link Factory} 接口时。
	 * <p>
	 * 请注意，此方法仅在当前线程上注册回调。
	 * 如果您希望为由多个线程创建的实例注册回调，
	 * 请使用 {@link #registerStaticCallbacks}。
	 * <p>
	 * 注册的回调在调用任何 <code>create</code> 方法（例如
	 * {@link #create}）或任何 {@link Factory} <code>newInstance</code> 方法时，
	 * 将被覆盖并随后清除。否则它们<i>不会</i>被清除，如果您担心内存泄漏，
	 * 则在通过反射创建新实例后，应小心地将其设置回 <code>null</code>。
	 * @param generatedClass 之前由 {@link Enhancer} 创建的类
	 * @param callbacks 在创建生成的类实例时要使用的回调数组
	 * @see #setUseFactory
	 */
	public static void registerCallbacks(Class generatedClass, Callback[] callbacks) {
		setThreadCallbacks(generatedClass, callbacks);
	}

	/**
	 * 类似于 {@link #registerCallbacks}，但适用于
	 * 当多个线程将创建生成的类实例时。
	 * 线程级回调将始终覆盖静态回调。
	 * 静态回调永远不会被清除。
	 * @param generatedClass 之前由 {@link Enhancer} 创建的类
	 * @param callbacks 在创建生成的类实例时要使用的回调数组
	 */
	public static void registerStaticCallbacks(Class generatedClass, Callback[] callbacks) {
		setCallbacksHelper(generatedClass, callbacks, SET_STATIC_CALLBACKS_NAME);
	}

	/**
	 * 判断一个类是否是使用 <code>Enhancer</code> 生成的。
	 * @param type 任何类
	 * @return 该类是否是使用 <code>Enhancer</code> 生成的
	 */
	public static boolean isEnhanced(Class type) {
		try {
			getCallbacksSetter(type, SET_THREAD_CALLBACKS_NAME);
			return true;
		}
		catch (NoSuchMethodException e) {
			return false;
		}
	}

	private static void setThreadCallbacks(Class type, Callback[] callbacks) {
		setCallbacksHelper(type, callbacks, SET_THREAD_CALLBACKS_NAME);
	}

	private static void setCallbacksHelper(Class type, Callback[] callbacks, String methodName) {
		// TODO: optimize
		try {
			Method setter = getCallbacksSetter(type, methodName);
			setter.invoke(null, new Object[]{callbacks});
		}
		catch (NoSuchMethodException e) {
			throw new IllegalArgumentException(type + " is not an enhanced class");
		}
		catch (IllegalAccessException e) {
			throw new CodeGenerationException(e);
		}
		catch (InvocationTargetException e) {
			throw new CodeGenerationException(e);
		}
	}

	private static Method getCallbacksSetter(Class type, String methodName) throws NoSuchMethodException {
		return type.getDeclaredMethod(methodName, new Class[]{Callback[].class});
	}

	/**
	 * 实例化一个代理实例并分配回调值。
	 * 实现细节：java.lang.reflect 实例不会被缓存，因此此方法不应在热路径上使用。
	 * 当 {@link #setUseCache(boolean)} 设置为 {@code false} 时，将使用此方法。
	 * @param type 要实例化的类
	 * @return 新创建的实例
	 */
	private Object createUsingReflection(Class type) {
		setThreadCallbacks(type, callbacks);
		try {

			if (argumentTypes != null) {

				return ReflectUtils.newInstance(type, argumentTypes, arguments);

			}
			else {

				return ReflectUtils.newInstance(type);

			}
		}
		finally {
			// 清除线程回调，以便它们可以被垃圾回收
			setThreadCallbacks(type, null);
		}
	}

	/**
	 * 辅助方法，用于创建一个被拦截的对象。
	 * 为了对生成的实例进行更精细的控制，请使用 <code>Enhancer</code> 的新实例，
	 * 而不是此静态方法。
	 * @param type 要扩展的类或要实现的接口
	 * @param callback 用于所有方法的回调
	 */
	public static Object create(Class type, Callback callback) {
		Enhancer e = new Enhancer();
		e.setSuperclass(type);
		e.setCallback(callback);
		return e.create();
	}

	/**
	 * 辅助方法，用于创建一个被拦截的对象。
	 * 为了对生成的实例进行更精细的控制，请使用 <code>Enhancer</code> 的新实例，
	 * 而不是此静态方法。
	 * @param superclass 要扩展的类或要实现的接口
	 * @param interfaces 要实现的接口数组，如果没有则为 null
	 * @param callback 用于所有方法的回调
	 */
	public static Object create(Class superclass, Class interfaces[], Callback callback) {
		Enhancer e = new Enhancer();
		e.setSuperclass(superclass);
		e.setInterfaces(interfaces);
		e.setCallback(callback);
		return e.create();
	}

	/**
	 * 辅助方法，用于创建一个被拦截的对象。
	 * 为了对生成的实例进行更精细的控制，请使用 <code>Enhancer</code> 的新实例，
	 * 而不是此静态方法。
	 * @param superclass 要扩展的类或要实现的接口
	 * @param interfaces 要实现的接口数组，如果没有则为 null
	 * @param filter 生成新类时使用的回调过滤器
	 * @param callbacks 用于增强对象的 Callback 实现
	 */
	public static Object create(Class superclass, Class[] interfaces, CallbackFilter filter, Callback[] callbacks) {
		Enhancer e = new Enhancer();
		e.setSuperclass(superclass);
		e.setInterfaces(interfaces);
		e.setCallbackFilter(filter);
		e.setCallbacks(callbacks);
		return e.create();
	}

	private void emitDefaultConstructor(ClassEmitter ce) {
		Constructor<Object> declaredConstructor;
		try {
			declaredConstructor = Object.class.getDeclaredConstructor();
		}
		catch (NoSuchMethodException e) {
			throw new IllegalStateException("Object should have default constructor ", e);
		}
		MethodInfo constructor = (MethodInfo) MethodInfoTransformer.getInstance().transform(declaredConstructor);
		CodeEmitter e = EmitUtils.begin_method(ce, constructor, Constants.ACC_PUBLIC);
		e.load_this();
		e.dup();
		Signature sig = constructor.getSignature();
		e.super_invoke_constructor(sig);
		e.return_value();
		e.end_method();
	}

	private void emitConstructors(ClassEmitter ce, List constructors) {
		boolean seenNull = false;
		for (Iterator it = constructors.iterator(); it.hasNext(); ) {
			MethodInfo constructor = (MethodInfo) it.next();
			if (currentData != null && !"()V".equals(constructor.getSignature().getDescriptor())) {
				continue;
			}
			CodeEmitter e = EmitUtils.begin_method(ce, constructor, Constants.ACC_PUBLIC);
			e.load_this();
			e.dup();
			e.load_args();
			Signature sig = constructor.getSignature();
			seenNull = seenNull || sig.getDescriptor().equals("()V");
			e.super_invoke_constructor(sig);
			if (currentData == null) {
				e.invoke_static_this(BIND_CALLBACKS);
				if (!interceptDuringConstruction) {
					e.load_this();
					e.push(1);
					e.putfield(CONSTRUCTED_FIELD);
				}
			}
			e.return_value();
			e.end_method();
		}
		if (!classOnly && !seenNull && arguments == null)
			throw new IllegalArgumentException("Superclass has no null constructors but no arguments were given");
	}

	private int[] getCallbackKeys() {
		int[] keys = new int[callbackTypes.length];
		for (int i = 0; i < callbackTypes.length; i++) {
			keys[i] = i;
		}
		return keys;
	}

	private void emitGetCallback(ClassEmitter ce, int[] keys) {
		final CodeEmitter e = ce.begin_method(Constants.ACC_PUBLIC, GET_CALLBACK, null);
		e.load_this();
		e.invoke_static_this(BIND_CALLBACKS);
		e.load_this();
		e.load_arg(0);
		e.process_switch(keys, new ProcessSwitchCallback() {
			public void processCase(int key, Label end) {
				e.getfield(getCallbackField(key));
				e.goTo(end);
			}

			public void processDefault() {
				e.pop(); // 堆栈高度
				e.aconst_null();
			}
		});
		e.return_value();
		e.end_method();
	}

	private void emitSetCallback(ClassEmitter ce, int[] keys) {
		final CodeEmitter e = ce.begin_method(Constants.ACC_PUBLIC, SET_CALLBACK, null);
		e.load_arg(0);
		e.process_switch(keys, new ProcessSwitchCallback() {
			public void processCase(int key, Label end) {
				e.load_this();
				e.load_arg(1);
				e.checkcast(callbackTypes[key]);
				e.putfield(getCallbackField(key));
				e.goTo(end);
			}

			public void processDefault() {
				// TODO: error?
			}
		});
		e.return_value();
		e.end_method();
	}

	private void emitSetCallbacks(ClassEmitter ce) {
		CodeEmitter e = ce.begin_method(Constants.ACC_PUBLIC, SET_CALLBACKS, null);
		e.load_this();
		e.load_arg(0);
		for (int i = 0; i < callbackTypes.length; i++) {
			e.dup2();
			e.aaload(i);
			e.checkcast(callbackTypes[i]);
			e.putfield(getCallbackField(i));
		}
		e.return_value();
		e.end_method();
	}

	private void emitGetCallbacks(ClassEmitter ce) {
		CodeEmitter e = ce.begin_method(Constants.ACC_PUBLIC, GET_CALLBACKS, null);
		e.load_this();
		e.invoke_static_this(BIND_CALLBACKS);
		e.load_this();
		e.push(callbackTypes.length);
		e.newarray(CALLBACK);
		for (int i = 0; i < callbackTypes.length; i++) {
			e.dup();
			e.push(i);
			e.load_this();
			e.getfield(getCallbackField(i));
			e.aastore();
		}
		e.return_value();
		e.end_method();
	}

	private void emitNewInstanceCallbacks(ClassEmitter ce) {
		CodeEmitter e = ce.begin_method(Constants.ACC_PUBLIC, NEW_INSTANCE, null);
		Type thisType = getThisType(e);
		e.load_arg(0);
		e.invoke_static(thisType, SET_THREAD_CALLBACKS, false);
		emitCommonNewInstance(e);
	}

	private Type getThisType(CodeEmitter e) {
		if (currentData == null) {
			return e.getClassEmitter().getClassType();
		}
		else {
			return Type.getType(currentData.generatedClass);
		}
	}

	private void emitCommonNewInstance(CodeEmitter e) {
		Type thisType = getThisType(e);
		e.new_instance(thisType);
		e.dup();
		e.invoke_constructor(thisType);
		e.aconst_null();
		e.invoke_static(thisType, SET_THREAD_CALLBACKS, false);
		e.return_value();
		e.end_method();
	}

	private void emitNewInstanceCallback(ClassEmitter ce) {
		CodeEmitter e = ce.begin_method(Constants.ACC_PUBLIC, SINGLE_NEW_INSTANCE, null);
		switch (callbackTypes.length) {
			case 0:
				// TODO: 确保回调为null
				break;
			case 1:
				// 现在只是做一个新的数组; TODO: 优化
				e.push(1);
				e.newarray(CALLBACK);
				e.dup();
				e.push(0);
				e.load_arg(0);
				e.aastore();
				e.invoke_static(getThisType(e), SET_THREAD_CALLBACKS, false);
				break;
			default:
				e.throw_exception(ILLEGAL_STATE_EXCEPTION, "More than one callback object required");
		}
		emitCommonNewInstance(e);
	}

	private void emitNewInstanceMultiarg(ClassEmitter ce, List constructors) {
		final CodeEmitter e = ce.begin_method(Constants.ACC_PUBLIC, MULTIARG_NEW_INSTANCE, null);
		final Type thisType = getThisType(e);
		e.load_arg(2);
		e.invoke_static(thisType, SET_THREAD_CALLBACKS, false);
		e.new_instance(thisType);
		e.dup();
		e.load_arg(0);
		EmitUtils.constructor_switch(e, constructors, new ObjectSwitchCallback() {
			public void processCase(Object key, Label end) {
				MethodInfo constructor = (MethodInfo) key;
				Type types[] = constructor.getSignature().getArgumentTypes();
				for (int i = 0; i < types.length; i++) {
					e.load_arg(1);
					e.push(i);
					e.aaload();
					e.unbox(types[i]);
				}
				e.invoke_constructor(thisType, constructor.getSignature());
				e.goTo(end);
			}

			public void processDefault() {
				e.throw_exception(ILLEGAL_ARGUMENT_EXCEPTION, "Constructor not found");
			}
		});
		e.aconst_null();
		e.invoke_static(thisType, SET_THREAD_CALLBACKS, false);
		e.return_value();
		e.end_method();
	}

	private void emitMethods(final ClassEmitter ce, List methods, List actualMethods) {
		CallbackGenerator[] generators = CallbackInfo.getGenerators(callbackTypes);

		Map groups = new HashMap();
		final Map indexes = new HashMap();
		final Map originalModifiers = new HashMap();
		final Map positions = CollectionUtils.getIndexMap(methods);
		final Map declToBridge = new HashMap();

		Iterator it1 = methods.iterator();
		Iterator it2 = (actualMethods != null) ? actualMethods.iterator() : null;

		while (it1.hasNext()) {
			MethodInfo method = (MethodInfo) it1.next();
			Method actualMethod = (it2 != null) ? (Method) it2.next() : null;
			int index = filter.accept(actualMethod);
			if (index >= callbackTypes.length) {
				throw new IllegalArgumentException("Callback filter returned an index that is too large: " + index);
			}
			originalModifiers.put(method, (actualMethod != null ? actualMethod.getModifiers() : method.getModifiers()));
			indexes.put(method, index);
			List group = (List) groups.get(generators[index]);
			if (group == null) {
				groups.put(generators[index], group = new ArrayList(methods.size()));
			}
			group.add(method);

			// 优化：为类构建一个 Class -> 桥接方法的映射，以便我们可以在一次遍历中查找类的所有桥接方法。
			if (TypeUtils.isBridge(actualMethod.getModifiers())) {
				Set bridges = (Set) declToBridge.get(actualMethod.getDeclaringClass());
				if (bridges == null) {
					bridges = new HashSet();
					declToBridge.put(actualMethod.getDeclaringClass(), bridges);
				}
				bridges.add(method.getSignature());
			}
		}

		final Map bridgeToTarget = new BridgeMethodResolver(declToBridge, getClassLoader()).resolveAll();

		Set seenGen = new HashSet();
		CodeEmitter se = ce.getStaticHook();
		se.new_instance(THREAD_LOCAL);
		se.dup();
		se.invoke_constructor(THREAD_LOCAL, CSTRUCT_NULL);
		se.putfield(THREAD_CALLBACKS_FIELD);

		final Object[] state = new Object[1];
		CallbackGenerator.Context context = new CallbackGenerator.Context() {
			public ClassLoader getClassLoader() {
				return Enhancer.this.getClassLoader();
			}

			public int getOriginalModifiers(MethodInfo method) {
				return ((Integer) originalModifiers.get(method)).intValue();
			}

			public int getIndex(MethodInfo method) {
				return ((Integer) indexes.get(method)).intValue();
			}

			public void emitCallback(CodeEmitter e, int index) {
				emitCurrentCallback(e, index);
			}

			public Signature getImplSignature(MethodInfo method) {
				return rename(method.getSignature(), ((Integer) positions.get(method)).intValue());
			}

			public void emitLoadArgsAndInvoke(CodeEmitter e, MethodInfo method) {
				// 如果这是一个桥接方法，并且我们知道目标是从 invokespecial 调用的，
				// 那么我们需要用桥接目标来调用 invoke_virtual 而不是 super，
				// 因为 super 本身可能正在使用 super，这会绕过目标上的任何代理。
				Signature bridgeTarget = (Signature) bridgeToTarget.get(method.getSignature());
				if (bridgeTarget != null) {
					// 针对目标的参数类型检查每个参数的类型转换
					for (int i = 0; i < bridgeTarget.getArgumentTypes().length; i++) {
						e.load_arg(i);
						Type target = bridgeTarget.getArgumentTypes()[i];
						if (!target.equals(method.getSignature().getArgumentTypes()[i])) {
							e.checkcast(target);
						}
					}

					e.invoke_virtual_this(bridgeTarget);

					Type retType = method.getSignature().getReturnType();
					// 如果目标和桥接方法具有相同的返回类型，则无需进行类型转换。
					// （这方便地包括 void 和原始类型，如果进行类型转换则会失败。
					// 不可能从装箱类型协变到拆箱类型（反之亦然），因此无需为桥接方法进行装箱/拆箱）。
					// TODO：如果返回类型可从目标赋值，也无需进行类型转换。
					// （如果子类使用协变返回在桥接方法中缩小返回类型，则会发生这种情况）。
					if (!retType.equals(bridgeTarget.getReturnType())) {
						e.checkcast(retType);
					}
				}
				else {
					e.load_args();
					e.super_invoke(method.getSignature());
				}
			}

			public CodeEmitter beginMethod(ClassEmitter ce, MethodInfo method) {
				CodeEmitter e = EmitUtils.begin_method(ce, method);
				if (!interceptDuringConstruction &&
						!TypeUtils.isAbstract(method.getModifiers())) {
					Label constructed = e.make_label();
					e.load_this();
					e.getfield(CONSTRUCTED_FIELD);
					e.if_jump(CodeEmitter.NE, constructed);
					e.load_this();
					e.load_args();
					e.super_invoke();
					e.return_value();
					e.mark(constructed);
				}
				return e;
			}
		};
		for (int i = 0; i < callbackTypes.length; i++) {
			CallbackGenerator gen = generators[i];
			if (!seenGen.contains(gen)) {
				seenGen.add(gen);
				final List fmethods = (List) groups.get(gen);
				if (fmethods != null) {
					try {
						gen.generate(ce, context, fmethods);
						gen.generateStatic(se, context, fmethods);
					}
					catch (RuntimeException x) {
						throw x;
					}
					catch (Exception x) {
						throw new CodeGenerationException(x);
					}
				}
			}
		}
		se.return_value();
		se.end_method();
	}

	private void emitSetThreadCallbacks(ClassEmitter ce) {
		CodeEmitter e = ce.begin_method(Constants.ACC_PUBLIC | Constants.ACC_STATIC,
				SET_THREAD_CALLBACKS,
				null);
		e.getfield(THREAD_CALLBACKS_FIELD);
		e.load_arg(0);
		e.invoke_virtual(THREAD_LOCAL, THREAD_LOCAL_SET);
		e.return_value();
		e.end_method();
	}

	private void emitSetStaticCallbacks(ClassEmitter ce) {
		CodeEmitter e = ce.begin_method(Constants.ACC_PUBLIC | Constants.ACC_STATIC,
				SET_STATIC_CALLBACKS,
				null);
		e.load_arg(0);
		e.putfield(STATIC_CALLBACKS_FIELD);
		e.return_value();
		e.end_method();
	}

	private void emitCurrentCallback(CodeEmitter e, int index) {
		e.load_this();
		e.getfield(getCallbackField(index));
		e.dup();
		Label end = e.make_label();
		e.ifnonnull(end);
		e.pop(); // stack height
		e.load_this();
		e.invoke_static_this(BIND_CALLBACKS);
		e.load_this();
		e.getfield(getCallbackField(index));
		e.mark(end);
	}

	private void emitBindCallbacks(ClassEmitter ce) {
		CodeEmitter e = ce.begin_method(Constants.PRIVATE_FINAL_STATIC,
				BIND_CALLBACKS,
				null);
		Local me = e.make_local();
		e.load_arg(0);
		e.checkcast_this();
		e.store_local(me);

		Label end = e.make_label();
		e.load_local(me);
		e.getfield(BOUND_FIELD);
		e.if_jump(CodeEmitter.NE, end);
		e.load_local(me);
		e.push(1);
		e.putfield(BOUND_FIELD);

		e.getfield(THREAD_CALLBACKS_FIELD);
		e.invoke_virtual(THREAD_LOCAL, THREAD_LOCAL_GET);
		e.dup();
		Label found_callback = e.make_label();
		e.ifnonnull(found_callback);
		e.pop();

		e.getfield(STATIC_CALLBACKS_FIELD);
		e.dup();
		e.ifnonnull(found_callback);
		e.pop();
		e.goTo(end);

		e.mark(found_callback);
		e.checkcast(CALLBACK_ARRAY);
		e.load_local(me);
		e.swap();
		for (int i = callbackTypes.length - 1; i >= 0; i--) {
			if (i != 0) {
				e.dup2();
			}
			e.aaload(i);
			e.checkcast(callbackTypes[i]);
			e.putfield(getCallbackField(i));
		}

		e.mark(end);
		e.return_value();
		e.end_method();
	}

	private static String getCallbackField(int index) {
		return "CGLIB$CALLBACK_" + index;
	}

}
