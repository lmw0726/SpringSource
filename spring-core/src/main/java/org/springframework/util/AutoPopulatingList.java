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

package org.springframework.util;

import org.springframework.lang.Nullable;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * 简单的 {@link List} 包装类，允许在请求元素时自动填充元素。
 * 这对于数据绑定到 {@link List} 特别有用，可以"即时"创建元素并添加到列表中。
 *
 * <p>注意：此类是非线程安全的。要创建线程安全版本，
 * 请使用 {@link java.util.Collections#synchronizedList} 工具方法。
 *
 * <p>灵感来自 Commons Collections 的 {@code LazyList}。
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @since 2.0
 * @param <E> 元素类型
 */
@SuppressWarnings("serial")
public class AutoPopulatingList<E> implements List<E>, Serializable {

	/**
	 * 所有操作最终委托给的基础 {@link List}。
	 */
	private final List<E> backingList;

	/**
	 * 用于按需创建新 {@link List} 元素的 {@link ElementFactory}。
	 */
	private final ElementFactory<E> elementFactory;


	/**
	 * 创建一个新的 {@code AutoPopulatingList}，由标准 {@link ArrayList} 支持，
	 * 并按需将提供的 {@link Class 元素类} 的新实例添加到基础列表中。
	 */
	public AutoPopulatingList(Class<? extends E> elementClass) {
		this(new ArrayList<>(), elementClass);
	}

	/**
	 * 创建一个新的 {@code AutoPopulatingList}，由指定的 {@link List} 支持，
	 * 并按需将提供的 {@link Class 元素类} 的新实例添加到基础列表中。
	 */
	public AutoPopulatingList(List<E> backingList, Class<? extends E> elementClass) {
		this(backingList, new ReflectiveElementFactory<>(elementClass));
	}

	/**
	 * 创建一个新的 {@code AutoPopulatingList}，由标准 {@link ArrayList} 支持，
	 * 并使用提供的 {@link ElementFactory} 按需创建新元素。
	 */
	public AutoPopulatingList(ElementFactory<E> elementFactory) {
		this(new ArrayList<>(), elementFactory);
	}

	/**
	 * 创建一个新的 {@code AutoPopulatingList}，由指定的 {@link List} 支持，
	 * 并使用提供的 {@link ElementFactory} 按需创建新元素。
	 */
	public AutoPopulatingList(List<E> backingList, ElementFactory<E> elementFactory) {
		Assert.notNull(backingList, "Backing List must not be null");
		Assert.notNull(elementFactory, "Element factory must not be null");
		this.backingList = backingList;
		this.elementFactory = elementFactory;
	}


	@Override
	public void add(int index, E element) {
		this.backingList.add(index, element);
	}

	@Override
	public boolean add(E o) {
		return this.backingList.add(o);
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		return this.backingList.addAll(c);
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		return this.backingList.addAll(index, c);
	}

	@Override
	public void clear() {
		this.backingList.clear();
	}

	@Override
	public boolean contains(Object o) {
		return this.backingList.contains(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return this.backingList.containsAll(c);
	}

	/**
	 * 获取指定索引处的元素，如果该索引位置没有元素则创建新元素。
	 */
	@Override
	public E get(int index) {
		int backingListSize = this.backingList.size();
		E element = null;
		if (index < backingListSize) {
			element = this.backingList.get(index);
			if (element == null) {
				element = this.elementFactory.createElement(index);
				this.backingList.set(index, element);
			}
		}
		else {
			for (int x = backingListSize; x < index; x++) {
				this.backingList.add(null);
			}
			element = this.elementFactory.createElement(index);
			this.backingList.add(element);
		}
		return element;
	}

	@Override
	public int indexOf(Object o) {
		return this.backingList.indexOf(o);
	}

	@Override
	public boolean isEmpty() {
		return this.backingList.isEmpty();
	}

	@Override
	public Iterator<E> iterator() {
		return this.backingList.iterator();
	}

	@Override
	public int lastIndexOf(Object o) {
		return this.backingList.lastIndexOf(o);
	}

	@Override
	public ListIterator<E> listIterator() {
		return this.backingList.listIterator();
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		return this.backingList.listIterator(index);
	}

	@Override
	public E remove(int index) {
		return this.backingList.remove(index);
	}

	@Override
	public boolean remove(Object o) {
		return this.backingList.remove(o);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return this.backingList.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return this.backingList.retainAll(c);
	}

	@Override
	public E set(int index, E element) {
		return this.backingList.set(index, element);
	}

	@Override
	public int size() {
		return this.backingList.size();
	}

	@Override
	public List<E> subList(int fromIndex, int toIndex) {
		return this.backingList.subList(fromIndex, toIndex);
	}

	@Override
	public Object[] toArray() {
		return this.backingList.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return this.backingList.toArray(a);
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return this.backingList.equals(other);
	}

	@Override
	public int hashCode() {
		return this.backingList.hashCode();
	}


	/**
	 * 用于为基于索引访问的数据结构（如 {@link java.util.List}）创建元素的工厂接口。
	 *
	 * @param <E> 元素类型
	 */
	@FunctionalInterface
	public interface ElementFactory<E> {

		/**
		 * 为指定索引创建元素。
		 * @return 元素对象
		 * @throws ElementInstantiationException 如果实例化过程失败
		 * （目标构造函数抛出的任何异常都应原样传播）
		 */
		E createElement(int index) throws ElementInstantiationException;
	}


	/**
	 * ElementFactory 可能抛出的异常。
	 */
	public static class ElementInstantiationException extends RuntimeException {

		public ElementInstantiationException(String msg) {
			super(msg);
		}

		public ElementInstantiationException(String message, Throwable cause) {
			super(message, cause);
		}
	}


	/**
	 * ElementFactory 接口的反射实现，使用给定元素类的
	 * {@code Class.getDeclaredConstructor().newInstance()} 方法。
	 */
	private static class ReflectiveElementFactory<E> implements ElementFactory<E>, Serializable {

		private final Class<? extends E> elementClass;

		public ReflectiveElementFactory(Class<? extends E> elementClass) {
			Assert.notNull(elementClass, "Element class must not be null");
			Assert.isTrue(!elementClass.isInterface(), "Element class must not be an interface type");
			Assert.isTrue(!Modifier.isAbstract(elementClass.getModifiers()), "Element class cannot be an abstract class");
			this.elementClass = elementClass;
		}

		@Override
		public E createElement(int index) {
			try {
				return ReflectionUtils.accessibleConstructor(this.elementClass).newInstance();
			}
			catch (NoSuchMethodException ex) {
				throw new ElementInstantiationException(
						"No default constructor on element class: " + this.elementClass.getName(), ex);
			}
			catch (InstantiationException ex) {
				throw new ElementInstantiationException(
						"Unable to instantiate element class: " + this.elementClass.getName(), ex);
			}
			catch (IllegalAccessException ex) {
				throw new ElementInstantiationException(
						"Could not access element constructor: " + this.elementClass.getName(), ex);
			}
			catch (InvocationTargetException ex) {
				throw new ElementInstantiationException(
						"Failed to invoke element constructor: " + this.elementClass.getName(), ex.getTargetException());
			}
		}
	}

}
