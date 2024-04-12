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

package org.springframework.web.servlet.view.feed;

import com.rometools.rome.feed.WireFeed;
import com.rometools.rome.io.WireFeedOutput;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.view.AbstractView;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStreamWriter;
import java.util.Map;

/**
 * Atom和RSS Feed视图的抽象基类，使用
 * <a href="https://github.com/rometools/rome">ROME</a>包。
 *
 * <p><b>注意：从Spring 4.1开始，这是基于ROME的{@code com.rometools}变体，
 * 版本为1.5。请升级您的构建依赖项。</b>
 *
 * <p>应用程序特定的视图类通常将从{@link AbstractRssFeedView}或{@link AbstractAtomFeedView}扩展，
 * 而不是从此类扩展。
 *
 * <p>感谢Jettro Coenradie和Sergio Bossa提供的原始Feed视图原型！
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @since 3.0
 * @param <T> {@link WireFeed}类型
 * @see AbstractRssFeedView
 * @see AbstractAtomFeedView
 */
public abstract class AbstractFeedView<T extends WireFeed> extends AbstractView {

	@Override
	protected final void renderMergedOutputModel(
			Map<String, Object> model, HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		// 创建新的 Feed 对象
		T wireFeed = newFeed();
		// 构建 Feed 的元数据
		buildFeedMetadata(model, wireFeed, request);
		// 构建 Feed 的条目
		buildFeedEntries(model, wireFeed, request, response);

		// 设置响应的内容类型
		setResponseContentType(request, response);
		// 如果 Feed 对象 未指定编码格式，则默认设置为 UTF-8
		if (!StringUtils.hasText(wireFeed.getEncoding())) {
			wireFeed.setEncoding("UTF-8");
		}

		// 创建 WireFeedOutput 实例
		WireFeedOutput feedOutput = new WireFeedOutput();
		// 获取响应输出流
		ServletOutputStream out = response.getOutputStream();
		// 使用指定编码将 wireFeed 输出到输出流
		feedOutput.output(wireFeed, new OutputStreamWriter(out, wireFeed.getEncoding()));
		// 刷新输出流
		out.flush();
	}

	/**
	 * 创建一个新的Feed以容纳条目。
	 * @return 新创建的Feed实例
	 */
	protected abstract T newFeed();

	/**
	 * 填充Feed元数据（标题、链接、描述等）。
	 * <p>默认为空实现。子类可以重写此方法以添加标题、链接、描述等元字段。
	 * @param model 模型，如果需要从中填充元信息
	 * @param feed 正在填充的Feed
	 * @param request 如果我们需要区域设置等。不应查看属性。
	 */
	protected void buildFeedMetadata(Map<String, Object> model, T feed, HttpServletRequest request) {
	}

	/**
	 * 子类必须实现此方法以构建Feed条目，给定模型。
	 * <p>请注意，传入的HTTP响应仅应用于设置cookie或其他HTTP头。构建的Feed本身将在此方法返回后自动写入响应。
	 * @param model 模型Map
	 * @param feed 要添加条目的Feed
	 * @param request 如果我们需要区域设置等。不应查看属性。
	 * @param response 如果我们需要设置cookie。不应写入它。
	 * @throws Exception 构建过程中发生的任何异常
	 */
	protected abstract void buildFeedEntries(Map<String, Object> model, T feed,
											 HttpServletRequest request, HttpServletResponse response) throws Exception;

}
