// 订单管理页
// 当前下单请求号：每次打开弹窗生成一次，同一个弹窗多次提交用同一个号，防止重复下单
  let currentRequestNo = '';

    function genRequestNo() {
    // 优先用浏览器自带的 UUID，兼容旧浏览器用时间戳+随机数兜底
    if (window.crypto && crypto.randomUUID) return crypto.randomUUID();
    return Date.now() + '-' + Math.random().toString(16).slice(2); }
async function loadOrders() {
  const list = await request('/orders');
  const tbody = document.getElementById('order-tbody');
  if (!list.length) {
    tbody.innerHTML = '<tr><td colspan="5" class="empty">暂无订单，点击右上角创建订单</td></tr>';
    return;
  }
  tbody.innerHTML = list.map(o => {
    const actions = o.status === 0
      ? `<button class="btn btn-sm btn-success" onclick="payOrder(${o.id})">支付</button>
         <button class="btn btn-sm btn-danger-ghost" onclick="cancelOrder(${o.id})">取消</button>`
      : '<span class="muted">-</span>';
    return `
    <tr>
      <td>${o.orderNo}</td>
      <td>${fmtMoney(o.totalAmount)}</td>
      <td>${statusBadge(o.status)}</td>
      <td class="muted">${o.createTime || '-'}</td>
      <td>
        <button class="btn btn-sm btn-ghost" onclick="showDetail(${o.id})">详情</button>
        ${actions}
      </td>
    </tr>`;
  }).join('');
}

// ---- 创建订单 ----
async function openCreateModal() {
  const list = await request('/products');
  const box = document.getElementById('create-products');
  if (!list.length) {
    box.innerHTML = '<div class="empty">暂无商品，请先到商品管理页新增</div>';
  } else {
    box.innerHTML = list.map(p => `
      <div class="pick-row">
        <div class="pick-info">
          <div class="pick-name">${p.name}</div>
          <div class="pick-meta">${fmtMoney(p.price)} · 库存 ${p.stock}</div>
        </div>
        <input type="number" min="0" max="${p.stock}" placeholder="0" id="qty-${p.id}">
      </div>`).join('');
  }
  currentRequestNo = genRequestNo();   // 每次打开弹窗 = 一次新的下单意图，生成新请求号
      openModal('create-modal');
}

async function submitCreate() {
    const items = [];
    document.querySelectorAll('#create-products input[type=number]').forEach(input => {
      const qty = Number(input.value);
      if (qty > 0) items.push({ productId: Number(input.id.replace('qty-', '')), quantity: qty });
    });
    if (!items.length) return showToast('请至少填写一件商品的数量', 'error');

    // 防重复提交：提交期间禁用按钮，防止连点
    const btn = document.getElementById('submit-order-btn');
    btn.disabled = true;
    try {
      await request('/orders', {
        method: 'POST',
        body: JSON.stringify({ requestNo: currentRequestNo, items }),   // 把请求号一起发出去
      });
      closeModal('create-modal');
      showToast('下单成功！');
      loadOrders();
    } catch (e) {
      showToast(e.message, 'error');
    } finally {
      btn.disabled = false;   // 无论成功失败，都恢复按钮
    }
  }

// ---- 订单详情 ----
async function showDetail(id) {
  try {
    const o = await request(`/orders/${id}`);
    document.getElementById('detail-order-no').textContent = o.orderNo;
    document.getElementById('detail-status').innerHTML = statusBadge(o.status);
    document.getElementById('detail-amount').textContent = fmtMoney(o.totalAmount);
    document.getElementById('detail-time').textContent = o.createTime || '-';
    document.getElementById('detail-items').innerHTML = o.items.map(i => `
      <tr>
        <td>${i.productName}</td>
        <td>${fmtMoney(i.price)}</td>
        <td>${i.quantity}</td>
        <td>${fmtMoney(i.totalPrice)}</td>
      </tr>`).join('');
    openModal('detail-modal');
  } catch (e) {
    showToast(e.message, 'error');
  }
}

// ---- 支付 / 取消 ----
async function payOrder(id) {
  try {
    await request(`/orders/${id}/pay`, { method: 'POST' });
    showToast('支付成功');
    loadOrders();
  } catch (e) {
    showToast(e.message, 'error');
  }
}

async function cancelOrder(id) {
  if (!confirm('确定取消该订单吗？取消后会回补库存')) return;
  try {
    await request(`/orders/${id}/cancel`, { method: 'POST' });
    showToast('订单已取消，库存已回补');
    loadOrders();
  } catch (e) {
    showToast(e.message, 'error');
  }
}

loadOrders().catch(e => showToast(e.message, 'error'));
