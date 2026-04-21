package com.xl.can.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xl.can.entity.ShopMetricDaily;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface ShopMetricDailyMapper extends BaseMapper<ShopMetricDaily> {

    @Select("SELECT * FROM shop_metric_daily WHERE shop_id = #{shopId} AND stat_date BETWEEN #{startDate} AND #{endDate} ORDER BY stat_date")
    List<ShopMetricDaily> findByShopIdAndDateRange(@Param("shopId") Long shopId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Select("<script>SELECT * FROM shop_metric_daily WHERE shop_id IN <foreach collection='shopIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> AND stat_date BETWEEN #{startDate} AND #{endDate} ORDER BY stat_date</script>")
    List<ShopMetricDaily> findByShopIdsAndDateRange(@Param("shopIds") List<Long> shopIds, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
