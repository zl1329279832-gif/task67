# 会议室预约校验功能 - 验证指南

## 一、数据库迁移 SQL

在执行代码前，需要先执行以下 SQL 添加设备编号字段：

```sql
-- 添加设备编号字段到预约表
ALTER TABLE huiyishiyuyue ADD COLUMN shebeibianhao VARCHAR(500) DEFAULT NULL COMMENT '设备编号（多个用逗号分隔）';

-- 可选：为常用查询字段添加索引以提升性能
CREATE INDEX idx_huiyishibianhao_sfsh ON huiyishiyuyue(huiyishibianhao, sfsh);
CREATE INDEX idx_kaishishijian_jieshushijian ON huiyishiyuyue(kaishishijian, jieshushijian);
CREATE INDEX idx_shebeibianhao ON huiyishiyuyue(shebeibianhao);
```

---

## 二、测试数据准备

```sql
-- 清理测试数据（可选）
DELETE FROM huiyishiyuyue WHERE yuyuebianhao LIKE 'TEST%';
DELETE FROM shiyongjilu WHERE beizhu LIKE '%TEST%';

-- 插入测试会议室
INSERT INTO huiyishi (id, huiyishibianhao, huiyishimingcheng, huiyishiguimo, huiyishiweizhi, rongnarenshu, addtime)
VALUES
(1001, 'HY001', '第一会议室', '大型', 'A栋3楼', '50', NOW()),
(1002, 'HY002', '第二会议室', '中型', 'A栋4楼', '20', NOW()),
(1003, 'HY003', '第三会议室', '小型', 'B栋2楼', '10', NOW());

-- 插入测试设备
INSERT INTO shebeixinxi (id, shebeibianhao, shebeimingcheng, shebeiweizhi, yongtu, shuliang, addtime)
VALUES
(2001, 'SB001', '投影仪A', 'A栋3楼', '会议演示', 1, NOW()),
(2002, 'SB002', '音响系统B', 'A栋3楼', '会议扩音', 1, NOW()),
(2003, 'SB003', '投影仪C', 'A栋4楼', '会议演示', 1, NOW());

-- 插入测试用户
INSERT INTO yonghu (id, yonghuzhanghao, mima, yonghuxingming, shoujihaoma, bumen, addtime)
VALUES
(3001, 'user001', '123456', '张三', '13800138001', '技术部', NOW()),
(3002, 'user002', '123456', '李四', '13800138002', '技术部', NOW()),
(3003, 'user003', '123456', '王五', '13800138003', '市场部', NOW());

-- 插入测试部门
INSERT INTO bumen (id, bumen, addtime)
VALUES
(4001, '技术部', NOW()),
(4002, '市场部', NOW()),
(4003, '财务部', NOW());
```

---

## 三、冲突场景验证步骤

### 场景 1：同一会议室时间完全重叠（应拒绝）

**测试目的**：验证同一会议室在同一时间段不能重复预约

**步骤**：

1. **创建第一个预约**（应成功）
```bash
curl -X POST http://localhost:8080/ssmu8xr0/huiyishiyuyue/save \
  -H "Content-Type: application/json" \
  -H "Token: <your_token>" \
  -d '{
    "yuyuebianhao": "TEST001",
    "huiyishibianhao": "HY001",
    "huiyishimingcheng": "第一会议室",
    "huiyishiguimo": "大型",
    "huiyishiweizhi": "A栋3楼",
    "yujirenshu": "30",
    "kaishishijian": "2026-06-15 09:00:00",
    "jieshushijian": "2026-06-15 11:00:00",
    "huiyishuoming": "技术部周会",
    "yonghuzhanghao": "user001",
    "yonghuxingming": "张三",
    "shoujihaoma": "13800138001",
    "bumen": "技术部",
    "sfsh": "待审核"
  }'
```
**预期结果**：`{"code": 0, "msg": "操作成功"}`

2. **创建第二个预约（时间完全重叠）**（应失败）
```bash
curl -X POST http://localhost:8080/ssmu8xr0/huiyishiyuyue/save \
  -H "Content-Type: application/json" \
  -H "Token: <your_token>" \
  -d '{
    "yuyuebianhao": "TEST002",
    "huiyishibianhao": "HY001",
    "huiyishimingcheng": "第一会议室",
    "huiyishiguimo": "大型",
    "huiyishiweizhi": "A栋3楼",
    "yujirenshu": "20",
    "kaishishijian": "2026-06-15 09:30:00",
    "jieshushijian": "2026-06-15 10:30:00",
    "huiyishuoming": "产品评审会",
    "yonghuzhanghao": "user002",
    "yonghuxingming": "李四",
    "shoujihaoma": "13800138002",
    "bumen": "技术部",
    "sfsh": "待审核"
  }'
```
**预期结果**：`{"code": 500, "msg": "该会议室在所选时间段内已有预约（待审核或通过），存在时间冲突"}`

---

### 场景 2：同一会议室时间部分重叠（应拒绝）

**测试目的**：验证部分时间重叠也会被拒绝

**步骤**：

1. **创建预约（09:00-11:00）**（应成功）
```bash
curl -X POST http://localhost:8080/ssmu8xr0/huiyishiyuyue/save \
  -H "Content-Type: application/json" \
  -H "Token: <your_token>" \
  -d '{
    "yuyuebianhao": "TEST003",
    "huiyishibianhao": "HY002",
    "huiyishimingcheng": "第二会议室",
    "kaishishijian": "2026-06-15 09:00:00",
    "jieshushijian": "2026-06-15 11:00:00",
    "yonghuzhanghao": "user001",
    "yonghuxingming": "张三",
    "bumen": "技术部",
    "sfsh": "待审核"
  }'
```
**预期结果**：成功

2. **创建预约（10:00-12:00，部分重叠）**（应失败）
```bash
curl -X POST http://localhost:8080/ssmu8xr0/huiyishiyuyue/save \
  -H "Content-Type: application/json" \
  -H "Token: <your_token>" \
  -d '{
    "yuyuebianhao": "TEST004",
    "huiyishibianhao": "HY002",
    "huiyishimingcheng": "第二会议室",
    "kaishishijian": "2026-06-15 10:00:00",
    "jieshushijian": "2026-06-15 12:00:00",
    "yonghuzhanghao": "user002",
    "yonghuxingming": "李四",
    "bumen": "技术部",
    "sfsh": "待审核"
  }'
```
**预期结果**：`{"code": 500, "msg": "该会议室在所选时间段内已有预约（待审核或通过），存在时间冲突"}`

3. **创建预约（11:00-13:00，边界接触，不应冲突）**（应成功）
```bash
curl -X POST http://localhost:8080/ssmu8xr0/huiyishiyuyue/save \
  -H "Content-Type: application/json" \
  -H "Token: <your_token>" \
  -d '{
    "yuyuebianhao": "TEST005",
    "huiyishibianhao": "HY002",
    "huiyishimingcheng": "第二会议室",
    "kaishishijian": "2026-06-15 11:00:00",
    "jieshushijian": "2026-06-15 13:00:00",
    "yonghuzhanghao": "user002",
    "yonghuxingming": "李四",
    "bumen": "技术部",
    "sfsh": "待审核"
  }'
```
**预期结果**：成功（11:00开始与前一个11:00结束不冲突）

---

### 场景 3：设备时间冲突（应拒绝）

**测试目的**：验证同一设备在同一时间段不能被多个预约占用

**步骤**：

1. **创建带设备的预约（09:00-10:00，设备SB001）**（应成功）
```bash
curl -X POST http://localhost:8080/ssmu8xr0/huiyishiyuyue/save \
  -H "Content-Type: application/json" \
  -H "Token: <your_token>" \
  -d '{
    "yuyuebianhao": "TEST006",
    "huiyishibianhao": "HY001",
    "huiyishimingcheng": "第一会议室",
    "kaishishijian": "2026-06-16 09:00:00",
    "jieshushijian": "2026-06-16 10:00:00",
    "yonghuzhanghao": "user001",
    "yonghuxingming": "张三",
    "bumen": "技术部",
    "sfsh": "待审核",
    "shebeibianhao": "SB001"
  }'
```
**预期结果**：成功

2. **在不同会议室预约同一设备（09:30-10:30，设备SB001）**（应失败）
```bash
curl -X POST http://localhost:8080/ssmu8xr0/huiyishiyuyue/save \
  -H "Content-Type: application/json" \
  -H "Token: <your_token>" \
  -d '{
    "yuyuebianhao": "TEST007",
    "huiyishibianhao": "HY002",
    "huiyishimingcheng": "第二会议室",
    "kaishishijian": "2026-06-16 09:30:00",
    "jieshushijian": "2026-06-16 10:30:00",
    "yonghuzhanghao": "user002",
    "yonghuxingming": "李四",
    "bumen": "技术部",
    "sfsh": "待审核",
    "shebeibianhao": "SB001"
  }'
```
**预期结果**：`{"code": 500, "msg": "设备 SB001 在该时间段内已被占用"}`

3. **预约其他设备（09:30-10:30，设备SB002）**（应成功）
```bash
curl -X POST http://localhost:8080/ssmu8xr0/huiyishiyuyue/save \
  -H "Content-Type: application/json" \
  -H "Token: <your_token>" \
  -d '{
    "yuyuebianhao": "TEST008",
    "huiyishibianhao": "HY002",
    "huiyishimingcheng": "第二会议室",
    "kaishishijian": "2026-06-16 09:30:00",
    "jieshushijian": "2026-06-16 10:30:00",
    "yonghuzhanghao": "user002",
    "yonghuxingming": "李四",
    "bumen": "技术部",
    "sfsh": "待审核",
    "shebeibianhao": "SB002"
  }'
```
**预期结果**：成功（不同设备不冲突）

---

### 场景 4：部门权限校验（应拒绝）

**测试目的**：验证用户只能预约本部门的会议室

**步骤**：

1. **用户（技术部）预约时填写市场部**（应失败）
```bash
curl -X POST http://localhost:8080/ssmu8xr0/huiyishiyuyue/save \
  -H "Content-Type: application/json" \
  -H "Token: <user001_token>" \
  -d '{
    "yuyuebianhao": "TEST009",
    "huiyishibianhao": "HY003",
    "huiyishimingcheng": "第三会议室",
    "kaishishijian": "2026-06-17 14:00:00",
    "jieshushijian": "2026-06-17 16:00:00",
    "yonghuzhanghao": "user001",
    "yonghuxingming": "张三",
    "bumen": "市场部",
    "sfsh": "待审核"
  }'
```
**预期结果**：`{"code": 500, "msg": "只能预约本部门的会议室"}`

2. **用户（技术部）预约时填写技术部**（应成功）
```bash
curl -X POST http://localhost:8080/ssmu8xr0/huiyishiyuyue/save \
  -H "Content-Type: application/json" \
  -H "Token: <user001_token>" \
  -d '{
    "yuyuebianhao": "TEST010",
    "huiyishibianhao": "HY003",
    "huiyishimingcheng": "第三会议室",
    "kaishishijian": "2026-06-17 14:00:00",
    "jieshushijian": "2026-06-17 16:00:00",
    "yonghuzhanghao": "user001",
    "yonghuxingming": "张三",
    "bumen": "技术部",
    "sfsh": "待审核"
  }'
```
**预期结果**：成功

---

### 场景 5：审核通过自动写入使用记录

**测试目的**：验证审核通过后自动创建使用记录

**步骤**：

1. **创建待审核预约**
```bash
curl -X POST http://localhost:8080/ssmu8xr0/huiyishiyuyue/save \
  -H "Content-Type: application/json" \
  -H "Token: <your_token>" \
  -d '{
    "yuyuebianhao": "TEST011",
    "huiyishibianhao": "HY001",
    "huiyishimingcheng": "第一会议室",
    "huiyishiguimo": "大型",
    "huiyishiweizhi": "A栋3楼",
    "kaishishijian": "2026-06-18 09:00:00",
    "jieshushijian": "2026-06-18 11:00:00",
    "yonghuzhanghao": "user001",
    "yonghuxingming": "张三",
    "bumen": "技术部",
    "sfsh": "待审核"
  }'
```
**预期结果**：成功，记录 ID（假设为 1234567890）

2. **审核通过**
```bash
curl -X POST http://localhost:8080/ssmu8xr0/huiyishiyuyue/shenhe \
  -H "Content-Type: application/json" \
  -H "Token: <admin_token>" \
  -d '{
    "id": 1234567890,
    "sfsh": "通过",
    "shhf": "同意使用"
  }'
```
**预期结果**：`{"code": 0, "msg": "审核通过"}`

3. **验证使用记录已创建**
```bash
curl -X GET "http://localhost:8080/ssmu8xr0/shiyongjilu/page?page=1&limit=10" \
  -H "Token: <your_token>"
```
**预期结果**：列表中包含一条记录，`huiyishibianhao=HY001`，`shiyongshijian=2026-06-18 09:00:00`，`beizhu` 包含预约编号和使用人信息

4. **验证预约状态已更新**
```bash
curl -X GET "http://localhost:8080/ssmu8xr0/huiyishiyuyue/info/1234567890" \
  -H "Token: <your_token>"
```
**预期结果**：`sfsh="通过"`，`shhf="同意使用"`

---

### 场景 6：审核驳回释放时段

**测试目的**：验证审核驳回后，该时段可以被重新预约

**步骤**：

1. **创建待审核预约（09:00-11:00）**
```bash
curl -X POST http://localhost:8080/ssmu8xr0/huiyishiyuyue/save \
  -H "Content-Type: application/json" \
  -H "Token: <your_token>" \
  -d '{
    "yuyuebianhao": "TEST012",
    "huiyishibianhao": "HY001",
    "huiyishimingcheng": "第一会议室",
    "kaishishijian": "2026-06-19 09:00:00",
    "jieshushijian": "2026-06-19 11:00:00",
    "yonghuzhanghao": "user001",
    "yonghuxingming": "张三",
    "bumen": "技术部",
    "sfsh": "待审核"
  }'
```
**预期结果**：成功，记录 ID（假设为 1234567891）

2. **尝试创建重叠时间的预约**（应失败）
```bash
curl -X POST http://localhost:8080/ssmu8xr0/huiyishiyuyue/save \
  -H "Content-Type: application/json" \
  -H "Token: <your_token>" \
  -d '{
    "yuyuebianhao": "TEST013",
    "huiyishibianhao": "HY001",
    "huiyishimingcheng": "第一会议室",
    "kaishishijian": "2026-06-19 10:00:00",
    "jieshushijian": "2026-06-19 12:00:00",
    "yonghuzhanghao": "user002",
    "yonghuxingming": "李四",
    "bumen": "技术部",
    "sfsh": "待审核"
  }'
```
**预期结果**：`{"code": 500, "msg": "该会议室在所选时间段内已有预约（待审核或通过），存在时间冲突"}`

3. **审核驳回第一个预约**
```bash
curl -X POST http://localhost:8080/ssmu8xr0/huiyishiyuyue/shenhe \
  -H "Content-Type: application/json" \
  -H "Token: <admin_token>" \
  -d '{
    "id": 1234567891,
    "sfsh": "驳回",
    "shhf": "时间冲突，请改期"
  }'
```
**预期结果**：`{"code": 0, "msg": "审核驳回"}`

4. **再次创建重叠时间的预约**（应成功）
```bash
curl -X POST http://localhost:8080/ssmu8xr0/huiyishiyuyue/save \
  -H "Content-Type: application/json" \
  -H "Token: <your_token>" \
  -d '{
    "yuyuebianhao": "TEST014",
    "huiyishibianhao": "HY001",
    "huiyishimingcheng": "第一会议室",
    "kaishishijian": "2026-06-19 10:00:00",
    "jieshushijian": "2026-06-19 12:00:00",
    "yonghuzhanghao": "user002",
    "yonghuxingming": "李四",
    "bumen": "技术部",
    "sfsh": "待审核"
  }'
```
**预期结果**：成功（驳回的记录不再参与冲突检查）

---

### 场景 7：审核通过后再次预约冲突（应拒绝）

**测试目的**：验证审核通过的预约也会参与冲突检查

**步骤**：

1. **创建预约并审核通过**
```bash
# 创建预约（记录 ID 假设为 1234567892）
curl -X POST http://localhost:8080/ssmu8xr0/huiyishiyuyue/save \
  -H "Content-Type: application/json" \
  -H "Token: <your_token>" \
  -d '{
    "yuyuebianhao": "TEST015",
    "huiyishibianhao": "HY002",
    "huiyishimingcheng": "第二会议室",
    "kaishishijian": "2026-06-20 14:00:00",
    "jieshushijian": "2026-06-20 16:00:00",
    "yonghuzhanghao": "user001",
    "yonghuxingming": "张三",
    "bumen": "技术部",
    "sfsh": "待审核"
  }'

# 审核通过
curl -X POST http://localhost:8080/ssmu8xr0/huiyishiyuyue/shenhe \
  -H "Content-Type: application/json" \
  -H "Token: <admin_token>" \
  -d '{
    "id": 1234567892,
    "sfsh": "通过"
  }'
```
**预期结果**：两次都成功

2. **创建重叠时间的预约**（应失败）
```bash
curl -X POST http://localhost:8080/ssmu8xr0/huiyishiyuyue/save \
  -H "Content-Type: application/json" \
  -H "Token: <your_token>" \
  -d '{
    "yuyuebianhao": "TEST016",
    "huiyishibianhao": "HY002",
    "huiyishimingcheng": "第二会议室",
    "kaishishijian": "2026-06-20 15:00:00",
    "jieshushijian": "2026-06-20 17:00:00",
    "yonghuzhanghao": "user002",
    "yonghuxingming": "李四",
    "bumen": "技术部",
    "sfsh": "待审核"
  }'
```
**预期结果**：`{"code": 500, "msg": "该会议室在所选时间段内已有预约（待审核或通过），存在时间冲突"}`

---

### 场景 8：修改预约时重新校验冲突

**测试目的**：验证修改预约时间时会重新校验冲突

**步骤**：

1. **创建两个不冲突的预约**
```bash
# 预约A（09:00-10:00）
curl -X POST http://localhost:8080/ssmu8xr0/huiyishiyuyue/save \
  -H "Content-Type: application/json" \
  -H "Token: <your_token>" \
  -d '{
    "yuyuebianhao": "TEST017",
    "huiyishibianhao": "HY003",
    "huiyishimingcheng": "第三会议室",
    "kaishishijian": "2026-06-21 09:00:00",
    "jieshushijian": "2026-06-21 10:00:00",
    "yonghuzhanghao": "user001",
    "yonghuxingming": "张三",
    "bumen": "技术部",
    "sfsh": "待审核"
  }'

# 预约B（10:00-11:00）
curl -X POST http://localhost:8080/ssmu8xr0/huiyishiyuyue/save \
  -H "Content-Type: application/json" \
  -H "Token: <your_token>" \
  -d '{
    "yuyuebianhao": "TEST018",
    "huiyishibianhao": "HY003",
    "huiyishimingcheng": "第三会议室",
    "kaishishijian": "2026-06-21 10:00:00",
    "jieshushijian": "2026-06-21 11:00:00",
    "yonghuzhanghao": "user002",
    "yonghuxingming": "李四",
    "bumen": "技术部",
    "sfsh": "待审核"
  }'
```
**预期结果**：两次都成功

2. **修改预约B的时间为 09:30-10:30（与预约A冲突）**（应失败）
```bash
curl -X POST http://localhost:8080/ssmu8xr0/huiyishiyuyue/update \
  -H "Content-Type: application/json" \
  -H "Token: <your_token>" \
  -d '{
    "id": <预约B的ID>,
    "kaishishijian": "2026-06-21 09:30:00",
    "jieshushijian": "2026-06-21 10:30:00"
  }'
```
**预期结果**：`{"code": 500, "msg": "该会议室在所选时间段内已有预约（待审核或通过），存在时间冲突"}`

---

## 四、验证检查清单

- [ ] 执行数据库迁移 SQL
- [ ] 重新编译并部署应用
- [ ] 场景 1：完全重叠冲突 ✓
- [ ] 场景 2：部分重叠冲突 ✓
- [ ] 场景 3：设备时间冲突 ✓
- [ ] 场景 4：部门权限校验 ✓
- [ ] 场景 5：审核通过自动写入使用记录 ✓
- [ ] 场景 6：审核驳回释放时段 ✓
- [ ] 场景 7：审核通过后冲突检查 ✓
- [ ] 场景 8：修改预约重新校验 ✓

---

## 五、注意事项

1. **Token 获取**：测试前需要先通过 `/yonghu/login` 或 `/users/login` 获取有效的 Token
2. **时间格式**：所有时间字段必须使用 `yyyy-MM-dd HH:mm:ss` 格式
3. **审核状态**：只有 `待审核` 状态的预约才能被审核，已审核的不能重复审核
4. **边界情况**：时间边界接触（如 11:00 结束和 11:00 开始）不算冲突
5. **设备编号**：多个设备用逗号分隔，如 `"SB001,SB002"`
6. **部门校验**：管理员（users 表）不受部门限制，只有普通用户（yonghu 表）受限制

---

## 六、回滚 SQL

如需回滚，执行以下 SQL：

```sql
-- 删除新增字段
ALTER TABLE huiyishiyuyue DROP COLUMN shebeibianhao;

-- 删除索引
DROP INDEX idx_huiyishibianhao_sfsh ON huiyishiyuyue;
DROP INDEX idx_kaishishijian_jieshushijian ON huiyishiyuyue;
DROP INDEX idx_shebeibianhao ON huiyishiyuyue;

-- 清理测试数据
DELETE FROM huiyishiyuyue WHERE yuyuebianhao LIKE 'TEST%';
DELETE FROM shiyongjilu WHERE beizhu LIKE '%TEST%';
```
