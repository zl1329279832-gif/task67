const base = {
    get() {
        // Development: proxy via vue.config.js devServer
        // Production:  derive from current page location (no hardcoded domain)
        const isDev = process.env.NODE_ENV === 'development';
        const baseUrl = isDev
            ? 'http://localhost:8080/ssmu8xr0/'
            : (window.location.protocol + '//' + window.location.host + '/ssmu8xr0/');
        return {
            url: baseUrl,
            name: "ssmu8xr0",
            indexUrl: baseUrl + 'front/index.html'
        };
    },
    getProjectName() {
        return {
            projectName: "白云会议管理系统"
        }
    }
}
export default base
