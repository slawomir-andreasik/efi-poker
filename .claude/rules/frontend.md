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
| Page loading | `<PageSpinner />` (full-page centered spinner) |
| Button loading | `<ButtonSpinner />` (inline in buttons) |
| 404 state | `<NotFoundState message="..." backTo="..." />` |
| Trace button | `<TraceCopyButton traceId={error.traceId} />` |
| Share room | `<ShareButton roomSlug={slug} />` |
| Modal shell | `<Modal isOpen onClose title>{children}</Modal>` (Escape + backdrop + ARIA) |
| Role badge | `<RoleBadge isAdmin={bool} />` |
| Dropdown dismiss | `useDropdownDismiss(ref, isOpen, onClose)` hook |
| Auth fallback | `useProjectAuth(slug)` hook (adminCode + participant recovery for logged-in users) |

Error handling pattern: `logger.warn('Failed to X:', getErrorMessage(err))` + `showToast(getErrorMessage(err))`

## Layout

- Content pages: `max-w-6xl mx-auto px-3 sm:px-4`
- Form-only pages (Login, Register, Join): `max-w-sm`
- Form inputs: `text-base` (16px) minimum for iOS compatibility

## Performance

- `React.memo` on components that receive handler props from polling parents (e.g. `TaskCard`, `EstimateButtons`) - prevents N re-renders every poll cycle
- `useCallback` for handlers passed as props to memoized children
- `useMemo` for derived data (sorting, filtering, mapping) that recalculates on every poll
- Don't read `localStorage` (JSON.parse) in render body - use `useState(() => ...)` initializer or `useMemo`
- Granular TanStack Query invalidation: `queryKey: ['rooms', roomId]` not `['rooms']` (prevents refetching all cached room queries)
- Stop polling on terminal states: `refetchInterval` should return `false` when room status is REVEALED or CLOSED

## Interaction

- Entire card is clickable - wrap in `<Link>` or use `role="link"` + keyboard handlers
- One action per context on each screen
- Glass tiers (CSS classes in `styles/theme.css`): `glass-whisper` (sidebars), `glass-frost` (cards/forms), `glass-crystal` (modals/toasts)
- Navigation: use `<Link to={...}>` not `navigate()` for navigable items (enables right-click "Open in new tab")
- Modals: wrap with `<Modal>` component (handles Escape, backdrop click, `role="dialog"`, `aria-modal`)
