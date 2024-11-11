package org.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.example.model.CloudService;
import org.example.model.ServiceLimits;

public class ServiceRegistry {

    public static List<CloudService> createServices() {
        List<CloudService> serviceList = new ArrayList<>();

        ServiceLimits googleTranslateLimits =
            new ServiceLimits(30000, "characters");
        CloudService gcpTranslate = CloudServiceFactory.createService(
            "TextServices",
            "Google Cloud Translate",
            "GCP",
            Arrays.asList("Text", "HTML"),
            Arrays.asList("Text", "HTML"),
            Arrays.asList("Translation"),
            googleTranslateLimits
        );
        serviceList.add(gcpTranslate);

        ServiceLimits awsTranslateLimits = new ServiceLimits(10, "KB");
        CloudService awsTranslate = CloudServiceFactory.createService(
            "TextServices",
            "AWS Translate",
            "AWS",
            Arrays.asList("Text", "HTML"),
            Arrays.asList("Text", "HTML"),
            Arrays.asList("Translation"),
            awsTranslateLimits
        );
        serviceList.add(awsTranslate);

        ServiceLimits googleTextToSpeechLimits =
            new ServiceLimits(5000, "characters");
        CloudService googleTextToSpeech = CloudServiceFactory.createService(
            "SpeechServices",
            "Google Cloud Text-to-Speech",
            "GCP",
            Arrays.asList("Text"),
            Arrays.asList("MP3", "WAV", "OGG"),
            Arrays.asList("T2S"),
            googleTextToSpeechLimits
        );
        serviceList.add(googleTextToSpeech);

        ServiceLimits awsPollyLimits = new ServiceLimits(100000, "characters");
        CloudService awsPolly = CloudServiceFactory.createService(
            "SpeechServices",
            "AWS Polly",
            "AWS",
            Arrays.asList("Text", "SSML"),
            Arrays.asList("MP3", "PCM"),
            Arrays.asList("T2S"),
            awsPollyLimits
        );
        serviceList.add(awsPolly);

        ServiceLimits googleSpeechToTextLimits =
            new ServiceLimits(480, "minutes");
        CloudService googleSpeechToText = CloudServiceFactory.createService(
            "SpeechServices",
            "Google Cloud Speech-to-Text",
            "GCP",
            Arrays.asList("FLAC", "WAV", "MP3"),
            Arrays.asList("Text"),
            Arrays.asList("S2T"),
            googleSpeechToTextLimits
        );
        serviceList.add(googleSpeechToText);

        ServiceLimits awsTranscribeLimits = new ServiceLimits(4, "hours");
        CloudService awsTranscribe = CloudServiceFactory.createService(
            "SpeechServices",
            "AWS Transcribe",
            "AWS",
            Arrays.asList("MP3", "WAV", "FLAC"),
            Arrays.asList("Text", "SRT", "VTT"),
            Arrays.asList("S2T"),
            awsTranscribeLimits
        );
        serviceList.add(awsTranscribe);

        ServiceLimits googleVisionLimits = new ServiceLimits(20, "MB");
        CloudService googleVision = CloudServiceFactory.createService(
            "VisionServices",
            "Google Cloud Vision",
            "GCP",
            Arrays.asList("JPG", "PNG", "GIF"),
            Arrays.asList("Text", "JSON"),
            Arrays.asList("Labels", "Objects"),
            googleVisionLimits
        );
        serviceList.add(googleVision);

        ServiceLimits awsRekognitionLimits = new ServiceLimits(5, "MB");
        CloudService awsRekognition = CloudServiceFactory.createService(
            "VisionServices",
            "AWS Rekognition",
            "AWS",
            Arrays.asList("JPG", "PNG", "MP4"),
            Arrays.asList("JSON"),
            Arrays.asList("Face", "Labels"),
            awsRekognitionLimits
        );
        serviceList.add(awsRekognition);

        ServiceLimits awsTextractLimits = new ServiceLimits(500, "MB");
        CloudService awsTextract = CloudServiceFactory.createService(
            "DocumentServices",
            "AWS Textract",
            "AWS",
            Arrays.asList("PDF", "TIFF", "PNG"),
            Arrays.asList("Text", "JSON", "CSV"),
            Arrays.asList("OCR", "Forms", "Tables"),
            awsTextractLimits
        );
        serviceList.add(awsTextract);

        ServiceLimits googleDocumentAILimits = new ServiceLimits(1, "GB");
        CloudService googleDocumentAI = CloudServiceFactory.createService(
            "DocumentServices",
            "Google Document AI",
            "GCP",
            Arrays.asList("PDF", "TIFF", "GIF"),
            Arrays.asList("JSON", "CSV"),
            Arrays.asList("OCR", "Forms", "Parsing"),
            googleDocumentAILimits
        );
        serviceList.add(googleDocumentAI);

        return serviceList;
    }
}