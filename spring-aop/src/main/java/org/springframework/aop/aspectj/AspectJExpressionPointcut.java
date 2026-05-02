/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.aop.aspectj;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.weaver.patterns.NamePattern;
import org.aspectj.weaver.reflect.ReflectionWorld.ReflectionWorldException;
import org.aspectj.weaver.reflect.ShadowMatchImpl;
import org.aspectj.weaver.tools.ContextBasedMatcher;
import org.aspectj.weaver.tools.FuzzyBoolean;
import org.aspectj.weaver.tools.JoinPointMatch;
import org.aspectj.weaver.tools.MatchingContext;
import org.aspectj.weaver.tools.PointcutDesignatorHandler;
import org.aspectj.weaver.tools.PointcutExpression;
import org.aspectj.weaver.tools.PointcutParameter;
import org.aspectj.weaver.tools.PointcutParser;
import org.aspectj.weaver.tools.PointcutPrimitive;
import org.aspectj.weaver.tools.ShadowMatch;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.IntroductionAwareMethodMatcher;
import org.springframework.aop.MethodMatcher;
import org.springframework.aop.ProxyMethodInvocation;
import org.springframework.aop.framework.autoproxy.ProxyCreationContext;
import org.springframework.aop.interceptor.ExposeInvocationInterceptor;
import org.springframework.aop.support.AbstractExpressionPointcut;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * 使用 AspectJ 编织器来评估切点表达式的
 * Spring {@link org.springframework.aop.Pointcut} 实现。
 *
 * <p>切点表达式的值是 AspectJ 表达式。它可以引用其他切点，
 * 并使用组合及其他操作。
 *
 * <p>当然，由于这是由 Spring AOP 基于代理的模型处理，
 * 因此仅支持方法执行切点。
 *
 * @author Rob Harrop
 * @author Adrian Colyer
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Ramnivas Laddad
 * @author Dave Syer
 * @since 2.0
 */
@SuppressWarnings("serial")
public class AspectJExpressionPointcut extends AbstractExpressionPointcut
		implements ClassFilter, IntroductionAwareMethodMatcher, BeanFactoryAware {

	private static final Set<PointcutPrimitive> SUPPORTED_PRIMITIVES = new HashSet<>();

	static {
		SUPPORTED_PRIMITIVES.add(PointcutPrimitive.EXECUTION);
		SUPPORTED_PRIMITIVES.add(PointcutPrimitive.ARGS);
		SUPPORTED_PRIMITIVES.add(PointcutPrimitive.REFERENCE);
		SUPPORTED_PRIMITIVES.add(PointcutPrimitive.THIS);
		SUPPORTED_PRIMITIVES.add(PointcutPrimitive.TARGET);
		SUPPORTED_PRIMITIVES.add(PointcutPrimitive.WITHIN);
		SUPPORTED_PRIMITIVES.add(PointcutPrimitive.AT_ANNOTATION);
		SUPPORTED_PRIMITIVES.add(PointcutPrimitive.AT_WITHIN);
		SUPPORTED_PRIMITIVES.add(PointcutPrimitive.AT_ARGS);
		SUPPORTED_PRIMITIVES.add(PointcutPrimitive.AT_TARGET);
	}


	private static final Log logger = LogFactory.getLog(AspectJExpressionPointcut.class);

	@Nullable
	private Class<?> pointcutDeclarationScope;

	private String[] pointcutParameterNames = new String[0];

	private Class<?>[] pointcutParameterTypes = new Class<?>[0];

	@Nullable
	private BeanFactory beanFactory;

	@Nullable
	private transient ClassLoader pointcutClassLoader;

	@Nullable
	private transient PointcutExpression pointcutExpression;

	private transient Map<Method, ShadowMatch> shadowMatchCache = new ConcurrentHashMap<>(32);


	/**
	 * 创建新的默认 AspectJExpressionPointcut。
	 */
	public AspectJExpressionPointcut() {
	}

	/**
	 * 使用给定设置创建新的 AspectJExpressionPointcut。
	 * @param declarationScope 切点的声明作用域
	 * @param paramNames 切点的参数名
	 * @param paramTypes 切点的参数类型
	 */
	public AspectJExpressionPointcut(Class<?> declarationScope, String[] paramNames, Class<?>[] paramTypes) {
		this.pointcutDeclarationScope = declarationScope;
		if (paramNames.length != paramTypes.length) {
			throw new IllegalStateException(
					"Number of pointcut parameter names must match number of pointcut parameter types");
		}
		this.pointcutParameterNames = paramNames;
		this.pointcutParameterTypes = paramTypes;
	}


	/**
	 * 设置切点的声明作用域。
	 */
	public void setPointcutDeclarationScope(Class<?> pointcutDeclarationScope) {
		this.pointcutDeclarationScope = pointcutDeclarationScope;
	}

	/**
	 * 设置切点的参数名。
	 */
	public void setParameterNames(String... names) {
		this.pointcutParameterNames = names;
	}

	/**
	 * 设置切点的参数类型。
	 */
	public void setParameterTypes(Class<?>... types) {
		this.pointcutParameterTypes = types;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}


	@Override
	public ClassFilter getClassFilter() {
		obtainPointcutExpression();
		return this;
	}

	@Override
	public MethodMatcher getMethodMatcher() {
		obtainPointcutExpression();
		return this;
	}


	/**
	 * 检查此切点是否已准备好进行匹配，
	 * 并延迟构建底层 AspectJ 切点表达式。
	 */
	private PointcutExpression obtainPointcutExpression() {
		if (getExpression() == null) {
			throw new IllegalStateException("Must set property 'expression' before attempting to match");
		}
		if (this.pointcutExpression == null) {
			this.pointcutClassLoader = determinePointcutClassLoader();
			this.pointcutExpression = buildPointcutExpression(this.pointcutClassLoader);
		}
		return this.pointcutExpression;
	}

	/**
	 * 确定用于切点评估的 ClassLoader。
	 */
	@Nullable
	private ClassLoader determinePointcutClassLoader() {
		if (this.beanFactory instanceof ConfigurableBeanFactory) {
			return ((ConfigurableBeanFactory) this.beanFactory).getBeanClassLoader();
		}
		if (this.pointcutDeclarationScope != null) {
			return this.pointcutDeclarationScope.getClassLoader();
		}
		return ClassUtils.getDefaultClassLoader();
	}

	/**
	 * 构建底层 AspectJ 切点表达式。
	 */
	private PointcutExpression buildPointcutExpression(@Nullable ClassLoader classLoader) {
		PointcutParser parser = initializePointcutParser(classLoader);
		PointcutParameter[] pointcutParameters = new PointcutParameter[this.pointcutParameterNames.length];
		for (int i = 0; i < pointcutParameters.length; i++) {
			pointcutParameters[i] = parser.createPointcutParameter(
					this.pointcutParameterNames[i], this.pointcutParameterTypes[i]);
		}
		return parser.parsePointcutExpression(replaceBooleanOperators(resolveExpression()),
				this.pointcutDeclarationScope, pointcutParameters);
	}

	private String resolveExpression() {
		String expression = getExpression();
		Assert.state(expression != null, "No expression set");
		return expression;
	}

	/**
	 * 初始化底层 AspectJ 切点解析器。
	 */
	private PointcutParser initializePointcutParser(@Nullable ClassLoader classLoader) {
		PointcutParser parser = PointcutParser
				.getPointcutParserSupportingSpecifiedPrimitivesAndUsingSpecifiedClassLoaderForResolution(
						SUPPORTED_PRIMITIVES, classLoader);
		parser.registerPointcutDesignatorHandler(new BeanPointcutDesignatorHandler());
		return parser;
	}


	/**
	 * 如果在 XML 中指定了切点表达式，用户不能将 {@code and} 写作 "&&"
	 * （不过 &amp;&amp; 可以工作）。
	 * 我们也允许在两个切点子表达式之间使用 {@code and}。
	 * <p>此方法会转换回 {@code &&}，供 AspectJ 切点解析器使用。
	 */
	private String replaceBooleanOperators(String pcExpr) {
		String result = StringUtils.replace(pcExpr, " and ", " && ");
		result = StringUtils.replace(result, " or ", " || ");
		result = StringUtils.replace(result, " not ", " ! ");
		return result;
	}


	/**
	 * 返回底层 AspectJ 切点表达式。
	 */
	public PointcutExpression getPointcutExpression() {
		return obtainPointcutExpression();
	}

	@Override
	public boolean matches(Class<?> targetClass) {
		PointcutExpression pointcutExpression = obtainPointcutExpression();
		try {
			try {
				return pointcutExpression.couldMatchJoinPointsInType(targetClass);
			}
			catch (ReflectionWorldException ex) {
				logger.debug("PointcutExpression matching rejected target class - trying fallback expression", ex);
				// 实际上这仍然是 "maybe" —— 如果我们目前还了解得不够，就将切点视为动态切点
				PointcutExpression fallbackExpression = getFallbackPointcutExpression(targetClass);
				if (fallbackExpression != null) {
					return fallbackExpression.couldMatchJoinPointsInType(targetClass);
				}
			}
		}
		catch (Throwable ex) {
			logger.debug("PointcutExpression matching rejected target class", ex);
		}
		return false;
	}

	@Override
	public boolean matches(Method method, Class<?> targetClass, boolean hasIntroductions) {
		obtainPointcutExpression();
		ShadowMatch shadowMatch = getTargetShadowMatch(method, targetClass);

		// 对 this、target、@this、@target、@annotation 进行特殊处理。
		// 在 Spring 中，我们可以优化，因为我们知道确切就是这个类，
		// 并且运行时绝不会有匹配的子类。
		if (shadowMatch.alwaysMatches()) {
			return true;
		}
		else if (shadowMatch.neverMatches()) {
			return false;
		}
		else {
			// maybe 情况
			if (hasIntroductions) {
				return true;
			}
			// 匹配测试返回 maybe —— 如果测试中涉及任何对子类型敏感的变量
			// （this、target、at_this、at_target、at_annotation），那么
			// 我们认为这不是匹配，因为在 Spring 中绝不会存在不同的
			// 运行时子类型。
			RuntimeTestWalker walker = getRuntimeTestWalker(shadowMatch);
			return (!walker.testsSubtypeSensitiveVars() || walker.testTargetInstanceOfResidue(targetClass));
		}
	}

	@Override
	public boolean matches(Method method, Class<?> targetClass) {
		return matches(method, targetClass, false);
	}

	@Override
	public boolean isRuntime() {
		return obtainPointcutExpression().mayNeedDynamicTest();
	}

	@Override
	public boolean matches(Method method, Class<?> targetClass, Object... args) {
		obtainPointcutExpression();
		ShadowMatch shadowMatch = getTargetShadowMatch(method, targetClass);

		// 将 Spring AOP 代理绑定到 AspectJ 的 "this"，并将 Spring AOP 目标绑定到 AspectJ target，
		// 与 MethodInvocationProceedingJoinPoint 的返回保持一致
		ProxyMethodInvocation pmi = null;
		Object targetObject = null;
		Object thisObject = null;
		try {
			MethodInvocation mi = ExposeInvocationInterceptor.currentInvocation();
			targetObject = mi.getThis();
			if (!(mi instanceof ProxyMethodInvocation)) {
				throw new IllegalStateException("MethodInvocation is not a Spring ProxyMethodInvocation: " + mi);
			}
			pmi = (ProxyMethodInvocation) mi;
			thisObject = pmi.getProxy();
		}
		catch (IllegalStateException ex) {
			// 没有当前调用...
			if (logger.isDebugEnabled()) {
				logger.debug("Could not access current invocation - matching with limited context: " + ex);
			}
		}

		try {
			JoinPointMatch joinPointMatch = shadowMatch.matchesJoinPoint(thisObject, targetObject, args);

			/*
			 * 执行最终检查，查看是否存在 this(TYPE) 这类残留匹配。
			 * 为此，我们使用原始方法（代理方法）的 shadow，
			 * 以确保正确地针对 'this' 进行检查。没有此检查时，
			 * 当 TYPE 匹配目标类型但不匹配 'this' 时（如 JDK 动态代理的情况），
			 * 会在 this(TYPE) 上得到错误匹配。
			 * <p>有关原始 bug，请参见 SPR-2979。
			 */
			if (pmi != null && thisObject != null) {  // 存在当前调用
				RuntimeTestWalker originalMethodResidueTest = getRuntimeTestWalker(getShadowMatch(method, method));
				if (!originalMethodResidueTest.testThisInstanceOfResidue(thisObject.getClass())) {
					return false;
				}
				if (joinPointMatch.matches()) {
					bindParameters(pmi, joinPointMatch);
				}
			}

			return joinPointMatch.matches();
		}
		catch (Throwable ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Failed to evaluate join point for arguments " + Arrays.toString(args) +
						" - falling back to non-match", ex);
			}
			return false;
		}
	}

	@Nullable
	protected String getCurrentProxiedBeanName() {
		return ProxyCreationContext.getCurrentProxiedBeanName();
	}


	/**
	 * 基于目标类的加载器而不是默认加载器获取新的切点表达式。
	 */
	@Nullable
	private PointcutExpression getFallbackPointcutExpression(Class<?> targetClass) {
		try {
			ClassLoader classLoader = targetClass.getClassLoader();
			if (classLoader != null && classLoader != this.pointcutClassLoader) {
				return buildPointcutExpression(classLoader);
			}
		}
		catch (Throwable ex) {
			logger.debug("Failed to create fallback PointcutExpression", ex);
		}
		return null;
	}

	private RuntimeTestWalker getRuntimeTestWalker(ShadowMatch shadowMatch) {
		if (shadowMatch instanceof DefensiveShadowMatch) {
			return new RuntimeTestWalker(((DefensiveShadowMatch) shadowMatch).primary);
		}
		return new RuntimeTestWalker(shadowMatch);
	}

	private void bindParameters(ProxyMethodInvocation invocation, JoinPointMatch jpm) {
		// 注意：不能使用 JoinPointMatch.getClass().getName() 作为键，因为
		// Spring AOP 会在某个连接点完成所有匹配，然后执行所有调用。
		// 在这种场景下，如果仅使用 JoinPointMatch 作为键，那么
		// “最后一个获胜”，这完全不是我们想要的。
		// 使用表达式一定是安全的，因为两个相同的表达式
		// 一定会以完全相同的方式进行绑定。
		invocation.setUserAttribute(resolveExpression(), jpm);
	}

	private ShadowMatch getTargetShadowMatch(Method method, Class<?> targetClass) {
		Method targetMethod = AopUtils.getMostSpecificMethod(method, targetClass);
		if (targetMethod.getDeclaringClass().isInterface()) {
			// 尝试构建尽可能具体的接口，以便继承的方法也能被视为
			// 子接口匹配的候选，尤其是对于代理类。
			// 注意：AspectJ 只会考虑 Method.getDeclaringClass()。
			Set<Class<?>> ifcs = ClassUtils.getAllInterfacesForClassAsSet(targetClass);
			if (ifcs.size() > 1) {
				try {
					Class<?> compositeInterface = ClassUtils.createCompositeInterface(
							ClassUtils.toClassArray(ifcs), targetClass.getClassLoader());
					targetMethod = ClassUtils.getMostSpecificMethod(targetMethod, compositeInterface);
				}
				catch (IllegalArgumentException ex) {
					// 已实现的接口可能暴露了冲突的方法签名...
					// 继续使用原始目标方法。
				}
			}
		}
		return getShadowMatch(targetMethod, method);
	}

	private ShadowMatch getShadowMatch(Method targetMethod, Method originalMethod) {
		// 避免对已知 Method 的并发访问产生锁竞争...
		ShadowMatch shadowMatch = this.shadowMatchCache.get(targetMethod);
		if (shadowMatch == null) {
			synchronized (this.shadowMatchCache) {
				// 未找到 —— 现在使用完整锁再次检查...
				PointcutExpression fallbackExpression = null;
				shadowMatch = this.shadowMatchCache.get(targetMethod);
				if (shadowMatch == null) {
					Method methodToMatch = targetMethod;
					try {
						try {
							shadowMatch = obtainPointcutExpression().matchesMethodExecution(methodToMatch);
						}
						catch (ReflectionWorldException ex) {
							// 无法内省目标方法，可能是因为它已在
							// 特殊 ClassLoader 中加载。改为尝试声明类的 ClassLoader...
							try {
								fallbackExpression = getFallbackPointcutExpression(methodToMatch.getDeclaringClass());
								if (fallbackExpression != null) {
									shadowMatch = fallbackExpression.matchesMethodExecution(methodToMatch);
								}
							}
							catch (ReflectionWorldException ex2) {
								fallbackExpression = null;
							}
						}
						if (targetMethod != originalMethod && (shadowMatch == null ||
								(shadowMatch.neverMatches() && Proxy.isProxyClass(targetMethod.getDeclaringClass())))) {
							// 在没有可解析匹配，或者在代理类上出现负匹配（代理类重新声明的
							// 方法上没有任何注解）的情况下，回退到普通的原始方法。
							methodToMatch = originalMethod;
							try {
								shadowMatch = obtainPointcutExpression().matchesMethodExecution(methodToMatch);
							}
							catch (ReflectionWorldException ex) {
								// 既无法内省目标类，也无法内省代理类 ->
								// 在放弃之前，尝试原始方法的声明类...
								try {
									fallbackExpression = getFallbackPointcutExpression(methodToMatch.getDeclaringClass());
									if (fallbackExpression != null) {
										shadowMatch = fallbackExpression.matchesMethodExecution(methodToMatch);
									}
								}
								catch (ReflectionWorldException ex2) {
									fallbackExpression = null;
								}
							}
						}
					}
					catch (Throwable ex) {
						// 可能是 AspectJ 1.8.10 遇到了无效签名
						logger.debug("PointcutExpression matching rejected target method", ex);
						fallbackExpression = null;
					}
					if (shadowMatch == null) {
						shadowMatch = new ShadowMatchImpl(org.aspectj.util.FuzzyBoolean.NO, null, null, null);
					}
					else if (shadowMatch.maybeMatches() && fallbackExpression != null) {
						shadowMatch = new DefensiveShadowMatch(shadowMatch,
								fallbackExpression.matchesMethodExecution(methodToMatch));
					}
					this.shadowMatchCache.put(targetMethod, shadowMatch);
				}
			}
		}
		return shadowMatch;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof AspectJExpressionPointcut)) {
			return false;
		}
		AspectJExpressionPointcut otherPc = (AspectJExpressionPointcut) other;
		return ObjectUtils.nullSafeEquals(this.getExpression(), otherPc.getExpression()) &&
				ObjectUtils.nullSafeEquals(this.pointcutDeclarationScope, otherPc.pointcutDeclarationScope) &&
				ObjectUtils.nullSafeEquals(this.pointcutParameterNames, otherPc.pointcutParameterNames) &&
				ObjectUtils.nullSafeEquals(this.pointcutParameterTypes, otherPc.pointcutParameterTypes);
	}

	@Override
	public int hashCode() {
		int hashCode = ObjectUtils.nullSafeHashCode(this.getExpression());
		hashCode = 31 * hashCode + ObjectUtils.nullSafeHashCode(this.pointcutDeclarationScope);
		hashCode = 31 * hashCode + ObjectUtils.nullSafeHashCode(this.pointcutParameterNames);
		hashCode = 31 * hashCode + ObjectUtils.nullSafeHashCode(this.pointcutParameterTypes);
		return hashCode;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("AspectJExpressionPointcut: (");
		for (int i = 0; i < this.pointcutParameterTypes.length; i++) {
			sb.append(this.pointcutParameterTypes[i].getName());
			sb.append(' ');
			sb.append(this.pointcutParameterNames[i]);
			if ((i+1) < this.pointcutParameterTypes.length) {
				sb.append(", ");
			}
		}
		sb.append(") ");
		if (getExpression() != null) {
			sb.append(getExpression());
		}
		else {
			sb.append("<pointcut expression not set>");
		}
		return sb.toString();
	}

	//---------------------------------------------------------------------
	// 序列化支持
	//---------------------------------------------------------------------

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		// 依赖默认序列化，只在反序列化后初始化状态。
		ois.defaultReadObject();

		// 初始化 transient 字段。
		// pointcutExpression 将由 checkReadyToMatch() 延迟初始化
		this.shadowMatchCache = new ConcurrentHashMap<>(32);
	}


	/**
	 * Spring 特有的 {@code bean()} 切点指示符扩展到 AspectJ 的处理器。
	 * <p>此处理器必须添加到每个需要处理 {@code bean()} PCD 的切点对象上。
	 * 匹配上下文通过检查线程局部变量自动获得，
	 * 因此无需在切点上设置匹配上下文。
	 */
	private class BeanPointcutDesignatorHandler implements PointcutDesignatorHandler {

		private static final String BEAN_DESIGNATOR_NAME = "bean";

		@Override
		public String getDesignatorName() {
			return BEAN_DESIGNATOR_NAME;
		}

		@Override
		public ContextBasedMatcher parse(String expression) {
			return new BeanContextMatcher(expression);
		}
	}


	/**
	 * BeanNamePointcutDesignatorHandler 的匹配器类。
	 * <p>此匹配器的动态匹配测试始终返回 true，
	 * 因为匹配决定是在代理创建时作出的。
	 * 对于静态匹配测试，此匹配器选择弃权，
	 * 以便即使对 bean() 切点使用否定时，整体切点也能匹配。
	 */
	private class BeanContextMatcher implements ContextBasedMatcher {

		private final NamePattern expressionPattern;

		public BeanContextMatcher(String expression) {
			this.expressionPattern = new NamePattern(expression);
		}

		@Override
		@SuppressWarnings("rawtypes")
		@Deprecated
		public boolean couldMatchJoinPointsInType(Class someClass) {
			return (contextMatch(someClass) == FuzzyBoolean.YES);
		}

		@Override
		@SuppressWarnings("rawtypes")
		@Deprecated
		public boolean couldMatchJoinPointsInType(Class someClass, MatchingContext context) {
			return (contextMatch(someClass) == FuzzyBoolean.YES);
		}

		@Override
		public boolean matchesDynamically(MatchingContext context) {
			return true;
		}

		@Override
		public FuzzyBoolean matchesStatically(MatchingContext context) {
			return contextMatch(null);
		}

		@Override
		public boolean mayNeedDynamicTest() {
			return false;
		}

		private FuzzyBoolean contextMatch(@Nullable Class<?> targetType) {
			String advisedBeanName = getCurrentProxiedBeanName();
			if (advisedBeanName == null) {  // 没有正在进行的代理创建
				// 弃权；不能返回 YES，因为那会使带否定的切点失败
				return FuzzyBoolean.MAYBE;
			}
			if (BeanFactoryUtils.isGeneratedBeanName(advisedBeanName)) {
				return FuzzyBoolean.NO;
			}
			if (targetType != null) {
				boolean isFactory = FactoryBean.class.isAssignableFrom(targetType);
				return FuzzyBoolean.fromBoolean(
						matchesBean(isFactory ? BeanFactory.FACTORY_BEAN_PREFIX + advisedBeanName : advisedBeanName));
			}
			else {
				return FuzzyBoolean.fromBoolean(matchesBean(advisedBeanName) ||
						matchesBean(BeanFactory.FACTORY_BEAN_PREFIX + advisedBeanName));
			}
		}

		private boolean matchesBean(String advisedBeanName) {
			return BeanFactoryAnnotationUtils.isQualifierMatch(
					this.expressionPattern::matches, advisedBeanName, beanFactory);
		}
	}


	private static class DefensiveShadowMatch implements ShadowMatch {

		private final ShadowMatch primary;

		private final ShadowMatch other;

		public DefensiveShadowMatch(ShadowMatch primary, ShadowMatch other) {
			this.primary = primary;
			this.other = other;
		}

		@Override
		public boolean alwaysMatches() {
			return this.primary.alwaysMatches();
		}

		@Override
		public boolean maybeMatches() {
			return this.primary.maybeMatches();
		}

		@Override
		public boolean neverMatches() {
			return this.primary.neverMatches();
		}

		@Override
		public JoinPointMatch matchesJoinPoint(Object thisObject, Object targetObject, Object[] args) {
			try {
				return this.primary.matchesJoinPoint(thisObject, targetObject, args);
			}
			catch (ReflectionWorldException ex) {
				return this.other.matchesJoinPoint(thisObject, targetObject, args);
			}
		}

		@Override
		public void setMatchingContext(MatchingContext aMatchContext) {
			this.primary.setMatchingContext(aMatchContext);
			this.other.setMatchingContext(aMatchContext);
		}
	}

}
