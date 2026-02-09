package com.movie.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.movie.annotation.SysLog;
import com.movie.common.Result;
import com.movie.service.StorageService;

@RestController
@RequestMapping("/file")
public class FileController {

    @Autowired
    private StorageService storageService;

    @SysLog("上传文件")
    @PostMapping("/upload")
    public Result<String> upload(@RequestParam("file") MultipartFile file) {
        try {
            String url = storageService.upload(file);
            return Result.success(url);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}