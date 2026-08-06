package com.ats.resumeparsingservice.service;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.ats.resumeparsingservice.dto.ParsedResumeResponse;
import com.ats.resumeparsingservice.exception.ResumeParsingException;

@Service
public class ResumeParsingService {

    private static final Pattern YEARS_PATTERN = Pattern.compile(
            "(\\d{1,2})\\+?\\s*(?:years?|yrs?)\\s*(?:of\\s*)?(?:experience|exp)?",
            Pattern.CASE_INSENSITIVE);
    private static final int MAX_RESUME_TEXT_CHARS = 4000;

    private final RestClient downloadClient;
    private final Tika tika = new Tika();

    public ResumeParsingService(
            @Value("${http-client.connect-timeout-ms:2000}") int connectTimeoutMs,
            @Value("${http-client.read-timeout-ms:10000}") int readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        this.downloadClient = RestClient.builder().requestFactory(factory).build();
    }

    public ParsedResumeResponse parse(String resumeUrl) {
        byte[] content = download(resumeUrl);
        String text = extractText(content);
        List<String> skills = SkillDictionary.SKILLS.stream()
                .filter(skill -> containsSkill(text, skill))
                .toList();
        Integer years = extractYearsOfExperience(text);
        String excerpt = text.length() > MAX_RESUME_TEXT_CHARS ? text.substring(0, MAX_RESUME_TEXT_CHARS) : text;
        return new ParsedResumeResponse(skills, years, text.length(), excerpt);
    }

    private byte[] download(String resumeUrl) {
        try {
            byte[] bytes = downloadClient.get().uri(URI.create(resumeUrl)).retrieve().body(byte[].class);
            if (bytes == null || bytes.length == 0) {
                throw new ResumeParsingException("Downloaded resume was empty: " + resumeUrl);
            }
            return bytes;
        } catch (RestClientException e) {
            throw new ResumeParsingException("Failed to download resume from " + resumeUrl + ": " + e.getMessage());
        }
    }

    private String extractText(byte[] content) {
        try {
            return tika.parseToString(new ByteArrayInputStream(content));
        } catch (Exception e) {
            throw new ResumeParsingException("Failed to extract text from resume: " + e.getMessage());
        }
    }

    private boolean containsSkill(String text, String skill) {
        String lowerText = text.toLowerCase(Locale.ROOT);
        String lowerSkill = skill.toLowerCase(Locale.ROOT);
        int idx = lowerText.indexOf(lowerSkill);
        while (idx != -1) {
            boolean leftOk = idx == 0 || !Character.isLetterOrDigit(lowerText.charAt(idx - 1));
            int endIdx = idx + lowerSkill.length();
            boolean rightOk = endIdx == lowerText.length() || !Character.isLetterOrDigit(lowerText.charAt(endIdx));
            if (leftOk && rightOk) {
                return true;
            }
            idx = lowerText.indexOf(lowerSkill, idx + 1);
        }
        return false;
    }

    private Integer extractYearsOfExperience(String text) {
        Matcher matcher = YEARS_PATTERN.matcher(text);
        int max = -1;
        while (matcher.find()) {
            int value = Integer.parseInt(matcher.group(1));
            if (value <= 60 && value > max) {
                max = value;
            }
        }
        return max >= 0 ? max : 0;
    }
}
