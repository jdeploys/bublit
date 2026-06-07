## Worktree Preference

For this project, do not create or use a git worktree by default.

- Work directly in the main project checkout unless the user explicitly asks for an isolated worktree.
- If a skill or workflow recommends worktree isolation, treat this project rule as the default override and continue in-place.
- Still protect user changes: check `git status` before edits and do not revert unrelated work.
