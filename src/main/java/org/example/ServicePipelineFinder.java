package org.example;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import org.example.model.CloudService;
import org.example.model.Intent;

public class ServicePipelineFinder {

    // Class to represent a graph node connecting Cloud Services
    static class ServiceNode {

        CloudService service;
        String currentOutputType;
        String currentOutputLanguage;
        ServiceNode parent;

        public ServiceNode(CloudService service, String currentOutputType,
                           String currentOutputLanguage, ServiceNode parent) {
            this.service = service;
            this.currentOutputType = currentOutputType;
            this.currentOutputLanguage = currentOutputLanguage;
            this.parent = parent;
        }
    }

    // Method to create a pipeline
    public static List<CloudService> findPipeline(List<CloudService> services,
                                                  Intent intent) {
        boolean needsTranslation = !intent.getInputLanguage().equalsIgnoreCase(
            intent.getOutputLanguage());

        // Queue for BFS search
        Queue<ServiceNode> queue = new LinkedList<>();

        // Set to store visited nodes
        Set<String> visited = new HashSet<>();

        // Start search by adding all services that support the desired input type and input language
        for (CloudService service : services) {
            if (service.getInputFormat().stream().anyMatch(
                i -> i.equalsIgnoreCase(intent.getInputType()))) {
                queue.add(new ServiceNode(service, intent.getInputType(),
                                          intent.getInputLanguage(), null));
                visited.add(getVisitedKey(service, intent.getInputType(),
                                          intent.getInputLanguage()));
            }
        }

        // BFS loop
        while (!queue.isEmpty()) {
            ServiceNode currentNode = queue.poll();
            CloudService currentService = currentNode.service;

            // Check if the current service provides the desired output type and output language
            boolean outputMatches = currentService.getOutputFormat().stream()
                                                  .anyMatch(
                                                      i -> i.equalsIgnoreCase(
                                                          intent.getOutputType()));
            boolean languageMatches =
                currentNode.currentOutputLanguage.equalsIgnoreCase(
                    intent.getOutputLanguage());

            // Additional check if translation is needed
            boolean translationMatches =
                !needsTranslation || (needsTranslation &&
                                      currentService.getFeatures()
                                                    .contains("Translation") &&
                                      currentNode.currentOutputLanguage.equalsIgnoreCase(
                                          intent.getInputLanguage()));

            if (outputMatches && languageMatches && translationMatches) {
                // If found, return the entire pipeline
                List<CloudService> pipeline = new ArrayList<>();
                while (currentNode != null) {
                    pipeline.add(0, currentNode.service);
                    currentNode = currentNode.parent;
                }
                return pipeline;
            }

            // Otherwise, find all possible next services that accept the output of the current service as input
            for (CloudService nextService : services) {
                for (String nextInputType : nextService.getInputFormat()) {
                    if (currentService.getOutputFormat()
                                      .contains(nextInputType) &&
                        !visited.contains(
                            getVisitedKey(nextService, nextInputType,
                                          currentNode.currentOutputLanguage))) {

                        // Determine the next output language
                        String nextOutputLanguage =
                            currentNode.currentOutputLanguage;
                        if (nextService.getFeatures().contains("Translation")) {
                            nextOutputLanguage = intent.getOutputLanguage();
                        }

                        // Add the next service to the queue
                        queue.add(new ServiceNode(nextService, nextInputType,
                                                  nextOutputLanguage,
                                                  currentNode));
                        visited.add(getVisitedKey(nextService, nextInputType,
                                                  nextOutputLanguage));
                    }
                }
            }
        }

        // If no path was found, return an empty list
        return Collections.emptyList();
    }

    // Helper method to create a unique key for visited nodes
    private static String getVisitedKey(CloudService service, String outputType,
                                        String outputLanguage) {
        return service.getName() + "|" + outputType + "|" + outputLanguage;
    }
}

