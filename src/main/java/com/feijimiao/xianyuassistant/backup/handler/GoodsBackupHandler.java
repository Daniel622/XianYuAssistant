package com.feijimiao.xianyuassistant.backup.handler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.feijimiao.xianyuassistant.backup.DataBackupHandler;
import com.feijimiao.xianyuassistant.entity.XianyuGoodsInfo;
import com.feijimiao.xianyuassistant.mapper.XianyuGoodsInfoMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
public class GoodsBackupHandler implements DataBackupHandler {

    @Autowired
    private XianyuGoodsInfoMapper goodsInfoMapper;

    @Override
    public String getModuleKey() {
        return "goods";
    }

    @Override
    public String getModuleName() {
        return "商品管理";
    }

    @Override
    public Map<String, Object> exportData() {
        List<XianyuGoodsInfo> goodsList = goodsInfoMapper.selectList(null);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("goodsList", goodsList);
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
        List<Map<String, Object>> goodsMaps = (List<Map<String, Object>>) data.get("goodsList");
        if (goodsMaps == null) return;

        for (Map<String, Object> map : goodsMaps) {
            try {
                XianyuGoodsInfo goods = mapToGoodsInfo(map);
                if (goods == null) continue;

                if (goods.getXianyuAccountId() != null && accountIdMapping.containsKey(goods.getXianyuAccountId())) {
                    goods.setXianyuAccountId(accountIdMapping.get(goods.getXianyuAccountId()));
                }

                LambdaQueryWrapper<XianyuGoodsInfo> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(XianyuGoodsInfo::getXyGoodId, goods.getXyGoodId());
                XianyuGoodsInfo existing = goodsInfoMapper.selectOne(wrapper);
                if (existing == null) {
                    goods.setId(null);
                    goodsInfoMapper.insert(goods);
                } else {
                    goods.setId(existing.getId());
                    goodsInfoMapper.updateById(goods);
                }
            } catch (Exception e) {
                log.warn("[GoodsBackup] 导入单条商品数据失败: {}", e.getMessage());
            }
        }
    }

    private XianyuGoodsInfo mapToGoodsInfo(Map<String, Object> map) {
        try {
            XianyuGoodsInfo g = new XianyuGoodsInfo();
            g.setXyGoodId((String) map.get("xyGoodId"));
            g.setTitle((String) map.get("title"));
            g.setCoverPic((String) map.get("coverPic"));
            g.setInfoPic((String) map.get("infoPic"));
            g.setDetailInfo((String) map.get("detailInfo"));
            g.setDetailUrl((String) map.get("detailUrl"));
            g.setXianyuAccountId(map.get("xianyuAccountId") != null ? ((Number) map.get("xianyuAccountId")).longValue() : null);
            g.setSoldPrice((String) map.get("soldPrice"));
            g.setStatus(map.get("status") != null ? ((Number) map.get("status")).intValue() : null);
            g.setCreatedTime((String) map.get("createdTime"));
            g.setUpdatedTime((String) map.get("updatedTime"));
            return g;
        } catch (Exception e) {
            log.warn("解析商品数据失败", e);
            return null;
        }
    }
}
