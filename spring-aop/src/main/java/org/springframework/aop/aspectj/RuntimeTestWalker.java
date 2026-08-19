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

package org.springframework.aop.aspectj;

import java.lang.reflect.Field;

import org.aspectj.weaver.ReferenceType;
import org.aspectj.weaver.ReferenceTypeDelegate;
import org.aspectj.weaver.ResolvedType;
import org.aspectj.weaver.ast.And;
import org.aspectj.weaver.ast.Call;
import org.aspectj.weaver.ast.FieldGetCall;
import org.aspectj.weaver.ast.HasAnnotation;
import org.aspectj.weaver.ast.ITestVisitor;
import org.aspectj.weaver.ast.Instanceof;
import org.aspectj.weaver.ast.Literal;
import org.aspectj.weaver.ast.Not;
import org.aspectj.weaver.ast.Or;
import org.aspectj.weaver.ast.Test;
import org.aspectj.weaver.internal.tools.MatchingContextBasedTest;
import org.aspectj.weaver.reflect.ReflectionBasedReferenceTypeDelegate;
import org.aspectj.weaver.reflect.ReflectionVar;
import org.aspectj.weaver.reflect.ShadowMatchImpl;
import org.aspectj.weaver.tools.ShadowMatch;

import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * 此类封装了一些 AspectJ 内部知识，这些知识应在未来版本中
 * 推回到 AspectJ 项目中。
 *
 * <p>它依赖于 AspectJ 中特定于实现的知识来破坏封装，
 * 并执行 AspectJ 未设计要做的事情：查询将要执行的运行时测试的类型。
 * 这里的代码应该迁移到 {@code ShadowMatch.getVariablesInvolvedInRuntimeTest()}
 * 或一些类似的操作。
 *
 * <p>参见 <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=151593">Bug 151593</a>
 *
 * @author Adrian Colyer
 * @author Ramnivas Laddad
 * @since 2.0
 */
class RuntimeTestWalker {

	private static final Field residualTestField;

	private static final Field varTypeField;

	private static final Field myClassField;


	static {
		try {
			residualTestField = ShadowMatchImpl.class.getDeclaredField("residualTest");
			varTypeField = ReflectionVar.class.getDeclaredField("varType");
			myClassField = ReflectionBasedReferenceTypeDelegate.class.getDeclaredField("myClass");
		}
		catch (NoSuchFieldException ex) {
			throw new IllegalStateException("The version of aspectjtools.jar / aspectjweaver.jar " +
					"on the classpath is incompatible with this version of Spring: " + ex);
		}
	}


	@Nullable
	private final Test runtimeTest;


	public RuntimeTestWalker(ShadowMatch shadowMatch) {
		try {
			ReflectionUtils.makeAccessible(residualTestField);
			this.runtimeTest = (Test) residualTestField.get(shadowMatch);
		}
		catch (IllegalAccessException ex) {
			throw new IllegalStateException(ex);
		}
	}


	/**
	 * 如果测试使用了 this、target、at_this、at_target 和 at_annotation 变量中的任何一个，
	 * 则它测试子类型敏感变量。
	 */
	public boolean testsSubtypeSensitiveVars() {
		return (this.runtimeTest != null &&
				new SubtypeSensitiveVarTypeTestVisitor().testsSubtypeSensitiveVars(this.runtimeTest));
	}

	public boolean testThisInstanceOfResidue(Class<?> thisClass) {
		return (this.runtimeTest != null &&
				new ThisInstanceOfResidueTestVisitor(thisClass).thisInstanceOfMatches(this.runtimeTest));
	}

	public boolean testTargetInstanceOfResidue(Class<?> targetClass) {
		return (this.runtimeTest != null &&
				new TargetInstanceOfResidueTestVisitor(targetClass).targetInstanceOfMatches(this.runtimeTest));
	}


	private static class TestVisitorAdapter implements ITestVisitor {

		protected static final int THIS_VAR = 0;
		protected static final int TARGET_VAR = 1;
		protected static final int AT_THIS_VAR = 3;
		protected static final int AT_TARGET_VAR = 4;
		protected static final int AT_ANNOTATION_VAR = 8;

		@Override
		public void visit(And e) {
			e.getLeft().accept(this);
			e.getRight().accept(this);
		}

		@Override
		public void visit(Or e) {
			e.getLeft().accept(this);
			e.getRight().accept(this);
		}

		@Override
		public void visit(Not e) {
			e.getBody().accept(this);
		}

		@Override
		public void visit(Instanceof i) {
		}

		@Override
		public void visit(Literal literal) {
		}

		@Override
		public void visit(Call call) {
		}

		@Override
		public void visit(FieldGetCall fieldGetCall) {
		}

		@Override
		public void visit(HasAnnotation hasAnnotation) {
		}

		@Override
		public void visit(MatchingContextBasedTest matchingContextTest) {
		}

		protected int getVarType(ReflectionVar v) {
			try {
				ReflectionUtils.makeAccessible(varTypeField);
				return (Integer) varTypeField.get(v);
			}
			catch (IllegalAccessException ex) {
				throw new IllegalStateException(ex);
			}
		}
	}


	private abstract static class InstanceOfResidueTestVisitor extends TestVisitorAdapter {

		private final Class<?> matchClass;

		private boolean matches;

		private final int matchVarType;

		public InstanceOfResidueTestVisitor(Class<?> matchClass, boolean defaultMatches, int matchVarType) {
			this.matchClass = matchClass;
			this.matches = defaultMatches;
			this.matchVarType = matchVarType;
		}

		public boolean instanceOfMatches(Test test) {
			test.accept(this);
			return this.matches;
		}

		@Override
		public void visit(Instanceof i) {
			int varType = getVarType((ReflectionVar) i.getVar());
			if (varType != this.matchVarType) {
				return;
			}
			Class<?> typeClass = null;
			ResolvedType type = (ResolvedType) i.getType();
			if (type instanceof ReferenceType) {
				ReferenceTypeDelegate delegate = ((ReferenceType) type).getDelegate();
				if (delegate instanceof ReflectionBasedReferenceTypeDelegate) {
					try {
						ReflectionUtils.makeAccessible(myClassField);
						typeClass = (Class<?>) myClassField.get(delegate);
					}
					catch (IllegalAccessException ex) {
						throw new IllegalStateException(ex);
					}
				}
			}
			try {
				// 不要使用 ResolvedType.isAssignableFrom()，因为它无法感知（Spring 的）mixins
				if (typeClass == null) {
					typeClass = ClassUtils.forName(type.getName(), this.matchClass.getClassLoader());
				}
				this.matches = typeClass.isAssignableFrom(this.matchClass);
			}
			catch (ClassNotFoundException ex) {
				this.matches = false;
			}
		}
	}


	/**
	 * 检查是否为 target(TYPE) 类型的残留。有关更多详细信息，请参阅 SPR-3783。
	 */
	private static class TargetInstanceOfResidueTestVisitor extends InstanceOfResidueTestVisitor {

		public TargetInstanceOfResidueTestVisitor(Class<?> targetClass) {
			super(targetClass, false, TARGET_VAR);
		}

		public boolean targetInstanceOfMatches(Test test) {
			return instanceOfMatches(test);
		}
	}


	/**
	 * 检查是否为 this(TYPE) 类型的残留。有关更多详细信息，请参阅 SPR-2979。
	 */
	private static class ThisInstanceOfResidueTestVisitor extends InstanceOfResidueTestVisitor {

		public ThisInstanceOfResidueTestVisitor(Class<?> thisClass) {
			super(thisClass, true, THIS_VAR);
		}

		// TODO：优化：仅当 this() 指定类型而不是标识符时才处理。
		public boolean thisInstanceOfMatches(Test test) {
			return instanceOfMatches(test);
		}
	}


	private static class SubtypeSensitiveVarTypeTestVisitor extends TestVisitorAdapter {

		private final Object thisObj = new Object();

		private final Object targetObj = new Object();

		private final Object[] argsObjs = new Object[0];

		private boolean testsSubtypeSensitiveVars = false;

		public boolean testsSubtypeSensitiveVars(Test aTest) {
			aTest.accept(this);
			return this.testsSubtypeSensitiveVars;
		}

		@Override
		public void visit(Instanceof i) {
			ReflectionVar v = (ReflectionVar) i.getVar();
			Object varUnderTest = v.getBindingAtJoinPoint(this.thisObj, this.targetObj, this.argsObjs);
			if (varUnderTest == this.thisObj || varUnderTest == this.targetObj) {
				this.testsSubtypeSensitiveVars = true;
			}
		}

		@Override
		public void visit(HasAnnotation hasAnn) {
			// 如果你认为以前的情况很糟糕，现在我们会陷入新的恐怖级别...
			ReflectionVar v = (ReflectionVar) hasAnn.getVar();
			int varType = getVarType(v);
			if (varType == AT_THIS_VAR || varType == AT_TARGET_VAR || varType == AT_ANNOTATION_VAR) {
				this.testsSubtypeSensitiveVars = true;
			}
		}
	}

}
