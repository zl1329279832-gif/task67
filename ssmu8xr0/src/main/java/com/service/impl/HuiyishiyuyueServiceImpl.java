package com.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.mapper.Wrapper;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.entity.EIException;
import com.utils.PageUtils;
import com.utils.Query;


import com.dao.HuiyishiyuyueDao;
import com.entity.HuiyishiyuyueEntity;
import com.service.HuiyishiyuyueService;
import com.entity.vo.HuiyishiyuyueVO;
import com.entity.view.HuiyishiyuyueView;

@Service("huiyishiyuyueService")
public class HuiyishiyuyueServiceImpl extends ServiceImpl<HuiyishiyuyueDao, HuiyishiyuyueEntity> implements HuiyishiyuyueService {


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        Page<HuiyishiyuyueEntity> page = this.selectPage(
                new Query<HuiyishiyuyueEntity>(params).getPage(),
                new EntityWrapper<HuiyishiyuyueEntity>()
        );
        return new PageUtils(page);
    }

    @Override
	public PageUtils queryPage(Map<String, Object> params, Wrapper<HuiyishiyuyueEntity> wrapper) {
		  Page<HuiyishiyuyueView> page =new Query<HuiyishiyuyueView>(params).getPage();
		        page.setRecords(baseMapper.selectListView(page,wrapper));
		    	PageUtils pageUtil = new PageUtils(page);
		    	return pageUtil;
 	}

    @Override
	public List<HuiyishiyuyueVO> selectListVO(Wrapper<HuiyishiyuyueEntity> wrapper) {
 		return baseMapper.selectListVO(wrapper);
	}

	@Override
	public HuiyishiyuyueVO selectVO(Wrapper<HuiyishiyuyueEntity> wrapper) {
 		return baseMapper.selectVO(wrapper);
	}

	@Override
	public List<HuiyishiyuyueView> selectListView(Wrapper<HuiyishiyuyueEntity> wrapper) {
		return baseMapper.selectListView(wrapper);
	}

	@Override
	public HuiyishiyuyueView selectView(Wrapper<HuiyishiyuyueEntity> wrapper) {
		return baseMapper.selectView(wrapper);
	}

	@Override
	public void checkTimeConflict(HuiyishiyuyueEntity huiyishiyuyue, Long excludeId) {
		EntityWrapper<HuiyishiyuyueEntity> ew = new EntityWrapper<>();
		ew.eq("huiyishibianhao", huiyishiyuyue.getHuiyishibianhao());
		ew.in("sfsh", Arrays.asList("待审核", "通过"));

		if (excludeId != null) {
			ew.ne("id", excludeId);
		}

		// 时间重叠判断: 已有记录的结束时间 > 新记录的开始时间 且 已有记录的开始时间 < 新记录的结束时间
		ew.and("jieshushijian > {0}", huiyishiyuyue.getKaishishijian());
		ew.and("kaishishijian < {0}", huiyishiyuyue.getJieshushijian());

		int count = this.selectCount(ew);
		if (count > 0) {
			throw new EIException("该会议室在所选时间段内已有预约（待审核或通过），存在时间冲突");
		}
	}

	@Override
	public void checkEquipmentConflict(HuiyishiyuyueEntity huiyishiyuyue, Long excludeId) {
		if (StringUtils.isBlank(huiyishiyuyue.getShebeibianhao())) {
			return;
		}

		List<String> newShebeiList = Arrays.asList(huiyishiyuyue.getShebeibianhao().split(","));

		EntityWrapper<HuiyishiyuyueEntity> ew = new EntityWrapper<>();
		ew.in("sfsh", Arrays.asList("待审核", "通过"));

		if (excludeId != null) {
			ew.ne("id", excludeId);
		}

		ew.and("jieshushijian > {0}", huiyishiyuyue.getKaishishijian());
		ew.and("kaishishijian < {0}", huiyishiyuyue.getJieshushijian());

		List<HuiyishiyuyueEntity> conflicts = this.selectList(ew);

		for (HuiyishiyuyueEntity existing : conflicts) {
			if (StringUtils.isNotBlank(existing.getShebeibianhao())) {
				List<String> existingShebeiList = Arrays.asList(existing.getShebeibianhao().split(","));
				for (String shebei : newShebeiList) {
					if (existingShebeiList.contains(shebei.trim())) {
						throw new EIException("设备 " + shebei.trim() + " 在该时间段内已被占用");
					}
				}
			}
		}
	}

	@Override
	public void validateDepartment(HuiyishiyuyueEntity huiyishiyuyue, String userBumen) {
		if (StringUtils.isNotBlank(userBumen) &&
			StringUtils.isNotBlank(huiyishiyuyue.getBumen()) &&
			!userBumen.equals(huiyishiyuyue.getBumen())) {
			throw new EIException("只能预约本部门的会议室");
		}
	}


}
