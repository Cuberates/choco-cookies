const { test } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { runInNewContext } = require('node:vm');
const { join } = require('node:path');

const source = readFileSync(join(__dirname, '../../main/resources/static/js/carousel.js'), 'utf8');

function element(name) {
  const attributes = new Map([['id', name]]);
  const classes = new Set();
  return {
    name, parent: null, inert: false, listeners: {}, attributes,
    classList: { add: value => classes.add(value), remove: value => classes.delete(value), contains: value => classes.has(value) },
    setAttribute(key, value) { attributes.set(key, value); },
    removeAttribute(key) { attributes.delete(key); },
    addEventListener(event, callback) { this.listeners[event] = callback; },
    remove() { this.parent.children.splice(this.parent.children.indexOf(this), 1); this.parent = null; },
    getBoundingClientRect() { return { width: 240 }; },
    cloneNode() {
      const clone = element(name + '-clone');
      clone.descendants = [element('save-control')];
      return clone;
    },
    querySelectorAll() { return this.descendants || []; }
  };
}

function setup({ reduced = false, supported = true, count = 4 } = {}) {
  const buttons = ['previous', 'next'].map(direction => ({ ...element(direction), dataset: { carousel: direction } }));
  const viewport = element('viewport');
  viewport.scrollLeft = 0;
  viewport.closest = () => ({ querySelectorAll: () => buttons });
  const track = element('track');
  track.children = Array.from({ length: count }, (_, index) => element(String(index + 1)));
  track.children.forEach(card => { card.parent = track; });
  Object.defineProperties(track, {
    firstElementChild: { get() { return this.children[0]; } },
    lastElementChild: { get() { return this.children.at(-1); } }
  });
  track.append = node => { if (node.parent) node.remove(); track.children.push(node); node.parent = track; };
  track.prepend = node => { if (node.parent) node.remove(); track.children.unshift(node); node.parent = track; };
  track.closest = () => viewport;
  const animations = [];
  if (supported) track.animate = (frames, options) => {
    let finish, fail;
    const finished = new Promise((resolve, reject) => { finish = resolve; fail = reject; });
    const animation = { frames, options, finished, finish, fail, cancelled: false, cancel() { this.cancelled = true; fail(new Error('Cancelled')); } };
    animations.push(animation);
    return animation;
  };
  const windowEvents = {};
  const preference = { matches: reduced, addEventListener(event, callback) { this.change = callback; } };
  runInNewContext(source, {
    document: { getElementById: () => track },
    window: { matchMedia: () => preference, addEventListener: (name, callback) => { windowEvents[name] = callback; } },
    getComputedStyle: () => ({ columnGap: '16px' })
  });
  return {
    track, viewport, animations, buttons, preference, windowEvents,
    next: () => buttons[1].listeners.click(), previous: () => buttons[0].listeners.click(),
    order: () => track.children.map(card => card.name)
  };
}

test('forward slides one measured card, uses an inert edge clone, and preserves original nodes', async () => {
  const carousel = setup();
  const originals = [...carousel.track.children];
  carousel.next();
  assert.equal(carousel.track.children.length, 5);
  const clone = carousel.track.lastElementChild;
  assert.equal(clone.inert, true);
  assert.equal(clone.attributes.get('aria-hidden'), 'true');
  assert.equal(clone.attributes.has('id'), false);
  assert.equal(clone.descendants[0].attributes.has('id'), false);
  assert.equal(clone.descendants[0].attributes.get('tabindex'), '-1');
  assert.equal(carousel.animations[0].frames[1].transform, 'translateX(-256px)');
  assert.equal(carousel.animations[0].options.duration, 320);
  assert.equal(carousel.buttons[1].attributes.get('aria-disabled'), 'true');
  carousel.animations[0].finish();
  await Promise.resolve();
  assert.deepEqual(carousel.order(), ['2', '3', '4', '1']);
  assert.equal(carousel.track.lastElementChild, originals[0]);
  assert.equal(carousel.viewport.classList.contains('is-sliding'), false);
  assert.equal(carousel.buttons[1].attributes.get('aria-disabled'), 'false');
});

test('reverse starts at the negative offset and returns the previous card to the leading edge', async () => {
  const carousel = setup();
  carousel.previous();
  assert.equal(carousel.track.firstElementChild.name, '4-clone');
  assert.equal(carousel.animations[0].frames[0].transform, 'translateX(-256px)');
  assert.equal(carousel.animations[0].frames[1].transform, 'translateX(0)');
  carousel.animations[0].finish();
  await Promise.resolve();
  assert.deepEqual(carousel.order(), ['4', '1', '2', '3']);
});

test('rapid clicks cannot create multiple animations or duplicate permanent cards', async () => {
  const carousel = setup();
  carousel.next(); carousel.next(); carousel.previous();
  assert.equal(carousel.animations.length, 1);
  carousel.animations[0].finish(); await Promise.resolve();
  carousel.previous(); carousel.animations[1].finish(); await Promise.resolve();
  assert.deepEqual(carousel.order(), ['1', '2', '3', '4']);
});

test('resize settles once and stale completion cannot affect a later transition', async () => {
  const carousel = setup();
  carousel.next(); carousel.windowEvents.resize();
  assert.deepEqual(carousel.order(), ['2', '3', '4', '1']);
  assert.equal(carousel.animations[0].cancelled, true);
  carousel.previous(); await Promise.resolve();
  assert.equal(carousel.track.children.length, 5);
  carousel.animations[1].finish(); await Promise.resolve();
  assert.deepEqual(carousel.order(), ['1', '2', '3', '4']);
});

test('animation cancellation and a reduced-motion change both remove temporary state', async () => {
  const carousel = setup();
  carousel.next(); carousel.animations[0].fail(new Error('Browser cancellation')); await Promise.resolve();
  assert.deepEqual(carousel.order(), ['2', '3', '4', '1']);
  carousel.previous(); carousel.preference.matches = true; carousel.preference.change();
  assert.deepEqual(carousel.order(), ['1', '2', '3', '4']);
  assert.equal(carousel.viewport.classList.contains('is-sliding'), false);
  await Promise.resolve();
});

test('reduced motion and unsupported animation rotate immediately without clones', () => {
  for (const options of [{ reduced: true }, { supported: false }]) {
    const carousel = setup(options);
    carousel.next(); assert.deepEqual(carousel.order(), ['2', '3', '4', '1']);
    carousel.previous(); assert.deepEqual(carousel.order(), ['1', '2', '3', '4']);
    assert.equal(carousel.animations.length, 0);
  }
});

test('native scroll position becomes the leading card before arrow navigation', () => {
  const carousel = setup({ reduced: true });
  carousel.viewport.scrollLeft = 512;
  carousel.next();
  assert.deepEqual(carousel.order(), ['4', '1', '2', '3']);
  assert.equal(carousel.viewport.scrollLeft, 0);
});

test('zero or one card keeps both controls unavailable and navigation does nothing', () => {
  for (const count of [0, 1]) {
    const carousel = setup({ count });
    carousel.next(); carousel.previous();
    assert.equal(carousel.track.children.length, count);
    assert.equal(carousel.animations.length, 0);
    assert.equal(carousel.buttons[0].attributes.get('aria-disabled'), 'true');
  }
});
