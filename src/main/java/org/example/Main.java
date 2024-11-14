package org.example;

import static org.example.OrchestratorModule.findPipeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.example.model.CloudService;
import org.example.model.Intent;

public class Main {

    public static void main(String[] args) {
        ObjectMapper objectMapper = new ObjectMapper();
        String filePath = "./input.json";
        Intent intent = null;

        try {
            intent =
                objectMapper.readValue(new File(filePath), Intent.class);

            System.out.println(intent.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<CloudService> services =
            ServiceRegistry.createServicesFromJson(intent.getServiceFilePath());

        List<CloudService> pipeline =
            findPipeline(services, intent);

        if (pipeline.isEmpty()) {
            System.out.println(
                "No pipeline found that supports the desired transformation process.");
        } else {
            System.out.println("Found Pipeline:");
            for (CloudService service : pipeline) {
                System.out.println(service.getName());
            }
        }

        String apiKey =
            "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";
        LLMService llmService =
            LLMServiceFactory.getLLMService(intent.getLlmProvider(), apiKey);
        LLMRequestExecutor executor =
            new LLMRequestExecutor(llmService, intent, pipeline);
        executor.process();
    }
}
