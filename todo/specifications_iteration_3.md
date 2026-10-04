# Specification: Iteration 3 UI Fixes for Bowler's Journal

## Objective

Update the existing Bowler's Journal UI to clean up the title page and footer styling issues described in the current iteration requirements. The goal is to fix the visual inconsistencies and remove content that should not appear in the footer while preserving the page structure and core branding.

## Problem Statement

The current UI contains several styling and content issues:

- The title page card component uses overly rounded corners.
- The color scheme is visually harsh because the cyan action color and white text are blending and reducing readability.
- The footer includes content that should be removed.
- Footer content should not include trademark language or branding elements.

## Scope

This change is limited to the front-end visual and footer content of the Bowler's Journal application. It does not require redesigning the product or introducing new site features.

## Detailed Requirements

### 1. Card styling

- Reduce the border radius of the card component on the title page.
- Make the card corners less exaggerated and visually softer.
- Preserve the existing layout and keep the design consistent with the rest of the page.

### 2. Color scheme

- Replace the cyan-heavy button styling with a more neutral palette.
- Ensure white text remains readable against the chosen backgrounds.
- Remove visual blending issues caused by insufficient contrast.
- Keep the UI professional and understated.

### 3. Footer cleanup

Remove the following footer items entirely:

- reaction glossary
- ball selector
- about
- privacy
- terms
- accessibility
- socials

The footer must be simplified to exclude all of the above and should not contain unnecessary navigation or marketing content.

### 4. Trademark and branding restrictions

- Do not include trademark references in the footer.
- Remove any branding language that implies official product or brand ownership beyond the application name itself.
- Keep branding minimal and non-promotional.

## Acceptance Criteria

1. The title page card radius is visibly reduced and feels less exaggerated.
2. The button and text colors no longer clash and provide acceptable contrast.
3. The footer no longer includes the reaction glossary, ball selector, About, Privacy, Terms, Accessibility, or social links.
4. No trademark text appears in the footer.
5. The page remains functional and visually coherent after the change.

## Constraints

- Do not add new pages or major features.
- Do not rewrite unrelated sections of the application.
- Keep the fix focused on the identified UI and footer issues.
- The change should be implemented within the existing files that currently control the title page and footer.

## Agent Todo Prompt

Replace the outdated Iteration 3 requirements in the current file with the finalized specification above. Update the existing implementation instructions to reflect the actual UI issues to fix, remove stale content, and ensure the task is scoped to the title page card styling, neutral color palette, footer cleanup, and trademark removal only. Do not broaden the scope beyond the listed issues. Keep the fix targeted and aligned with the requirement details above.
