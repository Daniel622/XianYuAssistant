package com.feijimiao.xianyuassistant.service.reply;

import com.feijimiao.xianyuassistant.entity.bo.KeywordReplyRuleBO;
import com.feijimiao.xianyuassistant.event.chatMessageEvent.ChatMessageData;

import java.util.ArrayList;
import java.util.List;

public interface ReplyStrategy {

    ReplyResult execute(List<ChatMessageData> messageList);

    @lombok.Data
    class ReplyResult {
        private boolean success;
        private String textContent;
        private String imageUrl;
        private int replyType;
        private String matchedKeyword;
        private KeywordReplyRuleBO matchedRule;
        private List<KeywordReplyRuleBO> matchedRules = new ArrayList<>();

        public static ReplyResult text(String text, int replyType) {
            ReplyResult r = new ReplyResult();
            r.setSuccess(true);
            r.setTextContent(text);
            r.setReplyType(replyType);
            return r;
        }

        public static ReplyResult image(String imageUrl, int replyType) {
            ReplyResult r = new ReplyResult();
            r.setSuccess(true);
            r.setImageUrl(imageUrl);
            r.setReplyType(replyType);
            return r;
        }

        public static ReplyResult textAndImage(String text, String imageUrl, int replyType) {
            ReplyResult r = new ReplyResult();
            r.setSuccess(true);
            r.setTextContent(text);
            r.setImageUrl(imageUrl);
            r.setReplyType(replyType);
            return r;
        }

        public static ReplyResult fail() {
            ReplyResult r = new ReplyResult();
            r.setSuccess(false);
            return r;
        }
    }
}
