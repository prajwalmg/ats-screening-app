package com.ats.resumeparsingservice.service;

import java.util.Set;

/**
 * Fixed keyword list resume text is matched against. Not exhaustive by
 * design — see the README for why a keyword dictionary was chosen over an
 * NLP/embedding approach for this project.
 */
final class SkillDictionary {

    static final Set<String> SKILLS = Set.of(
            // Languages
            "Java", "Python", "JavaScript", "TypeScript", "C++", "C#", "Go", "Rust",
            "Kotlin", "Swift", "Ruby", "PHP", "Scala", "SQL",
            // Frameworks / libraries
            "Spring", "Spring Boot", "React", "Angular", "Vue", "Node.js", "Express",
            "Django", "Flask", ".NET", "Hibernate", "JPA", "Next.js",
            // Data stores
            "PostgreSQL", "MySQL", "MongoDB", "Redis", "Elasticsearch", "Oracle",
            "Cassandra", "DynamoDB", "SQLite",
            // Cloud / DevOps
            "AWS", "Azure", "GCP", "Docker", "Kubernetes", "Terraform", "Jenkins",
            "CI/CD", "GitHub Actions", "Ansible", "Helm",
            // Messaging
            "Kafka", "RabbitMQ", "SQS",
            // Testing
            "JUnit", "Mockito", "Selenium", "Cypress", "Testcontainers", "Jest",
            // Concepts / practices
            "Microservices", "REST", "GraphQL", "Agile", "Scrum", "TDD",
            "System Design", "Machine Learning", "Data Structures", "Algorithms",
            "OAuth", "JWT",
            // Tools
            "Git", "Maven", "Gradle", "Jira", "Linux"
    );

    private SkillDictionary() {
    }
}
