package com.yupi.yuaicodemother.service;


public interface ScreenshotService{
    /**
     * 通用截图服务，可以得到访问地址
     *
     * @param webUrl 网页URL
     * @return 截图上传的URL，失败返回null
     */
    String generateAndUploadScreenshot(String webUrl);
}
