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

            // Only create a new pipeline with a copy of previous pipeline if previous pipeline is not empty
            this.currentPipeline = previousPipeline.isEmpty()
                                   ? new ArrayList<>(
                Collections.singletonList(service))
                                   : new ArrayList<>(previousPipeline);

            // Only add the current service if it's not already the last service in the pipeline
            if (!previousPipeline.isEmpty() &&
                !previousPipeline.get(previousPipeline.size() - 1)
                                 .equals(service)) {
                this.currentPipeline.add(service);
            }
        }
    }

    public static List<CloudService> findPipeline(List<CloudService> services,
                                                  Intent intent) {
        Queue<ServiceNode> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();

        // Extract initial services filtering to separate method
        initializeQueueWithStartingServices(services, intent, queue, visited);

        while (!queue.isEmpty()) {
            ServiceNode currentNode = queue.poll();

            if (isPipelineComplete(currentNode, intent)) {
                return currentNode.currentPipeline;
            }

            // Explore possible next services
            for (CloudService nextService : services) {
                boolean inputTypeMatch = nextService.getInputFormat().stream()
                                                    .anyMatch(
                                                        format -> format.equalsIgnoreCase(
                                                            currentNode.currentOutputType));

                String potentialOutputLanguage = determineOutputLanguage(
                    currentNode.currentOutputLanguage,
                    nextService,
                    intent
                );

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

        return Collections.emptyList();
    }

    private static void initializeQueueWithStartingServices(
        List<CloudService> services,
        Intent intent,
        Queue<ServiceNode> queue,
        Set<String> visited
    ) {
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