package com.controller;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Date;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import com.utils.ValidatorUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.annotation.IgnoreAuth;

import com.entity.HuiyishiyuyueEntity;
import com.entity.YonghuEntity;
import com.entity.view.HuiyishiyuyueView;

import com.service.HuiyishiyuyueService;
import com.service.YonghuService;
import com.service.TokenService;
import com.utils.PageUtils;
import com.utils.R;
import com.utils.MD5Util;
import com.utils.MPUtil;
import com.utils.CommonUtil;

/**
 * 会议室预约
 * 后端接口
 * @author
 * @email
 * @date 2022-04-21 23:05:48
 */
@RestController
@RequestMapping("/huiyishiyuyue")
public class HuiyishiyuyueController {
    @Autowired
    private HuiyishiyuyueService huiyishiyuyueService;

    @Autowired
    private YonghuService yonghuService;


    /**
     * 后端列表
     */
    @RequestMapping("/page")
    public R page(@RequestParam Map<String, Object> params,HuiyishiyuyueEntity huiyishiyuyue,
		HttpServletRequest request){

		String tableName = request.getSession().getAttribute("tableName").toString();
		if(tableName.equals("yonghu")) {
			huiyishiyuyue.setYonghuzhanghao((String)request.getSession().getAttribute("username"));
		}
        EntityWrapper<HuiyishiyuyueEntity> ew = new EntityWrapper<HuiyishiyuyueEntity>();
		PageUtils page = huiyishiyuyueService.queryPage(params, MPUtil.sort(MPUtil.between(MPUtil.likeOrEq(ew, huiyishiyuyue), params), params));
        return R.ok().put("data", page);
    }

    /**
     * 前端列表
     */
	@IgnoreAuth
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params,HuiyishiyuyueEntity huiyishiyuyue,
		HttpServletRequest request){
        EntityWrapper<HuiyishiyuyueEntity> ew = new EntityWrapper<HuiyishiyuyueEntity>();
		PageUtils page = huiyishiyuyueService.queryPage(params, MPUtil.sort(MPUtil.between(MPUtil.likeOrEq(ew, huiyishiyuyue), params), params));
        return R.ok().put("data", page);
    }

	/**
     * 列表
     */
    @RequestMapping("/lists")
    public R list( HuiyishiyuyueEntity huiyishiyuyue){
       	EntityWrapper<HuiyishiyuyueEntity> ew = new EntityWrapper<HuiyishiyuyueEntity>();
      	ew.allEq(MPUtil.allEQMapPre( huiyishiyuyue, "huiyishiyuyue"));
        return R.ok().put("data", huiyishiyuyueService.selectListView(ew));
    }

	 /**
     * 查询
     */
    @RequestMapping("/query")
    public R query(HuiyishiyuyueEntity huiyishiyuyue){
        EntityWrapper< HuiyishiyuyueEntity> ew = new EntityWrapper< HuiyishiyuyueEntity>();
 		ew.allEq(MPUtil.allEQMapPre( huiyishiyuyue, "huiyishiyuyue"));
		HuiyishiyuyueView huiyishiyuyueView =  huiyishiyuyueService.selectView(ew);
		return R.ok("查询会议室预约成功").put("data", huiyishiyuyueView);
    }

    /**
     * 后端详情
     */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id){
        HuiyishiyuyueEntity huiyishiyuyue = huiyishiyuyueService.selectById(id);
        return R.ok().put("data", huiyishiyuyue);
    }

    /**
     * 前端详情
     */
	@IgnoreAuth
    @RequestMapping("/detail/{id}")
    public R detail(@PathVariable("id") Long id){
        HuiyishiyuyueEntity huiyishiyuyue = huiyishiyuyueService.selectById(id);
        return R.ok().put("data", huiyishiyuyue);
    }


    /**
     * 后端保存
     */
    @RequestMapping("/save")
    public R save(@RequestBody HuiyishiyuyueEntity huiyishiyuyue, HttpServletRequest request){
    	huiyishiyuyue.setId(new Date().getTime()+new Double(Math.floor(Math.random()*1000)).longValue());

    	// 如果是用户角色，用数据库中的真实部门防止篡改
    	fillUserBumen(huiyishiyuyue, request);

    	// 预约校验：时间冲突、设备冲突、部门权限
    	String error = huiyishiyuyueService.validateYuyue(huiyishiyuyue, null);
    	if (error != null) {
    		return R.error(error);
    	}

    	// 新预约默认待审核
    	huiyishiyuyue.setSfsh("待审核");
        huiyishiyuyueService.insert(huiyishiyuyue);
        return R.ok();
    }

    /**
     * 前端保存
     */
    @RequestMapping("/add")
    public R add(@RequestBody HuiyishiyuyueEntity huiyishiyuyue, HttpServletRequest request){
    	huiyishiyuyue.setId(new Date().getTime()+new Double(Math.floor(Math.random()*1000)).longValue());

    	// 如果是用户角色，用数据库中的真实部门防止篡改
    	fillUserBumen(huiyishiyuyue, request);

    	// 预约校验：时间冲突、设备冲突、部门权限
    	String error = huiyishiyuyueService.validateYuyue(huiyishiyuyue, null);
    	if (error != null) {
    		return R.error(error);
    	}

    	// 新预约默认待审核
    	huiyishiyuyue.setSfsh("待审核");
        huiyishiyuyueService.insert(huiyishiyuyue);
        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    @Transactional
    public R update(@RequestBody HuiyishiyuyueEntity huiyishiyuyue, HttpServletRequest request){
        // 预约校验：时间冲突、设备冲突、部门权限（排除自身）
        String error = huiyishiyuyueService.validateYuyue(huiyishiyuyue, huiyishiyuyue.getId());
        if (error != null) {
        	return R.error(error);
        }

        huiyishiyuyueService.updateById(huiyishiyuyue);//全部更新
        return R.ok();
    }

    /**
     * 预约审核（替代通用sh接口）
     * sfsh=是：审核通过，再次校验冲突后自动生成使用记录
     * sfsh=否：审核驳回，释放时段
     */
    @RequestMapping("/shenhe")
    @Transactional
    public R shenhe(@RequestBody Map<String, Object> params) {
    	Long id = Long.valueOf(params.get("id").toString());
    	String sfsh = (String) params.get("sfsh");
    	String shhf = params.get("shhf") != null ? params.get("shhf").toString() : "";

    	HuiyishiyuyueEntity yuyue = huiyishiyuyueService.selectById(id);
    	if (yuyue == null) {
    		return R.error("预约记录不存在");
    	}

    	if ("是".equals(sfsh)) {
    		// 审核通过前再次校验时间冲突（防止多条待审核记录同时通过导致冲突）
    		String error = huiyishiyuyueService.validateYuyue(yuyue, yuyue.getId());
    		if (error != null) {
    			return R.error("审核通过失败：" + error);
    		}
    		yuyue.setSfsh("是");
    		yuyue.setShhf(shhf);
    		huiyishiyuyueService.updateById(yuyue);
    		// 自动生成使用记录
    		huiyishiyuyueService.approve(yuyue);
    	} else if ("否".equals(sfsh)) {
    		// 驳回，释放时段（后续新预约不再与此记录冲突）
    		yuyue.setSfsh("否");
    		yuyue.setShhf(shhf);
    		huiyishiyuyueService.updateById(yuyue);
    	} else {
    		return R.error("审核状态无效，请传入'是'或'否'");
    	}

    	return R.ok("审核操作成功");
    }


    /**
     * 删除
     */
    @RequestMapping("/delete")
    public R delete(@RequestBody Long[] ids){
        huiyishiyuyueService.deleteBatchIds(Arrays.asList(ids));
        return R.ok();
    }

    /**
     * 提醒接口
     */
	@RequestMapping("/remind/{columnName}/{type}")
	public R remindCount(@PathVariable("columnName") String columnName, HttpServletRequest request,
						 @PathVariable("type") String type,@RequestParam Map<String, Object> map) {
		map.put("column", columnName);
		map.put("type", type);

		if(type.equals("2")) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			Calendar c = Calendar.getInstance();
			Date remindStartDate = null;
			Date remindEndDate = null;
			if(map.get("remindstart")!=null) {
				Integer remindStart = Integer.parseInt(map.get("remindstart").toString());
				c.setTime(new Date());
				c.add(Calendar.DAY_OF_MONTH,remindStart);
				remindStartDate = c.getTime();
				map.put("remindstart", sdf.format(remindStartDate));
			}
			if(map.get("remindend")!=null) {
				Integer remindEnd = Integer.parseInt(map.get("remindend").toString());
				c.setTime(new Date());
				c.add(Calendar.DAY_OF_MONTH,remindEnd);
				remindEndDate = c.getTime();
				map.put("remindend", sdf.format(remindEndDate));
			}
		}

		Wrapper<HuiyishiyuyueEntity> wrapper = new EntityWrapper<HuiyishiyuyueEntity>();
		if(map.get("remindstart")!=null) {
			wrapper.ge(columnName, map.get("remindstart"));
		}
		if(map.get("remindend")!=null) {
			wrapper.le(columnName, map.get("remindend"));
		}

		String tableName = request.getSession().getAttribute("tableName").toString();
		if(tableName.equals("yonghu")) {
			wrapper.eq("yonghuzhanghao", (String)request.getSession().getAttribute("username"));
		}

		int count = huiyishiyuyueService.selectCount(wrapper);
		return R.ok().put("count", count);
	}

	/**
	 * 根据session中的用户信息填充真实部门（防止前端篡改）
	 */
	private void fillUserBumen(HuiyishiyuyueEntity yuyue, HttpServletRequest request) {
		Object tableNameObj = request.getSession().getAttribute("tableName");
		if (tableNameObj != null && "yonghu".equals(tableNameObj.toString())) {
			String username = (String) request.getSession().getAttribute("username");
			if (StringUtils.isNotBlank(username)) {
				YonghuEntity user = yonghuService.selectOne(
						new EntityWrapper<YonghuEntity>().eq("yonghuzhanghao", username));
				if (user != null && StringUtils.isNotBlank(user.getBumen())) {
					yuyue.setBumen(user.getBumen());
				}
			}
		}
	}

}
