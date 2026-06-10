package com.example.trackingbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "idea-chart")
public record IdeaChartProperties(
        String nodeBin,
        String nodePath,
        String rendererScript,
        String outputDir
) {
    public String nodeBinOrDefault() {
        return nodeBin == null || nodeBin.isBlank() ? "node" : nodeBin;
    }

    public String rendererScriptOrDefault() {
        return rendererScript == null || rendererScript.isBlank()
                ? "scripts/render-idea-chart.js"
                : rendererScript;
    }

    public String outputDirOrDefault() {
        return outputDir == null || outputDir.isBlank()
                ? System.getProperty("java.io.tmpdir") + "/trackingbot-idea-charts"
                : outputDir;
    }
}
