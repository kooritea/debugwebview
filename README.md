# [xposed]debugwebview
强制打开webview的debug模式和注入vConsole


## 换坑
建议用 https://github.com/WankkoRee/EnableWebViewDebugging

## 用法
建议使用Lsposed,因为本模块没有界面，安装后直接使用Lsposed的作用域选择需要注入的应用，普通的xposed框架将会全部应用都注入

## todo
- [ ] 支持x5内核
- [ ] 改用直接注入而不是远程拉取vConsole防止被安全策略拦截
