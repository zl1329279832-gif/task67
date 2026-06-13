/**
 * 动态计算 contextPath，避免硬编码域名。
 * 生产环境: /ssmu8xr0/admin/dist/index.html → contextPath = "/ssmu8xr0"
 * 开发环境: / → contextPath = "" (由 vue.config.js dev proxy 处理)
 */
function getContextPath() {
    var pathname = window.location.pathname
    var adminIdx = pathname.indexOf('/admin/')
    if (adminIdx > 0) {
        return pathname.substring(0, adminIdx)
    }
    // 开发模式或从 front/ 访问时
    var frontIdx = pathname.indexOf('/front/')
    if (frontIdx > 0) {
        return pathname.substring(0, frontIdx)
    }
    return pathname.length > 1 ? pathname.replace(/\/$/, '') : ''
}

var _contextPath = getContextPath()

const base = {
    get() {
        return {
            url : window.location.protocol + '//' + window.location.host + _contextPath + '/',
            name: _contextPath.replace(/^\//, '') || 'ssmu8xr0',
            // 退出到首页链接
            indexUrl: _contextPath + '/front/index.html'
        };
    },
    getProjectName(){
        return {
            projectName: "白云会议管理系统"
        }
    }
}
export default base
