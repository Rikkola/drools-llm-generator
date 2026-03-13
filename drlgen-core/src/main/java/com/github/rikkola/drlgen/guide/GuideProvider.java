package com.github.rikkola.drlgen.guide;

import java.nio.file.Path;

/**
 * Interface for providing the DRL reference guide used in code generation.
 *
 * <p>Implementations can load guides from various sources such as classpath,
 * filesystem, or remote services.</p>
 *
 * <p>This abstraction allows for:
 * <ul>
 *   <li>Model-specific guides (different guides for different LLM families)</li>
 *   <li>Domain-specific guide customization</li>
 *   <li>Composite guides combining base and domain instructions</li>
 *   <li>Runtime guide updates without code changes</li>
 * </ul>
 */
public interface GuideProvider {

    /**
     * Returns the DRL reference guide content.
     *
     * <p>The guide typically contains syntax documentation, examples,
     * and best practices for writing DRL code.</p>
     *
     * @return the guide content, never null
     */
    String getGuide();

    /**
     * Creates a default GuideProvider that loads the guide from classpath.
     *
     * @return a classpath-based guide provider
     */
    static GuideProvider createDefault() {
        return new ClasspathGuideProvider();
    }

    /**
     * Creates a GuideProvider that loads a guide from the filesystem.
     *
     * @param filePath path to the guide file
     * @return a filesystem-based guide provider
     */
    static GuideProvider fromFile(Path filePath) {
        return new FileGuideProvider(filePath);
    }

    /**
     * Creates a composite GuideProvider that combines a base guide with domain instructions.
     *
     * @param baseProvider the base guide provider
     * @param domainInstructionsPath path to domain-specific instructions (may be null)
     * @return a composite guide provider
     */
    static GuideProvider withDomainInstructions(GuideProvider baseProvider, Path domainInstructionsPath) {
        return new CompositeGuideProvider(baseProvider, domainInstructionsPath);
    }
}
