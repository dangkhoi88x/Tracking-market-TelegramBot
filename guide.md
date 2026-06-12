# Tracking Market Telegram Bot - Presentation Guide

File này dùng để bạn thuyết trình project. Nội dung tập trung vào 3 ý chính:

1. Project giải quyết bài toán gì.
2. Flow code chạy như thế nào.
3. Những kỹ thuật/kỹ năng backend đã dùng để hoàn thành project.

> Cách dùng: đọc file này như một kịch bản. Khi thuyết trình, không cần nói hết từng dòng, chỉ cần nắm flow và chọn phần phù hợp với thời gian.

## 1. Giới Thiệu Project

Tracking Market Telegram Bot là một backend application viết bằng Spring Boot, dùng để theo dõi thị trường crypto thông qua Telegram.

User có thể nhập các command như:

```text
/crypto BTC
/signal BTC
/ai BTC
/watch BTC
/alert BTC > 70000
/tintuc
```

Bot sẽ trả về:

- Giá hiện tại.
- Phần trăm tăng giảm 24h.
- Volume, high, low.
- Biểu đồ.
- Watchlist cá nhân.
- Alert giá.
- Portfolio lãi/lỗ.
- Signal score.
- AI market analysis.
- Tin tức từ Telegram channel.

Câu giới thiệu ngắn khi thuyết trình:

```text
Project của em là một Telegram bot theo dõi thị trường crypto. Em dùng Spring Boot
làm backend, Telegram webhook để nhận tin nhắn realtime, PostgreSQL để lưu dữ liệu
user, Redis để cache dữ liệu giá, scheduler để tự check alert và gửi summary, đồng
thời tích hợp Binance, CoinGecko, OpenAI và chart renderer bằng Node.js/Playwright.
```

## 2. Vì Sao Chọn Telegram Bot?

Telegram bot phù hợp với project này vì:

- User không cần cài app riêng.
- Giao diện command đơn giản.
- Có thể gửi text, ảnh, inline button.
- Phù hợp các tác vụ realtime như price alert.
- Dễ học webhook, API integration và backend automation.

Ví dụ user chỉ cần gõ:

```text
/crypto BTC
```

Bot có thể trả ngay:

```text
BTC
Price 61,927
24h +0.83% Vol 1.09B
High 62,446
Low 60,755
```

## 3. High-Level Flow

Flow tổng quan của project:

```text
Telegram User
-> Telegram Bot API
-> Spring Boot Webhook
-> TelegramCommandService
-> Business Service
-> External API / Database / Cache
-> TelegramMessageService
-> Telegram User
```

Diễn giải dễ hiểu:

1. User gửi command cho bot trên Telegram.
2. Telegram gửi request đến backend qua webhook.
3. Spring Boot nhận request ở controller.
4. Command service đọc command user nhập.
5. App gọi service tương ứng.
6. Service lấy dữ liệu từ API ngoài, database hoặc cache.
7. App format kết quả.
8. Bot gửi message hoặc ảnh về Telegram.

## 4. Flow Code Khi User Gọi `/crypto BTC`

Đây là flow cơ bản nhất để thuyết trình vì nó dễ hiểu và thể hiện đủ backend layer.

```text
User nhập /crypto BTC
-> TelegramWebhookController
-> TelegramCommandService
-> CryptoPriceService
-> CoinGeckoClient
-> Redis Cache
-> TelegramMessageService
-> Bot trả kết quả
```

Chi tiết từng bước:

1. `TelegramWebhookController` nhận update từ Telegram.
2. Controller lấy `chatId` và text message.
3. Gọi `TelegramCommandService`.
4. `TelegramCommandService` kiểm tra rate limit của user.
5. Nếu command là `/crypto`, service lấy symbol là `BTC`.
6. Gọi `CryptoPriceService` để lấy giá.
7. `CryptoPriceService` gọi `CoinGeckoClient`.
8. Nếu dữ liệu có trong Redis cache, app dùng cache để giảm gọi API ngoài.
9. Bot format message giá.
10. `TelegramMessageService` gọi Telegram Bot API để gửi message.

Câu thuyết trình mẫu:

```text
Với command /crypto BTC, controller chỉ nhận webhook và chuyển dữ liệu vào
TelegramCommandService. CommandService đóng vai trò router, nó parse command rồi gọi
CryptoPriceService. Service này lấy giá từ CoinGecko thông qua client layer, kết quả
được cache bằng Redis để tránh gọi API lặp lại. Cuối cùng TelegramMessageService gửi
message về cho user.
```

## 5. Flow Code Khi User Tạo Alert

Ví dụ:

```text
/alert BTC > 70000
```

Flow tạo alert:

```text
TelegramCommandService
-> AlertService
-> TelegramUserService
-> PriceAlertRepository
-> PostgreSQL
```

Chi tiết:

1. User gửi `/alert BTC > 70000`.
2. Command service parse symbol, operator và target price.
3. `AlertService` validate dữ liệu.
4. Nếu user chưa tồn tại, `TelegramUserService` tạo user trong database.
5. Alert được lưu vào bảng `price_alerts`.
6. Bot trả message xác nhận.

Sau đó background job tự kiểm tra:

```text
AlertCheckerScheduler
-> AlertService.getActiveAlerts()
-> CryptoPriceService.getPrice()
-> so sánh giá hiện tại với điều kiện
-> TelegramAsyncService gửi notification
-> AlertService deactivate alert
```

Câu thuyết trình mẫu:

```text
Khi user tạo alert, em lưu điều kiện vào PostgreSQL thay vì lưu memory, vì alert phải
còn tồn tại sau khi app restart. Sau đó một scheduled job chạy mỗi phút để lấy các
alert active, gọi giá hiện tại, so sánh điều kiện và gửi notification nếu đạt target.
```

## 6. Flow Watchlist

Các command:

```text
/watch BTC
/unwatch BTC
/mywatchlist
/watch_updates_on
/watch_updates_off
```

Flow thêm watchlist:

```text
TelegramCommandService
-> WatchlistService
-> WatchlistItemRepository
-> PostgreSQL
```

Flow auto update:

```text
WatchlistUpdateScheduler
-> WatchlistService.getAllWatchlists()
-> CryptoPriceService
-> TelegramAsyncService
-> TelegramMessageService
```

Ý nghĩa:

- Watchlist là dữ liệu cá nhân nên lưu PostgreSQL.
- Auto update chạy bằng scheduler.
- Gửi message dùng async để không làm nghẽn scheduler.

Câu thuyết trình mẫu:

```text
Watchlist được lưu theo từng Telegram user trong PostgreSQL. User có thể bật hoặc tắt
auto update. Nếu bật, scheduler sẽ chạy định kỳ, lấy danh sách watchlist, gọi giá mới
và gửi cập nhật qua TelegramAsyncService.
```

## 7. Flow Portfolio

Các command:

```text
/buy BTC 0.1 65000
/buy BTC 65000
/sell BTC 61600
/myportfolio
```

Project hỗ trợ 2 kiểu:

- Có số lượng coin: tính P/L bằng USD.
- Không có số lượng coin: tính phần trăm chênh lệch entry so với giá hiện tại.

Flow:

```text
TelegramCommandService
-> PortfolioService
-> PortfolioPositionRepository
-> PostgreSQL
-> CryptoPriceService
-> format P/L
```

Câu thuyết trình mẫu:

```text
Portfolio giúp user theo dõi entry mua hoặc bán. Em lưu position vào PostgreSQL.
Khi user gọi /myportfolio, app lấy danh sách position, gọi giá hiện tại rồi tính lãi/lỗ
dựa trên entry price và side là buy hoặc sell.
```

## 8. Flow AI Quant Analysis

Command:

```text
/ai BTC
```

Nguyên tắc thiết kế:

```text
AI không tự đoán giá từ không khí.
AI chỉ tổng hợp dữ liệu kỹ thuật đã được backend tính trước.
```

Flow:

```text
TelegramCommandService
-> AiPredictionService
-> TechnicalAnalysisService
-> OrderFlowService
-> BinanceFuturesClient
-> OpenAiClient
-> TelegramMessageService
```

Dữ liệu đưa cho AI:

- Price.
- EMA20.
- EMA50.
- RSI14.
- Volume delta.
- Support.
- Resistance.
- Breakout status.
- Trendline status.
- Order flow.
- Funding rate.
- Open interest.

AI trả về:

- Bias: Bullish / Bearish / Neutral.
- Confidence.
- Market context.
- Quant evidence.
- Bullish scenario.
- Bearish scenario.
- Invalidation.
- Risk management.

Câu thuyết trình mẫu:

```text
Ở phần AI, em không để AI tự dự đoán giá. Backend sẽ tính trước các chỉ báo như EMA,
RSI, volume delta, breakout, trendline và order flow. Sau đó em gửi dữ liệu có cấu trúc
này sang OpenAI để AI tổng hợp thành một bản quant market analysis có bias, confidence,
scenario và invalidation.
```

## 9. Flow Chart Renderer

Các command:

```text
/idea BTC
/chart_volume BTC
/chart_breakout BTC
/chart_trendline BTC
/chart_orderflow BTC
/ai_chart BTC
```

Flow:

```text
Spring Boot
-> chuẩn bị dữ liệu chart
-> gọi Node.js script
-> Lightweight Charts render chart
-> Playwright chụp screenshot
-> Spring Boot gửi ảnh về Telegram
```

Vì sao dùng Node.js/Playwright:

- Lightweight Charts chạy tốt trong browser.
- Playwright có thể tự mở browser và chụp ảnh.
- Java tập trung vào backend logic.
- Node.js tập trung vào rendering chart.

Câu thuyết trình mẫu:

```text
Phần chart em tách ra dùng Node.js, Lightweight Charts và Playwright. Spring Boot chuẩn
bị dữ liệu nến và phân tích, sau đó gọi script Node để render chart trong browser headless.
Playwright chụp screenshot và Spring Boot gửi ảnh đó về Telegram.
```

## 10. Flow Tin Tức `/tintuc`

Command:

```text
/tintuc
```

Flow:

```text
TelegramCommandService
-> TelegramChannelNewsService
-> gọi trang public t.me/s/vncointele
-> Jsoup parse HTML
-> format 5 tin/trang
-> Redis cache
-> inline button Before/Next
```

Điểm kỹ thuật:

- Dùng Jsoup để parse HTML.
- Dùng Redis cache để tránh gọi page liên tục.
- Chia pagination 5 tin/trang, tối đa 25 tin.
- Inline keyboard giúp user bấm Next/Before.

Câu thuyết trình mẫu:

```text
Với /tintuc, em lấy dữ liệu từ trang public của Telegram channel. Service gọi HTML,
parse bằng Jsoup, lấy nội dung post mới, format thành nhiều trang và gửi kèm inline
button Before/Next. Kết quả được cache để tránh request liên tục.
```

## 11. Database Skill: PostgreSQL, JPA, Flyway

Project dùng PostgreSQL cho dữ liệu cần lưu lâu dài.

Các loại dữ liệu:

- User.
- Watchlist.
- Alert.
- Portfolio.
- Daily settings.

Kỹ thuật đã dùng:

- JPA Entity để map table.
- Repository để query database.
- Flyway migration để quản lý schema.
- `ddl-auto: validate` để tránh Hibernate tự sửa database.

Câu thuyết trình mẫu:

```text
Em dùng PostgreSQL vì các dữ liệu như alert, portfolio và watchlist cần tồn tại sau khi
restart app. Em dùng Spring Data JPA để thao tác database qua repository và Flyway để
quản lý version schema. Hibernate chỉ validate schema, không tự update database.
```

## 12. Cache Skill: Redis

Redis dùng cho dữ liệu ngắn hạn.

Ví dụ:

- Cache giá crypto.
- Cache tin tức.
- Cache dữ liệu có thể gọi lại nhiều lần.

Lợi ích:

- Bot trả nhanh hơn.
- Giảm số lần gọi API ngoài.
- Giảm nguy cơ bị rate limit.

Câu thuyết trình mẫu:

```text
Em dùng Redis cache cho dữ liệu thị trường vì user có thể gọi cùng một symbol nhiều lần
trong thời gian ngắn. Cache 60 giây giúp bot nhanh hơn và giảm request tới API ngoài.
```

## 13. Resilience Skill: Retry, Circuit Breaker, Rate Limiter

Project phụ thuộc nhiều API ngoài:

- CoinGecko.
- Binance.
- OpenAI.
- Telegram Bot API.

Nếu API ngoài lỗi, bot cần xử lý ổn định.

Kỹ thuật dùng:

### Retry

Thử lại khi lỗi tạm thời như network timeout.

```text
API lỗi tạm thời
-> chờ một chút
-> thử lại
-> nếu vẫn lỗi thì trả fallback/error message
```

### Circuit Breaker

Ngắt tạm thời nếu API lỗi liên tục.

```text
CLOSED -> OPEN -> HALF_OPEN -> CLOSED
```

Ý nghĩa:

- Tránh gọi liên tục vào API đang lỗi.
- Bot fail nhanh hơn.
- Hệ thống ổn định hơn.

### Rate Limiter

Giới hạn request ra API ngoài.

Ý nghĩa:

- Tránh bị API ngoài chặn.
- Bảo vệ chi phí OpenAI.
- Kiểm soát traffic.

Câu thuyết trình mẫu:

```text
Vì bot gọi nhiều API ngoài nên em dùng Resilience4j. Retry xử lý lỗi tạm thời, Circuit
Breaker ngắt khi API lỗi liên tục, còn Rate Limiter giới hạn số request để tránh bị
rate limit hoặc tốn chi phí OpenAI.
```

## 14. User Rate Limit Skill

Ngoài Resilience4j cho API ngoài, project còn có rate limit theo từng user.

Ví dụ:

```text
Mỗi user tối đa 10 command/phút
/ai tối đa 3 lần/phút
/ai_chart tối đa 2 lần/phút
```

Mục tiêu:

- Chống spam.
- Bảo vệ server.
- Bảo vệ chi phí OpenAI.

Câu thuyết trình mẫu:

```text
Em tách rate limit thành hai lớp. Resilience4j bảo vệ khi gọi API ngoài, còn
UserCommandRateLimiter bảo vệ bot khỏi user spam command. Đặc biệt các command dùng
OpenAI được giới hạn thấp hơn vì có chi phí.
```

## 15. Scheduler Và Async Skill

Scheduler dùng cho việc chạy tự động.

Các job:

- Alert checker mỗi 1 phút.
- Watchlist update mỗi 5 phút.
- Daily summary mỗi sáng.

Async dùng cho gửi Telegram message không đồng bộ.

Vì sao cần async:

- Gửi message có thể chậm.
- Một job có thể gửi cho nhiều user.
- Không nên để scheduler bị nghẽn vì Telegram API.

Câu thuyết trình mẫu:

```text
Các tác vụ như alert và daily summary không phụ thuộc vào việc user gọi command, nên em
dùng scheduler để chạy tự động. Khi cần gửi nhiều message, em dùng @Async và thread pool
riêng để việc gửi Telegram không làm nghẽn scheduler.
```

## 16. Observability Skill

Project có admin command:

```text
/admin_health
/admin_metrics
```

Thông tin kiểm tra:

- DB status.
- Redis status.
- Circuit Breaker state.
- Active alerts.
- Daily subscribers.
- Thread pool metrics.

Câu thuyết trình mẫu:

```text
Em thêm admin commands để quan sát trạng thái hệ thống ngay trong Telegram. Ví dụ em có
thể kiểm tra DB, Redis, Circuit Breaker và thread pool metrics. Điều này giúp debug nhanh
hơn khi bot chạy thật.
```

## 17. Testing Skill

Project có unit test cho logic quan trọng:

- Alert.
- Watchlist.
- Portfolio.
- Signal score.
- User command rate limiter.
- News parser.

Lệnh test:

```bash
./mvnw test
```

Câu thuyết trình mẫu:

```text
Em viết unit test cho các service có logic quan trọng. Ví dụ alert service cần test điều
kiện giá, portfolio cần test tính P/L, rate limiter cần test chặn spam, còn news parser
cần test parse HTML đúng. Các dependency bên ngoài được mock để test không phụ thuộc API thật.
```

## 18. Clean Code Và Refactor Skill

Project đã được refactor package rõ ràng:

```text
entity   -> JPA entity
dto      -> request/response object
model    -> object nghiệp vụ nội bộ
service  -> business logic
client   -> external API call
```

Ví dụ:

- `PriceAlertEntity`: lưu database.
- `CryptoPrice`: dữ liệu response giá.
- `SignalScore`: model nội bộ do bot tự tính.

Câu thuyết trình mẫu:

```text
Em tách rõ entity, DTO và model để code dễ hiểu hơn. Entity chỉ dùng cho database, DTO
dùng cho request/response, còn model dùng cho dữ liệu nghiệp vụ nội bộ như SignalScore
hoặc TechnicalAnalysis.
```

## 19. Docker Skill

Project dùng Docker Compose để chạy local infrastructure.

Services:

- PostgreSQL.
- Redis.

Command:

```bash
docker compose up -d
```

Câu thuyết trình mẫu:

```text
Em dùng Docker Compose để chạy PostgreSQL và Redis ở local. Như vậy môi trường dev dễ
tái tạo, không cần cài thủ công từng service vào máy.
```

## 20. Những Kỹ Năng Có Thể Ghi Vào CV

Backend:

- Java 21.
- Spring Boot 4.
- REST API integration.
- Telegram Bot API webhook.
- Spring Data JPA.
- PostgreSQL.
- Flyway migration.
- Redis cache.
- Scheduler.
- Async processing.
- Thread pool configuration.
- Resilience4j.
- Unit testing.

External API:

- CoinGecko API.
- Binance Futures API.
- Binance P2P API.
- OpenAI API.
- Telegram Bot API.

Infra/tooling:

- Docker Compose.
- Ngrok.
- Maven.
- Node.js.
- Playwright.
- Lightweight Charts.

Observability:

- Spring Actuator.
- Admin health command.
- Circuit Breaker metrics.
- Thread pool metrics.

## 21. Kịch Bản Thuyết Trình 3 Phút

Bạn có thể nói theo flow này:

```text
Project của em là Tracking Market Telegram Bot, một bot Telegram để theo dõi thị trường
crypto. User có thể xem giá, biểu đồ, watchlist, alert, portfolio, signal score và AI
analysis.

Về kiến trúc, em dùng Spring Boot làm backend. Telegram gửi message vào app thông qua
webhook. Controller nhận request, sau đó chuyển sang TelegramCommandService để parse
command. Tùy command, service này gọi các service con như CryptoPriceService,
AlertService, PortfolioService hoặc AiPredictionService.

Dữ liệu user như watchlist, alert, portfolio được lưu trong PostgreSQL qua Spring Data
JPA. Schema database được quản lý bằng Flyway. Dữ liệu thị trường lấy từ API ngoài như
CoinGecko và Binance được cache bằng Redis trong thời gian ngắn để giảm request lặp.

Với các tác vụ tự động như check alert, gửi watchlist update và daily summary, em dùng
Spring Scheduler. Việc gửi message hàng loạt được xử lý async bằng thread pool riêng để
không làm nghẽn scheduler.

Vì project phụ thuộc API ngoài, em dùng Resilience4j gồm Retry, Circuit Breaker và Rate
Limiter để hệ thống ổn định hơn khi API lỗi hoặc bị giới hạn request. Ngoài ra em còn có
rate limit theo từng user để chống spam, đặc biệt với các command dùng OpenAI.

Phần AI analysis không để AI tự đoán giá. Backend sẽ tính trước EMA, RSI, volume delta,
breakout, trendline và order flow, sau đó gửi dữ liệu này sang OpenAI để tổng hợp thành
market analysis chuyên nghiệp.

Cuối cùng, project có unit test cho các service quan trọng, Docker Compose để chạy
PostgreSQL và Redis local, và admin command để kiểm tra trạng thái DB, Redis, Circuit
Breaker và thread pool.
```

## 22. Câu Hỏi Phỏng Vấn Có Thể Gặp

### Vì sao dùng webhook thay vì polling?

Webhook realtime hơn và phù hợp khi backend có public URL. Telegram chủ động gọi app khi có message mới, thay vì app phải liên tục hỏi Telegram có message hay không.

### Vì sao cần Redis nếu đã có PostgreSQL?

PostgreSQL lưu dữ liệu lâu dài. Redis cache dữ liệu ngắn hạn để tăng tốc và giảm gọi API ngoài. Hai công nghệ giải quyết hai vấn đề khác nhau.

### Vì sao cần scheduler?

Vì alert, watchlist update và daily summary phải tự chạy kể cả khi user không gửi command.

### Vì sao cần async?

Gửi Telegram message có thể chậm. Nếu gửi đồng bộ trong scheduler, một message chậm có thể làm chậm cả job. Async giúp tách việc gửi ra thread pool riêng.

### Vì sao cần Resilience4j?

Vì bot phụ thuộc API ngoài. Retry xử lý lỗi tạm thời, Circuit Breaker tránh gọi liên tục khi API chết, Rate Limiter tránh vượt quota.

### Vì sao không để AI tự dự đoán giá?

Vì AI có thể hallucinate. Cách tốt hơn là backend tính dữ liệu kỹ thuật trước, sau đó AI chỉ tổng hợp và diễn giải dựa trên dữ liệu thật.

### Nếu deploy nhiều instance thì cần cải thiện gì?

Cần chuyển user rate limit từ in-memory sang Redis, đảm bảo scheduler không chạy trùng nhiều instance, và cấu hình shared cache/database rõ ràng.

## 23. Checklist Trước Khi Demo

Trước khi thuyết trình/demo, kiểm tra:

```bash
docker compose up -d
./mvnw test
./mvnw spring-boot:run
```

Kiểm tra webhook:

```bash
curl "https://api.telegram.org/bot<token>/getWebhookInfo"
```

Các command nên demo:

```text
/start
/crypto BTC
/signal BTC
/idea BTC
/watch BTC
/mywatchlist
/alert BTC > 70000
/ai BTC
/tintuc
/admin_health
```

Nếu thời gian demo ngắn, ưu tiên:

```text
/crypto BTC
/signal BTC
/ai BTC
/tintuc
/admin_health
```
