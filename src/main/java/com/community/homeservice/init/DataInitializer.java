package com.community.homeservice.init;

import com.community.homeservice.entity.CustomerAddress;
import com.community.homeservice.entity.OrderReview;
import com.community.homeservice.entity.OrderStatusLog;
import com.community.homeservice.entity.ServiceCategory;
import com.community.homeservice.entity.ServiceItem;
import com.community.homeservice.entity.ServiceOrder;
import com.community.homeservice.entity.Staff;
import com.community.homeservice.entity.SysUser;
import com.community.homeservice.enums.OrderStatus;
import com.community.homeservice.mapper.CustomerAddressMapper;
import com.community.homeservice.mapper.OrderReviewMapper;
import com.community.homeservice.mapper.OrderStatusLogMapper;
import com.community.homeservice.mapper.ServiceCategoryMapper;
import com.community.homeservice.mapper.ServiceItemMapper;
import com.community.homeservice.mapper.ServiceOrderMapper;
import com.community.homeservice.mapper.StaffMapper;
import com.community.homeservice.mapper.SysUserMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 首次启动时写入演示数据（已有用户数据则跳过，保证可反复启动）。
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private final SysUserMapper userMapper;
    private final StaffMapper staffMapper;
    private final ServiceCategoryMapper categoryMapper;
    private final ServiceItemMapper itemMapper;
    private final ServiceOrderMapper orderMapper;
    private final OrderReviewMapper reviewMapper;
    private final OrderStatusLogMapper logMapper;
    private final CustomerAddressMapper addressMapper;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(SysUserMapper userMapper,
                           StaffMapper staffMapper,
                           ServiceCategoryMapper categoryMapper,
                           ServiceItemMapper itemMapper,
                           ServiceOrderMapper orderMapper,
                           OrderReviewMapper reviewMapper,
                           OrderStatusLogMapper logMapper,
                           CustomerAddressMapper addressMapper,
                           PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.staffMapper = staffMapper;
        this.categoryMapper = categoryMapper;
        this.itemMapper = itemMapper;
        this.orderMapper = orderMapper;
        this.reviewMapper = reviewMapper;
        this.logMapper = logMapper;
        this.addressMapper = addressMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Long count = userMapper.selectCount(null);
        if (count != null && count > 0) {
            return;
        }

        String pwd = passwordEncoder.encode("123456789");

        SysUser admin = user("admin", pwd, "平台管理员", "13900000001", "ADMIN");
        SysUser user1 = user("user", pwd, "张伟", "13800001111", "USER");
        SysUser user2 = user("lina", pwd, "李娜", "13800002222", "USER");
        SysUser staffUser1 = user("staff", pwd, "王秀兰", "13700003333", "STAFF");
        SysUser staffUser2 = user("chenjg", pwd, "陈建国", "13700004444", "STAFF");
        SysUser staffUser3 = user("liuhui", pwd, "刘慧", "13700005555", "STAFF");
        SysUser staffUser4 = user("zhaocm", pwd, "赵春梅", "13700006666", "STAFF");
        SysUser staffUser5 = user("zhoumin", pwd, "周敏", "13700007777", "STAFF");

        Staff staff1 = staff(staffUser1.getId(), "保洁", "金牌", 2, "8 年家政经验，擅长深度保洁与新房开荒，服务过 600+ 家庭。");
        Staff staff2 = staff(staffUser2.getId(), "维修", "中级", 0, "持证水电工，擅长管道疏通、水电改造与小家电维修。");
        Staff staff3 = staff(staffUser3.getId(), "母婴护理", "金牌", 0, "高级母婴护理师，持育婴师证，擅长新生儿护理与产后餐。");
        Staff staff4 = staff(staffUser4.getId(), "养老陪护", "中级", 0, "5 年居家照护经验，耐心细致，可提供陪诊与助浴服务。");
        Staff staff5 = staff(staffUser5.getId(), "收纳整理", "中级", 0, "持证整理师，擅长小户型收纳规划与衣橱改造。");

        ServiceCategory c1 = category("日常保洁", "🧹", "全屋清扫、厨卫除垢，让家清爽如新", 1);
        ServiceCategory c2 = category("家电清洗", "❄️", "空调、油烟机、洗衣机专业拆洗", 2);
        ServiceCategory c3 = category("母婴护理", "👶", "月嫂、育儿嫂、产后护理到家", 3);
        ServiceCategory c4 = category("家居维修", "🔧", "水电、管道、门窗快速上门", 4);
        ServiceCategory c5 = category("养老陪护", "👵", "居家照护、陪诊、助浴", 5);
        ServiceCategory c6 = category("收纳整理", "📦", "衣橱整理、全屋空间规划", 6);

        List<ServiceItem> services = new ArrayList<>();
        services.add(item(c1.getId(), "日常保洁 3 小时", "自带专业工具与环保清洁剂，适合 90㎡ 以内", "129", "次", 180, "🧽",
                "服务内容：客厅卧室除尘、地面清洁、厨房台面清洁、卫生间洁具清洁。\n服务标准：2 名服务人员上门，自带吸尘器、拖把及环保清洁剂。\n温馨提示：不含家电内部拆洗与高空外墙作业。",
                1286, "4.9"));
        services.add(item(c1.getId(), "深度保洁·全屋", "四小时深度清洁，厨卫重点除垢", "299", "次", 240, "🧴",
                "在基础保洁之上增加：厨房油污分解、卫生间霉斑处理、门窗轨道清理、踢脚线与开关面板擦拭。适合季度大扫除或入住前清洁。",
                642, "4.8"));
        services.add(item(c1.getId(), "厨房油污深度清洁", "灶台、油烟机表面、瓷砖缝隙除油", "199", "次", 150, "🍳",
                "针对中式厨房重油污，使用食品级除油剂，重点处理灶台、墙面瓷砖、橱柜表面与水槽。",
                438, "4.7"));
        services.add(item(c2.getId(), "空调挂机清洗", "拆洗滤网与蒸发器，除菌去味", "99", "台", 60, "❄️",
                "含内机外壳、滤网、蒸发器清洗及排水管疏通，清洗后现场测试制冷效果。壁挂机单台价，柜机另议。",
                905, "4.9"));
        services.add(item(c2.getId(), "油烟机深度拆洗", "风轮拆洗 + 油杯除油，恢复吸力", "169", "台", 90, "🌀",
                "拆洗风轮、油网、油杯与机身，使用高温蒸汽配合专用除油剂，清洗后复原并试机。",
                521, "4.8"));
        services.add(item(c2.getId(), "滚筒洗衣机清洗", "内筒深度除垢，去除异味", "129", "台", 60, "🧺",
                "拆洗内筒、门封圈、过滤器与洗涤剂盒，使用食品级清洗剂，适合使用满一年的洗衣机。",
                377, "4.8"));
        services.add(item(c3.getId(), "金牌月嫂 · 26 天", "新生儿护理 + 产妇月子餐", "12800", "月", 1560, "👶",
                "服务内容：新生儿喂养、洗澡抚触、黄疸观察、产妇伤口护理、月子餐制作、母婴房清洁。\n服务人员均持母婴护理师证，接受背景核查。",
                86, "5.0"));
        services.add(item(c3.getId(), "白班育儿嫂", "8 小时制，辅食与早教陪伴", "260", "天", 480, "🍼",
                "负责宝宝日常照料、辅食制作、衣物清洗与适龄早教游戏，工作时间 8:00-18:00。",
                142, "4.9"));
        services.add(item(c4.getId(), "水电维修上门", "跳闸、漏水、灯具更换", "80", "次", 60, "🔧",
                "上门检测费 80 元，维修材料另计。可处理跳闸排查、水龙头更换、灯具开关更换、下水管漏水等问题。",
                741, "4.8"));
        services.add(item(c4.getId(), "马桶/管道疏通", "专业工具疏通，不通不收费", "120", "次", 45, "🚽",
                "使用电动疏通簧与高压疏通设备，处理马桶、地漏、厨房下水堵塞。疏通不成功不收费。",
                623, "4.7"));
        services.add(item(c5.getId(), "老人居家陪护", "生活照料、用药提醒、康复陪伴", "45", "小时", 60, "👵",
                "服务内容：协助进食、翻身拍背、用药提醒、陪同散步、简单康复训练。护工均持健康证与护理培训证书。",
                318, "4.9"));
        services.add(item(c5.getId(), "医院陪诊服务", "挂号、取药、检查陪同", "199", "次", 240, "🏥",
                "半天陪诊：协助挂号、排队、取报告、取药，全程陪同并反馈就诊情况。不含交通与医疗费用。",
                205, "4.9"));
        services.add(item(c6.getId(), "衣橱收纳整理", "分类、折叠、空间重新规划", "299", "次", 180, "📦",
                "整理师上门，完成衣物分类、断舍离建议、折叠收纳与衣橱空间规划，可提供收纳用品清单。",
                176, "4.8"));
        services.add(item(c6.getId(), "全屋收纳规划", "储物空间诊断 + 落地方案", "599", "次", 300, "🏠",
                "针对全屋储物空间做诊断，输出分区方案并现场落地，适合搬家前后与新居入住。",
                94, "4.9"));
        services.add(item(c1.getId(), "新居开荒保洁", "装修后除尘除胶，入住前深度清洁", "499", "次", 300, "🏠",
                "针对新装修房屋，清除浮尘、涂料点、胶印与异味，含全屋玻璃内侧清洁。\n建议入住前 3 天预约，服务时长约 5 小时。",
                36, "4.9"));

        address(user1.getId(), "张伟", "13800001111", "阳光花园小区 3 号楼", "2 单元 501", 1);
        address(user1.getId(), "张伟", "13800001111", "阳光花园小区 1 号楼", "1 单元 202（父母家）", 0);
        address(user2.getId(), "李娜", "13800002222", "翠湖天地 7 号楼", "3 单元 1102", 1);

        LocalDateTime now = LocalDateTime.now();

        ServiceOrder o1 = order(user1.getId(), services.get(0), staff1.getId(), OrderStatus.COMPLETED,
                now.minusDays(4), "09:00-12:00", 1, "第一次体验，麻烦重点清理厨房，谢谢。");
        ServiceOrder o2 = order(user1.getId(), services.get(3), staff1.getId(), OrderStatus.PENDING_CONFIRM,
                now.minusDays(1), "14:00-15:00", 2, "两台挂机都需要清洗。");
        ServiceOrder o3 = order(user2.getId(), services.get(10), staff4.getId(), OrderStatus.SERVING,
                now.minusHours(2), "15:00-17:00", 2, "老人行动不便，需要协助上下床。");
        ServiceOrder o4 = order(user2.getId(), services.get(7), null, OrderStatus.PENDING_ACCEPT,
                now.minusHours(1), "明天 08:00-18:00", 1, "宝宝 3 个月，希望阿姨有早教经验。");
        ServiceOrder o5 = order(user1.getId(), services.get(1), null, OrderStatus.CANCELLED,
                now.minusDays(6), "10:00-14:00", 1, "临时有事，改天再约。");

        log(o1.getId(), null, OrderStatus.PENDING_ACCEPT.name(), "张伟", "提交预约", now.minusDays(4));
        log(o1.getId(), OrderStatus.PENDING_ACCEPT.name(), OrderStatus.ACCEPTED.name(), "王秀兰", "服务人员接单", now.minusDays(4).plusMinutes(6));
        log(o1.getId(), OrderStatus.ACCEPTED.name(), OrderStatus.SERVING.name(), "王秀兰", "开始服务", now.minusDays(4).plusHours(1));
        log(o1.getId(), OrderStatus.SERVING.name(), OrderStatus.PENDING_CONFIRM.name(), "王秀兰", "服务人员提交完工", now.minusDays(4).plusHours(4));
        log(o1.getId(), OrderStatus.PENDING_CONFIRM.name(), OrderStatus.COMPLETED.name(), "张伟", "用户确认完成", now.minusDays(4).plusHours(5));

        log(o2.getId(), null, OrderStatus.PENDING_ACCEPT.name(), "张伟", "提交预约", now.minusDays(1));
        log(o2.getId(), OrderStatus.PENDING_ACCEPT.name(), OrderStatus.ACCEPTED.name(), "王秀兰", "服务人员接单", now.minusDays(1).plusMinutes(9));
        log(o2.getId(), OrderStatus.ACCEPTED.name(), OrderStatus.SERVING.name(), "王秀兰", "开始服务", now.minusHours(4));
        log(o2.getId(), OrderStatus.SERVING.name(), OrderStatus.PENDING_CONFIRM.name(), "王秀兰", "服务人员提交完工", now.minusHours(1));

        log(o3.getId(), null, OrderStatus.PENDING_ACCEPT.name(), "李娜", "提交预约", now.minusHours(2));
        log(o3.getId(), OrderStatus.PENDING_ACCEPT.name(), OrderStatus.ACCEPTED.name(), "赵春梅", "服务人员接单", now.minusHours(2).plusMinutes(3));
        log(o3.getId(), OrderStatus.ACCEPTED.name(), OrderStatus.SERVING.name(), "赵春梅", "开始服务", now.minusHours(1));

        log(o4.getId(), null, OrderStatus.PENDING_ACCEPT.name(), "李娜", "提交预约", now.minusHours(1));
        log(o5.getId(), null, OrderStatus.PENDING_ACCEPT.name(), "张伟", "提交预约", now.minusDays(6));
        log(o5.getId(), OrderStatus.PENDING_ACCEPT.name(), OrderStatus.CANCELLED.name(), "张伟", "取消订单", now.minusDays(6).plusMinutes(20));

        OrderReview review = new OrderReview();
        review.setOrderId(o1.getId());
        review.setUserId(user1.getId());
        review.setStaffId(staff1.getId());
        review.setServiceId(services.get(0).getId());
        review.setRating(5);
        review.setContent("阿姨很准时，厨房和卫生间收拾得特别干净，工具也很齐全，下次还约。");
        review.setCreatedAt(now.minusDays(3));
        reviewMapper.insert(review);

        System.out.println("[init] demo data ready -> accounts: admin / user / staff, password: 123456789");
    }

    // ==================== 构造辅助 ====================

    private SysUser user(String username, String pwd, String realName, String phone, String role) {
        SysUser u = new SysUser();
        u.setUsername(username);
        u.setPassword(pwd);
        u.setRealName(realName);
        u.setPhone(phone);
        u.setRole(role);
        u.setStatus(1);
        u.setCreatedAt(LocalDateTime.now().minusDays(30));
        userMapper.insert(u);
        return u;
    }

    private Staff staff(Long userId, String jobType, String level, int orderCount, String intro) {
        Staff s = new Staff();
        s.setUserId(userId);
        s.setJobType(jobType);
        s.setStaffLevel(level);
        s.setRating(new BigDecimal("4.9"));
        s.setOrderCount(orderCount);
        s.setAvailable(1);
        s.setIntro(intro);
        s.setCreatedAt(LocalDateTime.now().minusDays(60));
        staffMapper.insert(s);
        return s;
    }

    private ServiceCategory category(String name, String icon, String desc, int sort) {
        ServiceCategory c = new ServiceCategory();
        c.setName(name);
        c.setIcon(icon);
        c.setDescription(desc);
        c.setSortNo(sort);
        c.setStatus(1);
        c.setCreatedAt(LocalDateTime.now().minusDays(90));
        categoryMapper.insert(c);
        return c;
    }

    private ServiceItem item(Long categoryId, String name, String subtitle, String price, String unit,
                             int duration, String cover, String desc, int sales, String rating) {
        ServiceItem i = new ServiceItem();
        i.setCategoryId(categoryId);
        i.setName(name);
        i.setSubtitle(subtitle);
        i.setPrice(new BigDecimal(price));
        i.setPriceUnit(unit);
        i.setDuration(duration);
        i.setCover(cover);
        i.setDescription(desc);
        i.setSales(sales);
        i.setRating(new BigDecimal(rating));
        i.setStatus(1);
        i.setCreatedAt(LocalDateTime.now().minusDays(90));
        itemMapper.insert(i);
        return i;
    }

    private ServiceOrder order(Long userId, ServiceItem item, Long staffId, OrderStatus status,
                               LocalDateTime createdAt, String time, int quantity, String remark) {
        ServiceOrder o = new ServiceOrder();
        o.setOrderNo("HS" + createdAt.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                + String.format("%04d", (int) (Math.random() * 10000)));
        o.setUserId(userId);
        o.setServiceId(item.getId());
        o.setStaffId(staffId);
        o.setServiceName(item.getName());
        o.setServicePrice(item.getPrice());
        o.setQuantity(quantity);
        o.setAmount(item.getPrice().multiply(BigDecimal.valueOf(quantity)));
        o.setAppointmentDate(createdAt.toLocalDate().plusDays(1));
        o.setAppointmentTime(time);

        SysUser customer = userMapper.selectById(userId);
        CustomerAddress addr = addressMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CustomerAddress>()
                .eq(CustomerAddress::getUserId, userId)
                .orderByDesc(CustomerAddress::getIsDefault)
                .last("limit 1"));
        o.setContactName(customer == null ? "社区用户" : customer.getRealName());
        o.setContactPhone(customer == null ? "13800000000" : customer.getPhone());
        o.setAddress(addr == null ? "阳光花园小区 3 号楼 2 单元 501" : addr.fullAddress());
        o.setRemark(remark);
        o.setStatus(status.name());
        o.setCreatedAt(createdAt);
        if (staffId != null) {
            o.setAcceptedAt(createdAt.plusMinutes(10));
        }
        if (status == OrderStatus.SERVING || status == OrderStatus.PENDING_CONFIRM || status == OrderStatus.COMPLETED) {
            o.setStartedAt(createdAt.plusHours(1));
        }
        if (status == OrderStatus.PENDING_CONFIRM || status == OrderStatus.COMPLETED) {
            o.setFinishedAt(createdAt.plusHours(4));
        }
        if (status == OrderStatus.CANCELLED) {
            o.setCancelledAt(createdAt.plusMinutes(20));
            o.setCancelReason("用户主动取消");
        }
        orderMapper.insert(o);
        return o;
    }

    private void log(Long orderId, String from, String to, String operator, String remark, LocalDateTime time) {
        OrderStatusLog l = new OrderStatusLog();
        l.setOrderId(orderId);
        l.setFromStatus(from);
        l.setToStatus(to);
        l.setOperator(operator);
        l.setRemark(remark);
        l.setCreatedAt(time);
        logMapper.insert(l);
    }

    private void address(Long userId, String name, String phone, String region, String detail, int isDefault) {
        CustomerAddress a = new CustomerAddress();
        a.setUserId(userId);
        a.setContactName(name);
        a.setContactPhone(phone);
        a.setRegion(region);
        a.setDetail(detail);
        a.setIsDefault(isDefault);
        a.setCreatedAt(LocalDateTime.now().minusDays(20));
        addressMapper.insert(a);
    }
}
