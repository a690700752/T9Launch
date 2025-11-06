# AGENTS Guidelines

## 多级 memory AGENTS.md

- 在查找代码的过程中，请充分利用本项目已经构建好的多层级 AGENTS.md 系统。在 Read 或 Edit 任何目录中有 AGENTS.md 的文件时，你都会自动 Read 一份该目录中的 AGENTS.md ，它会引导你了解本目录下的全部代码文件。在阅读多级目录的时候同理。请通过这种方式准确且精准地找到你需要找到的代码，而不是简单地完全依靠 Grep 和 Glob 等工具
- 当你完成了对应代码功能的更改后，需要立刻修改完善对应目录中的 AGENTS.md ，以确保项目记忆的准确

## Tool Priority

- Filename search: `fd`.
- Text/content search: `rg` (ripgrep).
- AST/structural search: `sg` (ast-grep) — preferred for code-aware queries (imports, call expressions, JSX/TSX nodes).

### Search Hygiene (fd/rg/sg)

- Exclude bulky folders to keep searches fast and relevant: `.git`, `node_modules`, `coverage`, `out`, `dist`.
- Prefer running searches against a scoped path (e.g., `src`) to implicitly avoid vendor and VCS directories.
- Examples:
  - `rg -n "pattern" -g "!{.git,node_modules,coverage,out,dist}" src`
  - `fd --hidden --exclude .git --exclude node_modules --exclude coverage --exclude out --exclude dist --type f ".tsx?$" src`
- ast-grep typically respects `.gitignore`; target `src` to avoid scanning vendor folders:
  - `ast-grep -p "import $$ from '@shared/$$'" src --lang ts,tsx,mts,cts`
  - If needed, add ignore patterns to your ignore files rather than disabling ignores.

## 2025-11-05 UI Refresh Notes

- `app/src/main/res/layout/activity_main.xml`: Gradient background retained while toolbar removed, list space expanded, search card compacted, keypad margin/elevation reduced for flat cards, and MaterialCardView elevation now 0dp for a flush surface.
- `app/src/main/res/values/styles.xml`: T9 keypad buttons use shorter height (56dp) and tonal container colors to match the refreshed look.
- `app/src/main/res/layout/item_app.xml`: Card-style tiles now have zero elevation, tighter padding, and 40dp icons to surface more apps per screen.
- New resources: `res/drawable/bg_main_gradient.xml` and `res/drawable/ic_search.xml`; `home_subtitle` remains available if a title component returns later.
- 2025-11-06 tweak: search box field height is 48dp and keypad grid padding reduced for a flatter, more compact card.
