package com.feijimiao.xianyuassistant.service.delivery;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 文本发货策略（deliveryMode=1）
 */
@Slf4j
@Component
public class TextDeliveryStrategy implements DeliveryContentStrategy {

    @Override
    public boolean supports(int deliveryMode) {
        return deliveryMode == 1;
    }

    @Override
    public String resolve(DeliveryContext context) {
        String content = buildContent(context);
        if (content == null || content.isEmpty()) {
            log.warn("【账号{}】文本发货模式下未配置发货内容: xyGoodsId={}", context.getAccountId(), context.getXyGoodsId());
            return null;
        }
        log.info("【账号{}】文本发货模式", context.getAccountId());
        return content;
    }

    private String buildContent(DeliveryContext context) {
        if (context.getDeliveryConfig() == null) {
            return null;
        }

        String link = trimToEmpty(context.getDeliveryConfig().getAutoDeliveryLink());
        String note = trimToEmpty(context.getDeliveryConfig().getAutoDeliveryNote());
        String legacy = trimToEmpty(context.getDeliveryConfig().getAutoDeliveryContent());

        if (note.isEmpty() && !legacy.isEmpty()) {
            note = legacy;
        }

        if (!link.isEmpty() && !note.isEmpty()) {
            return link + "\n" + note;
        }
        if (!link.isEmpty()) {
            return link;
        }
        return note;
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
