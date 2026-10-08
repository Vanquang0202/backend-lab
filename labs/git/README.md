# Git lab

## Bài 1 — Feature branch/cache
**Scenario** → Bạn được giao thêm cache cho GET order.

**Task** → Tạo `feature/order-cache`, sửa một dòng log hoặc comment, xem thay đổi, commit và push lên remote.

**Expected observation** → `git status` đổi sạch sau commit; branch có commit riêng.

**Hints** → `status`, `diff`, staging area, `-u origin`.

## Bài 2 — Merge và conflict
**Scenario** → Hai developer cùng sửa `README.md`.

**Task** → Tạo branch A sửa cùng dòng; quay main sửa dòng đó; merge để tạo conflict; tự chọn nội dung, `git add`, `git commit`.

**Expected observation** → Có marker `<<<<<<<`, `=======`, `>>>>>>>` trước khi resolve.

## Bài 3 — Undo an toàn
**Scenario** → Đã stage nhầm, commit local nhầm, hoặc commit đã push không được rewrite lịch sử.

**Task** → Thực hành `git restore`, `git reset --soft`, `git revert`; xem graph.

## Solution

```bash
git switch -c feature/order-cache
# sửa file
git status
git diff
git add .
git commit -m "Add order cache lab change"
git push -u origin feature/order-cache
git switch main
git merge feature/order-cache
# conflict: edit markers, then:
git add README.md
git commit
git restore --staged <file>
git restore <file>
git reset --soft HEAD~1
git revert <commit>
git log --oneline --graph --decorate --all
```

Tạo conflict nhanh: sửa cùng một dòng trên hai branch rồi merge. `reset --soft` giữ thay đổi trong staging; `revert` tạo commit đảo ngược và phù hợp commit đã chia sẻ.

Git conflict demo