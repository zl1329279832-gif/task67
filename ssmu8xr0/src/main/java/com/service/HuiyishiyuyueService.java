package com.service;

import com.baomidou.mybatisplus.mapper.Wrapper;
import com.baomidou.mybatisplus.service.IService;
import com.utils.PageUtils;
import com.entity.HuiyishiyuyueEntity;
import java.util.List;
import java.util.Map;
import com.entity.vo.HuiyishiyuyueVO;
import org.apache.ibatis.annotations.Param;
import com.entity.view.HuiyishiyuyueView;


/**
 * 会议室预约
 *
 * @author 
 * @email 
 * @date 2022-04-21 23:05:48
 */
public interface HuiyishiyuyueService extends IService<HuiyishiyuyueEntity> {

    PageUtils queryPage(Map<String, Object> params);

   	List<HuiyishiyuyueVO> selectListVO(Wrapper<HuiyishiyuyueEntity> wrapper);

   	HuiyishiyuyueVO selectVO(@Param("ew") Wrapper<HuiyishiyuyueEntity> wrapper);

   	List<HuiyishiyuyueView> selectListView(Wrapper<HuiyishiyuyueEntity> wrapper);

   	HuiyishiyuyueView selectView(@Param("ew") Wrapper<HuiyishiyuyueEntity> wrapper);

   	PageUtils queryPage(Map<String, Object> params,Wrapper<HuiyishiyuyueEntity> wrapper);

	/**
	 * 预约综合校验：时间冲突、设备冲突、部门权限
	 * @param yuyue 预约实体
	 * @param excludeId 排除的记录ID（更新时传当前记录ID，新增时传null）
	 * @return 校验失败返回错误信息，通过返回null
	 */
	String validateYuyue(HuiyishiyuyueEntity yuyue, Long excludeId);

	/**
	 * 审核通过：自动生成使用记录
	 */
	void approve(HuiyishiyuyueEntity yuyue);

}

