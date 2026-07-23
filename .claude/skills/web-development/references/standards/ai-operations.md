# AI 操作与自动化 — Web

> 本文档定义 Web 端 AI agent 可执行的浏览器操作与自动化能力。

---

## 1. Playwright / Puppeteer

<!-- TODO: 补充浏览器自动化方案 -->

### 1.1 启动与连接

<!-- TODO: 启动浏览器、连接已有浏览器 -->

### 1.2 页面导航

<!-- TODO: goto、reload、back、forward -->

### 1.3 元素交互

<!-- TODO: click、fill、select、hover、drag -->

### 1.4 等待策略

<!-- TODO: waitForSelector、waitForResponse、waitForNavigation -->

---

## 2. 截图与视觉比对

### 2.1 全页截图

<!-- TODO: page.screenshot({ fullPage: true }) -->

### 2.2 元素截图

<!-- TODO: element.screenshot() -->

### 2.3 视觉回归

<!-- TODO: toHaveScreenshot、视觉差异 -->

---

## 3. 网络请求拦截

### 3.1 API Mock

<!-- TODO: route.fulfill、模拟响应 -->

### 3.2 请求监听

<!-- TODO: page.on('request')、page.on('response') -->

### 3.3 请求修改

<!-- TODO: 修改 Header、Body -->

---

## 4. 日志与性能采集

### 4.1 Console 日志

<!-- TODO: page.on('console') -->

### 4.2 Performance 数据

<!-- TODO: page.evaluate 获取 Web Vitals -->

### 4.3 网络日志

<!-- TODO: HAR 导出 -->
