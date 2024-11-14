package org.example.executor;

import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.completion.chat.ChatMessageRole;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.example.llm.LLMService;
import org.example.model.CloudService;
import org.example.model.Intent;
import org.example.utlis.FileUtils;

public class LLMRequestExecutor {

    private static final int MAX_ITERATIONS = 10;
    private static final String SYSTEM_PROMPT =
        """
        You are a helpful assistant who generates and executes bash and Python code.
        You should adhere to the specified services pipeline, their order, and their limitations.
        Ensure each service's input/output requirements are met and properly handled in the code.
        """;


    private final List<ChatMessage> messages;
    private final List<CloudService> pipeline;
    private final String inputType;
    private final String inputLanguage;
    private final String outputType;
    private final String outputLanguage;
    private final String bucketName;
    private final String inputFilePath;
    private String inputFileTextContent;
    private long inputFileSize;
    private final LLMService llmService;

    public LLMRequestExecutor(LLMService llmService, Intent intent,
                              List<CloudService> pipeline) {
        this.llmService = llmService;
        this.messages = new ArrayList<>();
        this.pipeline = pipeline;
        this.inputType = intent.getInputType();
        this.inputLanguage = intent.getInputLanguage();
        this.outputType = intent.getOutputType();
        this.outputLanguage = intent.getOutputLanguage();
        this.bucketName = intent.getBucketName();
        this.inputFilePath = intent.getInputFilePath();

        loadInputFile();
    }

    private void loadInputFile() {
        try {
            if (inputFilePath == null || inputFilePath.isEmpty()) {
                throw new IllegalArgumentException(
                    "Input file path is not specified");
            }

            Path path = Path.of(inputFilePath);
            inputFileSize = FileUtils.getFileSize(path.toString());

            if (isTextBasedFormat(inputType)) {
                try {
                    inputFileTextContent =
                        FileUtils.readTextFile(path.toString());
                } catch (Exception e) {
                    inputFileTextContent = null;
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(
                "Failed to read input file: " + e.getMessage(), e);
        }
    }

    public void process() {
        messages.add(new ChatMessage(ChatMessageRole.SYSTEM.value(),
                                     SYSTEM_PROMPT));

        // Generate initial prompt with detailed pipeline information
        String pipelineInfo = generateDetailedPipelineDescription();
        String fileInfo =
            FileUtils.getInputFileDescription(inputType, inputFileSize,
                                              inputFilePath,
                                              inputFileTextContent);

        String initialPrompt = String.format("""
                                             Generate executable Python code for the following transformation task:
                                             
                                             Source Format: %s in %s
                                             Target Format: %s in %s
                                             
                                             %s
                                             Detailed Service Pipeline Configuration:
                                             %s
                                             Additional Requirements:
                                             - AWS credentials are already configured
                                             - If you need a bucket, use the following: %s
                                             - Implement proper error handling for each service
                                             - Validate input/output at each step
                                             Please generate code that follows this service pipeline, handles all limitations, and annotate it correctly with ```python.
                                             If the limits are surpassed by the input, you need to try to split the input before calling the service and merge it afterward.
                                             Merging together after splitting is very important and should also be done on audio outputs!
                                             If possible, save the output locally.
                                             """,
                                             inputType, inputLanguage,
                                             outputType, outputLanguage,
                                             fileInfo,
                                             pipelineInfo,
                                             bucketName);

        System.out.println(initialPrompt);

        messages.add(
            new ChatMessage(ChatMessageRole.USER.value(), initialPrompt));

        for (int i = 0; i < MAX_ITERATIONS; i++) {
            System.out.printf("Iteration %d: Sending prompt to LLM...%n",
                              i + 1);

            String responseText = callLLMService();
            System.out.printf("Received response:%n%s%n-------%n",
                              responseText);

            extractAndExecuteShellOrBashCode(responseText);

            String pythonCode = extractPythonCode(responseText);
            System.out.printf(
                "%n-------%nExtracted Python code:%n%s%n-------%n", pythonCode);

            messages.add(
                new ChatMessage(ChatMessageRole.ASSISTANT.value(), pythonCode));

            ExecutionResult result = CodeExecutor.executePythonCode(pythonCode);

            if (result.hasError()) {
                System.out.printf("Error during execution: %s%n",
                                  result.getError());
                messages.add(new ChatMessage(ChatMessageRole.USER.value(),
                                             String.format("""
                                                           Error during execution: %s
                                                           
                                                           Please fix the code while ensuring you:
                                                           1. Follow the exact service pipeline
                                                           2. Respect all service limitations
                                                           3. Handle the specific error encountered
                                                           4. Always annotate code as ```python
                                                           
                                                           Service Pipeline Reference:
                                                           %s
                                                           """,
                                                           result.getError(),
                                                           pipelineInfo)));
            } else if (result.getOutput().toLowerCase().contains("error")) {
                System.out.printf("Error during execution: %s%n",
                                  result.getOutput());
                messages.add(new ChatMessage(ChatMessageRole.USER.value(),
                                             String.format("""
                                                           Error during execution: %s
                                                           
                                                           Please fix the code while ensuring you:
                                                           1. Follow the exact service pipeline
                                                           2. Respect all service limitations
                                                           3. Handle the specific error encountered
                                                           
                                                           Service Pipeline Reference:
                                                           %s
                                                           """,
                                                           result.getOutput(),
                                                           pipelineInfo)));
            } else {
                System.out.printf("Execution result: %s%n-------%n",
                                  result.getOutput());
                System.out.println("Python code executed successfully.");
                System.out.println("Number of needed iterations: " + (i + 1));
                messages.add(new ChatMessage(ChatMessageRole.USER.value(),
                                             "The code executed successfully. The result was: " +
                                             result.getOutput()));
                break;
            }
        }
    }

    private boolean isTextBasedFormat(String type) {
        return type.contains("text") ||
               type.contains("json") ||
               type.contains("xml") ||
               type.contains("csv") ||
               type.contains("yaml") ||
               type.contains("html");
    }

    private String generateDetailedPipelineDescription() {
        if (pipeline == null || pipeline.isEmpty()) {
            return "No specific AWS services are required.";
        }

        StringBuilder description = new StringBuilder(
            """
            The transformation must use the following AWS services in this exact order.
            Each service has specific capabilities and limitations that must be respected:
            
            
            """);

        for (int i = 0; i < pipeline.size(); i++) {
            CloudService service = pipeline.get(i);
            description.append(
                String.format("%d. %s%n", i + 1, service.getName()));

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

        description.append("Overall Pipeline Requirements:\n");
        description.append("1. Initial input: ").append(inputType)
                   .append(" in ").append(inputLanguage).append("\n");
        description.append("2. Final output: ").append(outputType)
                   .append(" in ").append(outputLanguage).append("\n");
        description.append(
            "3. Each service must properly handle the output of the previous service " +
            "and prepare input for the next service\n");
        description.append(
            "4. Implement proper error handling and validation between service transitions\n");

        return description.toString();
    }

    private String callLLMService() {
        return llmService.sendMessages(messages);
    }

    private void extractAndExecuteShellOrBashCode(String responseText) {
        Pattern pattern =
            Pattern.compile("```(bash|shell)(.*?)```", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(responseText);

        while (matcher.find()) {
            String shellCode = matcher.group(2).trim();
            System.out.printf("Executing bash code:%n%s%n", shellCode);
            CodeExecutor.executeShellCode(shellCode);
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
}