# Frontend Conventions

## Architecture

- **Routing**: React Router v7, routes in `App.tsx` with `lazy()` + `Suspense`
- **State**: TanStack Query - queries in `api/queries.ts`, mutations in `api/mutations.ts`, keys in `api/queryKeys.ts`
- **API client**: `api/client.ts` with error handling + W3C tracing

## Directory Structure

```
src/
  api/          -> client, queries, mutations, queryKeys, types.ts (hand-maintained, not generated)
  components/   -> reusable UI components
    layout/     -> Header, Breadcrumbs, Footer
    charts/     -> chart components (recharts)
  hooks/        -> custom React hooks
  pages/        -> route-level page components
  utils/        -> logger, error helpers
  styles/       -> theme.css (glass tiers, colors)
```

## Project Utilities

| Concern | Tool |
|---------|------|
| Logging | `logger` from `@/utils/logger` |
| Error text | `getErrorMessage(err)` from `@/utils/error` |
| Toasts | `useToast()` - `showToast('msg')` (error), `showToast('msg', 'success')` |
| Loading | `<Spinner />` (pages), `<ButtonSpinner />` (buttons) |

Error handling pattern: `logger.warn('Failed to X:', getErrorMessage(err))` + `showToast(getErrorMessage(err))`

## Layout

- Content pages: `max-w-6xl mx-auto px-3 sm:px-4`
- Form-only pages (Login, Register, Join): `max-w-sm`
- Form inputs: `text-base` (16px) minimum for iOS compatibility

## Interaction

- Entire card is clickable - wrap in `<Link>` or use `role="link"` + keyboard handlers
- One action per context on each screen
- Glass tiers (CSS classes in `styles/theme.css`): `glass-whisper` (sidebars), `glass-frost` (cards/forms), `glass-crystal` (modals/toasts)
