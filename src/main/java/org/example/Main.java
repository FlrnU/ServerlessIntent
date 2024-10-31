package org.example;

import static org.example.ServicePipelineFinder.findPipeline;

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

        List<CloudService> services = ServiceCreator.createServices();

        try {
            intent =
                objectMapper.readValue(new File(filePath), Intent.class);

            System.out.println(intent.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        services.forEach(
            x -> System.out.println(x.toString())
        );

        List<CloudService> pipeline =
            findPipeline(services, intent);

        // Print the found pipeline
        if (pipeline.isEmpty()) {
            System.out.println(
                "No pipeline found that supports the desired transformation process.");
        } else {
            System.out.println("Found Pipeline:");
            for (CloudService service : pipeline) {
                System.out.println(service.getName());
            }
        }
    }
}
