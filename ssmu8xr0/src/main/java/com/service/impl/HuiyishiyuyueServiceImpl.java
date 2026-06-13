package com.service.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.List;
import java.util.Date;

import com.baomidou.mybatisplus.mapper.Wrapper;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.plugins.Page;
import com.baomidou.mybatisplus.service.impl.ServiceImpl;
import com.utils.PageUtils;
import com.utils.Query;


import com.dao.HuiyishiyuyueDao;
import com.entity.HuiyishiyuyueEntity;
import com.entity.HuiyishiEntity;
import com.entity.ShiyongjiluEntity;
import com.service.HuiyishiyuyueService;
import com.service.HuiyishiService;
import com.service.ShiyongjiluService;
import com.entity.vo.HuiyishiyuyueVO;
import com.entity.view.HuiyishiyuyueView;

@Service("huiyishiyuyueService")
public class HuiyishiyuyueServiceImpl extends ServiceImpl<HuiyishiyuyueDao, HuiyishiyuyueEntity> implements HuiyishiyuyueService {

	@Autowired
	private HuiyishiService huiyishiService;

	@Autowired
	private ShiyongjiluService shiyongjiluService;

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
	public String validateYuyue(HuiyishiyuyueEntity yuyue, Long excludeId) {
		// 基本校验
		if (yuyue.getKaishishijian() == null || yuyue.getJieshushijian() == null) {
			return "开始时间和结束时间不能为空";
		}
		if (!yuyue.getKaishishijian().before(yuyue.getJieshushijian())) {
			return "开始时间必须早于结束时间";
		}
		if (StringUtils.isBlank(yuyue.getHuiyishibianhao())) {
			return "会议室编号不能为空";
		}

		// 1. 部门权限校验：用户只能预约本部门可用的会议室
		HuiyishiEntity room = huiyishiService.selectOne(
				new EntityWrapper<HuiyishiEntity>().eq("huiyishibianhao", yuyue.getHuiyishibianhao()));
		if (room == null) {
			return "会议室不存在";
		}
		if (StringUtils.isNotBlank(room.getBumen()) && StringUtils.isNotBlank(yuyue.getBumen())) {
			if (!room.getBumen().equals(yuyue.getBumen())) {
				return "该会议室仅限【" + room.getBumen() + "】部门预约，您所在部门【" + yuyue.getBumen() + "】无权预约";
			}
		}

		// 2. 会议室时间冲突校验
		int roomConflict = baseMapper.selectTimeConflictCount(
				yuyue.getHuiyishibianhao(),
				yuyue.getKaishishijian(),
				yuyue.getJieshushijian(),
				excludeId);
		if (roomConflict > 0) {
			return "该会议室在所选时间段内已有预约，请更换时间或会议室";
		}

		// 3. 设备冲突校验（仅在绑定了设备时检查）
		if (StringUtils.isNotBlank(yuyue.getShebeibianhao())) {
			int shebeiConflict = baseMapper.selectShebeiConflictCount(
					yuyue.getShebeibianhao(),
					yuyue.getKaishishijian(),
					yuyue.getJieshushijian(),
					excludeId);
			if (shebeiConflict > 0) {
				return "设备【" + yuyue.getShebeibianhao() + "】在所选时间段内已被其他会议占用";
			}
		}

		return null;
	}

	@Override
	public void approve(HuiyishiyuyueEntity yuyue) {
		ShiyongjiluEntity jilu = new ShiyongjiluEntity();
		jilu.setId(new Date().getTime() + new Double(Math.floor(Math.random() * 1000)).longValue());
		jilu.setHuiyishibianhao(yuyue.getHuiyishibianhao());
		jilu.setHuiyishimingcheng(yuyue.getHuiyishimingcheng());
		jilu.setHuiyishiguimo(yuyue.getHuiyishiguimo());
		jilu.setHuiyishiweizhi(yuyue.getHuiyishiweizhi());
		jilu.setShiyongshijian(yuyue.getKaishishijian());
		jilu.setBeizhu("预约审核通过自动生成，预约编号：" + yuyue.getYuyuebianhao()
				+ "，使用人：" + yuyue.getYonghuxingming()
				+ "，时间：" + yuyue.getKaishishijian() + " ~ " + yuyue.getJieshushijian());
		jilu.setAddtime(new Date());
		// 从会议室表补充容纳人数
		HuiyishiEntity room = huiyishiService.selectOne(
				new EntityWrapper<HuiyishiEntity>().eq("huiyishibianhao", yuyue.getHuiyishibianhao()));
		if (room != null) {
			jilu.setRongnarenshu(room.getRongnarenshu());
		}
		shiyongjiluService.insert(jilu);
	}

}
