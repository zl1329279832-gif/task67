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
	 * 检查会议室时间冲突
	 * @param huiyishiyuyue 预约实体
	 * @param excludeId 排除的预约ID（更新时使用）
	 */
   	void checkTimeConflict(HuiyishiyuyueEntity huiyishiyuyue, Long excludeId);

   	/**
	 * 检查设备时间冲突
	 * @param huiyishiyuyue 预约实体
	 * @param excludeId 排除的预约ID
	 */
   	void checkEquipmentConflict(HuiyishiyuyueEntity huiyishiyuyue, Long excludeId);

   	/**
	 * 验证用户部门权限
	 * @param huiyishiyuyue 预约实体
	 * @param userBumen 用户所属部门
	 */
   	void validateDepartment(HuiyishiyuyueEntity huiyishiyuyue, String userBumen);


}

