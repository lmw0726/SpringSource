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

package org.springframework.aop.support;

import java.io.Serializable;
import java.lang.reflect.Method;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.IntroductionAwareMethodMatcher;
import org.springframework.aop.MethodMatcher;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 用于组合 {@link MethodMatcher MethodMatchers} 的静态工具方法。
 *
 * <p>MethodMatcher 可以静态评估（基于方法和目标类），
 * 也可能需要进一步动态评估（基于方法调用时的参数）。
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 11.11.2003
 * @see ClassFilters
 * @see Pointcuts
 */
public abstract class MethodMatchers {

	/**
	 * 匹配给定 MethodMatcher 中<i>任一</i>（或两个）匹配的所有方法。
	 * @param mm1 第一个 MethodMatcher
	 * @param mm2 第二个 MethodMatcher
	 * @return 一个独立的 MethodMatcher，匹配给定任一 MethodMatcher 匹配的所有方法
	 */
	public static MethodMatcher union(MethodMatcher mm1, MethodMatcher mm2) {
		return (mm1 instanceof IntroductionAwareMethodMatcher || mm2 instanceof IntroductionAwareMethodMatcher ?
				new UnionIntroductionAwareMethodMatcher(mm1, mm2) : new UnionMethodMatcher(mm1, mm2));
	}

	/**
	 * 匹配给定 MethodMatcher 中<i>任一</i>（或两个）匹配的所有方法。
	 * @param mm1 第一个 MethodMatcher
	 * @param cf1 第一个 MethodMatcher 对应的 ClassFilter
	 * @param mm2 第二个 MethodMatcher
	 * @param cf2 第二个 MethodMatcher 对应的 ClassFilter
	 * @return 一个独立的 MethodMatcher，匹配给定任一 MethodMatcher 匹配的所有方法
	 */
	static MethodMatcher union(MethodMatcher mm1, ClassFilter cf1, MethodMatcher mm2, ClassFilter cf2) {
		return (mm1 instanceof IntroductionAwareMethodMatcher || mm2 instanceof IntroductionAwareMethodMatcher ?
				new ClassFilterAwareUnionIntroductionAwareMethodMatcher(mm1, cf1, mm2, cf2) :
				new ClassFilterAwareUnionMethodMatcher(mm1, cf1, mm2, cf2));
	}

	/**
	 * 匹配给定两个 MethodMatcher<i>都</i>匹配的所有方法。
	 * @param mm1 第一个 MethodMatcher
	 * @param mm2 第二个 MethodMatcher
	 * @return 一个独立的 MethodMatcher，匹配给定两个 MethodMatcher 都匹配的所有方法
	 */
	public static MethodMatcher intersection(MethodMatcher mm1, MethodMatcher mm2) {
		return (mm1 instanceof IntroductionAwareMethodMatcher || mm2 instanceof IntroductionAwareMethodMatcher ?
				new IntersectionIntroductionAwareMethodMatcher(mm1, mm2) : new IntersectionMethodMatcher(mm1, mm2));
	}

	/**
	 * 将给定 MethodMatcher 应用于给定 Method，并支持
	 * {@link org.springframework.aop.IntroductionAwareMethodMatcher}
	 * （如果适用）。
	 * @param mm 要应用的 MethodMatcher（可以是 IntroductionAwareMethodMatcher）
	 * @param method 候选方法
	 * @param targetClass 目标类
	 * @param hasIntroductions 如果我们代表其询问的对象是一个或多个引介的主体，
	 * 则为 {@code true}；否则为 {@code false}
	 * @return 此方法是否静态匹配
	 */
	public static boolean matches(MethodMatcher mm, Method method, Class<?> targetClass, boolean hasIntroductions) {
		Assert.notNull(mm, "MethodMatcher must not be null");
		return (mm instanceof IntroductionAwareMethodMatcher ?
				((IntroductionAwareMethodMatcher) mm).matches(method, targetClass, hasIntroductions) :
				mm.matches(method, targetClass));
	}


	/**
	 * 给定两个 MethodMatcher 的并集 MethodMatcher 实现。
	 */
	@SuppressWarnings("serial")
	private static class UnionMethodMatcher implements MethodMatcher, Serializable {

		protected final MethodMatcher mm1;

		protected final MethodMatcher mm2;

		public UnionMethodMatcher(MethodMatcher mm1, MethodMatcher mm2) {
			Assert.notNull(mm1, "First MethodMatcher must not be null");
			Assert.notNull(mm2, "Second MethodMatcher must not be null");
			this.mm1 = mm1;
			this.mm2 = mm2;
		}

		@Override
		public boolean matches(Method method, Class<?> targetClass) {
			return (matchesClass1(targetClass) && this.mm1.matches(method, targetClass)) ||
					(matchesClass2(targetClass) && this.mm2.matches(method, targetClass));
		}

		protected boolean matchesClass1(Class<?> targetClass) {
			return true;
		}

		protected boolean matchesClass2(Class<?> targetClass) {
			return true;
		}

		@Override
		public boolean isRuntime() {
			return this.mm1.isRuntime() || this.mm2.isRuntime();
		}

		@Override
		public boolean matches(Method method, Class<?> targetClass, Object... args) {
			return this.mm1.matches(method, targetClass, args) || this.mm2.matches(method, targetClass, args);
		}

		@Override
		public boolean equals(@Nullable Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof UnionMethodMatcher)) {
				return false;
			}
			UnionMethodMatcher that = (UnionMethodMatcher) other;
			return (this.mm1.equals(that.mm1) && this.mm2.equals(that.mm2));
		}

		@Override
		public int hashCode() {
			return 37 * this.mm1.hashCode() + this.mm2.hashCode();
		}

		@Override
		public String toString() {
			return getClass().getName() + ": " + this.mm1 + ", " + this.mm2;
		}
	}


	/**
	 * 给定两个 MethodMatcher 的并集 MethodMatcher 实现，
	 * 其中至少一个是 IntroductionAwareMethodMatcher。
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	private static class UnionIntroductionAwareMethodMatcher extends UnionMethodMatcher
			implements IntroductionAwareMethodMatcher {

		public UnionIntroductionAwareMethodMatcher(MethodMatcher mm1, MethodMatcher mm2) {
			super(mm1, mm2);
		}

		@Override
		public boolean matches(Method method, Class<?> targetClass, boolean hasIntroductions) {
			return (matchesClass1(targetClass) && MethodMatchers.matches(this.mm1, method, targetClass, hasIntroductions)) ||
					(matchesClass2(targetClass) && MethodMatchers.matches(this.mm2, method, targetClass, hasIntroductions));
		}
	}


	/**
	 * 给定两个 MethodMatcher 的并集 MethodMatcher 实现，
	 * 支持每个 MethodMatcher 关联一个 ClassFilter。
	 */
	@SuppressWarnings("serial")
	private static class ClassFilterAwareUnionMethodMatcher extends UnionMethodMatcher {

		private final ClassFilter cf1;

		private final ClassFilter cf2;

		public ClassFilterAwareUnionMethodMatcher(MethodMatcher mm1, ClassFilter cf1, MethodMatcher mm2, ClassFilter cf2) {
			super(mm1, mm2);
			this.cf1 = cf1;
			this.cf2 = cf2;
		}

		@Override
		protected boolean matchesClass1(Class<?> targetClass) {
			return this.cf1.matches(targetClass);
		}

		@Override
		protected boolean matchesClass2(Class<?> targetClass) {
			return this.cf2.matches(targetClass);
		}

		@Override
		public boolean equals(@Nullable Object other) {
			if (this == other) {
				return true;
			}
			if (!super.equals(other)) {
				return false;
			}
			ClassFilter otherCf1 = ClassFilter.TRUE;
			ClassFilter otherCf2 = ClassFilter.TRUE;
			if (other instanceof ClassFilterAwareUnionMethodMatcher) {
				ClassFilterAwareUnionMethodMatcher cfa = (ClassFilterAwareUnionMethodMatcher) other;
				otherCf1 = cfa.cf1;
				otherCf2 = cfa.cf2;
			}
			return (this.cf1.equals(otherCf1) && this.cf2.equals(otherCf2));
		}

		@Override
		public int hashCode() {
			// 通过提供相同的哈希，允许与普通 UnionMethodMatcher 匹配...
			return super.hashCode();
		}

		@Override
		public String toString() {
			return getClass().getName() + ": " + this.cf1 + ", " + this.mm1 + ", " + this.cf2 + ", " + this.mm2;
		}
	}


	/**
	 * 给定两个 MethodMatcher 的并集 MethodMatcher 实现，
	 * 其中至少一个是 IntroductionAwareMethodMatcher，
	 * 并支持每个 MethodMatcher 关联一个 ClassFilter。
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	private static class ClassFilterAwareUnionIntroductionAwareMethodMatcher extends ClassFilterAwareUnionMethodMatcher
			implements IntroductionAwareMethodMatcher {

		public ClassFilterAwareUnionIntroductionAwareMethodMatcher(
				MethodMatcher mm1, ClassFilter cf1, MethodMatcher mm2, ClassFilter cf2) {

			super(mm1, cf1, mm2, cf2);
		}

		@Override
		public boolean matches(Method method, Class<?> targetClass, boolean hasIntroductions) {
			return (matchesClass1(targetClass) && MethodMatchers.matches(this.mm1, method, targetClass, hasIntroductions)) ||
					(matchesClass2(targetClass) && MethodMatchers.matches(this.mm2, method, targetClass, hasIntroductions));
		}
	}


	/**
	 * 给定两个 MethodMatcher 的交集 MethodMatcher 实现。
	 */
	@SuppressWarnings("serial")
	private static class IntersectionMethodMatcher implements MethodMatcher, Serializable {

		protected final MethodMatcher mm1;

		protected final MethodMatcher mm2;

		public IntersectionMethodMatcher(MethodMatcher mm1, MethodMatcher mm2) {
			Assert.notNull(mm1, "First MethodMatcher must not be null");
			Assert.notNull(mm2, "Second MethodMatcher must not be null");
			this.mm1 = mm1;
			this.mm2 = mm2;
		}

		@Override
		public boolean matches(Method method, Class<?> targetClass) {
			return (this.mm1.matches(method, targetClass) && this.mm2.matches(method, targetClass));
		}

		@Override
		public boolean isRuntime() {
			return (this.mm1.isRuntime() || this.mm2.isRuntime());
		}

		@Override
		public boolean matches(Method method, Class<?> targetClass, Object... args) {
			// 由于动态交集可能由静态部分和动态部分组成，
			// 我们必须避免在动态匹配器上调用 3 参数 matches 方法，
			// 因为它很可能是不支持的操作。
			boolean aMatches = (this.mm1.isRuntime() ?
					this.mm1.matches(method, targetClass, args) : this.mm1.matches(method, targetClass));
			boolean bMatches = (this.mm2.isRuntime() ?
					this.mm2.matches(method, targetClass, args) : this.mm2.matches(method, targetClass));
			return aMatches && bMatches;
		}

		@Override
		public boolean equals(@Nullable Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof IntersectionMethodMatcher)) {
				return false;
			}
			IntersectionMethodMatcher that = (IntersectionMethodMatcher) other;
			return (this.mm1.equals(that.mm1) && this.mm2.equals(that.mm2));
		}

		@Override
		public int hashCode() {
			return 37 * this.mm1.hashCode() + this.mm2.hashCode();
		}

		@Override
		public String toString() {
			return getClass().getName() + ": " + this.mm1 + ", " + this.mm2;
		}
	}


	/**
	 * 给定两个 MethodMatcher 的交集 MethodMatcher 实现，
	 * 其中至少一个是 IntroductionAwareMethodMatcher。
	 * @since 5.1
	 */
	@SuppressWarnings("serial")
	private static class IntersectionIntroductionAwareMethodMatcher extends IntersectionMethodMatcher
			implements IntroductionAwareMethodMatcher {

		public IntersectionIntroductionAwareMethodMatcher(MethodMatcher mm1, MethodMatcher mm2) {
			super(mm1, mm2);
		}

		@Override
		public boolean matches(Method method, Class<?> targetClass, boolean hasIntroductions) {
			return (MethodMatchers.matches(this.mm1, method, targetClass, hasIntroductions) &&
					MethodMatchers.matches(this.mm2, method, targetClass, hasIntroductions));
		}
	}

}
