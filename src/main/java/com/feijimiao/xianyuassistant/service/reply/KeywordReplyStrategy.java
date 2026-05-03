package com.feijimiao.xianyuassistant.service.reply;

import com.feijimiao.xianyuassistant.entity.bo.KeywordReplyRuleBO;
import com.feijimiao.xianyuassistant.event.chatMessageEvent.ChatMessageData;
import com.feijimiao.xianyuassistant.service.KeywordReplyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class KeywordReplyStrategy implements ReplyStrategy {

    private static final int REPLY_TYPE_KEYWORD = 1;

    @Autowired
    private KeywordReplyService keywordReplyService;

    private final Random random = new Random();

    @Override
    public ReplyResult execute(List<ChatMessageData> messageList) {
        ChatMessageData lastMessage = messageList.get(messageList.size() - 1);
        Long accountId = lastMessage.getXianyuAccountId();
        String xyGoodsId = lastMessage.getXyGoodsId();

        String buyerMessage = messageList.stream()
                .map(ChatMessageData::getMsgContent)
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");

        List<KeywordReplyRuleBO> matchedRules = keywordReplyService.matchKeyword(accountId, xyGoodsId, buyerMessage);
        if (matchedRules == null || matchedRules.isEmpty()) {
            return ReplyResult.fail();
        }

        List<KeywordReplyRuleBO.KeywordReplyContentBO> allContents = matchedRules.stream()
                .filter(r -> r.getContents() != null)
                .flatMap(r -> r.getContents().stream())
                .collect(Collectors.toList());

        if (allContents.isEmpty()) {
            return ReplyResult.fail();
        }

        KeywordReplyRuleBO.KeywordReplyContentBO selected = allContents.get(random.nextInt(allContents.size()));

        ReplyResult result;
        String text = selected.getReplyText();
        String image = selected.getReplyImageUrl();
        boolean hasText = text != null && !text.trim().isEmpty();
        boolean hasImage = image != null && !image.trim().isEmpty();

        if (hasText && hasImage) {
            result = ReplyResult.textAndImage(text, image, REPLY_TYPE_KEYWORD);
        } else if (hasText) {
            result = ReplyResult.text(text, REPLY_TYPE_KEYWORD);
        } else if (hasImage) {
            result = ReplyResult.image(image, REPLY_TYPE_KEYWORD);
        } else {
            return ReplyResult.fail();
        }

        String keywords = matchedRules.stream()
                .filter(r -> r.getIsFallback() == null || r.getIsFallback() == 0)
                .map(KeywordReplyRuleBO::getKeyword)
                .collect(Collectors.joining(", "));
        result.setMatchedKeyword(keywords);
        result.setMatchedRules(matchedRules);
        if (!matchedRules.isEmpty()) {
            result.setMatchedRule(matchedRules.get(0));
        }
        return result;
    }
}
