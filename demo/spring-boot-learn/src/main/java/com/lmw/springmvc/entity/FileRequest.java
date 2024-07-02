package com.lmw.springmvc.entity;

import org.springframework.web.multipart.MultipartFile;

import java.io.Serializable;

/**
 * 文件请求实体类
 *
 * @author LMW
 * @version 1.0
 * @since 2024-07-02 21:21
 */
public class FileRequest implements Serializable {
	private static final long serialVersionUID = -7047818647312839955L;
	/**
	 * 表单编号
	 */
	private String billNo;
	/**
	 * 上传的文件
	 */
	private MultipartFile file;

	public String getBillNo() {
		return billNo;
	}

	public void setBillNo(String billNo) {
		this.billNo = billNo;
	}

	public MultipartFile getFile() {
		return file;
	}

	public void setFile(MultipartFile file) {
		this.file = file;
	}
}
