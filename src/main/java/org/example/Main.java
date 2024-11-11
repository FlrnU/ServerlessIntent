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

        List<CloudService> services = ServiceRegistry.createServices();

        try {
            intent =
                objectMapper.readValue(new File(filePath), Intent.class);

            System.out.println(intent.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

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
        ChatGPTExecutor executor =
            new ChatGPTExecutor(apiKey, intent, pipeline);
        executor.process();
    }
}
