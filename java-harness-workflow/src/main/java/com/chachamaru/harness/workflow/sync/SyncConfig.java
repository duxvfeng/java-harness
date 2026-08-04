package com.chachamaru.harness.workflow.sync;

import java.util.List;
import java.util.Map;

/**
 * harness.toml 配置模型
 *
 * <p>此模型映射 harness.toml 的结构到内存对象，支持生成：
 * <ul>
 *   <li>plugin.json - 项目元数据</li>
 *   <li>settings.json - Agent 配置、环境变量、权限、沙箱规则</li>
 * </ul>
 *
 * <p>对应 TOML 结构：
 * <pre>
 * [project]
 * name = "..."
 * version = "..."
 * # ... 其他项目字段
 *
 * [agent]
 * defaultAgent = "..."
 *
 * [safety.permissions]
 * allow = ["..."]
 * deny = ["..."]
 * ask = ["..."]
 *
 * [safety.sandbox]
 * failIfUnavailable = true
 *
 * [safety.sandbox.network]
 * deniedDomains = ["..."]
 *
 * [safety.sandbox.filesystem]
 * denyRead = ["..."]
 * allowRead = ["..."]
 * </pre>
 *
 * @see SyncSkill
 * @since 4.0.0-java
 */
public class SyncConfig {

    /** 项目元数据配置，对应 TOML 的 [project] 部分 */
    private ProjectConfig project;

    /** Agent 配置，对应 TOML 的 [agent] 部分 */
    private AgentConfig agent;

    /** 环境变量映射，对应 TOML 的 [env] 部分 */
    private Map<String, String> env;

    /** 安全配置，对应 TOML 的 [safety] 部分 */
    private SafetyConfig safety;

    /**
     * 获取项目配置
     *
     * @return 项目配置对象，可能为 null
     */
    public ProjectConfig getProject() {
        return project;
    }

    /**
     * 设置项目配置
     *
     * @param project 项目配置对象
     */
    public void setProject(ProjectConfig project) {
        this.project = project;
    }

    /**
     * 获取 Agent 配置
     *
     * @return Agent 配置对象，可能为 null
     */
    public AgentConfig getAgent() {
        return agent;
    }

    /**
     * 设置 Agent 配置
     *
     * @param agent Agent 配置对象
     */
    public void setAgent(AgentConfig agent) {
        this.agent = agent;
    }

    /**
     * 获取环境变量映射
     *
     * @return 环境变量映射，键值对形式，可能为 null
     */
    public Map<String, String> getEnv() {
        return env;
    }

    /**
     * 设置环境变量映射
     *
     * @param env 环境变量映射
     */
    public void setEnv(Map<String, String> env) {
        this.env = env;
    }

    /**
     * 获取安全配置
     *
     * @return 安全配置对象，可能为 null
     */
    public SafetyConfig getSafety() {
        return safety;
    }

    /**
     * 设置安全配置
     *
     * @param safety 安全配置对象
     */
    public void setSafety(SafetyConfig safety) {
        this.safety = safety;
    }

    /**
     * 项目元数据配置，对应 harness.toml 的 [project] 部分
     *
     * <p>这些字段用于生成 plugin.json，包含项目的核心元数据。
     */
    public static class ProjectConfig {

        /** 项目名称 */
        private String name;

        /** 项目版本 */
        private String version;

        /** 项目描述 */
        private String description;

        /** 作者名称 */
        private String authorName;

        /** 作者 URL */
        private String authorUrl;

        /** 项目主页 URL */
        private String homepage;

        /** 代码仓库 URL */
        private String repository;

        /** 开源协议 */
        private String license;

        /** 项目关键词列表 */
        private List<String> keywords;

        /** 输出样式列表 */
        private List<String> outputStyles;

        /**
         * 获取项目名称
         *
         * @return 项目名称，可能为 null
         */
        public String getName() {
            return name;
        }

        /**
         * 设置项目名称
         *
         * @param name 项目名称
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * 获取项目版本
         *
         * @return 项目版本，可能为 null
         */
        public String getVersion() {
            return version;
        }

        /**
         * 设置项目版本
         *
         * @param version 项目版本
         */
        public void setVersion(String version) {
            this.version = version;
        }

        /**
         * 获取项目描述
         *
         * @return 项目描述，可能为 null
         */
        public String getDescription() {
            return description;
        }

        /**
         * 设置项目描述
         *
         * @param description 项目描述
         */
        public void setDescription(String description) {
            this.description = description;
        }

        /**
         * 获取作者名称
         *
         * @return 作者名称，可能为 null
         */
        public String getAuthorName() {
            return authorName;
        }

        /**
         * 设置作者名称
         *
         * @param authorName 作者名称
         */
        public void setAuthorName(String authorName) {
            this.authorName = authorName;
        }

        /**
         * 获取作者 URL
         *
         * @return 作者 URL，可能为 null
         */
        public String getAuthorUrl() {
            return authorUrl;
        }

        /**
         * 设置作者 URL
         *
         * @param authorUrl 作者 URL
         */
        public void setAuthorUrl(String authorUrl) {
            this.authorUrl = authorUrl;
        }

        /**
         * 获取项目主页 URL
         *
         * @return 项目主页 URL，可能为 null
         */
        public String getHomepage() {
            return homepage;
        }

        /**
         * 设置项目主页 URL
         *
         * @param homepage 项目主页 URL
         */
        public void setHomepage(String homepage) {
            this.homepage = homepage;
        }

        /**
         * 获取代码仓库 URL
         *
         * @return 代码仓库 URL，可能为 null
         */
        public String getRepository() {
            return repository;
        }

        /**
         * 设置代码仓库 URL
         *
         * @param repository 代码仓库 URL
         */
        public void setRepository(String repository) {
            this.repository = repository;
        }

        /**
         * 获取开源协议
         *
         * @return 开源协议，可能为 null
         */
        public String getLicense() {
            return license;
        }

        /**
         * 设置开源协议
         *
         * @param license 开源协议
         */
        public void setLicense(String license) {
            this.license = license;
        }

        /**
         * 获取项目关键词列表
         *
         * @return 关键词列表，可能为 null
         */
        public List<String> getKeywords() {
            return keywords;
        }

        /**
         * 设置项目关键词列表
         *
         * @param keywords 关键词列表
         */
        public void setKeywords(List<String> keywords) {
            this.keywords = keywords;
        }

        /**
         * 获取输出样式列表
         *
         * @return 输出样式列表，可能为 null
         */
        public List<String> getOutputStyles() {
            return outputStyles;
        }

        /**
         * 设置输出样式列表
         *
         * @param outputStyles 输出样式列表
         */
        public void setOutputStyles(List<String> outputStyles) {
            this.outputStyles = outputStyles;
        }
    }

    /**
     * Agent 配置，对应 harness.toml 的 [agent] 部分
     *
     * <p>用于设置默认 Agent 行为。
     */
    public static class AgentConfig {

        /** 默认 Agent 名称 */
        private String defaultAgent;

        /**
         * 获取默认 Agent 名称
         *
         * @return 默认 Agent 名称，可能为 null
         */
        public String getDefaultAgent() {
            return defaultAgent;
        }

        /**
         * 设置默认 Agent 名称
         *
         * @param defaultAgent 默认 Agent 名称
         */
        public void setDefaultAgent(String defaultAgent) {
            this.defaultAgent = defaultAgent;
        }
    }

    /**
     * 安全配置，对应 harness.toml 的 [safety] 部分
     *
     * <p>包含权限控制和沙箱配置。
     */
    public static class SafetyConfig {

        /** 权限配置 */
        private PermissionsConfig permissions;

        /** 沙箱配置 */
        private SandboxConfig sandbox;

        /**
         * 获取权限配置
         *
         * @return 权限配置对象，可能为 null
         */
        public PermissionsConfig getPermissions() {
            return permissions;
        }

        /**
         * 设置权限配置
         *
         * @param permissions 权限配置对象
         */
        public void setPermissions(PermissionsConfig permissions) {
            this.permissions = permissions;
        }

        /**
         * 获取沙箱配置
         *
         * @return 沙箱配置对象，可能为 null
         */
        public SandboxConfig getSandbox() {
            return sandbox;
        }

        /**
         * 设置沙箱配置
         *
         * @param sandbox 沙箱配置对象
         */
        public void setSandbox(SandboxConfig sandbox) {
            this.sandbox = sandbox;
        }
    }

    /**
     * 权限配置，对应 harness.toml 的 [safety.permissions] 部分
     *
     * <p>定义了不同权限级别的操作列表。
     */
    public static class PermissionsConfig {

        /** 允许的操作列表 */
        private List<String> allow;

        /** 拒绝的操作列表 */
        private List<String> deny;

        /** 需要询问的操作列表 */
        private List<String> ask;

        /**
         * 获取允许的操作列表
         *
         * @return 允许的操作列表，可能为 null
         */
        public List<String> getAllow() {
            return allow;
        }

        /**
         * 设置允许的操作列表
         *
         * @param allow 允许的操作列表
         */
        public void setAllow(List<String> allow) {
            this.allow = allow;
        }

        /**
         * 获取拒绝的操作列表
         *
         * @return 拒绝的操作列表，可能为 null
         */
        public List<String> getDeny() {
            return deny;
        }

        /**
         * 设置拒绝的操作列表
         *
         * @param deny 拒绝的操作列表
         */
        public void setDeny(List<String> deny) {
            this.deny = deny;
        }

        /**
         * 获取需要询问的操作列表
         *
         * @return 需要询问的操作列表，可能为 null
         */
        public List<String> getAsk() {
            return ask;
        }

        /**
         * 设置需要询问的操作列表
         *
         * @param ask 需要询问的操作列表
         */
        public void setAsk(List<String> ask) {
            this.ask = ask;
        }
    }

    /**
     * 沙箱配置，对应 harness.toml 的 [safety.sandbox] 部分
     *
     * <p>定义了沙箱的网络和文件系统访问限制。
     */
    public static class SandboxConfig {

        /** 如果沙箱不可用是否失败 */
        private boolean failIfUnavailable;

        /** 网络访问配置 */
        private NetworkConfig network;

        /** 文件系统访问配置 */
        private FilesystemConfig filesystem;

        /**
         * 检查如果沙箱不可用是否失败
         *
         * @return 如果沙箱不可用时是否失败，默认为 false
         */
        public boolean isFailIfUnavailable() {
            return failIfUnavailable;
        }

        /**
         * 设置如果沙箱不可用是否失败
         *
         * @param failIfUnavailable 如果为 true，沙箱不可用时将失败
         */
        public void setFailIfUnavailable(boolean failIfUnavailable) {
            this.failIfUnavailable = failIfUnavailable;
        }

        /**
         * 获取网络访问配置
         *
         * @return 网络访问配置对象，可能为 null
         */
        public NetworkConfig getNetwork() {
            return network;
        }

        /**
         * 设置网络访问配置
         *
         * @param network 网络访问配置对象
         */
        public void setNetwork(NetworkConfig network) {
            this.network = network;
        }

        /**
         * 获取文件系统访问配置
         *
         * @return 文件系统访问配置对象，可能为 null
         */
        public FilesystemConfig getFilesystem() {
            return filesystem;
        }

        /**
         * 设置文件系统访问配置
         *
         * @param filesystem 文件系统访问配置对象
         */
        public void setFilesystem(FilesystemConfig filesystem) {
            this.filesystem = filesystem;
        }
    }

    /**
     * 网络访问配置，对应 harness.toml 的 [safety.sandbox.network] 部分
     *
     * <p>定义了网络访问的域名黑名单。
     */
    public static class NetworkConfig {

        /** 拒绝访问的域名列表 */
        private List<String> deniedDomains;

        /**
         * 获取拒绝访问的域名列表
         *
         * @return 拒绝访问的域名列表，可能为 null
         */
        public List<String> getDeniedDomains() {
            return deniedDomains;
        }

        /**
         * 设置拒绝访问的域名列表
         *
         * @param deniedDomains 拒绝访问的域名列表
         */
        public void setDeniedDomains(List<String> deniedDomains) {
            this.deniedDomains = deniedDomains;
        }
    }

    /**
     * 文件系统访问配置，对应 harness.toml 的 [safety.sandbox.filesystem] 部分
     *
     * <p>定义了文件系统读访问的黑名单和白名单。
     */
    public static class FilesystemConfig {

        /** 拒绝读取的路径列表 */
        private List<String> denyRead;

        /** 允许读取的路径列表 */
        private List<String> allowRead;

        /**
         * 获取拒绝读取的路径列表
         *
         * @return 拒绝读取的路径列表，可能为 null
         */
        public List<String> getDenyRead() {
            return denyRead;
        }

        /**
         * 设置拒绝读取的路径列表
         *
         * @param denyRead 拒绝读取的路径列表
         */
        public void setDenyRead(List<String> denyRead) {
            this.denyRead = denyRead;
        }

        /**
         * 获取允许读取的路径列表
         *
         * @return 允许读取的路径列表，可能为 null
         */
        public List<String> getAllowRead() {
            return allowRead;
        }

        /**
         * 设置允许读取的路径列表
         *
         * @param allowRead 允许读取的路径列表
         */
        public void setAllowRead(List<String> allowRead) {
            this.allowRead = allowRead;
        }
    }
}
