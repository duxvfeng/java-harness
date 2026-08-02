package com.chachamaru.harness.foundation.template;

/**
 * 模板注册表异常 - 模板操作相关的异常
 *
 * @since 4.0.0
 */
public class TemplateRegistryException extends RuntimeException {

    private final ErrorCode errorCode;

    public enum ErrorCode {
        TEMPLATE_NOT_FOUND("模板未找到"),
        TEMPLATE_ALREADY_EXISTS("模板已存在"),
        INVALID_TEMPLATE_VERSION("无效的模板版本"),
        TEMPLATE_LOAD_FAILED("模板加载失败"),
        TEMPLATE_SAVE_FAILED("模板保存失败"),
        VERSION_CONFLICT("版本冲突"),
        INVALID_TEMPLATE_DEFINITION("无效的模板定义"),
        VARIABLE_VALIDATION_FAILED("变量验证失败"),
        TEMPLATE_PARSE_ERROR("模板解析错误");

        private final String message;

        ErrorCode(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }

    public TemplateRegistryException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public TemplateRegistryException(ErrorCode errorCode, String detail) {
        super(errorCode.getMessage() + ": " + detail);
        this.errorCode = errorCode;
    }

    public TemplateRegistryException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}