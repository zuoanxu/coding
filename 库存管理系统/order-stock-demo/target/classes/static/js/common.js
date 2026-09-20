// ============ 通用工具 ============
const API_BASE = '/api';

// 统一请求封装：自动解析 JSON，非 200 时抛出错误
async function request(url, options = {}) {
  const res = await fetch(API_BASE + url, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  });
  const data = await res.json();
  if (data.code !== 200) {
    throw new Error(data.message || '请求失败');
  }
  return data.data;
}

// 轻提示
function showToast(message, type = 'success') {
  let container = document.getElementById('toast-container');
  if (!container) {
    container = document.createElement('div');
    container.id = 'toast-container';
    document.body.appendChild(container);
  }
  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  toast.textContent = message;
  container.appendChild(toast);
  requestAnimationFrame(() => toast.classList.add('show'));
  setTimeout(() => {
    toast.classList.remove('show');
    setTimeout(() => toast.remove(), 300);
  }, 2500);
}

// 金额格式化
function fmtMoney(n) {
  const num = Number(n || 0);
  return '¥' + num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

// 订单状态
const ORDER_STATUS = {
  0: { text: '待支付', cls: 'badge-warning' },
  1: { text: '已支付', cls: 'badge-success' },
  2: { text: '已取消', cls: 'badge-danger' },
};
function statusBadge(status) {
  const s = ORDER_STATUS[status] || { text: '未知', cls: 'badge-danger' };
  return `<span class="badge ${s.cls}">${s.text}</span>`;
}

// 模态框开关
function openModal(id) { document.getElementById(id).classList.add('open'); }
function closeModal(id) { document.getElementById(id).classList.remove('open'); }

// 点击遮罩关闭模态框
document.addEventListener('DOMContentLoaded', () => {
  document.querySelectorAll('.modal').forEach(m => {
    m.addEventListener('click', e => { if (e.target === m) m.classList.remove('open'); });
  });
  // 侧边栏高亮当前页
  const path = location.pathname.split('/').pop() || 'index.html';
  document.querySelectorAll('.nav a').forEach(a => {
    if (a.getAttribute('href') === path) a.classList.add('active');
  });
});
