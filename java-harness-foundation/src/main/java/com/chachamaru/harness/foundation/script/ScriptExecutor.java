package com.chachamaru.harness.foundation.script;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 脚本执行器 - 执行 shell 脚本并管理输出
 *
 * <p>功能特性：</p>
 * <ul>
 *   <li>脚本执行管理</li>
 *   <li>输出捕获和处理</li>
 *   <li>错误处理和重试</li>
 *   <li>环境变量管理</li>
 *   <li>超时控制</li>
 * </ul>
 *
 * @since 4.0.0
 */
public class ScriptExecutor {

    private static final int DEFAULT_TIMEOUT_SECONDS = 300; // 5 minutes

    /**
     * 执行脚本
     *
     * @param scriptName 脚本名称
     * @return 执行结果
     */
    public static ScriptResult execute(String scriptName) {
        return execute(scriptName, null, null);
    }

    /**
     * 执行脚本（带参数）
     *
     * @param scriptName 脚本名称
     * @param args 命令行参数
     * @return 执行结果
     */
    public static ScriptResult execute(String scriptName, String[] args) {
        return execute(scriptName, args, null);
    }

    /**
     * 执行脚本（带参数和环境变量）
     *
     * @param scriptName 脚本名称
     * @param args 命令行参数
     * @param envVars 环境变量
     * @return 执行结果
     */
    public static ScriptResult execute(String scriptName, String[] args, Map<String, String> envVars) {
        String scriptPath = ScriptPathManager.getScriptPath(scriptName);
        return executeScript(scriptPath, args, envVars, DEFAULT_TIMEOUT_SECONDS);
    }

    /**
     * 执行脚本（带超时）
     *
     * @param scriptName 脚本名称
     * @param args 命令行参数
     * @param timeoutSeconds 超时时间（秒）
     * @return 执行结果
     */
    public static ScriptResult execute(String scriptName, String[] args, int timeoutSeconds) {
        String scriptPath = ScriptPathManager.getScriptPath(scriptName);
        return executeScript(scriptPath, args, null, timeoutSeconds);
    }

    /**
     * 执行脚本文件
     *
     * @param scriptPath 脚本完整路径
     * @param args 命令行参数
     * @param envVars 环境变量
     * @param timeoutSeconds 超时时间
     * @return 执行结果
     */
    private static ScriptResult executeScript(String scriptPath, String[] args, Map<String, String> envVars, int timeoutSeconds) {
        ScriptResult result = new ScriptResult();
        result.setScriptPath(scriptPath);

        try {
            // 构建命令
            List<String> command = new ArrayList<>();
            command.add("bash");
            command.add(scriptPath);
            if (args != null && args.length > 0) {
                for (String arg : args) {
                    if (arg != null && !arg.isEmpty()) {
                        command.add(arg);
                    }
                }
            }

            // 创建进程
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true); // 合并标准错误和标准输出

            // 设置环境变量
            if (envVars != null && !envVars.isEmpty()) {
                Map<String, String> environment = processBuilder.environment();
                environment.putAll(envVars);
            }

            // 设置工作目录
            File workingDir = new File(ScriptPathManager.getProjectRoot());
            processBuilder.directory(workingDir);

            // 启动进程
            long startTime = System.currentTimeMillis();
            Process process = processBuilder.start();

            // 捕获输出
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            // 等待进程完成（带超时）
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                result.setExitCode(-1);
                result.setOutput("脚本执行超时");
                result.setSuccess(false);
                result.setExecutionTime(System.currentTimeMillis() - startTime);
                return result;
            }

            // 设置结果
            result.setExitCode(process.exitValue());
            result.setOutput(output.toString());
            result.setSuccess(process.exitValue() == 0);
            result.setExecutionTime(System.currentTimeMillis() - startTime);

        } catch (IOException e) {
            result.setExitCode(-1);
            result.setOutput("IO 错误: " + e.getMessage());
            result.setSuccess(false);
        } catch (InterruptedException e) {
            result.setExitCode(-1);
            result.setOutput("执行被中断: " + e.getMessage());
            result.setSuccess(false);
        }

        return result;
    }

    /**
     * 脚本执行结果
     */
    public static class ScriptResult {
        private String scriptPath;
        private int exitCode;
        private String output;
        private boolean success;
        private long executionTime;

        public String getScriptPath() {
            return scriptPath;
        }

        public void setScriptPath(String scriptPath) {
            this.scriptPath = scriptPath;
        }

        public int getExitCode() {
            return exitCode;
        }

        public void setExitCode(int exitCode) {
            this.exitCode = exitCode;
        }

        public String getOutput() {
            return output;
        }

        public void setOutput(String output) {
            this.output = output;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public long getExecutionTime() {
            return executionTime;
        }

        public void setExecutionTime(long executionTime) {
            this.executionTime = executionTime;
        }

        @Override
        public String toString() {
            return "ScriptResult{" +
                    "scriptPath='" + scriptPath + '\'' +
                    ", exitCode=" + exitCode +
                    ", success=" + success +
                    ", executionTime=" + executionTime + "ms" +
                    '}';
        }
    }
}