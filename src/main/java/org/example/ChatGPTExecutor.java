package org.example;

import com.theokanning.openai.service.OpenAiService;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.example.model.CloudService;
import org.example.model.Intent;

public class ChatGPTExecutor {

    private final String apiKey;
    private final OpenAiService openAiService;
    private final List<ChatMessage> messages;
    private final int maxIterations;
    private final List<CloudService> pipeline;
    private String inputType;
    private String inputLanguage;
    private String outputType;
    private String outputLanguage;
    private String bucketName;
    private String inputFilePath;
    private byte[] inputFileContent;
    private String inputFileTextContent;
    private long inputFileSize;

    public ChatGPTExecutor(String apiKey, Intent intent,
                           List<CloudService> pipeline) {
        this.apiKey = apiKey;
        this.openAiService = new OpenAiService(apiKey, Duration.ofSeconds(50));
        this.messages = new ArrayList<>();
        this.maxIterations = 10;
        this.pipeline = pipeline;

        // Initialize from Intent
        this.inputType = intent.getInputType();
        this.inputLanguage = intent.getInputLanguage();
        this.outputType = intent.getOutputType();
        this.outputLanguage = intent.getOutputLanguage();
        this.bucketName = intent.getBucketName();
        this.inputFilePath = intent.getInputFilePath();

        // Load the input file
        loadInputFile();
    }

    private void loadInputFile() {
        try {
            if (inputFilePath == null || inputFilePath.isEmpty()) {
                throw new IllegalArgumentException(
                    "Input file path is not specified");
            }

            Path path = Path.of(inputFilePath);
            inputFileContent = Files.readAllBytes(path);
            inputFileSize = Files.size(path);

            // Try to read as text if it's a text-based format
            if (isTextBasedFormat()) {
                try {
                    inputFileTextContent = Files.readString(path);
                } catch (Exception e) {
                    inputFileTextContent = null;
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(
                "Failed to read input file: " + e.getMessage(), e);
        }
    }

    private boolean isTextBasedFormat() {
        String type = inputType.toLowerCase();
        return type.contains("text") ||
               type.contains("json") ||
               type.contains("xml") ||
               type.contains("csv") ||
               type.contains("yaml") ||
               type.contains("html");
    }

    private String getInputFileDescription() {
        StringBuilder description = new StringBuilder();
        description.append("Input File Information:\n");
        description.append("- File type: ").append(inputType).append("\n");
        description.append("- File size: ")
                   .append(formatFileSize(inputFileSize)).append("\n");
        description.append("- File path: ").append(inputFilePath).append("\n");

        if (inputFileTextContent != null) {
            // For text files, include a preview
            String preview = inputFileTextContent.length() > 1000
                             ? inputFileTextContent.substring(0, 1000) +
                               "... (truncated)"
                             : inputFileTextContent;
            description.append("- Content preview:\n").append(preview)
                       .append("\n");
        } else {
            // For binary files, include metadata and handling instructions
            description.append("- Binary file detected\n");
            if (inputType.toLowerCase().contains("pdf")) {
                description.append(
                    "- PDF handling instructions: Use boto3's AWS Textract client for text extraction\n");
            } else if (inputType.toLowerCase().contains("image")) {
                description.append(
                    "- Image handling instructions: Use boto3's AWS Rekognition or Textract as appropriate\n");
            }
        }

        return description.toString();
    }

    private String formatFileSize(long size) {
        final String[] units = new String[]{"B", "KB", "MB", "GB"};
        int unitIndex = 0;
        double fileSize = size;

        while (fileSize >= 1024 && unitIndex < units.length - 1) {
            fileSize /= 1024;
            unitIndex++;
        }

        return String.format("%.2f %s", fileSize, units[unitIndex]);
    }

    private String generateDetailedPipelineDescription() {
        if (pipeline == null || pipeline.isEmpty()) {
            return "No specific AWS services are required.";
        }

        StringBuilder description = new StringBuilder(
            "The transformation must use the following AWS services in this exact order. " +
            "Each service has specific capabilities and limitations that must be respected:\n\n");

        for (int i = 0; i < pipeline.size(); i++) {
            CloudService service = pipeline.get(i);
            description.append(
                String.format("%d. %s\n", i + 1, service.getName()));

            // Input Capabilities
            description.append("   Accepts: ");
            if (service.getInputFormat() != null &&
                !service.getInputFormat().isEmpty()) {
                description.append(String.join(", ", service.getInputFormat()));
            } else {
                description.append("No specific input type restrictions");
            }
            description.append("\n");

            // Output Capabilities
            description.append("   Produces: ");
            if (service.getOutputFormat() != null &&
                !service.getOutputFormat().isEmpty()) {
                description.append(
                    String.join(", ", service.getOutputFormat()));
            } else {
                description.append("No specific output type restrictions");
            }
            description.append("\n");

            // Service-specific limitations
            if (service.getServiceLimits() != null &&
                service.getServiceLimits().getLimit() != 0) {
                description.append("   Limitations:\n");

                description.append("    - ")
                           .append(service.getServiceLimits().getLimit())
                           .append(service.getServiceLimits().getUnit())
                           .append("\n");

            }

            // Service-specific capabilities
            if (service.getFeatures() != null &&
                !service.getFeatures().isEmpty()) {
                description.append("   Capabilities:\n");
                for (String capability : service.getFeatures()) {
                    description.append("    - ").append(capability)
                               .append("\n");
                }
            }

            // Add connection information to next service if not the last service
            if (i < pipeline.size() - 1) {
                CloudService nextService = pipeline.get(i + 1);
                description.append("   Must connect to: ")
                           .append(nextService.getName());

            }

            description.append("\n");
        }

        // Add overall pipeline requirements
        description.append("Overall Pipeline Requirements:\n");
        description.append("1. Initial input: ").append(inputType)
                   .append(" in ").append(inputLanguage).append("\n");
        description.append("2. Final output: ").append(outputType)
                   .append(" in ").append(outputLanguage).append("\n");
        description.append(
            "3. Each service must properly handle the output of the previous service and prepare input for the next service\n");
        description.append(
            "4. Implement proper error handling and validation between service transitions\n");

        return description.toString();
    }

    public void process() {
        // Add system message with enhanced context
        messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(),
                                     "You are a helpful assistant who generates and executes bash and Python code. " +
                                     "You should adhere to the specified services pipeline, their order, and their limitations. " +
                                     "Ensure each service's input/output requirements are met and properly handled in the code."));

        // Generate initial prompt with detailed pipeline information
        String pipelineInfo = generateDetailedPipelineDescription();
        String fileInfo = getInputFileDescription();

        String initialPrompt = String.format(
            "Generate executable Python code for the following transformation task:\n\n" +
            "Source Format: %s in %s\n" +
            "Target Format: %s in %s\n\n" +
            "%s\n" +
            "Detailed Service Pipeline Configuration:\n%s\n" +
            "Additional Requirements:\n" +
            "- AWS credentials are already configured\n" +
            "- If you need bucket use the following: %s\n" +
            "- Implement proper error handling for each service\n" +
            "- Validate input/output at each step\n" +
            "Please generate code that follows this service pipeline and handles all limitations.\n" +
            "If the limits are surpassed by the input you need to try to split the input before calling the service and merge it afterwards" +
            "The merging together after splitting is very important and should also be done on audio outputs!",
            inputType, inputLanguage,
            outputType, outputLanguage,
            fileInfo,
            pipelineInfo,
            bucketName);

        System.out.println(initialPrompt);

        messages.add(
            new ChatMessage(ChatMessageRole.USER.value(), initialPrompt));

        for (int i = 0; i < maxIterations; i++) {
            System.out.printf("Iteration %d: Sending prompt to ChatGPT...%n",
                              i + 1);

            String responseText = callChatGPTApi();
            System.out.printf("Received response:\n%s\n-------\n",
                              responseText);

            // Extract and execute bash code
            extractAndExecuteShellOrBashCode(responseText);

            // Extract Python code
            String pythonCode = extractPythonCode(responseText);
            System.out.printf(
                "\n-------\nExtracted Python code:\n%s\n-------\n", pythonCode);

            messages.add(
                new ChatMessage(ChatMessageRole.ASSISTANT.value(), pythonCode));

            // Execute Python code
            ExecutionResult result = executePythonCode(pythonCode);

            if (result.hasError() || result.output.contains("error") ||
                result.output.contains("Error")) {
                System.out.printf("Error during execution: %s%n",
                                  result.getError());
                messages.add(new ChatMessage(ChatMessageRole.USER.value(),
                                             String.format(
                                                 "Error during execution: %s\n\n" +
                                                 "Please fix the code while ensuring you:\n" +
                                                 "1. Follow the exact service pipeline\n" +
                                                 "2. Respect all service limitations\n" +
                                                 "3. Handle the specific error encountered\n\n" +
                                                 "Service Pipeline Reference:\n%s",
                                                 result.getError(),
                                                 pipelineInfo)));
            } else {
                System.out.printf("Execution result: %s\n-------\n",
                                  result.getOutput());
                System.out.println("Python code executed successfully.");
                messages.add(new ChatMessage(ChatMessageRole.USER.value(),
                                             "The code executed successfully. The result was: " +
                                             result.getOutput()));
                break;
            }
        }
    }

    private String callChatGPTApi() {
        ChatCompletionRequest completionRequest =
            ChatCompletionRequest.builder()
                                 .model("gpt-4o")
                                 .messages(messages)
                                 .maxTokens(5000)
                                 .build();

        return openAiService.createChatCompletion(completionRequest)
                            .getChoices().get(0).getMessage().getContent();
    }

    private void extractAndExecuteShellOrBashCode(String responseText) {
        Pattern pattern =
            Pattern.compile("```(bash|shell)(.*?)```", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(responseText);

        while (matcher.find()) {
            String shellCode = matcher.group(2).trim();
            System.out.printf("Executing bash code:\n%s%n", shellCode);
            executeBashCode(shellCode);
        }
    }

    private void executeBashCode(String bashCode) {
        try {
            Process process =
                Runtime.getRuntime().exec(new String[]{"bash", "-c", bashCode});
            String output = new String(process.getInputStream().readAllBytes());
            String error = new String(process.getErrorStream().readAllBytes());

            if (!error.isEmpty()) {
                System.out.printf("Error executing bash command: %s%n", error);
            } else {
                System.out.printf(
                    "Bash command executed successfully: %s\n-------\n",
                    output);
            }
        } catch (IOException e) {
            System.out.printf("Error while executing bash code: %s%n",
                              e.getMessage());
        }
    }

    private String extractPythonCode(String responseText) {
        Pattern pattern = Pattern.compile("```python(.*?)```", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(responseText);

        StringBuilder pythonCode = new StringBuilder();
        while (matcher.find()) {
            pythonCode.append(matcher.group(1).trim()).append("\n");
        }
        return pythonCode.toString();
    }

    private ExecutionResult executePythonCode(String pythonCode) {
        if (pythonCode.isEmpty()) {
            return new ExecutionResult("",
                                       "No code block found in the response.");
        }

        try {
            Path tempFile = Files.createTempFile("script", ".py");
            Files.writeString(tempFile, pythonCode);

            Process process = Runtime.getRuntime().exec(
                new String[]{"python", tempFile.toString()});
            String output = new String(process.getInputStream().readAllBytes());
            String error = new String(process.getErrorStream().readAllBytes());

            Files.delete(tempFile);
            return new ExecutionResult(output, error);
        } catch (IOException e) {
            return new ExecutionResult("", e.getMessage());
        }
    }

    private static class ExecutionResult {

        private final String output;
        private final String error;

        public ExecutionResult(String output, String error) {
            this.output = output;
            this.error = error;
        }

        public String getOutput() {
            return output;
        }

        public String getError() {
            return error;
        }

        public boolean hasError() {
            return !error.isEmpty();
        }
    }
}