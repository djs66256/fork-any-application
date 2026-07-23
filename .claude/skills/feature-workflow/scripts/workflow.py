#!/usr/bin/env python3
"""
feature-workflow 流程状态管理脚本。

Agent 不应直接读写 workflow.json，所有状态变更通过此脚本完成。
脚本是幂等的（读操作）并对写操作进行状态转移校验。

用法:
    python3 scripts/workflow.py init <name> [--branch <branch>] [--worktree-path <path>]
    python3 scripts/workflow.py status [--json]
    python3 scripts/workflow.py advance [--stage <name>] [--skip <stage>]
    python3 scripts/workflow.py review-loop <stage> [--platform <name>] [--increment]
    python3 scripts/workflow.py human-review <stage> [--platform <name>] --approve|--reject
    python3 scripts/workflow.py mark-platform <stage> <platform> --status <status>
"""

import argparse
import json
import os
import sys
from datetime import datetime, timezone
from pathlib import Path
from typing import Optional

# ============================================================
# Constants
# ============================================================

STAGE_ORDER = [
    "worktree-setup",
    "spec-writing",
    "spec-review",
    "spec-human-review",
    "design-shared",
    "design-platforms",
    "design-review",
    "design-human-review",
    "plan-platforms",
    "coding-platforms",
    "code-human-review",
    "worktree-merge",
    "wiki-inclusion",
    "completed",
]

# human-review 驳回时的回退目标：跳过 review 阶段，直接回到撰写阶段
HUMAN_REVIEW_REJECT_BACK_TO = {
    "spec-human-review": "spec-writing",
    "design-human-review": "design-shared",
    "code-human-review": "coding-platforms",
}

# 可跳过的阶段（通过 advance --skip）
SKIPPABLE_STAGES = {"wiki-inclusion"}

STAGES_WITH_REVIEW_LOOP = {"spec-review", "design-review", "coding-platforms"}
STAGES_WITH_HUMAN_REVIEW = {"spec-human-review", "design-human-review", "code-human-review"}
STAGES_WITH_PLATFORMS = {"design-platforms", "plan-platforms", "coding-platforms"}

PLATFORM_NAMES = {"backend", "ios", "android", "web"}

MAX_REVIEW_LOOPS = 3

# ============================================================
# Helpers
# ============================================================

def find_workflow_json() -> Optional[Path]:
    """在 docs/specs/ 下查找 workflow.json 文件。

    搜索策略：
    1. 从当前目录（可能是 worktree）的 docs/specs/ 开始
    2. 如找不到，尝试从 GIT_COMMON_DIR（主仓库）的 docs/specs/ 查找
    """
    cwd = Path.cwd()
    specs_dir = cwd / "docs" / "specs"
    if specs_dir.exists():
        for spec_dir in sorted(specs_dir.iterdir(), reverse=True):
            if spec_dir.is_dir():
                wf = spec_dir / "workflow.json"
                if wf.exists():
                    return wf

    # fallback: 尝试从主仓库路径查找（当 worktree 中找不到时）
    git_common = os.environ.get("GIT_COMMON_DIR")
    if git_common:
        main_repo = Path(git_common).parent
        specs_dir = main_repo / "docs" / "specs"
        if specs_dir.exists():
            for spec_dir in sorted(specs_dir.iterdir(), reverse=True):
                if spec_dir.is_dir():
                    wf = spec_dir / "workflow.json"
                    if wf.exists():
                        return wf
    return None


def load_workflow(path: Path) -> dict:
    """加载 workflow.json。"""
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def save_workflow(path: Path, data: dict) -> None:
    """保存 workflow.json，自动更新 updated_at。"""
    data["metadata"]["updated_at"] = datetime.now(timezone.utc).isoformat()
    # 如果内容未变则不写入（保持幂等）
    with open(path, "r", encoding="utf-8") as f:
        original = json.load(f)
    if json.dumps(original, sort_keys=True) != json.dumps(data, sort_keys=True):
        with open(path, "w", encoding="utf-8") as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
            f.write("\n")


def get_current_stage(data: dict) -> Optional[str]:
    """返回当前 in_progress 阶段名，若无则返回 None。"""
    for stage in STAGE_ORDER:
        s = data["stages"].get(stage)
        if s and s.get("status") == "in_progress":
            return stage
    return None


def get_stage_index(stage: str) -> int:
    """返回阶段在 STAGE_ORDER 中的索引。"""
    return STAGE_ORDER.index(stage)


def stamp_now() -> str:
    """返回当前 ISO 时间戳。"""
    return datetime.now(timezone.utc).isoformat()


def init_stage_entry(stage: str) -> dict:
    """创建阶段初始 entry。"""
    entry = {
        "status": "pending",
        "started_at": None,
        "completed_at": None,
    }
    if stage in STAGES_WITH_REVIEW_LOOP:
        entry["review_loops"] = 0
    if stage in STAGES_WITH_PLATFORMS:
        entry["platforms"] = {}
        for plat in PLATFORM_NAMES:
            plat_entry = {"status": "pending", "started_at": None, "completed_at": None}
            if stage == "coding-platforms":
                plat_entry["review_loops"] = 0
            entry["platforms"][plat] = plat_entry
    return entry


# ============================================================
# Commands
# ============================================================

def cmd_init(args):
    """初始化一个新的 feature workflow。

    应在 worktree 创建完成后执行，这样 workflow.json 在 worktree 中可用。
    """
    from datetime import date

    name = args.name
    today = date.today().isoformat()
    spec_dir = f"docs/specs/{today}-{name}"
    branch = args.branch or f"feature/{today}-{name}"
    worktree_path = args.worktree_path or f".worktree/{today}-{name}"

    # 创建目录（在 worktree 中）
    target_dir = Path.cwd() / spec_dir
    if target_dir.exists():
        print(json.dumps({"ok": False, "error": f"目录已存在: {spec_dir}"}, ensure_ascii=False))
        sys.exit(1)

    target_dir.mkdir(parents=True)

    # 构建 workflow.json
    stages = {}
    first = True
    for stage in STAGE_ORDER:
        entry = init_stage_entry(stage)
        if first:
            entry["status"] = "in_progress"
            entry["started_at"] = stamp_now()
            first = False
        stages[stage] = entry

    workflow = {
        "feature": {
            "name": name,
            "dir": spec_dir,
            "date": today,
            "branch": branch,
            "worktree_path": worktree_path,
        },
        "stages": stages,
        "metadata": {
            "created_at": stamp_now(),
            "updated_at": stamp_now(),
            "version": "1.0",
        },
    }

    wf_path = target_dir / "workflow.json"
    with open(wf_path, "w", encoding="utf-8") as f:
        json.dump(workflow, f, indent=2, ensure_ascii=False)
        f.write("\n")

    print(json.dumps({
        "ok": True,
        "feature": workflow["feature"],
        "workflow_json": str(wf_path),
        "current_stage": "worktree-setup",
    }, ensure_ascii=False))


def cmd_status(args):
    """显示当前 workflow 状态。"""
    wf_path = find_workflow_json()
    if wf_path is None:
        print(json.dumps({"ok": False, "error": "未找到 workflow.json，请先执行 init（在 worktree 中执行）"}, ensure_ascii=False))
        sys.exit(1)

    data = load_workflow(wf_path)
    current = get_current_stage(data)

    completed = []
    skipped = []
    pending = []
    for stage in STAGE_ORDER:
        s = data["stages"][stage]
        if s["status"] == "completed":
            completed.append(stage)
        elif s["status"] == "skipped":
            skipped.append(stage)
        elif s["status"] == "pending":
            pending.append(stage)

    current_idx = STAGE_ORDER.index(current) if current else -1
    next_stage = STAGE_ORDER[current_idx + 1] if current and current_idx + 1 < len(STAGE_ORDER) else None

    # 检查并行阶段的详细状态
    platform_details = {}
    for stage in STAGES_WITH_PLATFORMS:
        s = data["stages"][stage]
        if s["status"] in ("in_progress", "pending"):
            platform_details[stage] = {}
            for plat, pdata in s.get("platforms", {}).items():
                platform_details[stage][plat] = pdata.get("status", "unknown")

    result = {
        "ok": True,
        "feature": data["feature"],
        "current_stage": current,
        "current_stage_index": current_idx + 1,
        "total_stages": len(STAGE_ORDER),
        "completed_stages": completed,
        "skipped_stages": skipped,
        "pending_stages": pending,
        "next_stage": next_stage,
        "can_advance": current is not None,
        "platform_details": platform_details,
    }

    if args.json:
        print(json.dumps(result, ensure_ascii=False))
    else:
        print(f"需求: {data['feature']['name']}")
        print(f"分支: {data['feature']['branch']}")
        print(f"产物目录: {data['feature']['dir']}")
        print(f"当前阶段: {current} ({current_idx + 1}/{len(STAGE_ORDER)})")
        print(f"已完成: {', '.join(completed) if completed else '无'}")
        if skipped:
            print(f"已跳过: {', '.join(skipped)}")
        if next_stage:
            print(f"下一阶段: {next_stage}")
        if platform_details:
            print("平台详情:")
            for stage, plats in platform_details.items():
                print(f"  {stage}:")
                for plat, status in plats.items():
                    print(f"    {plat}: {status}")


def cmd_advance(args):
    """标记当前阶段完成，推进到下一阶段。

    支持 --skip 跳过某个 pending 阶段（仅限 SKIPPABLE_STAGES 中的阶段）。
    """
    wf_path = find_workflow_json()
    if wf_path is None:
        print(json.dumps({"ok": False, "error": "未找到 workflow.json"}, ensure_ascii=False))
        sys.exit(1)

    data = load_workflow(wf_path)
    current = get_current_stage(data)

    # 处理 --skip：跳过指定阶段
    if args.skip:
        if args.skip not in SKIPPABLE_STAGES:
            print(json.dumps({"ok": False, "error": f"阶段 {args.skip} 不可跳过，可跳过的阶段: {list(SKIPPABLE_STAGES)}"}, ensure_ascii=False))
            sys.exit(1)
        skip_idx = get_stage_index(args.skip)
        if data["stages"][args.skip]["status"] != "pending":
            print(json.dumps({"ok": False, "error": f"阶段 {args.skip} 状态为 {data['stages'][args.skip]['status']}，不是 pending"}, ensure_ascii=False))
            sys.exit(1)
        data["stages"][args.skip]["status"] = "skipped"
        data["stages"][args.skip]["completed_at"] = stamp_now()
        save_workflow(wf_path, data)
        print(json.dumps({"ok": True, "action": "skipped", "stage": args.skip}, ensure_ascii=False))
        return

    # 处理 skipped 阶段：如果当前 in_progress 的阶段已经是 skipped，自动推进
    if current and data["stages"][current]["status"] == "skipped":
        current_idx = get_stage_index(current)
        if current_idx + 1 < len(STAGE_ORDER):
            next_stage = STAGE_ORDER[current_idx + 1]
            data["stages"][next_stage]["status"] = "in_progress"
            data["stages"][next_stage]["started_at"] = stamp_now()
            save_workflow(wf_path, data)
            print(json.dumps({
                "ok": True,
                "action": "auto-skipped",
                "skipped": current,
                "advanced_to": next_stage,
            }, ensure_ascii=False))
            return

    if current is None:
        print(json.dumps({"ok": False, "error": "没有 in_progress 的阶段"}, ensure_ascii=False))
        sys.exit(1)

    stage_to_complete = args.stage or current

    if stage_to_complete != current:
        print(json.dumps({"ok": False, "error": f"参数 stage={stage_to_complete} 与当前 in_progress={current} 不匹配"}, ensure_ascii=False))
        sys.exit(1)

    # 并行阶段的特殊处理：检查所有平台是否完成（skipped 也视为完成）
    if stage_to_complete in STAGES_WITH_PLATFORMS:
        platforms = data["stages"][stage_to_complete]["platforms"]
        incomplete = [p for p, d in platforms.items() if d["status"] not in ("completed", "skipped")]
        if incomplete:
            print(json.dumps({
                "ok": False,
                "error": f"阶段 {stage_to_complete} 有未完成的平台: {incomplete}",
                "incomplete_platforms": incomplete,
            }, ensure_ascii=False))
            sys.exit(1)

    # 标记当前阶段 completed
    data["stages"][stage_to_complete]["status"] = "completed"
    data["stages"][stage_to_complete]["completed_at"] = stamp_now()

    # 推进到下一阶段（跳过已 skipped 的阶段）
    current_idx = get_stage_index(stage_to_complete)
    next_idx = current_idx + 1
    while next_idx < len(STAGE_ORDER):
        next_stage = STAGE_ORDER[next_idx]
        next_entry = data["stages"][next_stage]
        if next_entry["status"] == "skipped":
            next_entry["completed_at"] = stamp_now()
            next_idx += 1
            continue
        if next_entry["status"] in ("pending",):
            next_entry["status"] = "in_progress"
            next_entry["started_at"] = stamp_now()
            break
        # completed 或 in_progress，继续查找
        next_idx += 1

    save_workflow(wf_path, data)

    if next_idx < len(STAGE_ORDER):
        print(json.dumps({
            "ok": True,
            "completed": stage_to_complete,
            "advanced_to": STAGE_ORDER[next_idx],
            "next_index": next_idx + 1,
            "total_stages": len(STAGE_ORDER),
        }, ensure_ascii=False))
    else:
        print(json.dumps({
            "ok": True,
            "completed": stage_to_complete,
            "advanced_to": None,
            "message": "所有阶段已完成！",
        }, ensure_ascii=False))


def cmd_review_loop(args):
    """递增 review 循环计数。超过上限时输出 warning。"""
    wf_path = find_workflow_json()
    if wf_path is None:
        print(json.dumps({"ok": False, "error": "未找到 workflow.json"}, ensure_ascii=False))
        sys.exit(1)

    data = load_workflow(wf_path)
    stage = args.stage

    if stage not in STAGES_WITH_REVIEW_LOOP:
        print(json.dumps({"ok": False, "error": f"阶段 {stage} 不支持 review-loop"}, ensure_ascii=False))
        sys.exit(1)

    if args.platform:
        if stage != "coding-platforms":
            print(json.dumps({"ok": False, "error": "仅 coding-platforms 支持 --platform 参数"}, ensure_ascii=False))
            sys.exit(1)
        if args.platform not in PLATFORM_NAMES:
            print(json.dumps({"ok": False, "error": f"未知平台: {args.platform}"}, ensure_ascii=False))
            sys.exit(1)
        if args.increment:
            data["stages"][stage]["platforms"][args.platform]["review_loops"] += 1
        loops = data["stages"][stage]["platforms"][args.platform]["review_loops"]
    else:
        if args.increment:
            data["stages"][stage]["review_loops"] += 1
        loops = data["stages"][stage]["review_loops"]

    save_workflow(wf_path, data)
    result = {
        "ok": True,
        "stage": stage,
        "review_loops": loops,
    }
    if args.platform:
        result["platform"] = args.platform

    # 超过上限时输出 warning
    if loops >= MAX_REVIEW_LOOPS:
        result["warning"] = f"已达到 {MAX_REVIEW_LOOPS} 轮 review 上限，请停止自动循环，上报给人工决策"

    print(json.dumps(result, ensure_ascii=False))


def cmd_human_review(args):
    """人工确认阶段。

    支持 --platform 参数用于 code-human-review 的平台级驳回：
    - approve：标记本阶段完成，推进到下一阶段
    - reject：驳回，回到对应撰写阶段（而非 review 阶段）
      - spec-human-review → spec-writing
      - design-human-review → design-shared
      - code-human-review → coding-platforms（如指定 --platform 则只回退该平台）
    """
    wf_path = find_workflow_json()
    if wf_path is None:
        print(json.dumps({"ok": False, "error": "未找到 workflow.json"}, ensure_ascii=False))
        sys.exit(1)

    data = load_workflow(wf_path)
    stage = args.stage

    if stage not in STAGES_WITH_HUMAN_REVIEW:
        print(json.dumps({"ok": False, "error": f"阶段 {stage} 不是人工确认阶段"}, ensure_ascii=False))
        sys.exit(1)

    if stage not in data["stages"]:
        print(json.dumps({"ok": False, "error": f"阶段 {stage} 不存在"}, ensure_ascii=False))
        sys.exit(1)

    if args.approve:
        data["stages"][stage]["status"] = "completed"
        data["stages"][stage]["completed_at"] = stamp_now()

        current_idx = get_stage_index(stage)
        next_idx = current_idx + 1
        # 跳过已 skipped 的阶段
        while next_idx < len(STAGE_ORDER):
            next_stage = STAGE_ORDER[next_idx]
            if data["stages"][next_stage]["status"] == "skipped":
                data["stages"][next_stage]["completed_at"] = stamp_now()
                next_idx += 1
                continue
            if data["stages"][next_stage]["status"] == "pending":
                data["stages"][next_stage]["status"] = "in_progress"
                data["stages"][next_stage]["started_at"] = stamp_now()
                break
            next_idx += 1

        save_workflow(wf_path, data)
        print(json.dumps({
            "ok": True,
            "action": "approved",
            "stage": stage,
            "advanced_to": STAGE_ORDER[next_idx] if next_idx < len(STAGE_ORDER) else None,
        }, ensure_ascii=False))
    elif args.reject:
        back_to = HUMAN_REVIEW_REJECT_BACK_TO.get(stage)
        if back_to is None:
            print(json.dumps({"ok": False, "error": f"阶段 {stage} 没有定义驳回回退目标"}, ensure_ascii=False))
            sys.exit(1)

        # 回退当前 human-review 阶段
        data["stages"][stage]["status"] = "pending"
        data["stages"][stage]["started_at"] = None
        data["stages"][stage]["completed_at"] = None

        # 对于 code-human-review，支持平台级驳回
        platform_back = None
        if stage == "code-human-review" and args.platform:
            if args.platform not in PLATFORM_NAMES:
                print(json.dumps({"ok": False, "error": f"未知平台: {args.platform}"}, ensure_ascii=False))
                sys.exit(1)
            # 只回退指定平台的 coding 状态
            data["stages"]["coding-platforms"]["platforms"][args.platform]["status"] = "in_progress"
            data["stages"]["coding-platforms"]["platforms"][args.platform]["completed_at"] = None
            data["stages"]["coding-platforms"]["status"] = "in_progress"
            data["stages"]["coding-platforms"]["completed_at"] = None
            platform_back = args.platform
            # 不重置 coding-platforms 其他平台和 code-human-review 本身的回退
        else:
            # 重置 back_to 阶段及其后续的 review 阶段
            back_idx = get_stage_index(back_to)
            current_idx = get_stage_index(stage)
            for i in range(back_idx, current_idx + 1):
                s = STAGE_ORDER[i]
                if s in STAGES_WITH_HUMAN_REVIEW:
                    data["stages"][s]["status"] = "pending"
                    data["stages"][s]["started_at"] = None
                    data["stages"][s]["completed_at"] = None
                elif s in STAGES_WITH_REVIEW_LOOP:
                    data["stages"][s]["status"] = "pending"
                    data["stages"][s]["started_at"] = None
                    data["stages"][s]["completed_at"] = None
                    data["stages"][s]["review_loops"] = 0
                elif s != stage:
                    data["stages"][s]["status"] = "pending" if s != back_to else "in_progress"
                    data["stages"][s]["started_at"] = None if s != back_to else stamp_now()
                    data["stages"][s]["completed_at"] = None

            # 重新激活 back_to 阶段
            data["stages"][back_to]["status"] = "in_progress"
            data["stages"][back_to]["completed_at"] = None
            data["stages"][back_to]["started_at"] = stamp_now()

        save_workflow(wf_path, data)
        result = {
            "ok": True,
            "action": "rejected",
            "stage": stage,
            "back_to": back_to,
        }
        if platform_back:
            result["platform"] = platform_back
        print(json.dumps(result, ensure_ascii=False))


def cmd_mark_platform(args):
    """标记并行阶段中单个平台的状态。"""
    wf_path = find_workflow_json()
    if wf_path is None:
        print(json.dumps({"ok": False, "error": "未找到 workflow.json"}, ensure_ascii=False))
        sys.exit(1)

    data = load_workflow(wf_path)
    stage = args.stage
    platform = args.platform
    status = args.status

    if stage not in STAGES_WITH_PLATFORMS:
        print(json.dumps({"ok": False, "error": f"阶段 {stage} 不是并行平台阶段"}, ensure_ascii=False))
        sys.exit(1)

    if platform not in PLATFORM_NAMES:
        print(json.dumps({"ok": False, "error": f"未知平台: {platform}"}, ensure_ascii=False))
        sys.exit(1)

    plat_data = data["stages"][stage]["platforms"][platform]
    old_status = plat_data["status"]
    plat_data["status"] = status
    if status in ("completed", "skipped"):
        plat_data["completed_at"] = stamp_now()
    if status == "in_progress" and not plat_data.get("started_at"):
        plat_data["started_at"] = stamp_now()

    save_workflow(wf_path, data)

    # 检查是否所有平台都已完成（skipped 也算完成）
    all_done = all(p["status"] in ("completed", "skipped") for p in data["stages"][stage]["platforms"].values())
    remaining = [p for p, d in data["stages"][stage]["platforms"].items() if d["status"] not in ("completed", "skipped")]

    print(json.dumps({
        "ok": True,
        "stage": stage,
        "platform": platform,
        "old_status": old_status,
        "new_status": status,
        "all_platforms_done": all_done,
        "remaining_platforms": remaining,
    }, ensure_ascii=False))


# ============================================================
# CLI
# ============================================================

def main():
    parser = argparse.ArgumentParser(
        description="feature-workflow 流程状态管理",
    )
    subparsers = parser.add_subparsers(dest="command", help="子命令")

    # init
    p_init = subparsers.add_parser("init", help="初始化新 feature workflow（应在 worktree 中执行）")
    p_init.add_argument("name", help="需求名称（kebab-case）")
    p_init.add_argument("--branch", help="分支名（默认 feature/<YYYY-MM-dd>-<name>）")
    p_init.add_argument("--worktree-path", help="worktree 路径（默认 .worktree/<YYYY-MM-dd>-<name>）")

    # status
    p_status = subparsers.add_parser("status", help="显示当前状态")
    p_status.add_argument("--json", action="store_true", help="JSON 格式输出")

    # advance
    p_advance = subparsers.add_parser("advance", help="推进到下一阶段")
    p_advance.add_argument("--stage", help="要完成的阶段名（默认当前 in_progress 阶段）")
    p_advance.add_argument("--skip", help="跳过指定阶段（仅限 wiki-inclusion）")

    # review-loop
    p_rl = subparsers.add_parser("review-loop", help="管理 review 循环")
    p_rl.add_argument("stage", help="阶段名")
    p_rl.add_argument("--platform", help="平台名（仅 coding-platforms）")
    p_rl.add_argument("--increment", action="store_true", help="递增计数")

    # human-review
    p_hr = subparsers.add_parser("human-review", help="人工确认阶段")
    p_hr.add_argument("stage", help="阶段名")
    p_hr.add_argument("--platform", help="平台名（仅 code-human-review 的平台级驳回）")
    p_hr.add_argument("--approve", action="store_true", help="通过")
    p_hr.add_argument("--reject", action="store_true", help="驳回")

    # mark-platform
    p_mp = subparsers.add_parser("mark-platform", help="标记并行平台状态")
    p_mp.add_argument("stage", help="阶段名")
    p_mp.add_argument("platform", help="平台名 (backend/ios/android/web)")
    p_mp.add_argument("--status", required=True,
                       choices=["pending", "in_progress", "completed", "failed", "skipped"],
                       help="新状态（skipped 表示该平台不涉及此需求）")

    args = parser.parse_args()

    if args.command == "init":
        cmd_init(args)
    elif args.command == "status":
        cmd_status(args)
    elif args.command == "advance":
        cmd_advance(args)
    elif args.command == "review-loop":
        cmd_review_loop(args)
    elif args.command == "human-review":
        cmd_human_review(args)
    elif args.command == "mark-platform":
        cmd_mark_platform(args)
    else:
        parser.print_help()
        sys.exit(1)


if __name__ == "__main__":
    main()
