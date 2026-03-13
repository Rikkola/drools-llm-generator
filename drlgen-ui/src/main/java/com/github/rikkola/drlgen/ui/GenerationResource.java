package com.github.rikkola.drlgen.ui;

import dev.langchain4j.model.chat.ChatModel;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import com.github.rikkola.drlgen.DRLGenerator;
import com.github.rikkola.drlgen.config.ModelConfiguration;
import com.github.rikkola.drlgen.model.GenerationResult;
import com.github.rikkola.drlgen.ui.dto.GenerationRequest;
import com.github.rikkola.drlgen.ui.dto.GenerationResponse;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * REST resource for DRL generation endpoints.
 *
 * <p>Provides endpoints for:
 * <ul>
 *   <li>Listing available AI models</li>
 *   <li>Generating DRL from natural language requirements</li>
 * </ul>
 */
@Path("/api/generate")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GenerationResource {

    // Cache generators per model to avoid recreating them
    private final Map<String, DRLGenerator> generatorCache = new ConcurrentHashMap<>();

    @GET
    @Path("/models")
    public List<String> getAvailableModels() {
        return ModelConfiguration.getAvailableModels();
    }

    @POST
    @Path("/drl")
    public GenerationResponse generateDRL(GenerationRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            // Get or create generator for this model
            DRLGenerator generator = getOrCreateGenerator(request.modelName());

            String factTypesDescription = buildFactTypesDescription(request);

            GenerationResult result = generator.generate(
                    request.requirement(), factTypesDescription);

            long elapsed = System.currentTimeMillis() - startTime;

            if (result.validationPassed()) {
                return GenerationResponse.success(result.generatedDrl(), elapsed);
            } else {
                return GenerationResponse.validationFailed(
                        result.generatedDrl(),
                        "Validation failed: " + result.validationMessage(),
                        elapsed);
            }
        } catch (Exception e) {
            return GenerationResponse.error("Generation failed: " + e.getMessage());
        }
    }

    private DRLGenerator getOrCreateGenerator(String modelName) {
        return generatorCache.computeIfAbsent(modelName, name -> {
            ChatModel chatModel = ModelConfiguration.createModel(name);
            return DRLGenerator.builder()
                    .chatModel(chatModel)
                    .build();
        });
    }

    private String buildFactTypesDescription(GenerationRequest request) {
        if (request.factTypes() == null || request.factTypes().isEmpty()) {
            return "Use appropriate fact types based on the requirement.";
        }

        StringBuilder sb = new StringBuilder();
        for (var ft : request.factTypes()) {
            sb.append("- ").append(ft.name()).append(": ");
            sb.append(ft.fields().entrySet().stream()
                    .map(e -> e.getKey() + " (" + e.getValue() + ")")
                    .collect(Collectors.joining(", ")));
            sb.append("\n");
        }
        return sb.toString();
    }
}
