package com.lmw.springmvc.multipart;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartRequest;

import java.io.File;
import java.io.IOException;

/**
 * 文件上传
 *
 * @author LMW
 * @version 1.0
 * @since 2024-07-01 22:52
 */
@Controller
public class MultiPartController {
	// 上传文件保存的相对路径
	private static final String UPLOAD_DIR = "uploads/";
	/**
	 * 上传单个文件，使用MultipartFile实现
	 *
	 * @param file 文件参数
	 * @return 上传结果
	 */
	@ResponseBody
	@PostMapping("upload1")
	public String upload(@RequestParam MultipartFile file) {
		try {
			// 获取项目的相对路径
			String uploadPath = new File(UPLOAD_DIR).getAbsolutePath();

			// 创建目标文件
			File destFile = new File(uploadPath, file.getOriginalFilename());
			File parentFile = destFile.getParentFile();
			if (!parentFile.exists()) {
				parentFile.mkdirs();
			}
			file.transferTo(destFile);
		} catch (IOException e) {
			throw new RuntimeException("上传文件失败，异常消息为：{}", e);
		}
		return "上传成功！";
	}

	/**
	 * 使用MultipartRequest，这种方式支持单个或多个文件上传
	 *
	 * @param files 文件上传请求
	 * @return 上传结果
	 */
	@ResponseBody
	@PostMapping("upload2")
	public String upload2(MultipartRequest files) {
		// 获得文件：
		MultipartFile file = files.getFile("file");
		// 获得文件名：
		String filename = file.getOriginalFilename();
		try {
			// 获取项目的相对路径
			String uploadPath = new File(UPLOAD_DIR).getAbsolutePath();

			// 创建目标文件
			File destFile = new File(uploadPath, file.getOriginalFilename());
			File parentFile = destFile.getParentFile();
			if (!parentFile.exists()) {
				parentFile.mkdirs();
			}
			file.transferTo(destFile);
		} catch (IOException e) {
			throw new RuntimeException("上传文件失败，异常消息为：{}", e);
		}
		return "上传成功！";

	}

}
