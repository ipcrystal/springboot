package org.iproute.springboot.design.tdengine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.iproute.springboot.design.tdengine.entity.Demo;
import org.springframework.stereotype.Repository;

/**
 * DemoMapper
 *
 * @author zhuzhenjie
 * @since 2022/5/30
 */
@Mapper
@Repository
public interface DemoMapper extends BaseMapper<Demo> {
}
