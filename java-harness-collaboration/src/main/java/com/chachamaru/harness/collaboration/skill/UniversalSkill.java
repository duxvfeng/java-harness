package com.chachamaru.harness.collaboration.skill;

import com.chachamaru.harness.collaboration.platform.Platform;

/**
 * Universal skill interface that supports platform-specific adaptations.
 *
 * <p>UniversalSkill extends the base Skill interface with platform adaptation capabilities,
 * enabling skills to work seamlessly across different AI development environments
 * (Claude Code, Codex CLI, etc.).</p>
 *
 * <p>Platform adaptation allows skills to:
 * <ul>
 *   <li>Customize behavior for specific platforms</li>
 *   <li>Leverage platform-specific features</li>
 *   <li>Maintain compatibility across platforms</li>
 *   <li>Provide fallback behavior when platforms don't support certain features</li>
 * </ul>
 *
 * <h3>Key Design Principles:</h3>
 * <ul>
 *   <li><b>Backward Compatible</b>: Existing skills continue to work without modification</li>
 *   <li><b>Platform Aware</b>: Skills can detect and adapt to current platform</li>
 *   <li><b>Graceful Degradation</b>: Unsupported features fall back to default behavior</li>
 *   <li><b>Explicit Support</b>: Skills declare which platforms they support</li>
 * </ul>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * public class MyUniversalSkill implements UniversalSkill {
 *     private final Set<Platform> supportedPlatforms = Set.of(Platform.CLAUDE_CODE, Platform.CODEX);
 *
 *     @Override
 *     public UniversalSkill adaptForPlatform(Platform platform) {
 *         if (!isPlatformSupported(platform)) {
 *             throw new IllegalArgumentException("Platform not supported: " + platform);
 *         }
 *         // Return platform-adapted version
 *         return switch (platform) {
 *             case CLAUDE_CODE -> new ClaudeCodeVersion(this);
 *             case CODEX -> new CodexVersion(this);
 *         };
 *     }
 *
 *     @Override
 *     public Set<Platform> getSupportedPlatforms() {
 *         return supportedPlatforms;
 *     }
 *
 *     @Override
 *     public boolean isPlatformSupported(Platform platform) {
 *         return supportedPlatforms.contains(platform);
 *     }
 * }
 * }</pre>
 *
 * @spec_reference Phase 7: Dual Platform Support
 * @since 4.1.1
 */
public interface UniversalSkill extends Skill {

    /**
     * Adapts this skill for the specified platform.
     *
     * <p>This method returns a platform-adapted version of the skill.
     * The returned skill may have customized behavior, API calls, or output format
     * specific to the target platform.</p>
     *
     * <p>If the platform is not supported, implementations should throw
     * {@link IllegalArgumentException}.</p>
     *
     * <p>Implementations may:
     * <ul>
     *   <li>Return {@code this} if the skill is platform-agnostic</li>
     *   <li>Return a wrapped/adapted version with platform-specific behavior</li>
     *   <li>Cache adapted instances for performance</li>
     * </ul>
     *
     * @param platform the target platform to adapt for
     * @return a platform-adapted version of this skill
     * @throws IllegalArgumentException if the platform is not supported
     */
    UniversalSkill adaptForPlatform(Platform platform);

    /**
     * Returns the set of platforms supported by this skill.
     *
     * <p>This allows skills to declare their platform compatibility.
     * Empty set means the skill supports all platforms (platform-agnostic).</p>
     *
     * <p>Common patterns:
     * <ul>
     *   <li>{@code Set.of(Platform.CLAUDE_CODE)} - Claude Code only</li>
     *   <li>{@code Set.of(Platform.CLAUDE_CODE, Platform.CODEX)} - Both platforms</li>
     *   <li>{@code Set.of()} - All platforms (default)</li>
     * </ul>
     *
     * @return the set of supported platforms (empty means all platforms)
     */
    java.util.Set<Platform> getSupportedPlatforms();

    /**
     * Checks if this skill supports the specified platform.
     *
     * <p>This is a convenience method equivalent to:
     * {@code getSupportedPlatforms().isEmpty() || getSupportedPlatforms().contains(platform)}</p>
     *
     * @param platform the platform to check
     * @return true if the skill supports the platform, false otherwise
     */
    default boolean isPlatformSupported(Platform platform) {
        java.util.Set<Platform> supported = getSupportedPlatforms();
        return supported.isEmpty() || supported.contains(platform);
    }
}
