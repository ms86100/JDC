# Root Cause Analysis: Edit Issue Popup Not Closing

## Problem
When editing an issue and clicking Save, the popup showed "Failed to update issue. Check required fields and try again." and refused to close — even though the issue data was actually saved successfully to the database.

## Timeline of Investigation

### Attempt 1: Suspected backend validation error
**Hypothesis:** The issue-service was rejecting the update request.
**Investigation:** Tested with `curl PUT /api/issues/{id}` directly — returned 200 OK every time, even with empty payload `{}`.
**Result:** Backend was fine. Issue was purely frontend.

### Attempt 2: Suspected post-save operations throwing
**Hypothesis:** After `issueApi.update()`, the mutation called `syncIssueVersionComponentLinks`, sprint operations, comment creation, and issue linking. If any of these threw, the entire mutation's `onError` would fire.
**Fix applied:** Wrapped all post-save operations in try/catch blocks.
**Result:** Still failed. The `syncIssueVersionComponentLinks` was a dynamic import that could fail, and sprint/comment/link APIs returned errors when services were unavailable.

### Attempt 3: Made all post-save operations best-effort
**Hypothesis:** Even with try/catch, the async chain inside `mutationFn` could throw from unexpected places.
**Fix applied:** Moved post-save operations to `onSuccess` callback with `.catch(() => {})` on every call.
**Result:** Still failed — but now the error message changed to `TypeError: a is not a function` in the minified JS.

### Attempt 4: Identified `onSuccess` name collision
**Hypothesis:** React Query's `onSuccess` callback was `async`, and errors thrown inside it (including from calling `onSuccess()` prop) were caught by React Query and routed to `onError`.
**Fix applied:** Changed from `onSuccess` to `onSettled` callback (fires regardless of success/error), added `typeof onSuccess === 'function'` guard before calling the prop.
**Result:** No more errors, but the popup STILL didn't close. No errors in console either.

### Attempt 5: Found the actual root cause — prop name mismatch
**Investigation:** Checked how `EditIssueModal` was rendered in `IssueDetailPage.tsx`:

```tsx
// IssueDetailPage.tsx (parent)
<EditIssueModal
  issue={issue}
  onClose={() => setShowEditModal(false)}
  onSave={() => {                          // <-- "onSave"
    queryClient.invalidateQueries({ queryKey: ['issue', issueId] });
    setShowEditModal(false);
  }}
/>
```

```tsx
// EditIssueModal.tsx (component interface)
interface EditIssueModalProps {
  issue: IssueResponse | Record<string, unknown>;
  onClose: () => void;
  onSuccess: () => void;                   // <-- "onSuccess"
}
```

**Root cause:** The parent passed the close callback as `onSave` but the modal's interface expected `onSuccess`. TypeScript didn't catch this because `onSave` is not in the interface — it was silently ignored as an extra prop. Inside the modal, `onSuccess` was always `undefined`, so `typeof onSuccess === 'function'` returned `false`, and the close callback was never called.

**Fix:** Changed the parent from `onSave={...}` to `onSuccess={...}`.

## Root Cause Summary

| Layer | Issue | Impact |
|-------|-------|--------|
| **Primary** | Prop name mismatch: parent sends `onSave`, modal expects `onSuccess` | Modal never closes |
| **Secondary** | Post-save operations (sprint, comment, link sync) threw errors inside `mutationFn` | "Failed to update" error message displayed |
| **Tertiary** | React Query `onSuccess` callback was async — errors inside it routed to `onError` | Error appeared even after successful save |

## Lessons Learned

1. **Prop name mismatches are silent killers in React.** TypeScript only validates props that ARE in the interface — extra props passed by the parent are silently ignored. The `onSave` prop was never consumed, and `onSuccess` was always `undefined`. Consider using strict prop checking or ESLint rules for unused props.

2. **Don't mix React Query callback names with component prop names.** Having both a React Query `onSuccess` mutation callback and an `onSuccess` component prop creates confusion. Rename one — e.g., use `onSaved` or `onComplete` for the prop.

3. **Never put fallible operations inside `mutationFn`.** The mutation function should do ONE thing — the core API call. All side effects (sprint sync, comment creation, link creation) should be fire-and-forget in `onSettled` or handled separately. If they fail, the user sees "update failed" even though the update succeeded.

4. **`onSettled` is safer than `onSuccess` for closing modals.** `onSettled` fires regardless of success or error, ensuring the UI always responds. Use it for cleanup operations like closing modals, clearing forms, and invalidating queries.

5. **Test the simplest case first.** The bug was reproducible by clicking Save with zero changes — the simplest possible edit. If we had tested this case with network monitoring from the start, we would have seen the 200 response and focused on the frontend immediately instead of investigating backend validation.

## Files Changed

| File | Change |
|------|--------|
| `jira-frontend/src/features/issues/pages/IssueDetailPage.tsx` | `onSave` → `onSuccess` prop name |
| `jira-frontend/src/features/issues/components/EditIssueModal.tsx` | Simplified mutation: core update in `mutationFn`, close in `onSettled`, post-save ops with `.catch()`, `typeof` guard on `onSuccess` prop |
