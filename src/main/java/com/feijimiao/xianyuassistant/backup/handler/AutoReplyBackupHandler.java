package com.feijimiao.xianyuassistant.backup.handler;

import com.feijimiao.xianyuassistant.backup.DataBackupHandler;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsConfig;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class AutoReplyBackupHandler implements DataBackupHandler {

    @Autowired
    private XianyuGoodsConfigMapper goodsConfigMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public String getModuleKey() {
        return "autoReply";
    }

    @Override
    public String getModuleName() {
        return "自动回复";
    }

    @Override
    public Map<String, Object> exportData() {
        List<Map<String, Object>> configs = jdbcTemplate.queryForList(
                "SELECT * FROM xianyu_goods_config WHERE xianyu_auto_reply_on IS NOT NULL");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("autoReplyConfigs", configs);
        return data;
    }

    @Override
    public void importData(Map<String, Object> data, Map<String, Object> context) {
        if (data == null) return;

        @SuppressWarnings("unchecked")
        Map<Long, Long> accountIdMapping = context.get("accountIdMapping") != null
                ? (Map<Long, Long>) context.get("accountIdMapping")
                : Collections.emptyMap();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> configMaps = (List<Map<String, Object>>) data.get("autoReplyConfigs");
        if (configMaps == null) return;

        for (Map<String, Object> map : configMaps) {
            try {
                String xyGoodsId = (String) map.get("xy_goods_id");
                Object accountIdObj = map.get("xianyu_account_id");
                Long xianyuAccountId = accountIdObj != null ? ((Number) accountIdObj).longValue() : null;

                if (xyGoodsId == null || xianyuAccountId == null) continue;

                if (accountIdMapping.containsKey(xianyuAccountId)) {
                    xianyuAccountId = accountIdMapping.get(xianyuAccountId);
                }

                Integer autoReplyOn = map.get("xianyu_auto_reply_on") != null ? ((Number) map.get("xianyu_auto_reply_on")).intValue() : null;
                Integer autoReplyContextOn = map.get("xianyu_auto_reply_context_on") != null ? ((Number) map.get("xianyu_auto_reply_context_on")).intValue() : null;
                String fixedMaterial = (String) map.get("fixed_material");

                List<Map<String, Object>> existing = jdbcTemplate.queryForList(
                        "SELECT * FROM xianyu_goods_config WHERE xianyu_account_id = ? AND xy_goods_id = ?",
                        xianyuAccountId, xyGoodsId);

                if (existing.isEmpty()) {
                    jdbcTemplate.update(
                            "INSERT INTO xianyu_goods_config (xianyu_account_id, xy_goods_id, xianyu_auto_reply_on, xianyu_auto_reply_context_on, fixed_material) VALUES (?, ?, ?, ?, ?)",
                            xianyuAccountId, xyGoodsId, autoReplyOn, autoReplyContextOn, fixedMaterial);
                } else {
                    jdbcTemplate.update(
                            "UPDATE xianyu_goods_config SET xianyu_auto_reply_on = ?, xianyu_auto_reply_context_on = ?, fixed_material = ? WHERE xianyu_account_id = ? AND xy_goods_id = ?",
                            autoReplyOn, autoReplyContextOn, fixedMaterial, xianyuAccountId, xyGoodsId);
                }
            } catch (Exception e) {
                log.warn("[AutoReplyBackup] 导入单条自动回复配置失败: {}", e.getMessage());
            }
        }
    }
}
