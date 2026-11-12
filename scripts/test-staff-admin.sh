#!/bin/bash
# =============================================================
# 功能回归测试（二）：服务人员端 / 管理后台 / 权限与安全
#
# 前置条件：应用已在 8080 端口启动，且为「初始演示数据」状态
#   重置方式：停止应用 -> 删除 home-service/data 目录 -> 重新启动
# 说明：脚本会创建若干测试数据（订单、分类、服务、员工、居民账号），
#       跑完后建议再次重置 data 目录，恢复干净演示数据。
# 运行：bash scripts/test-staff-admin.sh
# =============================================================
# 功能测试 Part 2：员工端 / 管理端 / 权限与安全
BASE=http://localhost:8080
D=/tmp/t
WP=$(cygpath -w $D 2>/dev/null || echo $D)
mkdir -p $D
P=0; F=0
ok(){ P=$((P+1)); printf "  [PASS] %s\n" "$1"; }
bad(){ F=$((F+1)); printf "  [FAIL] %s  <<< %s\n" "$1" "$2"; }
eq(){ if [ "$2" = "$3" ]; then ok "$1 => $2"; else bad "$1" "expect=$3 got=$2"; fi; }
has(){ case "$2" in *"$3"*) ok "$1";; *) bad "$1" "页面未包含: $3";; esac; }
hasnt(){ case "$2" in *"$3"*) bad "$1" "页面不应包含: $3";; *) ok "$1";; esac; }
sec(){ printf "\n== %s ==\n" "$1"; }

gf(){ curl -s -H "Accept: text/html" -b "$D/$1" -c "$D/$1" "$BASE$2"; }
gc(){ curl -s -H "Accept: text/html" -o /dev/null -w '%{http_code}' -b "$D/$1" -c "$D/$1" "$BASE$2"; }
gl(){ curl -s -o /dev/null -w '%{redirect_url}' -b "$D/$1" -c "$D/$1" "$BASE$2"; }
tok(){ curl -s -b "$D/$1" -c "$D/$1" "$BASE$2" | grep -o '_csrf" value="[^"]*"' | head -1 | sed 's/.*value="//;s/"$//'; }
postf(){ local jar="$D/$1"; local url="$2"; shift 2; curl -sL -H "Accept: text/html" -b "$jar" -c "$jar" -o "$D/last.html" -w '%{http_code}' "$BASE$url" "$@"; }
post(){ local jar="$D/$1"; local url="$2"; shift 2; curl -s -b "$jar" -c "$jar" -o "$D/last.html" -w '%{http_code}|%{redirect_url}' -X POST "$BASE$url" "$@"; }
pc(){ echo "$1" | cut -d'|' -f1; }
pl(){ echo "$1" | cut -d'|' -f2; }
login(){ local jar="$1" user="$2" pass="$3"; local t; t=$(tok "$jar" /login); post "$jar" /login --data-urlencode "username=$user" --data-urlencode "password=$pass" --data-urlencode "_csrf=$t"; }
idnear(){ python - "$WP/$1" "$2" "$3" "$4" <<'PY'
import io,re,sys
path,anchor,pat,back=sys.argv[1],sys.argv[2],sys.argv[3],int(sys.argv[4])
h=io.open(path,encoding='utf-8').read()
i=h.find(anchor)
if i<0: print(''); raise SystemExit
seg=h[max(0,i-back):i+back]
m=re.search(pat,seg)
print(m.group(1) if m else '')
PY
}
rowid(){ python - "$WP/$1" "$2" "$3" <<'PY'
import io,re,sys
path,anchor,pat=sys.argv[1],sys.argv[2],sys.argv[3]
h=io.open(path,encoding='utf-8').read()
for blk in re.split(r'<tr', h):
    if anchor in blk:
        m=re.search(pat, blk)
        if m: print(m.group(1)); raise SystemExit
print('')
PY
}
mkorder(){ # $1 jar $2 remark $3 slot -> 输出订单ID
  local jar="$1" remark="$2" slot="$3" t r
  t=$(tok "$jar" /booking/1)
  r=$(post "$jar" /orders --data-urlencode "serviceId=1" --data-urlencode "appointmentDate=$(date -d '+2 day' +%Y-%m-%d)" --data-urlencode "appointmentTime=$slot" --data-urlencode "contactName=张伟" --data-urlencode "contactPhone=13800001111" --data-urlencode "region=自动化小区 1 号楼" --data-urlencode "detail=1 单元 101" --data-urlencode "remark=$remark" --data-urlencode "quantity=1" --data-urlencode "_csrf=$t")
  pl "$r" | grep -o '[0-9]*$'
}

rm -f $D/*.jar
PW=$(gf anon.jar /login | grep -o '<code>[0-9]*</code>' | head -1 | sed 's/<code>//;s/<\/code>//')
SFX=$(date +%H%M%S)
login u.jar user "$PW" >/dev/null
login s.jar staff "$PW" >/dev/null
login a.jar admin "$PW" >/dev/null
eq "三端账号预热登录" "$(gc u.jar /profile)$(gc s.jar /staff/workbench)$(gc a.jar /admin/dashboard)" "200200200"

sec "5. 员工端：接单 -> 开工 -> 完工"
O1=$(mkorder u.jar "员工流程测试" "09:00-11:00")
O2=$(mkorder u.jar "管理员取消测试" "13:00-15:00")
ok "准备订单 O1=$O1 O2=$O2"
eq "GET /staff/workbench" "$(gc s.jar /staff/workbench)" "200"
has "工作台标题" "$(gf s.jar /staff/workbench)" "接单"
eq "我的工单页" "$(gc s.jar '/staff/workbench?tab=mine')" "200"
has "接单大厅含新单" "$(gf s.jar /staff/workbench)" "自动化小区 1 号楼"
T=$(tok s.jar /staff/workbench)
C=$(postf s.jar "/staff/orders/$O1/accept" --data-urlencode "_csrf=$T")
has "接单成功提示" "$(cat $D/last.html)" "接单成功"
has "接单后状态已接单" "$(cat $D/last.html)" "已接单"
T=$(tok s.jar /staff/workbench)
C=$(postf s.jar "/staff/orders/$O1/accept" --data-urlencode "_csrf=$T")
if grep -q "已被接单\|已被其他服务人员接走" $D/last.html; then ok "重复接单被拦截"; else bad "重复接单被拦截" "无提示"; fi
T=$(tok s.jar /staff/workbench)
C=$(postf s.jar "/staff/orders/$O1/finish" --data-urlencode "_csrf=$T")
has "未开工不能完工" "$(cat $D/last.html)" "只有「服务中」的订单可以提交完工"
T=$(tok s.jar /staff/workbench)
C=$(postf s.jar "/staff/orders/$O1/start" --data-urlencode "_csrf=$T")
has "开工成功" "$(cat $D/last.html)" "已开始服务"
T=$(tok s.jar /staff/workbench)
C=$(postf s.jar "/staff/orders/$O1/start" --data-urlencode "_csrf=$T")
has "重复开工被拦截" "$(cat $D/last.html)" "只有「已接单」的订单可以开始服务"
T=$(tok s.jar /staff/workbench)
C=$(postf s.jar "/staff/orders/$O1/finish" --data-urlencode "_csrf=$T")
has "完工成功" "$(cat $D/last.html)" "等待用户确认"
T=$(tok s.jar /staff/workbench)
C=$(postf s.jar "/staff/orders/3/start" --data-urlencode "_csrf=$T")
eq "操作他人工单被拒绝(PRG)" "$C" "200"
has "越权工单提示" "$(cat $D/last.html)" "这不是您的工单"

sec "6. 用户确认与评价（闭环）"
T=$(tok u.jar "/orders/$O1")
C=$(postf u.jar "/orders/$O1/confirm" --data-urlencode "_csrf=$T")
has "确认完成成功" "$(cat $D/last.html)" "已确认完成"
has "状态已完成" "$(cat $D/last.html)" "已完成"
T=$(tok u.jar "/orders/$O1")
C=$(postf u.jar "/orders/$O1/review" --data-urlencode "rating=5" --data-urlencode "content=自动化验收：服务很专业，准时到达" --data-urlencode "_csrf=$T")
has "评价提交成功" "$(cat $D/last.html)" "评价已提交"
has "评价内容展示" "$(cat $D/last.html)" "自动化验收：服务很专业"
has "已完成订单可评价入口消失" "$(cat $D/last.html)" "评价已提交"

sec "7. 管理端"
B=$(gf a.jar /admin/dashboard); eq "数据看板" "$(gc a.jar /admin/dashboard)" "200"
has "看板-累计订单" "$B" "累计订单"
has "看板-营收" "$B" "已完成营收"
has "看板-趋势图" "$B" "近 7 日订单趋势"
has "看板-热门服务" "$B" "热门服务"
eq "订单管理" "$(gc a.jar /admin/orders)" "200"
has "订单管理-列表" "$(gf a.jar /admin/orders)" "笔订单"
B=$(gf a.jar "/admin/orders?status=PENDING_ACCEPT"); eq "按状态筛选订单" "$(gc a.jar '/admin/orders?status=PENDING_ACCEPT')" "200"
has "筛选结果含待接单" "$B" "待接单"
ONO=$(gf u.jar "/orders/$O2" | grep -o 'HS[0-9A-Za-z]*' | head -1)
B=$(gf a.jar "/admin/orders?keyword=$ONO")
has "按订单号搜索 => $ONO" "$B" "$ONO"
T=$(tok a.jar /admin/orders)
C=$(postf a.jar "/admin/orders/$O2/assign" --data-urlencode "staffId=2" --data-urlencode "_csrf=$T")
has "派单成功" "$(cat $D/last.html)" "派单成功"
T=$(tok a.jar /admin/orders)
C=$(postf a.jar "/admin/orders/$O2/assign" --data-urlencode "staffId=9999" --data-urlencode "_csrf=$T")
has "派单给不存在人员被拦截" "$(cat $D/last.html)" "服务人员不存在"
T=$(tok a.jar /admin/orders)
C=$(postf a.jar "/admin/orders/$O2/cancel" --data-urlencode "reason=管理员取消测试" --data-urlencode "_csrf=$T")
has "管理端取消订单" "$(cat $D/last.html)" "订单已取消"
has "管理端取消后状态" "$(cat $D/last.html)" "已取消"

# 分类管理
eq "分类管理页" "$(gc a.jar /admin/categories)" "200"
T=$(tok a.jar /admin/categories)
C=$(postf a.jar /admin/categories/save --data-urlencode "name=测试分类$SFX" --data-urlencode "icon=🧪" --data-urlencode "description=自动化测试分类" --data-urlencode "sortNo=99" --data-urlencode "status=1" --data-urlencode "_csrf=$T")
has "新增分类" "$(cat $D/last.html)" "分类已新增"
has "分类已出现在列表" "$(cat $D/last.html)" "测试分类$SFX"
gf a.jar /admin/categories > $D/cat.html
CID=$(idnear cat.html "测试分类$SFX" 'categories/(\d+)/toggle' 700)
if [ -n "$CID" ]; then ok "分类ID=$CID"; else bad "分类ID" "未找到"; fi
T=$(tok a.jar /admin/categories)
C=$(postf a.jar /admin/categories/save --data-urlencode "id=$CID" --data-urlencode "name=测试分类${SFX}改" --data-urlencode "icon=🧪" --data-urlencode "description=改名验证" --data-urlencode "sortNo=99" --data-urlencode "status=1" --data-urlencode "_csrf=$T")
has "编辑分类" "$(cat $D/last.html)" "测试分类${SFX}改"
T=$(tok a.jar /admin/categories)
C=$(postf a.jar "/admin/categories/$CID/toggle" --data-urlencode "_csrf=$T")
has "分类停用" "$(cat $D/last.html)" "分类状态已更新"
hasnt "停用分类前台不可见" "$(gf anon.jar /services)" "测试分类${SFX}改"
T=$(tok a.jar /admin/categories)
C=$(postf a.jar "/admin/categories/$CID/delete" --data-urlencode "_csrf=$T")
has "删除空分类" "$(cat $D/last.html)" "分类已删除"
T=$(tok a.jar /admin/categories)
C=$(postf a.jar "/admin/categories/1/delete" --data-urlencode "_csrf=$T")
has "有服务的分类不可删" "$(cat $D/last.html)" "该分类下还有服务项目"

# 服务管理
eq "服务管理页" "$(gc a.jar /admin/services)" "200"
T=$(tok a.jar /admin/services)
C=$(postf a.jar /admin/services/save --data-urlencode "categoryId=1" --data-urlencode "name=自动化测试服务$SFX" --data-urlencode "subtitle=测试用服务" --data-urlencode "price=99" --data-urlencode "priceUnit=次" --data-urlencode "duration=90" --data-urlencode "cover=🧪" --data-urlencode "description=用于自动化测试的服务项目" --data-urlencode "status=1" --data-urlencode "_csrf=$T")
has "新增服务" "$(cat $D/last.html)" "服务已上架"
has "前台可搜到新服务" "$(gf anon.jar "/services?keyword=$SFX")" "自动化测试服务$SFX"
gf a.jar /admin/services > $D/svc.html
SID=$(rowid svc.html "自动化测试服务$SFX" 'services/(\d+)/toggle')
if [ -n "$SID" ]; then ok "服务ID=$SID"; else bad "服务ID" "未找到"; fi
T=$(tok a.jar /admin/services)
C=$(postf a.jar /admin/services/save --data-urlencode "id=$SID" --data-urlencode "categoryId=1" --data-urlencode "name=自动化测试服务$SFX" --data-urlencode "subtitle=改价验证" --data-urlencode "price=199" --data-urlencode "priceUnit=次" --data-urlencode "duration=90" --data-urlencode "cover=🧪" --data-urlencode "description=改价后描述" --data-urlencode "status=1" --data-urlencode "_csrf=$T")
has "编辑服务价格" "$(gf anon.jar "/services/$SID")" "199"
T=$(tok a.jar /admin/services)
C=$(postf a.jar "/admin/services/$SID/toggle" --data-urlencode "_csrf=$T")
eq "下架后前台 404" "$(gc anon.jar "/services/$SID")" "404"
T=$(tok u.jar /booking/1)
C=$(postf u.jar /orders --data-urlencode "serviceId=$SID" --data-urlencode "appointmentDate=$(date -d '+1 day' +%Y-%m-%d)" --data-urlencode "appointmentTime=09:00-11:00" --data-urlencode "contactName=张伟" --data-urlencode "contactPhone=13800001111" --data-urlencode "region=自动化小区" --data-urlencode "detail=1-101" --data-urlencode "quantity=1" --data-urlencode "_csrf=$T")
has "下架服务不可下单" "$(cat $D/last.html)" "该服务不存在或已下架"
T=$(tok a.jar /admin/services)
C=$(postf a.jar "/admin/services/$SID/toggle" --data-urlencode "_csrf=$T")
eq "重新上架后前台 200" "$(gc anon.jar "/services/$SID")" "200"

# 用户管理
eq "用户管理页" "$(gc a.jar /admin/users)" "200"
gf a.jar /admin/users > $D/usr.html
LID=$(rowid usr.html "lina" 'users/(\d+)/toggle')
if [ -n "$LID" ]; then ok "居民 lina 的ID=$LID"; else bad "居民ID" "未找到"; fi
T=$(tok a.jar /admin/users)
C=$(postf a.jar "/admin/users/$LID/toggle" --data-urlencode "status=0" --data-urlencode "_csrf=$T")
has "禁用账号" "$(cat $D/last.html)" "账号已禁用"
rm -f $D/lb.jar; R=$(login lb.jar lina "$PW")
has "禁用后无法登录" "$(pl "$R")" "/login?error"
T=$(tok a.jar /admin/users)
C=$(postf a.jar "/admin/users/$LID/toggle" --data-urlencode "status=1" --data-urlencode "_csrf=$T")
has "启用账号" "$(cat $D/last.html)" "账号已启用"
rm -f $D/lc.jar; R=$(login lc.jar lina "$PW")
eq "启用后可登录" "$(pl "$R")" "http://localhost:8080/"

# 员工管理
eq "员工管理页" "$(gc a.jar /admin/staffs)" "200"
SUN="st$SFX"
T=$(tok a.jar /admin/staffs)
C=$(postf a.jar /admin/staffs --data-urlencode "username=$SUN" --data-urlencode "password=abc123" --data-urlencode "realName=自动化员工" --data-urlencode "phone=13700008888" --data-urlencode "jobType=保洁" --data-urlencode "staffLevel=初级" --data-urlencode "intro=自动化测试创建的服务人员" --data-urlencode "_csrf=$T")
has "新增员工账号" "$(cat $D/last.html)" "服务人员账号已创建"
has "员工列表含新员工" "$(cat $D/last.html)" "自动化员工"
rm -f $D/sb.jar; R=$(login sb.jar "$SUN" abc123)
eq "新员工可登录工作台" "$(pl "$R")" "http://localhost:8080/staff/workbench"
gf a.jar /admin/staffs > $D/stf.html
TID=$(idnear stf.html "自动化员工" 'staffs/(\d+)/available' 900)
if [ -n "$TID" ]; then ok "新员工档案ID=$TID"; else bad "新员工档案ID" "未找到"; fi
T=$(tok a.jar /admin/staffs)
C=$(postf a.jar "/admin/staffs/$TID/update" --data-urlencode "jobType=家电清洗" --data-urlencode "staffLevel=金牌" --data-urlencode "intro=改成家电清洗" --data-urlencode "_csrf=$T")
has "编辑员工档案" "$(cat $D/last.html)" "服务人员信息已更新"
has "工种已更新" "$(cat $D/last.html)" "家电清洗"
T=$(tok a.jar /admin/staffs)
C=$(postf a.jar "/admin/staffs/$TID/available" --data-urlencode "available=0" --data-urlencode "_csrf=$T")
has "设为休息中" "$(cat $D/last.html)" "已设为休息中"

sec "8. 权限与安全"
eq "居民访问后台 => 403" "$(gc u.jar /admin/dashboard)" "403"
eq "居民访问员工端 => 403" "$(gc u.jar /staff/workbench)" "403"
eq "员工访问后台 => 403" "$(gc s.jar /admin/dashboard)" "403"
eq "管理员访问员工端 => 403" "$(gc a.jar /staff/workbench)" "403"
eq "无 CSRF 令牌提交 => 403" "$(curl -s -o /dev/null -w '%{http_code}' -b "$D/u.jar" -X POST "$BASE/orders" -d "serviceId=1")" "403"
eq "无 CSRF 令牌删除地址 => 403" "$(curl -s -o /dev/null -w '%{http_code}' -b "$D/u.jar" -X POST "$BASE/addresses/1/delete")" "403"
H2=$(curl -s -o /dev/null -w '%{http_code}' "$BASE/h2-console")
echo "  [INFO] /h2-console 状态码 = $H2（开发期调试入口，上线前应关闭）"

printf "\n========== Part2 结果: PASS=%d FAIL=%d ==========\n" "$P" "$F"
