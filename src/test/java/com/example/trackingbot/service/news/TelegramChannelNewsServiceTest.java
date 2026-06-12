package com.example.trackingbot.service.news;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TelegramChannelNewsServiceTest {

    private final TelegramChannelNewsService service = new TelegramChannelNewsService(RestClient.builder());

    @Test
    void parseLatestPosts_shouldReturnNewestPostsFirst() {
        String html = """
                <html>
                  <body>
                    <div class="tgme_widget_message" data-post="vncointele/100">
                      <div class="tgme_widget_message_text js-message_text">Tin cu<br>BTC 60000</div>
                    </div>
                    <div class="tgme_widget_message" data-post="vncointele/101">
                      <div class="tgme_widget_message_text js-message_text">Tin moi<br>ETH 3000</div>
                    </div>
                  </body>
                </html>
                """;

        List<TelegramChannelNewsService.ChannelPost> posts = service.parseLatestPosts(html, 2);

        assertThat(posts).hasSize(2);
        assertThat(posts.get(0).text()).isEqualTo("Tin moi\nETH 3000");
        assertThat(posts.get(0).url()).isEqualTo("https://t.me/vncointele/101");
        assertThat(posts.get(1).text()).isEqualTo("Tin cu\nBTC 60000");
    }

    @Test
    void parseLatestPosts_shouldIgnorePostsWithoutText() {
        String html = """
                <html>
                  <body>
                    <div class="tgme_widget_message" data-post="vncointele/100">
                      <a class="tgme_widget_message_photo_wrap"></a>
                    </div>
                    <div class="tgme_widget_message" data-post="vncointele/101">
                      <div class="tgme_widget_message_text js-message_text">Tin co text</div>
                    </div>
                  </body>
                </html>
                """;

        List<TelegramChannelNewsService.ChannelPost> posts = service.parseLatestPosts(html, 5);

        assertThat(posts).hasSize(1);
        assertThat(posts.getFirst().text()).isEqualTo("Tin co text");
    }
}
