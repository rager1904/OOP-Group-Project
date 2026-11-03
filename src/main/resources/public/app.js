'use strict';
let products = [];
const $ = id => document.getElementById(id);
const money = ngwee => new Intl.NumberFormat('en-ZM', {style:'currency',currency:'ZMW'}).format(ngwee / 100);
function message(text, error = false) { $('message').textContent = text; $('message').className = error ? 'error' : ''; }
async function api(path, method = 'GET', body) {
  const response = await fetch('/api/' + path, {method, headers:body ? {'Content-Type':'application/json'} : {}, body:body ? JSON.stringify(body) : undefined});
  const text = await response.text();
  let data; try { data = JSON.parse(text); } catch { data = {message:text || 'Request failed.'}; }
  if (!response.ok) throw new Error(data.message || data.title || 'Request failed.');
  return data;
}
function cell(row, value) { const td = document.createElement('td'); td.textContent = value; row.append(td); return td; }
function renderProducts() {
  $('products').replaceChildren();
  const query = $('search').value.toLowerCase();
  for (const p of products.filter(p => p.name.toLowerCase().includes(query))) {
    const row = document.createElement('tr');
    [p.name, money(p.buyingPrice), money(p.sellingPrice), p.quantity].forEach(v => cell(row,v));
    const edit = document.createElement('button'); edit.textContent = 'Edit'; edit.type = 'button';
    edit.addEventListener('click', () => {
      const form = $('product-form');
      form.elements.id.value = p.id; form.elements.name.value = p.name;
      form.elements.buyingPrice.value = (p.buyingPrice / 100).toFixed(2);
      form.elements.sellingPrice.value = (p.sellingPrice / 100).toFixed(2);
      form.elements.name.focus();
    });
    cell(row,'').append(edit); $('products').append(row);
  }
  if (!$('products').children.length) { const row = document.createElement('tr'); cell(row,'No matching products. Add a product to get started.').colSpan = 5; $('products').append(row); }
}
async function report() {
  const params = new URLSearchParams(new FormData($('report-form')));
  const data = await api('reports?' + params);
  $('report').replaceChildren();
  for (const [label,key] of [['Sales revenue','revenue'],['Cost of goods sold','costOfGoodsSold'],['Estimated gross profit','grossProfit'],['Current stock cost','remainingStockCost'],['Current stock retail value','remainingStockRetailValue'],['Sales transactions','saleCount']]) {
    const box = document.createElement('div'); box.className = 'metric'; box.textContent = label;
    const value = document.createElement('strong'); value.textContent = key === 'saleCount' ? data[key] : money(data[key]); box.append(value); $('report').append(box);
  }
}
async function refresh() {
  const [items,purchases,sales] = await Promise.all([api('products'),api('purchases'),api('sales')]);
  products = items; renderProducts();
  const previous = $('product-select').value; $('product-select').replaceChildren();
  const placeholder = new Option('Select a product',''); $('product-select').append(placeholder);
  products.forEach(p => $('product-select').append(new Option(`${p.name} (${p.quantity} in stock)`, p.id)));
  $('product-select').value = products.some(p => p.id === previous) ? previous : '';
  $('history').replaceChildren();
  const history = [...purchases.map(p => ({...p,type:'Purchase',total:p.unitCost*p.quantity})),...sales.map(s => ({...s,type:'Sale',total:s.unitPrice*s.quantity}))].sort((a,b) => b.date.localeCompare(a.date));
  for (const item of history) { const row = document.createElement('tr'); [item.date,item.type,item.productName,item.quantity,money(item.total)].forEach(v => cell(row,v)); $('history').append(row); }
  if (!history.length) { const row = document.createElement('tr'); cell(row,'No transactions recorded.').colSpan = 5; $('history').append(row); }
  await report();
}
function submit(id, action) {
  $(id).addEventListener('submit', async event => {
    event.preventDefault(); const button = event.submitter; button.disabled = true;
    try { await action(event.target); message('Saved successfully.'); try { await refresh(); } catch(e) { message('Saved, but refresh failed. Reload the page to view current data.',true); } }
    catch(e) { message(e.message,true); } finally { button.disabled = false; }
  });
}
submit('product-form', async form => {
  const id = form.elements.id.value;
  await api('products' + (id ? '/' + encodeURIComponent(id) : ''), id ? 'PUT' : 'POST', {
    name:form.elements.name.value, buyingPrice:form.elements.buyingPrice.value, sellingPrice:form.elements.sellingPrice.value
  }); form.reset();
});
submit('movement-form', async form => { await api(form.elements.type.value,'POST',{productId:form.elements.productId.value,quantity:Number(form.elements.quantity.value)}); form.elements.quantity.value = ''; });
$('cancel').addEventListener('click', () => $('product-form').reset());
$('search').addEventListener('input',renderProducts);
$('report-form').addEventListener('submit', async event => { event.preventDefault(); try { await report(); message('Report updated.'); } catch(e) { message(e.message,true); } });
refresh().then(() => message('Ready. All amounts are in Zambian kwacha.')).catch(e => message(e.message,true));
