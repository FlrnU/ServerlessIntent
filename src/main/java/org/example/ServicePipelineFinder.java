package org.example;

import java.util.*;
import org.example.model.CloudService;
import org.example.model.Intent;

public class ServicePipelineFinder {

    // Enhanced ServiceNode to track more transformation details
    static class ServiceNode {

        CloudService service;
        String currentInputType;
        String currentOutputType;
        String currentInputLanguage;
        String currentOutputLanguage;
        List<CloudService> currentPipeline;

        public ServiceNode(CloudService service,
                           String inputType,
                           String outputType,
                           String inputLanguage,
                           String outputLanguage,
                           List<CloudService> previousPipeline) {
            this.service = service;
            this.currentInputType = inputType;
            this.currentOutputType = outputType;
            this.currentInputLanguage = inputLanguage;
            this.currentOutputLanguage = outputLanguage;

            // Create a new pipeline by copying previous and adding current service
            this.currentPipeline = new ArrayList<>(previousPipeline);
            this.currentPipeline.add(service);
        }
    }

    public static List<CloudService> findPipeline(List<CloudService> services,
                                                  Intent intent) {
        // Enhanced pipeline finding with more flexible transformations
        Queue<ServiceNode> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();

        // Start with initial services matching input type
        for (CloudService initialService : services) {
            if (initialService.getInputFormat().stream()
                              .anyMatch(format -> format.equalsIgnoreCase(
                                  intent.getInputType()))) {

                ServiceNode startNode = new ServiceNode(
                    initialService,
                    intent.getInputType(),
                    intent.getInputType(),
                    intent.getInputLanguage(),
                    intent.getInputLanguage(),
                    new ArrayList<>()
                );

                queue.add(startNode);
                visited.add(createVisitKey(startNode));
            }
        }

        // Expanded BFS with more comprehensive path finding
        while (!queue.isEmpty()) {
            ServiceNode currentNode = queue.poll();

            // Check if current pipeline meets the final requirement
            if (isPipelineComplete(currentNode, intent)) {
                return currentNode.currentPipeline;
            }

            // Explore possible next services
            for (CloudService nextService : services) {
                // Check if next service can accept current output
                boolean inputTypeMatch = nextService.getInputFormat().stream()
                                                    .anyMatch(
                                                        format -> format.equalsIgnoreCase(
                                                            currentNode.currentOutputType));

                // Determine potential output language
                String potentialOutputLanguage = determineOutputLanguage(
                    currentNode.currentOutputLanguage,
                    nextService,
                    intent
                );

                // Validate language transformation
                boolean languageTransformationValid =
                    isLanguageTransformationValid(
                        currentNode.currentOutputLanguage,
                        potentialOutputLanguage,
                        intent
                    );

                if (inputTypeMatch && languageTransformationValid) {
                    ServiceNode nextNode = new ServiceNode(
                        nextService,
                        currentNode.currentOutputType,
                        nextService.getOutputFormat().get(0),
                        // Assume first output format
                        currentNode.currentOutputLanguage,
                        potentialOutputLanguage,
                        currentNode.currentPipeline
                    );

                    String visitKey = createVisitKey(nextNode);
                    if (!visited.contains(visitKey)) {
                        queue.add(nextNode);
                        visited.add(visitKey);
                    }
                }
            }
        }

        return Collections.emptyList();  // No valid pipeline found
    }

    private static boolean isPipelineComplete(ServiceNode node, Intent intent) {
        return
            node.currentOutputType.equalsIgnoreCase(intent.getOutputType()) &&
            node.currentOutputLanguage.equalsIgnoreCase(
                intent.getOutputLanguage());
    }

    private static String determineOutputLanguage(
        String currentLanguage,
        CloudService service,
        Intent intent
    ) {
        // If service supports translation, use target language
        if (service.getFeatures().contains("Translation")) {
            return intent.getOutputLanguage();
        }
        return currentLanguage;
    }

    private static boolean isLanguageTransformationValid(
        String currentLanguage,
        String potentialOutputLanguage,
        Intent intent
    ) {
        // Check language transformation rules
        return potentialOutputLanguage.equalsIgnoreCase(
            intent.getOutputLanguage()) ||
               potentialOutputLanguage.equalsIgnoreCase(currentLanguage);
    }

    private static String createVisitKey(ServiceNode node) {
        return node.service.getName() + "|" +
               node.currentInputType + "|" +
               node.currentOutputType + "|" +
               node.currentInputLanguage + "|" +
               node.currentOutputLanguage;
    }
}