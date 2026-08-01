package com.chachamaru.harness.workflow.parser;

import com.chachamaru.harness.workflow.model.PlansDocument;

import java.nio.file.Path;
import java.util.List;

/**
 * Parser interface for Plans.md files.
 *
 * <p>Implementations parse Plans.md files and produce {@link PlansDocument} instances.
 * The parser must handle table format and status markers.</p>
 *
 * @spec_reference spec.md#Workflow System
 */
public interface PlansParser {

    /**
     * Parses a Plans.md file into a PlansDocument.
     *
     * @param plansPath Path to the Plans.md file
     * @return Parsed document with all tasks
     * @throws ParseException if parsing fails
     */
    PlansDocument parse(Path plansPath) throws ParseException;

    /**
     * Parses Plans.md content from a string.
     *
     * @param content Markdown content of Plans.md
     * @param sourcePath Source path for error reporting
     * @return Parsed document with all tasks
     * @throws ParseException if parsing fails
     */
    PlansDocument parseString(String content, String sourcePath) throws ParseException;

    /**
     * Exception thrown when parsing fails.
     */
    class ParseException extends Exception {
        private final int lineNumber;
        private final String source;

        public ParseException(String message, int lineNumber, String source) {
            super(String.format("%s:%d: %s", source, lineNumber, message));
            this.lineNumber = lineNumber;
            this.source = source;
        }

        public ParseException(String message, Throwable cause, int lineNumber, String source) {
            super(String.format("%s:%d: %s", source, lineNumber, message), cause);
            this.lineNumber = lineNumber;
            this.source = source;
        }

        public int getLineNumber() {
            return lineNumber;
        }

        public String getSource() {
            return source;
        }
    }
}
