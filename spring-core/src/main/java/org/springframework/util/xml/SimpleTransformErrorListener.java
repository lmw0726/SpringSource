/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.util.xml;

import org.apache.commons.logging.Log;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;

/**
 * 简单的{@code javax.xml.transform.ErrorListener}实现：
 * 使用给定的Commons Logging日志记录器实例记录警告信息，
 * 并重新抛出错误以终止XML转换过程。
 *
 * @author Juergen Hoeller
 * @since 1.2
 */
public class SimpleTransformErrorListener implements ErrorListener {

	private final Log logger;


	/**
	 * 为指定的Commons Logging日志记录器实例创建新的SimpleTransformErrorListener。
	 */
	public SimpleTransformErrorListener(Log logger) {
		this.logger = logger;
	}


	@Override
	public void warning(TransformerException ex) throws TransformerException {
		logger.warn("XSLT transformation warning", ex);
	}

	@Override
	public void error(TransformerException ex) throws TransformerException {
		logger.error("XSLT transformation error", ex);
	}

	@Override
	public void fatalError(TransformerException ex) throws TransformerException {
		throw ex;
	}

}
