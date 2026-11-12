(function () {
    'use strict';

    // 顶部导航高亮
    var path = window.location.pathname;
    document.querySelectorAll('.nav-link[data-nav]').forEach(function (link) {
        var target = link.getAttribute('data-nav');
        var matched = target === '/' ? path === '/' : path.indexOf(target) === 0;
        if (matched) {
            link.classList.add('active');
        }
    });

    // 危险操作二次确认 + 提交后按钮防抖
    document.addEventListener('submit', function (event) {
        var form = event.target;
        if (!(form instanceof HTMLFormElement)) {
            return;
        }
        var message = form.getAttribute('data-confirm');
        if (message && !window.confirm(message)) {
            event.preventDefault();
            return;
        }
        var btn = form.querySelector('button[type="submit"]');
        if (btn && !btn.disabled) {
            window.setTimeout(function () {
                btn.disabled = true;
            }, 0);
        }
    });

    // 预约页：选择地址簿后自动回填联系人信息
    var picker = document.getElementById('addressPicker');
    if (picker) {
        picker.addEventListener('change', function () {
            var option = picker.options[picker.selectedIndex];
            if (!option || !option.getAttribute('data-name')) {
                return;
            }
            var name = document.getElementById('contactName');
            var phone = document.getElementById('contactPhone');
            var region = document.getElementById('region');
            var detail = document.getElementById('detail');
            if (name) name.value = option.getAttribute('data-name');
            if (phone) phone.value = option.getAttribute('data-phone');
            if (region) region.value = option.getAttribute('data-region');
            if (detail) detail.value = option.getAttribute('data-detail');
        });
        // 首次进入时同步一次，避免浏览器回填与显示不一致
        picker.dispatchEvent(new Event('change'));
    }

    // 管理端：表格筛选表单自动提交
    document.querySelectorAll('[data-auto-submit]').forEach(function (el) {
        el.addEventListener('change', function () {
            if (el.form) {
                el.form.submit();
            }
        });
    });
})();
