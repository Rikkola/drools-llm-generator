package com.github.rikkola.drlgen.prompt;

/**
 * Interface for providing prompts used in DRL generation.
 *
 * <p>Implementations can load prompts from various sources such as classpath,
 * filesystem, database, or configuration management systems.</p>
 *
 * <p>This abstraction allows for:
 * <ul>
 *   <li>Model-specific prompts (different prompts for different LLM families)</li>
 *   <li>Domain-specific prompt customization</li>
 *   <li>A/B testing of different prompt strategies</li>
 *   <li>Runtime prompt updates without code changes</li>
 * </ul>
 */
public interface PromptProvider {

    /**
     * Returns the system message that defines the AI's role and behavior.
     *
     * <p>The system message typically instructs the AI to act as a Drools DRL expert
     * and specifies output format requirements.</p>
     *
     * @return the system message prompt
     */
    String getSystemMessage();

    /**
     * Returns the user message template with placeholders for variable substitution.
     *
     * <p>The template should include placeholders using the format {@code {{variableName}}}
     * for the following variables:
     * <ul>
     *   <li>{@code {{drlGuide}}} - The DRL reference guide content</li>
     *   <li>{@code {{requirement}}} - The business rule requirement</li>
     *   <li>{@code {{factTypes}}} - Fact type definitions</li>
     *   <li>{@code {{exampleInput}}} - Example test cases</li>
     * </ul>
     *
     * @return the user message template
     */
    String getUserMessageTemplate();

    /**
     * Creates a default PromptProvider that loads prompts from classpath.
     *
     * @return a classpath-based prompt provider
     */
    static PromptProvider createDefault() {
        return new ClasspathPromptProvider();
    }

    /**
     * Creates a PromptProvider that loads prompts from the filesystem.
     *
     * @param basePath the base directory containing prompt files
     * @return a filesystem-based prompt provider
     */
    static PromptProvider fromDirectory(java.nio.file.Path basePath) {
        return new FilePromptProvider(basePath);
    }
}
