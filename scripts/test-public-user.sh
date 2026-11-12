#!/bin/bash
# =============================================================
# 功能回归测试（一）：公开页面 / 认证 / 用户中心 / 下单与订单状态
#
# 前置条件：应用已在 8080 端口启动，且为「初始演示数据」状态
#   重置方式：停止应用 -> 删除 home-service/data 目录 -> 重新启动
#   （部分断言依赖种子订单，例如 1 号已完成订单带评价、3 号订单属于另一居民）
# 运行：bash scripts/test-public-user.sh
# 输出：逐条 [PASS]/[FAIL]，末尾汇总 PASS/FAIL 数量
# =============================================================
# 功能测试 Part 1：公开页面 / 认证 / 用户中心 / 下单与订单状态
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
# 在 HTML 中查找包含某段文字的元素附近的链接编号
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

rm -f $D/*.jar $D/*.html
PW=$(gf anon.jar /login | grep -o '<code>[0-9]*</code>' | head -1 | sed 's/<code>//;s/<\/code>//')
UN="t$(date +%H%M%S)"
echo "登录页演示密码=$PW  本次测试账号=$UN"

sec "1. 公开页面"
eq "GET /" "$(gc anon.jar /)" "200"
B=$(gf anon.jar /); has "首页标题" "$B" "邻家帮"; has "首页-今日可约" "$B" "今日可约"
eq "GET /services" "$(gc anon.jar /services)" "200"
N=$(gf anon.jar /services | grep -o '共找到 [0-9]* 项服务' | head -1 | grep -o '[0-9]*'); eq "列表-默认 15 项" "$N" "15"
B=$(gf anon.jar "/services?categoryId=1"); eq "分类页状态" "$(gc anon.jar '/services?categoryId=1')" "200"
N2=$(echo "$B" | grep -o '共找到 [0-9]* 项服务' | head -1 | grep -o '[0-9]*')
if [ -n "$N2" ] && [ "$N2" -lt 15 ]; then ok "分类筛选生效 => $N2 项"; else bad "分类筛选生效" "got=$N2"; fi
has "关键字搜索" "$(gf anon.jar '/services?keyword=%E4%BF%9D%E6%B4%81')" "共找到"
hasnt "无结果搜索不显示服务卡" "$(gf anon.jar '/services?keyword=zzzznotexist')" "立即预约"
eq "列表分页 page=2" "$(gc anon.jar '/services?page=2')" "200"
hasnt "第2页不含首项" "$(gf anon.jar '/services?page=2')" "日常保洁 3 小时"
eq "服务详情" "$(gc anon.jar /services/1)" "200"
has "详情-未登录引导" "$(gf anon.jar /services/1)" "登录后预约"
eq "不存在的服务 => 404" "$(gc anon.jar /services/99999)" "404"
has "业务异常页提示" "$(gf anon.jar /services/99999)" "不存在"
eq "GET /login" "$(gc anon.jar /login)" "200"
eq "GET /register" "$(gc anon.jar /register)" "200"
eq "未知路径 => 404" "$(gc anon.jar /nope)" "404"
has "404 页文案" "$(gf anon.jar /nope)" "页面走丢了"

sec "2. 认证与授权"
eq "未登录 /orders" "$(gc anon.jar /orders)" "302"
has "未登录跳登录页" "$(gl anon.jar /orders)" "/login"
eq "未登录 /staff/workbench" "$(gc anon.jar /staff/workbench)" "302"
eq "未登录 /admin/dashboard" "$(gc anon.jar /admin/dashboard)" "302"
T=$(tok anon.jar /login); R=$(post anon.jar /login -d "username=user&password=badpwd&_csrf=$T")
eq "错误密码状态" "$(pc "$R")" "302"
has "错误密码跳错误页" "$(pl "$R")" "/login?error"
R=$(login u.jar user "$PW"); eq "用户登录跳首页" "$(pl "$R")" "http://localhost:8080/"
R=$(login s.jar staff "$PW"); eq "员工登录跳工作台" "$(pl "$R")" "http://localhost:8080/staff/workbench"
R=$(login a.jar admin "$PW"); eq "管理员登录跳后台" "$(pl "$R")" "http://localhost:8080/admin/dashboard"
eq "已登录访问 /login" "$(gl u.jar /login)" "http://localhost:8080/"

rm -f $D/n.jar
T=$(tok n.jar /register); post n.jar /register -d "username=$UN&password=111111&confirmPassword=222222&realName=测试&phone=13900001111&_csrf=$T" >/dev/null
has "注册-密码不一致" "$(cat $D/last.html)" "两次输入的密码不一致"
T=$(tok n.jar /register); post n.jar /register -d "username=user&password=111111&confirmPassword=111111&realName=测试&phone=13900001111&_csrf=$T" >/dev/null
has "注册-用户名重复" "$(cat $D/last.html)" "用户名已被注册"
T=$(tok n.jar /register); post n.jar /register -d "username=$UN&password=111111&confirmPassword=111111&realName=测试&phone=12345&_csrf=$T" >/dev/null
has "注册-手机号格式" "$(cat $D/last.html)" "手机号格式不正确"
T=$(tok n.jar /register); post n.jar /register -d "username=ab&password=111111&confirmPassword=111111&realName=测试&phone=13900001111&_csrf=$T" >/dev/null
has "注册-用户名长度" "$(cat $D/last.html)" "用户名长度需为 3-20 位"
T=$(tok n.jar /register); R=$(post n.jar /register -d "username=$UN&password=111111&confirmPassword=111111&realName=测试用户&phone=13900001111&_csrf=$T")
eq "注册成功跳登录" "$(pl "$R")" "http://localhost:8080/login"
has "注册成功提示" "$(gf n.jar /login)" "注册成功"
R=$(login n.jar "$UN" 111111); eq "新账号可登录" "$(pl "$R")" "http://localhost:8080/"
T=$(tok n.jar /profile); R=$(post n.jar /logout -d "_csrf=$T")
eq "登出状态" "$(pc "$R")" "302"
eq "登出后需登录" "$(gc n.jar /orders)" "302"

sec "3. 用户中心"
eq "GET /profile" "$(gc u.jar /profile)" "200"
has "资料卡姓名" "$(gf u.jar /profile)" "张伟"
T=$(tok u.jar /profile); R=$(post u.jar /profile -d "realName=张伟测&phone=13800001111&email=zhangwei@test.com&_csrf=$T")
eq "改资料状态" "$(pc "$R")" "302"; has "资料已更新" "$(gf u.jar /profile)" "张伟测"
T=$(tok u.jar /profile); post u.jar /profile -d "realName=张伟&phone=13800001111&email=bad-email&_csrf=$T" >/dev/null
has "邮箱格式校验" "$(cat $D/last.html)" "邮箱格式不正确"
T=$(tok u.jar /profile); post u.jar /profile -d "realName=张伟&phone=123&email=ok@test.com&_csrf=$T" >/dev/null
has "手机号格式校验" "$(cat $D/last.html)" "手机号格式不正确"
T=$(tok u.jar /profile); postf u.jar /profile/password --data-urlencode "oldPassword=wrongone" --data-urlencode "newPassword=654321" --data-urlencode "_csrf=$T" >/dev/null
has "改密-原密码错误" "$(cat $D/last.html)" "原密码不正确"
T=$(tok u.jar /profile); postf u.jar /profile/password --data-urlencode "oldPassword=$PW" --data-urlencode "newPassword=123" --data-urlencode "_csrf=$T" >/dev/null
has "改密-新密码过短" "$(cat $D/last.html)" "新密码至少 6 位"
login n2.jar "$UN" 111111 >/dev/null
T=$(tok n2.jar /profile); post n2.jar /profile/password --data-urlencode "oldPassword=111111" --data-urlencode "newPassword=888888" --data-urlencode "_csrf=$T" >/dev/null
rm -f $D/n3.jar; login n3.jar "$UN" 888888 >/dev/null
eq "改密后新密码生效" "$(gc n3.jar /profile)" "200"

T=$(tok u.jar /profile); R=$(post u.jar /addresses --data-urlencode "contactName=张伟" --data-urlencode "contactPhone=13800001111" --data-urlencode "region=测试花园${UN}9 号楼" --data-urlencode "detail=1 单元 202" --data-urlencode "setDefault=true" -d "_csrf=$T")
eq "新增地址" "$(pc "$R")" "302"
gf u.jar /profile > $D/pf.html
has "地址已显示" "$(cat $D/pf.html)" "测试花园${UN}9 号楼"
AID=$(idnear pf.html "测试花园${UN}9 号楼" 'addresses/(\d+)/delete' 900)
if [ -n "$AID" ]; then ok "地址ID=$AID"; else bad "地址ID" "未找到"; fi
T=$(tok u.jar /profile); R=$(post u.jar "/addresses/$AID/default" -d "_csrf=$T")
eq "设为默认" "$(pc "$R")" "302"
T=$(tok u.jar /profile); R=$(post u.jar "/addresses/$AID/delete" -d "_csrf=$T")
eq "删除地址" "$(pc "$R")" "302"
hasnt "地址已删除" "$(gf u.jar /profile)" "测试花园${UN}9 号楼"

sec "4. 预约下单与订单状态"
B=$(gf u.jar /booking/1); eq "GET /booking/1" "$(gc u.jar /booking/1)" "200"
TOMORROW=$(date -d '+1 day' +%Y-%m-%d)
has "默认日期预填" "$B" "$TOMORROW"
has "详情-登录后预约按钮" "$(gf u.jar /services/1)" "立即预约"
T=$(tok u.jar /booking/1); postf u.jar /orders --data-urlencode "serviceId=1" --data-urlencode "appointmentDate=$TOMORROW" --data-urlencode "appointmentTime=10:00-12:00" --data-urlencode "contactName=张伟" --data-urlencode "contactPhone=13800001111" --data-urlencode "region=阳光花园 3 号楼" --data-urlencode "detail=2 单元 501" --data-urlencode "quantity=0" --data-urlencode "_csrf=$T" >/dev/null
has "数量0拦截" "$(cat $D/last.html)" "数量至少为 1"
T=$(tok u.jar /booking/1); postf u.jar /orders --data-urlencode "serviceId=1" --data-urlencode "appointmentTime=10:00-12:00" --data-urlencode "contactName=张伟" --data-urlencode "contactPhone=13800001111" --data-urlencode "region=阳光花园 3 号楼" --data-urlencode "detail=2 单元 501" --data-urlencode "quantity=1" --data-urlencode "_csrf=$T" >/dev/null
has "缺日期拦截" "$(cat $D/last.html)" "请选择预约日期"
T=$(tok u.jar /booking/1); postf u.jar /orders --data-urlencode "serviceId=1" --data-urlencode "appointmentDate=2020-01-01" --data-urlencode "appointmentTime=10:00-12:00" --data-urlencode "contactName=张伟" --data-urlencode "contactPhone=13800001111" --data-urlencode "region=阳光花园 3 号楼" --data-urlencode "detail=2 单元 501" --data-urlencode "quantity=1" --data-urlencode "_csrf=$T" >/dev/null
has "过去日期拦截" "$(cat $D/last.html)" "预约日期不能早于今天"
FAR=$(date -d '+61 day' +%Y-%m-%d)
T=$(tok u.jar /booking/1); postf u.jar /orders --data-urlencode "serviceId=1" --data-urlencode "appointmentDate=$FAR" --data-urlencode "appointmentTime=10:00-12:00" --data-urlencode "contactName=张伟" --data-urlencode "contactPhone=13800001111" --data-urlencode "region=阳光花园 3 号楼" --data-urlencode "detail=2 单元 501" --data-urlencode "quantity=1" --data-urlencode "_csrf=$T" >/dev/null
has "超60天拦截" "$(cat $D/last.html)" "最多支持提前 60 天预约"
T=$(tok u.jar /booking/1); R=$(post u.jar /orders --data-urlencode "serviceId=1" --data-urlencode "appointmentDate=$TOMORROW" --data-urlencode "appointmentTime=14:00-16:00" --data-urlencode "contactName=张伟" --data-urlencode "contactPhone=13800001111" --data-urlencode "region=阳光花园 3 号楼" --data-urlencode "detail=2 单元 501" --data-urlencode "remark=自动化测试订单" --data-urlencode "quantity=2" --data-urlencode "_csrf=$T")
eq "下单成功跳详情" "$(pc "$R")" "302"
OID=$(pl "$R" | grep -o '[0-9]*$'); ok "新订单ID=$OID"
B=$(gf u.jar "/orders/$OID")
has "详情-待接单" "$B" "待接单"; has "详情-金额258" "$B" "258"; has "详情-备注" "$B" "自动化测试订单"
eq "GET /orders" "$(gc u.jar /orders)" "200"
has "列表含新订单" "$(gf u.jar /orders)" "$OID"
CT_ALL=$(gf u.jar /orders | grep -c 'class="order-card"')
gf u.jar "/orders?status=COMPLETED" > $D/oc.html
CT_DONE=$(grep -c 'class="order-card"' $D/oc.html)
CT_LABEL=$(grep -c '已完成' $D/oc.html)
if [ "$CT_DONE" -ge 1 ] && [ "$CT_DONE" -lt "$CT_ALL" ] && [ "$CT_LABEL" -ge 1 ]; then ok "状态筛选生效 => $CT_DONE/$CT_ALL"; else bad "状态筛选" "$CT_DONE/$CT_ALL label=$CT_LABEL"; fi
eq "越权访问他人订单 => 404" "$(gc u.jar /orders/3)" "404"
has "越权提示" "$(gf u.jar /orders/3)" "订单不存在"
# 未完成订单确认 -> 拦截
T=$(tok u.jar "/orders/$OID"); postf u.jar "/orders/$OID/confirm" --data-urlencode "_csrf=$T" >/dev/null
has "未完成确认被拦截" "$(cat $D/last.html)" "当前订单还不能确认完成"
# 取消
T=$(tok u.jar "/orders/$OID"); postf u.jar "/orders/$OID/cancel" --data-urlencode "reason=自动化测试取消" --data-urlencode "_csrf=$T" >/dev/null
has "取消后状态" "$(cat $D/last.html)" "已取消"
has "取消原因" "$(cat $D/last.html)" "自动化测试取消"
T=$(tok u.jar "/orders/$OID"); postf u.jar "/orders/$OID/cancel" --data-urlencode "reason=再取消" --data-urlencode "_csrf=$T" >/dev/null
has "重复取消被拦截" "$(cat $D/last.html)" "不支持取消"
T=$(tok u.jar "/orders/1"); postf u.jar "/orders/1/review" --data-urlencode "rating=6" --data-urlencode "content=x" --data-urlencode "_csrf=$T" >/dev/null
has "评分越界拦截" "$(cat $D/last.html)" "评分需为 1-5 星"
T=$(tok u.jar "/orders/1"); postf u.jar "/orders/1/review" --data-urlencode "rating=5" --data-urlencode "content=重复评价" --data-urlencode "_csrf=$T" >/dev/null
has "重复评价拦截" "$(cat $D/last.html)" "该订单已评价过了"
T=$(tok u.jar "/orders/2"); postf u.jar "/orders/2/review" --data-urlencode "rating=5" --data-urlencode "content=服务很好" --data-urlencode "_csrf=$T" >/dev/null
has "未完成评价拦截" "$(cat $D/last.html)" "服务完成后才能评价"

printf "\n========== Part1 结果: PASS=%d FAIL=%d ==========\n" "$P" "$F"
