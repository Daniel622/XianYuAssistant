package com.feijimiao.xianyuassistant.backup.handler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.feijimiao.xianyuassistant.backup.DataBackupHandler;
import com.feijimiao.xianyuassistant.entity.XianyuKamiConfig;
import com.feijimiao.xianyuassistant.entity.XianyuKamiItem;
import com.feijimiao.xianyuassistant.mapper.XianyuKamiConfigMapper;
import com.feijimiao.xianyuassistant.mapper.XianyuKamiItemMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class KamiBackupHandler implements DataBackupHandler {

    @Autowired
    private XianyuKamiConfigMapper kamiConfigMapper;

    @Autowired
    private XianyuKamiItemMapper kamiItemMapper;

    @Override
    public String getModuleKey() {
        return "kami";
    }

    @Override
    public String getModuleName() {
        return "卡密仓库";
    }

    @Override
    public Map<String, Object> exportData() {
        List<XianyuKamiConfig> kamiConfigs = kamiConfigMapper.selectList(null);
        List<XianyuKamiItem> kamiItems = kamiItemMapper.selectList(null);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("kamiConfigs", kamiConfigs);
        data.put("kamiItems", kamiItems);
        return data;
    }

    @Override
    public void importData(Map<String, Object> data, Map<String, Object> context) {
        if (data == null) return;

        @SuppressWarnings("unchecked")
        Map<Long, Long> accountIdMapping = context.get("accountIdMapping") != null
                ? (Map<Long, Long>) context.get("accountIdMapping")
                : Collections.emptyMap();

        Map<Long, Long> oldToNewConfigId = new HashMap<>();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> configMaps = (List<Map<String, Object>>) data.get("kamiConfigs");
        if (configMaps != null) {
            for (Map<String, Object> map : configMaps) {
                try {
                    XianyuKamiConfig config = mapToKamiConfig(map);
                    if (config == null) continue;

                    if (config.getXianyuAccountId() != null && accountIdMapping.containsKey(config.getXianyuAccountId())) {
                        config.setXianyuAccountId(accountIdMapping.get(config.getXianyuAccountId()));
                    }

                    Long oldId = map.get("id") != null ? ((Number) map.get("id")).longValue() : null;

                    LambdaQueryWrapper<XianyuKamiConfig> wrapper = new LambdaQueryWrapper<>();
                    wrapper.eq(XianyuKamiConfig::getAliasName, config.getAliasName())
                           .eq(XianyuKamiConfig::getXianyuAccountId, config.getXianyuAccountId());
                    XianyuKamiConfig existing = kamiConfigMapper.selectOne(wrapper);
                    if (existing == null) {
                        config.setId(null);
                        kamiConfigMapper.insert(config);
                        if (oldId != null) {
                            oldToNewConfigId.put(oldId, config.getId());
                        }
                    } else {
                        if (oldId != null) {
                            oldToNewConfigId.put(oldId, existing.getId());
                        }
                        config.setId(existing.getId());
                        kamiConfigMapper.updateById(config);
                    }
                } catch (Exception e) {
                    log.warn("[KamiBackup] 导入单条卡密配置失败: {}", e.getMessage());
                }
            }
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> itemMaps = (List<Map<String, Object>>) data.get("kamiItems");
        if (itemMaps != null) {
            for (Map<String, Object> map : itemMaps) {
                try {
                    XianyuKamiItem item = mapToKamiItem(map);
                    if (item == null) continue;

                    Long oldConfigId = map.get("kamiConfigId") != null ? ((Number) map.get("kamiConfigId")).longValue() : null;
                    if (oldConfigId != null && oldToNewConfigId.containsKey(oldConfigId)) {
                        item.setKamiConfigId(oldToNewConfigId.get(oldConfigId));
                    }

                    if (item.getKamiConfigId() == null) continue;

                    LambdaQueryWrapper<XianyuKamiItem> wrapper = new LambdaQueryWrapper<>();
                    wrapper.eq(XianyuKamiItem::getKamiConfigId, item.getKamiConfigId())
                           .eq(XianyuKamiItem::getKamiContent, item.getKamiContent());
                    XianyuKamiItem existing = kamiItemMapper.selectOne(wrapper);
                    if (existing == null) {
                        item.setId(null);
                        kamiItemMapper.insert(item);
                    }
                } catch (Exception e) {
                    log.warn("[KamiBackup] 导入单条卡密项失败: {}", e.getMessage());
                }
            }
        }
    }

    private XianyuKamiConfig mapToKamiConfig(Map<String, Object> map) {
        try {
            XianyuKamiConfig c = new XianyuKamiConfig();
            c.setXianyuAccountId(map.get("xianyuAccountId") != null ? ((Number) map.get("xianyuAccountId")).longValue() : null);
            c.setAliasName((String) map.get("aliasName"));
            c.setAlertEnabled(map.get("alertEnabled") != null ? ((Number) map.get("alertEnabled")).intValue() : null);
            c.setAlertThresholdType(map.get("alertThresholdType") != null ? ((Number) map.get("alertThresholdType")).intValue() : null);
            c.setAlertThresholdValue(map.get("alertThresholdValue") != null ? ((Number) map.get("alertThresholdValue")).intValue() : null);
            c.setAlertEmail((String) map.get("alertEmail"));
            c.setTotalCount(map.get("totalCount") != null ? ((Number) map.get("totalCount")).intValue() : null);
            c.setUsedCount(map.get("usedCount") != null ? ((Number) map.get("usedCount")).intValue() : null);
            return c;
        } catch (Exception e) {
            log.warn("解析卡密配置数据失败", e);
            return null;
        }
    }

    private XianyuKamiItem mapToKamiItem(Map<String, Object> map) {
        try {
            XianyuKamiItem i = new XianyuKamiItem();
            i.setKamiConfigId(map.get("kamiConfigId") != null ? ((Number) map.get("kamiConfigId")).longValue() : null);
            i.setKamiContent((String) map.get("kamiContent"));
            i.setStatus(map.get("status") != null ? ((Number) map.get("status")).intValue() : null);
            i.setOrderId((String) map.get("orderId"));
            i.setSortOrder(map.get("sortOrder") != null ? ((Number) map.get("sortOrder")).intValue() : null);
            return i;
        } catch (Exception e) {
            log.warn("解析卡密项数据失败", e);
            return null;
        }
    }
}
