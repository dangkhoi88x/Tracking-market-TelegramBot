package com.example.trackingbot.service.news;

import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TelegramChannelNewsService {

    private static final String CHANNEL_USERNAME = "vncointele";
    private static final String PUBLIC_CHANNEL_URL = "https://t.me/s/" + CHANNEL_USERNAME;
    private static final String POST_URL_PREFIX = "https://t.me/" + CHANNEL_USERNAME + "/";
    public static final int PAGE_SIZE = 5;
    public static final int MAX_PAGES = 5;
    private static final int MAX_NEWS_COUNT = PAGE_SIZE * MAX_PAGES;
    private static final int MAX_POST_LENGTH = 650;

    private final RestClient.Builder restClientBuilder;

    @Cacheable(cacheNames = "telegramChannelNews", key = "'vncointele-page-' + #page")
    public NewsPage getLatestNewsPage(int page) {
        String html = restClientBuilder.build()
                .get()
                .uri(PUBLIC_CHANNEL_URL)
                .header(HttpHeaders.USER_AGENT, "Mozilla/5.0 TrackingBot")
                .retrieve()
                .body(String.class);

        List<ChannelPost> posts = parseLatestPosts(html, MAX_NEWS_COUNT);
        if (posts.isEmpty()) {
            return new NewsPage(1, 1, """
                    Chua lay duoc tin moi tu @vncointele.

                    Ban thu lai sau nhe.
                    """);
        }

        int totalPages = Math.min(MAX_PAGES, Math.max(1, (int) Math.ceil((double) posts.size() / PAGE_SIZE)));
        int safePage = Math.max(1, Math.min(page, totalPages));
        int fromIndex = (safePage - 1) * PAGE_SIZE;
        int toIndex = Math.min(fromIndex + PAGE_SIZE, posts.size());

        StringBuilder message = new StringBuilder("Tin tuc moi tu @vncointele - trang %d/%d:\n\n".formatted(safePage, totalPages));
        for (int index = fromIndex; index < toIndex; index++) {
            ChannelPost post = posts.get(index);
            message.append("%d. %s\n".formatted(index + 1, post.text()));
            if (!post.url().isBlank()) {
                message.append("Link: ").append(post.url()).append("\n");
            }
            message.append("\n");
        }

        message.append("Nguon: @vncointele");
        return new NewsPage(safePage, totalPages, message.toString().trim());
    }

    List<ChannelPost> parseLatestPosts(String html, int limit) {
        if (html == null || html.isBlank()) {
            return List.of();
        }

        Document document = Jsoup.parse(html);
        List<Element> messageElements = document.select(".tgme_widget_message").stream()
                .filter(element -> element.selectFirst(".tgme_widget_message_text") != null)
                .toList();

        List<ChannelPost> posts = new ArrayList<>();
        for (Element element : messageElements) {
            Element textElement = element.selectFirst(".tgme_widget_message_text");
            String text = cleanText(textElement);
            if (text.isBlank()) {
                continue;
            }

            posts.add(new ChannelPost(text, extractPostUrl(element)));
        }

        Collections.reverse(posts);
        return posts.stream()
                .limit(limit)
                .toList();
    }

    private String cleanText(Element textElement) {
        String lineBreakMarker = "___TRACKING_BOT_LINE_BREAK___";
        String html = textElement.html()
                .replace("<br>", lineBreakMarker)
                .replace("<br/>", lineBreakMarker)
                .replace("<br />", lineBreakMarker);
        String text = Jsoup.parse(html).text()
                .replace(lineBreakMarker, "\n")
                .replace('\u00A0', ' ')
                .replaceAll("[ \\t]+", " ")
                .replaceAll(" *\\n *", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();

        if (text.length() <= MAX_POST_LENGTH) {
            return text;
        }

        return text.substring(0, MAX_POST_LENGTH).trim() + "...";
    }

    private String extractPostUrl(Element messageElement) {
        String dataPost = messageElement.attr("data-post");
        if (!dataPost.isBlank()) {
            int slashIndex = dataPost.lastIndexOf('/');
            if (slashIndex >= 0 && slashIndex < dataPost.length() - 1) {
                return POST_URL_PREFIX + dataPost.substring(slashIndex + 1);
            }
        }

        Element link = messageElement.selectFirst("a.tgme_widget_message_date");
        if (link != null && !link.attr("href").isBlank()) {
            return link.attr("href");
        }

        return "";
    }

    record ChannelPost(
            String text,
            String url
    ) {
    }

    public record NewsPage(
            int page,
            int totalPages,
            String message
    ) implements Serializable {
    }
}
