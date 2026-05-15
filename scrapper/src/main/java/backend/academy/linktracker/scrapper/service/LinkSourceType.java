package backend.academy.linktracker.scrapper.service;

import lombok.Getter;
import java.util.Arrays;

@Getter
public enum LinkSourceType {
    GITHUB("github.com", "github-updates"),
    STACKOVERFLOW("stackoverflow.com", "stackoverflow-updates");

    private final String host;
    private final String topic;

    LinkSourceType(String host, String topic) {
        this.host = host;
        this.topic = topic;
    }

    public static String getTopic(String url, String fallbackTopic) {
        return Arrays.stream(values())
            .filter(type -> url.contains(type.host))
            .map(type -> type.topic)
            .findFirst()
            .orElse(fallbackTopic);
    }

}
