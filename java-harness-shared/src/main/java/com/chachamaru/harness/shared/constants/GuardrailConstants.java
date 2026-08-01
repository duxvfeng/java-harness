package com.chachamaru.harness.shared.constants;

/**
 * Guardrail rule identifiers and constants
 */
public final class GuardrailConstants {
    private GuardrailConstants() {}

    // Rule IDs
    public static final String R01_NO_SUDO = "R01";
    public static final String R02_PROTECTED_PATH = "R02";
    public static final String R03_REDIRECTION_BYPASS = "R03";
    public static final String R04_PROJECT_PATH = "R04";
    public static final String R05_RM_RF = "R05";
    public static final String R06_GIT_PUSH_FORCE = "R06";
    public static final String R07_CODEX_DIRECT_WRITE = "R07";
    public static final String R08_BREEZING_WRITE = "R08";
    public static final String R09_SECRET_READ = "R09";
    public static final String R10_NO_VERIFY = "R10";
    public static final String R11_GIT_RESET_HARD = "R11";
    public static final String R12_PROTECTED_BRANCH_PUSH = "R12";
    public static final String R13_PACKAGE_FILE = "R13";
    public static final String R14_BILLING_EGRESS = "R14";
    public static final String R15_PRODUCTION_DEPLOY = "R15";
    public static final String R16_DATABASE_WRITE = "R16";
    public static final String R17_CONTAINER_MANAGEMENT = "R17";
    public static final String R18_CONFIG_FILE_WRITE = "R18";
    public static final String R19_EXECUTABLE_DOWNLOAD = "R19";
    public static final String R20_NETWORK_EXPOSURE = "R20";
    public static final String R21_SYSTEM_CRITICAL = "R21";
    public static final String R22_CERTIFICATE_MANAGEMENT = "R22";
    public static final String R23_BACKUP_DELETION = "R23";
    public static final String R24_LOG_MANIPULATION = "R24";
    public static final String R25_SERVICE_RESTART = "R25";
    public static final String R26_USER_PERMISSION = "R26";
    public static final String R27_CRON_SCHEDULE = "R27";

    // Protected paths
    public static final String[] PROTECTED_PATHS = {
        ".env",
        ".env.*",
        "*.pem",
        "*.key",
        "id_rsa",
        "id_ed25519",
        ".git/",
        "secrets/"
    };

    // Protected branch patterns
    public static final String[] PROTECTED_BRANCHES = {
        "main",
        "master",
        "develop",
        "release/*"
    };
}
