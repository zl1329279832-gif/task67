package com.dao;

import com.entity.HuiyishiyuyueEntity;
import com.baomidou.mybatisplus.mapper.BaseMapper;
import java.util.List;
import java.util.Map;
import java.util.Date;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.baomidou.mybatisplus.plugins.pagination.Pagination;

import org.apache.ibatis.annotations.Param;
import com.entity.vo.HuiyishiyuyueVO;
import com.entity.view.HuiyishiyuyueView;


/**
 * 会议室预约
 *
 * @author
 * @email
 * @date 2022-04-21 23:05:48
 */
public interface HuiyishiyuyueDao extends BaseMapper<HuiyishiyuyueEntity> {

	List<HuiyishiyuyueVO> selectListVO(@Param("ew") Wrapper<HuiyishiyuyueEntity> wrapper);

	HuiyishiyuyueVO selectVO(@Param("ew") Wrapper<HuiyishiyuyueEntity> wrapper);

	List<HuiyishiyuyueView> selectListView(@Param("ew") Wrapper<HuiyishiyuyueEntity> wrapper);

	List<HuiyishiyuyueView> selectListView(Pagination page,@Param("ew") Wrapper<HuiyishiyuyueEntity> wrapper);

	HuiyishiyuyueView selectView(@Param("ew") Wrapper<HuiyishiyuyueEntity> wrapper);

	/**
	 * 检查同一会议室在指定时间段内是否存在冲突预约（待审核或已通过）
	 */
	int selectTimeConflictCount(@Param("huiyishibianhao") String huiyishibianhao,
								@Param("kaishishijian") Date kaishishijian,
								@Param("jieshushijian") Date jieshushijian,
								@Param("excludeId") Long excludeId);

	/**
	 * 检查同一设备在指定时间段内是否已被其他预约占用（待审核或已通过）
	 */
	int selectShebeiConflictCount(@Param("shebeibianhao") String shebeibianhao,
								  @Param("kaishishijian") Date kaishishijian,
								  @Param("jieshushijian") Date jieshushijian,
								  @Param("excludeId") Long excludeId);

}
