(() => {
  'use strict';
  const track = document.getElementById('featured-balls');
  const viewport = track?.closest('.carousel-viewport');
  if (!viewport) return;
  const buttons = [...viewport.closest('.carousel').querySelectorAll('[data-carousel]')];
  const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
  let active = null;

  function updateControls(busy) {
    const unavailable = busy || track.children.length < 2;
    buttons.forEach(button => button.setAttribute('aria-disabled', String(unavailable)));
  }
  function rotate(direction) {
    if (direction === 'next') track.append(track.firstElementChild);
    else track.prepend(track.lastElementChild);
  }
  function settle() {
    if (!active) return;
    const slide = active;
    active = null;
    // Commit the same final order for completion, cancellation, and resize.
    slide.clone.remove();
    rotate(slide.direction);
    slide.animation?.cancel();
    viewport.scrollLeft = 0;
    viewport.classList.remove('is-sliding');
    updateControls(false);
  }
  function slide(direction) {
    if (active || track.children.length < 2) return;
    const gap = parseFloat(getComputedStyle(track).columnGap) || 0;
    const distance = track.firstElementChild.getBoundingClientRect().width + gap;
    if (distance <= 0) return;

    // Preserve the leading card after native swipe/scroll navigation.
    const offset = Math.min(track.children.length - 1, Math.max(0, Math.round(viewport.scrollLeft / distance)));
    viewport.classList.add('is-sliding');
    for (let index = 0; index < offset; index++) rotate('next');
    viewport.scrollLeft = 0;
    if (reducedMotion.matches || typeof track.animate !== 'function') {
      rotate(direction);
      viewport.classList.remove('is-sliding');
      updateControls(false);
      return;
    }

    const clone = (direction === 'next' ? track.firstElementChild : track.lastElementChild).cloneNode(true);
    clone.setAttribute('aria-hidden', 'true');
    clone.inert = true;
    clone.removeAttribute('id');
    clone.querySelectorAll('[id]').forEach(node => node.removeAttribute('id'));
    clone.querySelectorAll('a, button, input, select, textarea, [tabindex]').forEach(node => node.setAttribute('tabindex', '-1'));
    clone.classList.add('carousel-clone');
    if (direction === 'next') track.append(clone);
    else track.prepend(clone);
    active = { direction, clone, animation: null };
    updateControls(true);
    try {
      const animation = track.animate(
        direction === 'next'
          ? [{ transform: 'translateX(0)' }, { transform: `translateX(-${distance}px)` }]
          : [{ transform: `translateX(-${distance}px)` }, { transform: 'translateX(0)' }],
        { duration: 320, easing: 'cubic-bezier(0.22, 1, 0.36, 1)', fill: 'both' }
      );
      const current = active;
      current.animation = animation;
      const complete = () => { if (active === current) settle(); };
      animation.finished.then(complete, complete);
    } catch {
      settle();
    }
  }
  buttons.forEach(button => button.addEventListener('click', () => slide(button.dataset.carousel)));
  window.addEventListener('resize', settle);
  reducedMotion.addEventListener('change', settle);
  updateControls(false);
})();
