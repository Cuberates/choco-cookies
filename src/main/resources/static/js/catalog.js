(() => {
  'use strict';
  const search = document.getElementById('ball-name');
  const results = document.getElementById('catalog-results');
  const dialog = document.getElementById('info-dialog');
  const content = document.getElementById('dialog-content');
  const storageKey = 'lane-index-arsenal-v1';
  let saved = [];
  let toastTimer;
  try {
    const value = JSON.parse(localStorage.getItem(storageKey) || '[]');
    if (Array.isArray(value)) saved = value.filter(item => item && typeof item.name === 'string' && typeof item.brand === 'string');
  } catch { /* The catalog remains usable when browser storage is unavailable. */ }
  const productKey = product => `${product.brand}\u0000${product.name}`;
  const productFromCard = card => ({
    name: card.dataset.productName, brand: card.dataset.brand || 'Unspecified',
    cover: card.dataset.cover || 'Unspecified', core: card.dataset.core || 'Unspecified',
    rg: card.dataset.rg || 'Unspecified', diff: card.dataset.diff || 'Unspecified',
    finish: card.dataset.finish || 'Unspecified'
  });
  function toast(message) {
    const element = document.getElementById('toast');
    clearTimeout(toastTimer); element.textContent = message; element.hidden = false;
    toastTimer = setTimeout(() => { element.hidden = true; }, 4000);
  }
  function updateSavedButtons() {
    document.querySelectorAll('[data-save]').forEach(button => {
      const product = productFromCard(button.closest('[data-product-name]'));
      const selected = saved.some(item => productKey(item) === productKey(product));
      button.setAttribute('aria-pressed', String(selected));
      button.setAttribute('aria-label', `${selected ? 'Remove' : 'Save'} ${product.name} ${selected ? 'from' : 'to'} your arsenal`);
      if (!button.querySelector('img')) button.textContent = selected ? 'Saved' : 'Save';
    });
  }
  function focusSearch() { search.scrollIntoView({ block: 'center' }); search.focus(); }
  document.querySelectorAll('[data-focus-search]').forEach(button => button.addEventListener('click', focusSearch));
  document.querySelectorAll('[data-focus-brand]').forEach(button => button.addEventListener('click', () => {
    document.querySelector('.filter-panel').open = true;
    const brand = document.querySelector('input[name=brand]');
    brand.scrollIntoView({ block: 'center' }); brand.focus();
  }));
  document.addEventListener('keydown', event => {
    if ((event.metaKey || event.ctrlKey) && event.key.toLowerCase() === 'k') {
      event.preventDefault(); if (dialog.open) dialog.close(); focusSearch();
    }
  });
  document.querySelectorAll('[data-open-catalog]').forEach(link => link.addEventListener('click', () => { results.open = true; }));
  if (results.open) requestAnimationFrame(() => results.scrollIntoView({ block: 'start' }));
  const row = document.getElementById('featured-balls');
  document.querySelectorAll('[data-carousel]').forEach(button => button.addEventListener('click', () => {
    if (button.dataset.carousel === 'next') row.append(row.firstElementChild);
    else row.prepend(row.lastElementChild);
    row.scrollLeft = 0;
  }));
  document.querySelectorAll('[data-save]').forEach(button => button.addEventListener('click', () => {
    const product = productFromCard(button.closest('[data-product-name]'));
    const index = saved.findIndex(item => productKey(item) === productKey(product));
    if (index < 0) saved.push(product); else saved.splice(index, 1);
    try { localStorage.setItem(storageKey, JSON.stringify(saved)); }
    catch { toast('Saved for this visit. Your browser has disabled persistent storage.'); updateSavedButtons(); return; }
    updateSavedButtons(); toast(index < 0 ? `${product.name} saved to your arsenal.` : `${product.name} removed from your arsenal.`);
  }));
  const panels = {
    'sign-in': ['Sign in', 'Accounts are not available yet. You can browse, search, and save an arsenal in this browser without signing in.'],
    guides: ['Bowling ball guides', 'Coverstock affects how the ball responds to friction. Solid, pearl, and hybrid describe common reactive coverstock types.', 'Core type describes the distribution of mass inside a ball. Compare symmetric and asymmetric cores alongside coverstock and available weights.', 'Use the catalog filters to narrow your choices, then save balls to compare their specifications.'],
    glossary: ['Reaction glossary', 'RG (radius of gyration) describes the distribution of mass around a ball’s axis. Differential is the difference between its maximum and minimum RG.', 'Continuous describes a smooth, sustained direction change; angular describes a more pronounced change in direction downlane.'],
    surface: ['Surface guide', 'Surface finish influences when a bowling ball encounters friction. Lower grit finishes generally create earlier traction; smoother or polished finishes typically delay that response.', 'Surface can change with use and maintenance. Check the manufacturer’s recommendations before changing your ball’s finish.'],
    data: ['Our data', 'The searchable catalog contains records imported from Bowwwl. Each record links back to its source. Missing attributes are shown as Unspecified.', 'The three featured cards and hero are editorial content from the supplied Figma design. Catalog totals reflect the actual database. Imports run on demand when you search for a missing ball using its brand and full name.'],
    corrections: ['Corrections', 'Open a ball’s Bowwwl source link to check its details. A catalog administrator can refresh a record using the single-ball import command.'],
    releases: ['Release notes', 'The catalog supports search, combined filters, pagination, and single-ball Bowwwl lookup. This edition adds the Bowler’s Journal design, featured cards, and an arsenal saved in your browser.'],
    contact: ['Contact', 'A contact address has not been configured for Bowler’s Journal yet. For source information, use the Bowwwl link on each catalog record.'],
    privacy: ['Privacy', 'Saved arsenal selections stay in this browser’s local storage. This screen does not send your email to a mailing service or create an account.', 'Catalog search terms are sent to this application. When you look up a missing ball, its constructed URL is requested from Bowwwl.'],
    terms: ['Terms', 'Catalog information is provided for reference. Check manufacturer specifications and source details before making equipment decisions.'],
    accessibility: ['Accessibility', 'Use Tab to move between controls and Enter or Space to activate buttons. Press Control K or Command K to focus search. Dialogs can be closed with Escape.', 'The page supports keyboard navigation, visible focus indicators, labeled controls, responsive layouts, and reduced motion.'],
    updates: ['Email updates', 'Email updates are not available yet. Your address has not been submitted or saved. You can browse the catalog for equipment details at any time.']
  };
  function paragraph(text) { const p = document.createElement('p'); p.textContent = text; content.append(p); }
  function showPanel(panel) {
    content.replaceChildren();
    if (panel === 'arsenal' || panel === 'compare') {
      document.getElementById('dialog-title').textContent = panel === 'compare' ? 'Compare your arsenal' : 'Your arsenal';
      if (!saved.length) paragraph('Save a ball using its bookmark or Save button, then come back here to view and compare your selections.');
      else {
        paragraph('Your selections are saved in this browser. Unspecified fields are not available in the imported record.');
        const wrapper = document.createElement('div'); wrapper.className = 'table-scroll';
        const table = document.createElement('table');
        const header = table.createTHead().insertRow();
        const columns = ['Ball', 'Brand', 'Cover', 'Core', 'RG', 'Diff', 'Finish'];
        columns.forEach(label => { const th = document.createElement('th'); th.scope = 'col'; th.textContent = label; header.append(th); });
        const body = table.createTBody();
        saved.forEach(product => {
          const tr = body.insertRow();
          ['name', 'brand', 'cover', 'core', 'rg', 'diff', 'finish'].forEach(key => { tr.insertCell().textContent = product[key] || 'Unspecified'; });
        });
        wrapper.append(table); content.append(wrapper);
        const clear = document.createElement('button'); clear.className = 'secondary-button'; clear.textContent = 'Clear arsenal';
        clear.addEventListener('click', () => {
          saved = []; try { localStorage.removeItem(storageKey); } catch { /* Session state still clears. */ }
          updateSavedButtons(); showPanel(panel);
        }); content.append(clear);
      }
    } else {
      const [title, ...paragraphs] = panels[panel] || panels.data;
      document.getElementById('dialog-title').textContent = title;
      paragraphs.forEach(paragraph);
    }
    if (!dialog.open) dialog.showModal();
  }
  document.querySelectorAll('[data-panel]').forEach(button => button.addEventListener('click', () => showPanel(button.dataset.panel)));
  document.getElementById('close-dialog').addEventListener('click', () => dialog.close());
  dialog.addEventListener('click', event => {
    const rect = dialog.getBoundingClientRect();
    if (event.target === dialog && (event.clientX < rect.left || event.clientX > rect.right || event.clientY < rect.top || event.clientY > rect.bottom)) dialog.close();
  });
  document.getElementById('ball-search').addEventListener('submit', () => {
    const button = document.querySelector('.search-bar button'); button.disabled = true;
    const label = button.querySelector('span'); label.textContent = 'Searching…';
  });
  window.addEventListener('pageshow', () => {
    const button = document.querySelector('.search-bar button'); button.disabled = false;
    button.querySelector('span').textContent = 'Search database';
  });
  updateSavedButtons();
})();
