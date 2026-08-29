// 概览页：加载统计数据 + 最近订单
async function loadStats() {
  const s = await request('/stats');
  document.getElementById('stat-product').textContent = s.productCount;
  document.getElementById('stat-order').textContent = s.orderCount;
  document.getElementById('stat-sales').textContent = fmtMoney(s.totalSales);
  document.getElementById('stat-lowstock').textContent = s.lowStockCount;
}

async function loadRecentOrders() {
  const orders = await request('/orders');
  const tbody = document.getElementById('recent-orders');
  const recent = orders.slice(0, 10);
  if (!recent.length) {
    tbody.innerHTML = '<tr><td colspan="4" class="empty">暂无订单，去下单试试吧</td></tr>';
    return;
  }
  tbody.innerHTML = recent.map(o => `
    <tr>
      <td>${o.orderNo}</td>
      <td>${fmtMoney(o.totalAmount)}</td>
      <td>${statusBadge(o.status)}</td>
      <td class="muted">${o.createTime || '-'}</td>
    </tr>`).join('');
}

loadStats().catch(e => showToast(e.message, 'error'));
loadRecentOrders().catch(e => showToast(e.message, 'error'));
