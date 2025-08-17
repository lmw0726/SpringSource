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

package org.springframework.beans.support;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * PagedListHolder 是一个用于处理对象列表的简单状态持有器，
 * 将列表分成多个页面。页码从 0 开始。
 *
 * <p>主要用于 Web 界面（UI）。通常，实例会使用一个 bean 列表创建，
 * 放入 session 中，并作为模型导出。属性可以通过编程方式设置/获取，
 * 但最常见的方式是数据绑定，即从请求参数填充 bean。
 * getter 方法主要供视图使用。
 *
 * <p>支持通过 {@link SortDefinition} 实现对底层列表进行排序，
 * 对应属性为 "sort"。默认使用 {@link MutableSortDefinition} 实例，
 * 在再次设置同一属性时会切换升序标志。
 *
 * <p>数据绑定的名称必须为 "pageSize" 和 "sort.ascending"，与 BeanWrapper 一致。
 * 注意名称和嵌套语法与相应的 JSTL EL 表达式匹配，例如 "myModelAttr.pageSize"
 * 和 "myModelAttr.sort.ascending"。
 *
 * @author Juergen Hoeller
 * @since 19.05.2003
 * @param <E> 元素类型
 * @see #getPageList()
 * @see org.springframework.beans.support.MutableSortDefinition
 */
@SuppressWarnings("serial")
public class PagedListHolder<E> implements Serializable {

	/**
	 * 默认页大小。
	 */
	public static final int DEFAULT_PAGE_SIZE = 10;

	/**
	 * 默认的最大页链接数。
	 */
	public static final int DEFAULT_MAX_LINKED_PAGES = 10;


	private List<E> source = Collections.emptyList();

	@Nullable
	private Date refreshDate;

	@Nullable
	private SortDefinition sort;

	@Nullable
	private SortDefinition sortUsed;

	private int pageSize = DEFAULT_PAGE_SIZE;

	private int page = 0;

	private boolean newPageSet;

	private int maxLinkedPages = DEFAULT_MAX_LINKED_PAGES;


	/**
	 * 创建一个新的 holder 实例。
	 * 需要设置源列表才能使用该 holder。
	 * @see #setSource
	 */
	public PagedListHolder() {
		this(new ArrayList<>(0));
	}

	/**
	 * 使用给定的源列表创建一个新的 holder 实例，默认使用一个排序定义
	 * （并启用 "toggleAscendingOnProperty"）。
	 * @param source 源列表
	 * @see MutableSortDefinition#setToggleAscendingOnProperty
	 */
	public PagedListHolder(List<E> source) {
		this(source, new MutableSortDefinition(true));
	}

	/**
	 * 使用给定的源列表创建一个新的 holder 实例。
	 * @param source 源列表
	 * @param sort 初始排序定义
	 */
	public PagedListHolder(List<E> source, SortDefinition sort) {
		setSource(source);
		setSort(sort);
	}


	/**
	 * 设置该 holder 的源列表。
	 */
	public void setSource(List<E> source) {
		Assert.notNull(source, "Source List must not be null");
		this.source = source;
		this.refreshDate = new Date();
		this.sortUsed = null;
	}

	/**
	 * 返回该 holder 的源列表。
	 */
	public List<E> getSource() {
		return this.source;
	}

	/**
	 * 返回列表上次从源提供者获取的时间。
	 */
	@Nullable
	public Date getRefreshDate() {
		return this.refreshDate;
	}

	/**
	 * 设置该 holder 的排序定义。
	 * 通常为 MutableSortDefinition 的实例。
	 * @see org.springframework.beans.support.MutableSortDefinition
	 */
	public void setSort(@Nullable SortDefinition sort) {
		this.sort = sort;
	}

	/**
	 * 返回该 holder 的排序定义。
	 */
	@Nullable
	public SortDefinition getSort() {
		return this.sort;
	}

	/**
	 * 设置当前页大小。
	 * 如果改变，会重置当前页码。
	 * <p>默认值为 10。
	 */
	public void setPageSize(int pageSize) {
		if (pageSize != this.pageSize) {
			this.pageSize = pageSize;
			if (!this.newPageSet) {
				this.page = 0;
			}
		}
	}

	/**
	 * 返回当前页大小。
	 */
	public int getPageSize() {
		return this.pageSize;
	}

	/**
	 * 设置当前页码。
	 * 页码从 0 开始。
	 */
	public void setPage(int page) {
		this.page = page;
		this.newPageSet = true;
	}

	/**
	 * 返回当前页码。
	 * 页码从 0 开始。
	 */
	public int getPage() {
		this.newPageSet = false;
		if (this.page >= getPageCount()) {
			this.page = getPageCount() - 1;
		}
		return this.page;
	}

	/**
	 * 设置当前页周围的最大页链接数。
	 */
	public void setMaxLinkedPages(int maxLinkedPages) {
		this.maxLinkedPages = maxLinkedPages;
	}

	/**
	 * 返回当前页周围的最大页链接数。
	 */
	public int getMaxLinkedPages() {
		return this.maxLinkedPages;
	}


	/**
	 * 返回当前源列表的总页数。
	 */
	public int getPageCount() {
		float nrOfPages = (float) getNrOfElements() / getPageSize();
		return (int) ((nrOfPages > (int) nrOfPages || nrOfPages == 0.0) ? nrOfPages + 1 : nrOfPages);
	}

	/**
	 * 判断当前页是否为第一页。
	 */
	public boolean isFirstPage() {
		return getPage() == 0;
	}

	/**
	 * 判断当前页是否为最后一页。
	 */
	public boolean isLastPage() {
		return getPage() == getPageCount() - 1;
	}

	/**
	 * 切换到上一页。
	 * 如果已经是第一页，则保持不变。
	 */
	public void previousPage() {
		if (!isFirstPage()) {
			this.page--;
		}
	}

	/**
	 * 切换到下一页。
	 * 如果已经是最后一页，则保持不变。
	 */
	public void nextPage() {
		if (!isLastPage()) {
			this.page++;
		}
	}

	/**
	 * 返回源列表中的元素总数。
	 */
	public int getNrOfElements() {
		return getSource().size();
	}

	/**
	 * 返回当前页第一个元素的索引。
	 * 元素编号从 0 开始。
	 */
	public int getFirstElementOnPage() {
		return (getPageSize() * getPage());
	}

	/**
	 * 返回当前页最后一个元素的索引。
	 * 元素编号从 0 开始。
	 */
	public int getLastElementOnPage() {
		int endIndex = getPageSize() * (getPage() + 1);
		int size = getNrOfElements();
		return (endIndex > size ? size : endIndex) - 1;
	}

	/**
	 * 返回表示当前页的子列表。
	 */
	public List<E> getPageList() {
		return getSource().subList(getFirstElementOnPage(), getLastElementOnPage() + 1);
	}

	/**
	 * 返回用于在当前页周围创建链接的第一页。
	 */
	public int getFirstLinkedPage() {
		return Math.max(0, getPage() - (getMaxLinkedPages() / 2));
	}

	/**
	 * 返回用于在当前页周围创建链接的最后一页。
	 */
	public int getLastLinkedPage() {
		return Math.min(getFirstLinkedPage() + getMaxLinkedPages() - 1, getPageCount() - 1);
	}


	/**
	 * 如有必要，对列表重新排序，即当前的 {@code sort} 实例
	 * 与备份的 {@code sortUsed} 实例不相等时。
	 * <p>调用 {@code doSort} 执行实际排序。
	 * @see #doSort
	 */
	public void resort() {
		SortDefinition sort = getSort();
		if (sort != null && !sort.equals(this.sortUsed)) {
			this.sortUsed = copySortDefinition(sort);
			doSort(getSource(), sort);
			setPage(0);
		}
	}

	/**
	 * 创建给定排序定义的深拷贝，
	 * 用作状态持有者，以便将修改后的排序定义与之比较。
	 * <p>默认实现创建一个 MutableSortDefinition 实例。
	 * 可以在子类中重写，尤其是在自定义扩展 SortDefinition 接口时。
	 * 可以返回 null，这意味着不保存排序状态，从而在每次 {@code resort} 调用时执行实际排序。
	 * @param sort 当前的 SortDefinition 对象
	 * @return SortDefinition 对象的深拷贝
	 * @see MutableSortDefinition#MutableSortDefinition(SortDefinition)
	 */
	protected SortDefinition copySortDefinition(SortDefinition sort) {
		return new MutableSortDefinition(sort);
	}

	/**
	 * 根据给定的排序定义，对源列表执行实际排序。
	 * <p>默认实现使用 Spring 的 PropertyComparator。
	 * 可以在子类中重写。
	 * @see PropertyComparator#sort(java.util.List, SortDefinition)
	 */
	protected void doSort(List<E> source, SortDefinition sort) {
		PropertyComparator.sort(source, sort);
	}

}
