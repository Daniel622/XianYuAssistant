package com.feijimiao.xianyuassistant.backup.handler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.feijimiao.xianyuassistant.backup.DataBackupHandler;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsAutoDeliveryConfig;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsAutoDeliveryConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class AutoDeliveryBackupHandler implements DataBackupHandler {

    @Autowired
    private XianyuGoodsAutoDeliveryConfigMapper autoDeliveryConfigMapper;

    @Override
    public String getModuleKey() {
        return "autoDelivery";
    }

    @Override
    public String getModuleName() {
        return "自动发货";
    }

    @Override
    public Map<String, Object> exportData() {
        List<XianyuGoodsAutoDeliveryConfig> configs = autoDeliveryConfigMapper.selectList(null);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("autoDeliveryConfigs", configs);
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
        List<Map<String, Object>> configMaps = (List<Map<String, Object>>) data.get("autoDeliveryConfigs");
        if (configMaps == null) return;

        for (Map<String, Object> map : configMaps) {
            try {
                XianyuGoodsAutoDeliveryConfig config = mapToConfig(map);
                if (config == null) continue;

                if (config.getXianyuAccountId() != null && accountIdMapping.containsKey(config.getXianyuAccountId())) {
                    config.setXianyuAccountId(accountIdMapping.get(config.getXianyuAccountId()));
                }

                LambdaQueryWrapper<XianyuGoodsAutoDeliveryConfig> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(XianyuGoodsAutoDeliveryConfig::getXianyuAccountId, config.getXianyuAccountId())
                       .eq(XianyuGoodsAutoDeliveryConfig::getXyGoodsId, config.getXyGoodsId());
                XianyuGoodsAutoDeliveryConfig existing = autoDeliveryConfigMapper.selectOne(wrapper);
                if (existing == null) {
                    config.setId(null);
                    autoDeliveryConfigMapper.insert(config);
                } else {
                    config.setId(existing.getId());
                    autoDeliveryConfigMapper.updateById(config);
                }
            } catch (Exception e) {
                log.warn("[AutoDeliveryBackup] 导入单条自动发货配置失败: {}", e.getMessage());
            }
        }
    }

    private XianyuGoodsAutoDeliveryConfig mapToConfig(Map<String, Object> map) {
        try {
            XianyuGoodsAutoDeliveryConfig c = new XianyuGoodsAutoDeliveryConfig();
            c.setXianyuAccountId(map.get("xianyuAccountId") != null ? ((Number) map.get("xianyuAccountId")).longValue() : null);
            c.setXyGoodsId((String) map.get("xyGoodsId"));
            c.setDeliveryMode(map.get("deliveryMode") != null ? ((Number) map.get("deliveryMode")).intValue() : null);
            c.setAutoDeliveryContent((String) map.get("autoDeliveryContent"));
            c.setKamiConfigIds((String) map.get("kamiConfigIds"));
            c.setKamiDeliveryTemplate((String) map.get("kamiDeliveryTemplate"));
            c.setAutoDeliveryImageUrl((String) map.get("autoDeliveryImageUrl"));
            c.setAutoConfirmShipment(map.get("autoConfirmShipment") != null ? ((Number) map.get("autoConfirmShipment")).intValue() : null);
            c.setRagDelaySeconds(map.get("ragDelaySeconds") != null ? ((Number) map.get("ragDelaySeconds")).intValue() : null);
            return c;
        } catch (Exception e) {
            log.warn("解析自动发货配置数据失败", e);
            return null;
        }
    }
}
