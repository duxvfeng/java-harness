package com.chachamaru.harness.collaboration.skill.impl;

import com.chachamaru.harness.collaboration.skill.CoreSkill;
import com.chachamaru.harness.collaboration.skill.SkillExecutionException;
import com.chachamaru.harness.collaboration.skill.model.SkillContext;
import com.chachamaru.harness.workflow.model.PlansDocument;
import com.chachamaru.harness.workflow.parser.PlansParser;
import com.chachamaru.harness.workflow.parser.RegexPlansParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Skill for planning and creating project plans.
 *
 * <p>The PlanSkill is responsible for:
 * <ul>
 *   <li>Parsing Plans.md files</li>
 *   <li>Creating new project plans</li>
 *   <li>Validating plan structure</li>
 *   <li>Managing task dependencies</li>
 * </ul>
 *
 * @spec_reference spec.md#Collaboration System
 * @since 4.1.0
 */
public class PlanSkill extends CoreSkill {

    private static final Logger logger = LoggerFactory.getLogger(PlanSkill.class);

    private final PlansParser parser;

    /**
     * Creates a PlanSkill with default parser.
     */
    public PlanSkill() {
        this(new RegexPlansParser());
    }

    /**
     * Creates a PlanSkill with custom parser.
     *
     * @param parser the plans parser to use
     */
    public PlanSkill(PlansParser parser) {
        this.parser = parser;
    }

    @Override
    public String getId() {
        return "plan";
    }

    @Override
    public String getName() {
        return "Plan Skill";
    }

    @Override
    public String getDescription() {
        return "Skill for planning and creating project plans";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    protected Object doExecute(SkillContext context) throws SkillExecutionException {
        logger.info("Executing PlanSkill");

        try {
            // Get plans path from context or use default
            String plansPath = getPlansPath(context);

            // Parse the plans file
            PlansDocument document = parsePlans(plansPath);

            logger.info("Successfully parsed plans from {}", plansPath);
            return document;

        } catch (Exception e) {
            String message = "Failed to execute plan skill: " + e.getMessage();
            logger.error(message, e);
            throw new SkillExecutionException(getId(), message, e);
        }
    }

    @Override
    protected void validateContext(SkillContext context) throws SkillExecutionException {
        super.validateContext(context);

        // Additional validation: check if we can read plans file
        String plansPath = getPlansPath(context);
        if (plansPath != null && !Files.exists(Paths.get(plansPath))) {
            throw new SkillExecutionException(getId(), "Plans file not found: " + plansPath);
        }
    }

    /**
     * Gets the plans path from context.
     *
     * @param context the skill context
     * @return the plans path, or null if not configured
     */
    private String getPlansPath(SkillContext context) {
        String path = context.getConfiguration("plansPath", String.class);
        if (path == null) {
            path = "Plans.md";
        }
        return path;
    }

    /**
     * Parses the plans file.
     *
     * @param plansPath the path to the plans file
     * @return the parsed plans document
     * @throws Exception if parsing fails
     */
    private PlansDocument parsePlans(String plansPath) throws Exception {
        Path path = Paths.get(plansPath);

        if (!Files.exists(path)) {
            throw new SkillExecutionException(getId(), "Plans file not found: " + plansPath);
        }

        return parser.parse(path);
    }

    /**
     * Gets the parser used by this skill.
     *
     * @return the plans parser
     */
    protected PlansParser getParser() {
        return parser;
    }
}
