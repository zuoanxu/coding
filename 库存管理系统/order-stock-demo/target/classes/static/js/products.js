// 商品管理页
let products = [];
let stockProductId = null;
let stockMode = 'inc'; // inc 增加 / dec 减少

async function loadProducts() {
  products = await request('/products');
  const tbody = document.getElementById('product-tbody');
  if (!products.length) {
    tbody.innerHTML = '<tr><td colspan="7" class="empty">暂无商品，点击右上角新增</td></tr>';
    return;
  }
  tbody.innerHTML = products.map(p => `
    <tr>
      <td>${p.id}</td>
      <td class="cell-name">${p.name}</td>
      <td>${fmtMoney(p.price)}</td>
      <td>${stockCell(p.stock)}</td>
      <td class="cell-desc">${p.description || '-'}</td>
      <td class="cell-desc">${p.remark || '-'}</td>
      <td>
        <button class="btn btn-sm btn-ghost" onclick="openStockModal(${p.id})">调整库存</button>
        <button class="btn btn-sm btn-danger-ghost" onclick="delProduct(${p.id})">删除</button>
      </td>
    </tr>`).join('');
}

function stockCell(stock) {
  if (stock <= 0) return '<span class="badge badge-danger">缺货</span>';
  if (stock <= 10) return `<span class="badge badge-warning">${stock}（低库存）</span>`;
  return `<span class="stock-num">${stock}</span>`;
}

// ---- 新增商品 ----
function openAddModal() { openModal('add-modal'); }

async function submitAdd() {
  const name = document.getElementById('add-name').value.trim();
  const price = document.getElementById('add-price').value;
  const stock = document.getElementById('add-stock').value;
  const description = document.getElementById('add-desc').value.trim();
  const remark = document.getElementById('add-remark').value.trim();
  if (!name) return showToast('请输入商品名称', 'error');
  if (price === '') return showToast('请输入价格', 'error');

  try {
    await request('/products', {
      method: 'POST',
      body: JSON.stringify({ name, price: Number(price), stock: Number(stock || 0), description , remark}),
    });
    closeModal('add-modal');
    showToast('新增商品成功');
    ['add-name', 'add-price', 'add-stock', 'add-desc','add-remark'].forEach(id => document.getElementById(id).value = '');
    loadProducts();
  } catch (e) {
    showToast(e.message, 'error');
  }
}

// ---- 调整库存 ----
function openStockModal(id) {
  const p = products.find(x => x.id === id);
  if (!p) return;
  stockProductId = id;
  document.getElementById('stock-product-name').textContent = `${p.name}（当前库存 ${p.stock}）`;
  document.getElementById('stock-delta').value = '';
  setStockMode('inc');
  openModal('stock-modal');
}

function setStockMode(mode) {
  stockMode = mode;
  document.querySelectorAll('.seg-btn').forEach(b => b.classList.toggle('active', b.dataset.mode === mode));
}

async function submitStock() {
  const delta = Number(document.getElementById('stock-delta').value);
  if (!delta || delta <= 0) return showToast('请输入正确的数量', 'error');
  const realDelta = stockMode === 'inc' ? delta : -delta;
  try {
    await request(`/products/${stockProductId}/stock`, {
      method: 'PUT',
      body: JSON.stringify({ delta: realDelta }),
    });
    closeModal('stock-modal');
    showToast('库存调整成功');
    loadProducts();
  } catch (e) {
    showToast(e.message, 'error');
  }
}

// ---- 删除 ----
async function delProduct(id) {
  if (!confirm('确定删除该商品吗？')) return;
  try {
    await request(`/products/${id}`, { method: 'DELETE' });
    showToast('删除成功');
    loadProducts();
  } catch (e) {
    showToast(e.message, 'error');
  }
}
// ---- 按关键字搜索 ----
  async function searchProducts() {
    const keyword = document.getElementById('search-keyword').value.trim();
    try {
      products = await request(`/products/search?keyword=${encodeURIComponent(keyword)}`);
      const tbody = document.getElementById('product-tbody');
      if (!products.length) {
        tbody.innerHTML = '<tr><td colspan="7" class="empty">没有找到匹配的商品</td></tr>';
        return;
      }
      tbody.innerHTML = products.map(p => `
        <tr>
          <td>${p.id}</td>
          <td class="cell-name">${p.name}</td>
          <td>${fmtMoney(p.price)}</td>
          <td>${stockCell(p.stock)}</td>
          <td class="cell-desc">${p.description || '-'}</td>
          <td class="cell-desc">${p.remark || '-'}</td>
          <td>
            <button class="btn btn-sm btn-ghost" onclick="openStockModal(${p.id})">调整库存</button>
            <button class="btn btn-sm btn-danger-ghost" onclick="delProduct(${p.id})">删除</button>
          </td>
        </tr>`).join('');
    } catch (e) {
      showToast(e.message, 'error');
    }
  }
loadProducts().catch(e => showToast(e.message, 'error'));
