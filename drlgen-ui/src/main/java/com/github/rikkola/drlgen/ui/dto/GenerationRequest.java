package com.github.rikkola.drlgen.ui.dto;

import java.util.List;
import java.util.Map;

public record GenerationRequest(
        String requirement,
        String modelName,
        List<FactTypeDefinition> factTypes
) {
    public record FactTypeDefinition(
            String name,
            Map<String, String> fields
    ) {}
}
