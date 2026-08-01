package com.chachamaru.harness.cli.guardrail.rules;

import com.chachamaru.harness.cli.guardrail.GuardrailResult;
import com.chachamaru.harness.cli.guardrail.Rule;
import com.chachamaru.harness.cli.hook.HookInput;
import com.chachamaru.harness.shared.constants.GuardrailConstants;

/**
 * R22: Certificate management operation detection
 */
public class R22CertificateManagement implements Rule {

    @Override
    public String getId() {
        return GuardrailConstants.R22_CERTIFICATE_MANAGEMENT;
    }

    @Override
    public String getName() {
        return "Certificate Management Rule";
    }

    @Override
    public boolean matches(HookInput input) {
        return "Bash".equals(input.toolName()) || "Write".equals(input.toolName()) ||
               "Edit".equals(input.toolName());
    }

    @Override
    public GuardrailResult evaluate(HookInput input) {
        String command = (String) input.toolInput().get("command");
        String filePath = (String) input.toolInput().get("file_path");

        // Bash command checks
        if (command != null && "Bash".equals(input.toolName())) {
            String lowerCmd = command.toLowerCase();
            if (lowerCmd.contains("openssl") || lowerCmd.contains("keytool") ||
                lowerCmd.contains("certtool") || lowerCmd.contains("certutil") ||
                lowerCmd.contains("generate-certificate") || lowerCmd.contains("genkey")) {
                // Check for production certificate operations
                if (lowerCmd.contains("production") || lowerCmd.contains("prod") ||
                    lowerCmd.contains("wildcard") || lowerCmd.contains("--ca")) {
                    return GuardrailResult.denied(
                        GuardrailConstants.R22_CERTIFICATE_MANAGEMENT,
                        "Production certificate management operations are not allowed"
                    );
                }
            }
        }

        // File write checks for certificate files
        if (filePath != null && ("Write".equals(input.toolName()) || "Edit".equals(input.toolName()))) {
            String lowerPath = filePath.toLowerCase();
            if (lowerPath.endsWith(".pem") || lowerPath.endsWith(".crt") ||
                lowerPath.endsWith(".cer") || lowerPath.endsWith(".key") ||
                lowerPath.endsWith(".p12") || lowerPath.endsWith(".pfx") ||
                lowerPath.endsWith(".jks") || lowerPath.endsWith(".keystore")) {
                if (lowerPath.contains("production") || lowerPath.contains("prod")) {
                    return GuardrailResult.denied(
                        GuardrailConstants.R22_CERTIFICATE_MANAGEMENT,
                        "Production certificate file writes are not allowed"
                    );
                }
            }
        }

        return GuardrailResult.allowed();
    }
}