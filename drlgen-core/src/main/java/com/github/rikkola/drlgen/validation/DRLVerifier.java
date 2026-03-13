package com.github.rikkola.drlgen.validation;

import org.drools.io.ClassPathResource;
import org.drools.io.InputStreamResource;
import org.drools.verifier.EmptyVerifierConfiguration;
import org.drools.verifier.Verifier;
import org.drools.verifier.builder.VerifierBuilder;
import org.drools.verifier.builder.VerifierBuilderFactory;
import org.drools.verifier.report.components.Severity;
import org.drools.verifier.report.components.VerifierMessageBase;
import org.kie.api.io.ResourceType;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.List;

public class DRLVerifier {

    private String CODE_LOOKS_GOOD = "Code looks good";

    public String verify(String code) {

        final VerifierBuilder vBuilder = VerifierBuilderFactory.newVerifierBuilder();

        final EmptyVerifierConfiguration verifierConfiguration = new EmptyVerifierConfiguration();
        verifierConfiguration.getVerifyingResources().put(
                new ClassPathResource("MyValidation.drl",
                        DRLVerifier.class),
                ResourceType.DRL

        );

        final Verifier verifier = vBuilder.newVerifier(verifierConfiguration);
        verifier.addResourcesToVerify(
                new InputStreamResource(new ByteArrayInputStream(code.getBytes())),
                ResourceType.DRL);

        verifier.fireAnalysis();

        final StringBuilder result = new StringBuilder();
        boolean hasIssues = false;

        // First check for compilation/parsing errors (most critical)
        if (verifier.hasErrors()) {
            hasIssues = true;
            for (org.drools.verifier.VerifierError error : verifier.getErrors()) {
                result.append("ERROR: ").append(error.getMessage()).append("\n");
            }
        }

        // Check for messages of all severity levels (ERROR, WARNING, NOTE)
        final Collection<VerifierMessageBase> errorMessages = verifier.getResult().getBySeverity(Severity.ERROR);
        final Collection<VerifierMessageBase> warningMessages = verifier.getResult().getBySeverity(Severity.WARNING);
        final Collection<VerifierMessageBase> noteMessages = verifier.getResult().getBySeverity(Severity.NOTE);

        // Add error messages (highest priority)
        if (!errorMessages.isEmpty()) {
            hasIssues = true;
            for (VerifierMessageBase message : errorMessages) {
                result.append("ERROR: ").append(message.getMessage()).append("\n");
            }
        }

        // Add warning messages
        if (!warningMessages.isEmpty()) {
            hasIssues = true;
            for (VerifierMessageBase message : warningMessages) {
                result.append("WARNING: ").append(message.getMessage()).append("\n");
            }
        }

        // Add note messages
        if (!noteMessages.isEmpty()) {
            hasIssues = true;
            for (VerifierMessageBase message : noteMessages) {
                result.append("NOTE: ").append(message.getMessage()).append("\n");
            }
        }

        if (hasIssues) {
            return result.toString().trim();
        } else {
            return CODE_LOOKS_GOOD;
        }
    }

    public Result verify(List<String> drls) {
        StringBuilder builder = new StringBuilder();
        drls.forEach(builder::append);

        String drlCode = builder.toString();
        
        // Auto-add common imports if missing
        drlCode = ensureCommonImports(drlCode);

        String status = verify(drlCode);
        if (status.equals(CODE_LOOKS_GOOD)) {
            return new Result(true, "");
        }

        return new Result(false, status);
    }

    private String ensureCommonImports(String drlCode) {
        System.out.println("DEBUG: Original DRL Code:");
        System.out.println(drlCode);
        System.out.println("=== END ORIGINAL DRL ===");
        
        // Common Java types that might be used in DRL without explicit imports
        String[] commonTypes = {
            "List", "ArrayList", "Map", "HashMap", "Set", "HashSet", 
            "Date", "BigDecimal", "BigInteger", "Optional"
        };
        
        String[] commonImports = {
            "import java.util.List;",
            "import java.util.ArrayList;", 
            "import java.util.Map;",
            "import java.util.HashMap;",
            "import java.util.Set;",
            "import java.util.HashSet;",
            "import java.util.Date;",
            "import java.math.BigDecimal;",
            "import java.math.BigInteger;",
            "import java.util.Optional;"
        };

        StringBuilder importsToAdd = new StringBuilder();
        
        // Check which types are used but not imported
        for (int i = 0; i < commonTypes.length; i++) {
            String type = commonTypes[i];
            String importStatement = commonImports[i];
            
            // Check if type is referenced but import is missing - use word boundary checking
            boolean typeUsed = drlCode.matches(".*\\b" + type + "\\b.*");
            boolean importExists = drlCode.contains(importStatement);
            
            System.out.println("DEBUG: Checking type '" + type + "' - used: " + typeUsed + ", imported: " + importExists);
            
            if (typeUsed && !importExists) {
                System.out.println("DEBUG: Adding import: " + importStatement);
                importsToAdd.append(importStatement).append("\n");
            }
        }
        
        // If we need to add imports, insert them after package declaration or at the beginning
        if (importsToAdd.length() > 0) {
            System.out.println("DEBUG: Adding imports: " + importsToAdd.toString());
            String result;
            if (drlCode.contains("package ")) {
                // Insert after package declaration
                int packageEnd = drlCode.indexOf('\n', drlCode.indexOf("package "));
                if (packageEnd > 0) {
                    result = drlCode.substring(0, packageEnd + 1) + "\n" + 
                           importsToAdd.toString() + "\n" + 
                           drlCode.substring(packageEnd + 1);
                } else {
                    result = drlCode + "\n" + importsToAdd.toString();
                }
            } else {
                // Insert at the beginning
                result = importsToAdd.toString() + "\n" + drlCode;
            }
            
            System.out.println("DEBUG: Final DRL with imports:");
            System.out.println(result);
            System.out.println("=== END FINAL DRL ===");
            return result;
        }
        
        System.out.println("DEBUG: No imports needed, returning original");
        return drlCode;
    }

    public record Result(boolean pass, String issue) {
    }
}