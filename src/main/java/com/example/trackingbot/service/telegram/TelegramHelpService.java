package com.example.trackingbot.service.telegram;

import org.springframework.stereotype.Service;

@Service
public class TelegramHelpService {

    public String getMainHelpMessage() {
        return """
                Xin chao! Minh la bot theo doi thi truong.

                Lenh hien co:
                /start - xem huong dan
                /help - xem lai danh sach lenh
                /crypto BTC - xem gia crypto
                /idea BTC - tao chart idea bang Lightweight Charts
                /chart_volume BTC - xem chart Volume Delta
                /chart_breakout BTC - xem chart Breakout Confirmation
                /chart_trendline BTC - xem chart Trendline theo pivot
                /chart_orderflow BTC - xem chart Order Flow
                /ai BTC - AI quant market analysis bang GPT-5 mini
                /ai_chart BTC - ve AI Quant Map
                /signal BTC - tinh Signal Score tong hop technical + order flow
                /val 1 BTC - tinh value theo USDT va VND
                /notif BTC 100000 - nhac khi coin cham gia
                /usdt - xem gia USDT/USD theo VND P2P
                /trending - top 10 crypto dang trending
                /tintuc - xem tin moi tu @vncointele
                /daily_on - bat Daily Market Summary moi sang
                /daily_off - tat Daily Market Summary
                /buy BTC 0.1 65000 - luu lenh mua va tinh P/L
                /buy BTC 65000 - theo doi entry mua khong can so luong
                /sell BTC 61600 - theo doi entry ban
                /myportfolio - xem loi/lo portfolio
                /crypto_chart BTC 7d - xem bieu do crypto
                /watch BTC - them vao watchlist
                /unwatch BTC - xoa khoi watchlist
                /mywatchlist - xem watchlist
                /watch_updates_on - bat cap nhat watchlist
                /watch_updates_off - tat cap nhat watchlist
                /alert BTC > 70000 - tao canh bao gia
                /alert_builder - tao alert bang nut bam
                /myalerts - xem alert
                /delete_alert ALERT_ID - xoa alert
                /my_notifications - xem notification da gui gan day
                Tu nhien:
                gia btc
                ve chart eth 7 ngay
                mua btc gia 65000
                nhac toi khi sol vuot 200
                /admin_health - owner xem trang thai DB/Redis/CircuitBreaker
                /admin_metrics - owner xem metric users/alerts/subscribers
                /admin_top_commands - owner xem command duoc dung nhieu nhat
                /admin_errors - owner xem command loi gan day
                /admin_users - owner xem user activity
                /admin_set_plan CHAT_ID PRO - owner doi plan cho user
                /my_usage - xem plan va AI quota hom nay

                Sap toi minh se them:
                /stock VNM
                """;
    }

    public String getUnknownCommandMessage() {
        return """
                Minh chua hieu lenh nay.

                Thu:
                /start
                /help
                /crypto BTC
                /idea BTC
                /chart_volume BTC
                /chart_breakout BTC
                /chart_trendline BTC
                /chart_orderflow BTC
                /ai BTC
                /ai_chart BTC
                /signal BTC
                /val 1 BTC
                /notif BTC 100000
                /usdt
                /trending
                /tintuc
                /daily_on
                /daily_off
                /buy BTC 0.1 65000
                /sell BTC 61600
                /myportfolio
                /crypto_chart BTC 7d
                /watch BTC
                /mywatchlist
                /watch_updates_off
                /alert BTC > 70000
                /alert_builder
                /myalerts
                /admin_health
                /admin_metrics
                /admin_top_commands
                /admin_errors
                /admin_users
                /admin_set_plan 123456789 PRO
                /my_usage
                /my_notifications
                """;
    }
}
