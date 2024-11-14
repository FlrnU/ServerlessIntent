package org.example;

import com.theokanning.openai.completion.chat.ChatMessage;
import java.time.Duration;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

import java.util.List;

public abstract class AWSBedrockLLMService implements LLMService {

    protected final BedrockRuntimeClient bedrockClient;
    protected final String modelIdentifier;

    public AWSBedrockLLMService(String region, String modelIdentifier) {
        this.bedrockClient = BedrockRuntimeClient.builder()
                                                 .region(Region.of(region))
                                                 .credentialsProvider(
                                                     ProfileCredentialsProvider.create())
                                                 .build();
        this.modelIdentifier = modelIdentifier;
    }

    @Override
    public String sendMessages(List<ChatMessage> messages) {
        String prompt = convertMessagesToPrompt(messages);

        InvokeModelRequest request = InvokeModelRequest.builder()
                                                       .modelId(modelIdentifier)
                                                       .body(
                                                           SdkBytes.fromUtf8String(
                                                               prompt))
                                                       .contentType(
                                                           "application/json")
                                                       .accept(
                                                           "application/json")
                                                       .build();

        InvokeModelResponse response = bedrockClient.invokeModel(request);
        String responseBody = response.body().asUtf8String();

        System.out.println(
            "-------------------------\nResponse Body\n" + responseBody);

        return parseResponse(responseBody);
    }

    protected abstract String convertMessagesToPrompt(
        List<ChatMessage> messages);

    protected abstract String parseResponse(String responseBody);
}