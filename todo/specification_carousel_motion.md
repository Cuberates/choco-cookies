# Specification — Featured Ball Carousel Sliding Motion

## Objective

Animate the existing forward/reverse buttons for featured ball cards, replacing instantaneous reordering with a continuous directional slide. Preserve the four-brand showcase, adjacent ball/core images, saved arsenal actions, neutral palette, and responsive layout.

## Behavior

- Forward moves the cards left by one card plus its gap; the leading card wraps to the right.
- Reverse moves the cards right by one card plus its gap; the trailing card wraps to the left.
- Duration: 320 ms; easing: `cubic-bezier(0.22, 1, 0.36, 1)`.
- Card distance comes from the rendered card width and computed gap, rather than fixed pixel offsets.
- Use a clipped viewport and a flex track. Desktop shows four cards; tablet shows two; mobile shows one. Ball/core images remain adjacent inside each card.
- Use one temporary edge clone for seamless wrapping. The clone is inert, hidden from accessibility APIs, stripped of IDs, and never saved as a real card. Existing cards and their listeners remain intact.
- Accept only one arrow transition at a time; ignore additional clicks while sliding. Mark controls `aria-disabled` while busy without disabling their focusable DOM nodes.
- Native horizontal swipe/scroll remains available while idle. After a swipe, arrow navigation aligns the nearest card and makes it the leading card before moving one position.
- Resize or a change to reduced-motion settings settles an in-flight transition exactly once and removes temporary state.
- Respect `prefers-reduced-motion: reduce` by rotating cards immediately. Browsers without Web Animations support also use immediate rotation.
- With fewer than two cards, arrow controls are marked unavailable and do nothing.

## Implementation boundaries

- Keep animation logic in a dedicated local JavaScript file; no additional dependencies, image generation, network requests, or database changes.
- Wrap the current card row in a carousel viewport. Keep the existing card nodes, source links, Save controls, and iteration 6 refresh action.
- Remove the old instantaneous arrow handler so it cannot reorder a second time.
- Scope responsive layout and motion styles to the featured carousel.

## Acceptance and verification

1. Forward and reverse animate in the expected direction and wrap without a visible end jump.
2. Cards retain their source links, Save listeners, and data after repeated navigation.
3. Rapid repeated clicks cannot duplicate cards or leave a transform/clone behind.
4. Resize/cancellation and reduced motion leave exactly the original card count in a usable order.
5. Mobile keeps the ball/core image pair together and supports idle swipe navigation.
6. Focus stays on the activated arrow; temporary clones cannot receive focus.
7. Run focused JavaScript lifecycle tests, syntax/whitespace checks, and relevant server-rendering regressions.

## Specification review

The existing implementation directly appends/prepends cards on click and resets scrolling, so CSS transitions alone cannot animate the reorder. A viewport/track with a temporary edge clone provides continuous movement while preserving the actual interactive cards. Measured distance avoids assumptions about desktop/mobile widths. A single cleanup function must handle completion, animation cancellation, resize, and reduced-motion changes to avoid inconsistent card counts. Arrow focus is preserved using `aria-disabled` with a JavaScript guard. The changes are confined to presentation and do not alter iteration 6 selection or persistence behavior.

## Implementation review and results

Implemented in `static/js/carousel.js`, with the viewport wrapper in `catalog.html` and scoped styles in `catalog.css`. The original instant-reorder listener was removed from `catalog.js`. Animation completion callbacks check the active transition's identity so a stale callback cannot clean up a subsequent slide. Edge clones are removed before the original card nodes are rotated, preserving their event listeners and saved state.

Verification:

- `node --test src/test/js/carousel.test.cjs`: eight tests passed, covering both directions, measured travel distance, inert clones, original node identity, rapid clicks, resize, stale callbacks, cancellation, reduced motion, unsupported animation, swipe offsets, and empty/single-card behavior.
- `./mvnw -q -Dtest=FeaturedCatalogPageTests,CatalogSearchTests test`: four tests passed; catalog cards, source images, refresh POST/CSRF, filters, and search still render correctly.
- JavaScript syntax checks and `git diff --check` passed.

Browser animation timing and visual viewport behavior were not manually inspected; lifecycle verification uses a simulated DOM and the server tests render the actual templates.
