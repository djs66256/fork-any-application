#!/usr/bin/env python3
"""
Tests for feature-workflow workflow.py state management script.

Usage:
    cd .claude/skills/feature-workflow/scripts
    python3 -m unittest test_workflow.py -v
"""

import argparse
import io
import json
import os
import sys
import tempfile
import unittest
from datetime import date, datetime, timezone
from pathlib import Path
from unittest.mock import patch

import workflow


# ============================================================
# Helpers
# ============================================================

class WorkflowTestBase(unittest.TestCase):
    """Base class that provides a temp directory and helper methods."""

    def setUp(self):
        self.temp_dir = tempfile.TemporaryDirectory()
        self.original_cwd = os.getcwd()
        os.chdir(self.temp_dir.name)

    def tearDown(self):
        os.chdir(self.original_cwd)
        self.temp_dir.cleanup()

    # -- arg builder --

    def _make_args(self, **kwargs):
        """Create an argparse.Namespace with sensible defaults for command functions."""
        defaults = {
            "name": None,
            "branch": None,
            "worktree_path": None,
            "stage": None,
            "platform": None,
            "skip": None,
            "approve": False,
            "reject": False,
            "increment": False,
            "json": False,
            "status": None,
        }
        defaults.update(kwargs)
        return argparse.Namespace(**defaults)

    # -- command runner --

    def _run_cmd(self, cmd_func, args):
        """Run a command function, capturing stdout and SystemExit.

        Returns (exit_code, output_string).
        """
        buf = io.StringIO()
        exit_code = 0
        with patch("sys.stdout", buf):
            try:
                cmd_func(args)
            except SystemExit as e:
                exit_code = e.code
        return exit_code, buf.getvalue()

    # -- workflow data builder --

    @staticmethod
    def _make_workflow_data(name="test-feature", current_stage_idx=0):
        """Build a complete workflow data dict with the given stage in_progress."""
        today = date.today().isoformat()
        stages = {}
        for i, stage_name in enumerate(workflow.STAGE_ORDER):
            entry = workflow.init_stage_entry(stage_name)
            if i < current_stage_idx:
                entry["status"] = "completed"
                entry["started_at"] = "2026-01-01T00:00:00+00:00"
                entry["completed_at"] = "2026-01-01T00:00:01+00:00"
            elif i == current_stage_idx:
                entry["status"] = "in_progress"
                entry["started_at"] = "2026-01-01T00:00:00+00:00"
            stages[stage_name] = entry

        return {
            "feature": {
                "name": name,
                "dir": f"docs/specs/{today}-{name}",
                "date": today,
                "branch": f"feature/{today}-{name}",
                "worktree_path": f".worktree/{today}-{name}",
            },
            "stages": stages,
            "metadata": {
                "created_at": "2026-01-01T00:00:00+00:00",
                "updated_at": "2026-01-01T00:00:00+00:00",
                "version": "1.0",
            },
        }

    def _write_wf(self, data=None, name="test-feature", current_stage_idx=0):
        """Write a workflow.json into the temp dir and return its Path.

        If data is None, builds one using _make_workflow_data.
        """
        if data is None:
            data = self._make_workflow_data(name=name, current_stage_idx=current_stage_idx)

        spec_dir = Path(self.temp_dir.name) / data["feature"]["dir"]
        spec_dir.mkdir(parents=True)
        wf_path = spec_dir / "workflow.json"
        with open(wf_path, "w", encoding="utf-8") as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
            f.write("\n")
        return wf_path

    def _advance_to(self, wf_path, target_stage_name):
        """Repeatedly advance through stages until target_stage_name is in_progress.

        Automatically marks all platforms as completed when passing through
        platform stages.
        """
        with patch("workflow.find_workflow_json", return_value=wf_path):
            for _ in range(len(workflow.STAGE_ORDER)):
                data = workflow.load_workflow(wf_path)
                current = workflow.get_current_stage(data)
                if current == target_stage_name:
                    return data
                if current is None:
                    raise RuntimeError(
                        f"No in_progress stage, cannot reach {target_stage_name}"
                    )
                # Auto-complete platforms for platform stages
                if current in workflow.STAGES_WITH_PLATFORMS:
                    for plat in workflow.PLATFORM_NAMES:
                        p_args = self._make_args(
                            stage=current, platform=plat, status="completed"
                        )
                        self._run_cmd(workflow.cmd_mark_platform, p_args)
                args = self._make_args()
                exit_code, output = self._run_cmd(workflow.cmd_advance, args)
                if exit_code != 0:
                    raise RuntimeError(
                        f"Advance from {current} failed: {output.strip()}"
                    )
            raise RuntimeError(f"Could not advance to {target_stage_name}")


# ============================================================
# TestConstants
# ============================================================

class TestConstants(unittest.TestCase):
    """Verify that module-level constants have expected values."""

    def test_stage_order_length(self):
        self.assertEqual(len(workflow.STAGE_ORDER), 15)

    def test_stage_order_first(self):
        self.assertEqual(workflow.STAGE_ORDER[0], "worktree-setup")

    def test_stage_order_last(self):
        self.assertEqual(workflow.STAGE_ORDER[-1], "completed")

    def test_human_review_reject_back_to_keys(self):
        expected = {"spec-human-review", "design-human-review", "code-human-review"}
        self.assertEqual(set(workflow.HUMAN_REVIEW_REJECT_BACK_TO.keys()), expected)

    def test_human_review_reject_back_to_values(self):
        self.assertEqual(
            workflow.HUMAN_REVIEW_REJECT_BACK_TO["spec-human-review"], "spec-writing"
        )
        self.assertEqual(
            workflow.HUMAN_REVIEW_REJECT_BACK_TO["design-human-review"],
            "design-shared",
        )
        self.assertEqual(
            workflow.HUMAN_REVIEW_REJECT_BACK_TO["code-human-review"],
            "coding-platforms",
        )

    def test_skippable_stages(self):
        self.assertEqual(workflow.SKIPPABLE_STAGES, {"qa-blackbox-testing", "wiki-inclusion"})

    def test_stages_with_review_loop(self):
        self.assertEqual(
            workflow.STAGES_WITH_REVIEW_LOOP,
            {"spec-review", "design-review", "coding-platforms"},
        )

    def test_stages_with_human_review(self):
        self.assertEqual(
            workflow.STAGES_WITH_HUMAN_REVIEW,
            {"spec-human-review", "design-human-review", "code-human-review"},
        )

    def test_stages_with_platforms(self):
        self.assertEqual(
            workflow.STAGES_WITH_PLATFORMS,
            {"design-platforms", "plan-platforms", "coding-platforms"},
        )

    def test_platform_names(self):
        self.assertEqual(workflow.PLATFORM_NAMES, {"backend", "ios", "android", "web"})

    def test_max_review_loops(self):
        self.assertEqual(workflow.MAX_REVIEW_LOOPS, 3)

    def test_qa_blackbox_not_in_special_sets(self):
        """qa-blackbox-testing should be a normal stage, not in any special sets."""
        self.assertNotIn("qa-blackbox-testing", workflow.STAGES_WITH_REVIEW_LOOP)
        self.assertNotIn("qa-blackbox-testing", workflow.STAGES_WITH_HUMAN_REVIEW)
        self.assertNotIn("qa-blackbox-testing", workflow.STAGES_WITH_PLATFORMS)

    def test_qa_blackbox_is_skippable(self):
        self.assertIn("qa-blackbox-testing", workflow.SKIPPABLE_STAGES)

    def test_qa_blackbox_position(self):
        """qa-blackbox-testing should be between code-human-review and worktree-merge."""
        qa_idx = workflow.get_stage_index("qa-blackbox-testing")
        self.assertEqual(workflow.STAGE_ORDER[qa_idx - 1], "code-human-review")
        self.assertEqual(workflow.STAGE_ORDER[qa_idx + 1], "worktree-merge")

    def test_init_stage_entry_qa_blackbox(self):
        """qa-blackbox-testing should be a normal stage (no review_loops, no platforms)."""
        entry = workflow.init_stage_entry("qa-blackbox-testing")
        self.assertEqual(entry["status"], "pending")
        self.assertNotIn("review_loops", entry)
        self.assertNotIn("platforms", entry)


# ============================================================
# TestPureHelpers
# ============================================================

class TestPureHelpers(unittest.TestCase):
    """Unit tests for pure functions with no I/O or side effects."""

    # -- get_stage_index --

    def test_get_stage_index_first(self):
        self.assertEqual(workflow.get_stage_index("worktree-setup"), 0)

    def test_get_stage_index_last(self):
        self.assertEqual(workflow.get_stage_index("completed"), 14)

    def test_get_stage_index_middle(self):
        self.assertEqual(workflow.get_stage_index("coding-platforms"), 9)

    def test_get_stage_index_unknown_raises(self):
        with self.assertRaises(ValueError):
            workflow.get_stage_index("nonexistent-stage")

    # -- get_current_stage --

    def test_get_current_stage_found(self):
        data = WorkflowTestBase._make_workflow_data(current_stage_idx=4)
        self.assertEqual(workflow.get_current_stage(data), "design-shared")

    def test_get_current_stage_none_when_all_pending(self):
        data = WorkflowTestBase._make_workflow_data(current_stage_idx=0)
        # Manually set all to pending
        for s in data["stages"].values():
            s["status"] = "pending"
        self.assertIsNone(workflow.get_current_stage(data))

    def test_get_current_stage_none_when_all_completed(self):
        data = WorkflowTestBase._make_workflow_data(current_stage_idx=0)
        for s in data["stages"].values():
            s["status"] = "completed"
        self.assertIsNone(workflow.get_current_stage(data))

    def test_get_current_stage_works_with_string_status(self):
        data = WorkflowTestBase._make_workflow_data(current_stage_idx=3)
        self.assertEqual(workflow.get_current_stage(data), "spec-human-review")

    # -- init_stage_entry --

    def test_init_stage_entry_basic(self):
        entry = workflow.init_stage_entry("worktree-setup")
        self.assertEqual(entry["status"], "pending")
        self.assertIsNone(entry["started_at"])
        self.assertIsNone(entry["completed_at"])
        self.assertNotIn("review_loops", entry)
        self.assertNotIn("platforms", entry)

    def test_init_stage_entry_with_review_loop(self):
        entry = workflow.init_stage_entry("spec-review")
        self.assertEqual(entry["status"], "pending")
        self.assertEqual(entry["review_loops"], 0)
        self.assertNotIn("platforms", entry)

    def test_init_stage_entry_with_platforms(self):
        entry = workflow.init_stage_entry("design-platforms")
        self.assertIn("platforms", entry)
        self.assertEqual(len(entry["platforms"]), 4)
        for plat in ("backend", "ios", "android", "web"):
            self.assertIn(plat, entry["platforms"])
            self.assertEqual(entry["platforms"][plat]["status"], "pending")
            self.assertIsNone(entry["platforms"][plat]["started_at"])
            self.assertIsNone(entry["platforms"][plat]["completed_at"])
        self.assertNotIn("review_loops", entry)

    def test_init_stage_entry_coding_platforms(self):
        entry = workflow.init_stage_entry("coding-platforms")
        self.assertIn("platforms", entry)
        self.assertEqual(entry["review_loops"], 0)
        for plat in ("backend", "ios", "android", "web"):
            self.assertEqual(entry["platforms"][plat]["review_loops"], 0)

    def test_init_stage_entry_unknown_stage(self):
        entry = workflow.init_stage_entry("some-unknown-stage")
        self.assertEqual(entry["status"], "pending")
        self.assertNotIn("review_loops", entry)
        self.assertNotIn("platforms", entry)

    # -- stamp_now --

    def test_stamp_now_returns_string(self):
        self.assertIsInstance(workflow.stamp_now(), str)

    def test_stamp_now_is_iso_format(self):
        ts = workflow.stamp_now()
        # Should match ISO 8601: YYYY-MM-DDTHH:MM:SS...
        self.assertRegex(ts, r"^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}")

    def test_stamp_now_has_timezone(self):
        ts = workflow.stamp_now()
        self.assertTrue("+" in ts or ts.endswith("Z"))


# ============================================================
# TestIOHelpers
# ============================================================

class TestIOHelpers(WorkflowTestBase):
    """Tests for I/O helper functions using real temp directories."""

    # -- load_workflow --

    def test_load_workflow_reads_valid_json(self):
        wf_path = self._write_wf(name="load-test")
        data = workflow.load_workflow(wf_path)
        self.assertEqual(data["feature"]["name"], "load-test")
        self.assertIn("stages", data)

    def test_load_workflow_file_not_found(self):
        with self.assertRaises(FileNotFoundError):
            workflow.load_workflow(Path(self.temp_dir.name) / "nonexistent.json")

    def test_load_workflow_invalid_json(self):
        bad_path = Path(self.temp_dir.name) / "bad.json"
        bad_path.write_text("not valid json {{{")
        with self.assertRaises(json.JSONDecodeError):
            workflow.load_workflow(bad_path)

    # -- save_workflow --

    def test_save_workflow_writes_file(self):
        wf_path = self._write_wf(name="save-test")
        data = workflow.load_workflow(wf_path)
        data["stages"]["worktree-setup"]["status"] = "completed"
        workflow.save_workflow(wf_path, data)
        reloaded = workflow.load_workflow(wf_path)
        self.assertEqual(reloaded["stages"]["worktree-setup"]["status"], "completed")

    def test_save_workflow_updates_updated_at(self):
        wf_path = self._write_wf(name="timestamp-test")
        data = workflow.load_workflow(wf_path)
        old_ts = data["metadata"]["updated_at"]
        data["stages"]["worktree-setup"]["status"] = "completed"
        workflow.save_workflow(wf_path, data)
        reloaded = workflow.load_workflow(wf_path)
        self.assertNotEqual(reloaded["metadata"]["updated_at"], old_ts)

    def test_save_workflow_preserves_other_data(self):
        wf_path = self._write_wf(name="preserve-test")
        data = workflow.load_workflow(wf_path)
        original_name = data["feature"]["name"]
        data["stages"]["worktree-setup"]["status"] = "completed"
        workflow.save_workflow(wf_path, data)
        reloaded = workflow.load_workflow(wf_path)
        self.assertEqual(reloaded["feature"]["name"], original_name)

    # -- find_workflow_json --

    def test_find_workflow_json_found(self):
        wf_path = self._write_wf(name="find-test")
        found = workflow.find_workflow_json()
        self.assertIsNotNone(found)
        self.assertEqual(found.resolve(), wf_path.resolve())

    def test_find_workflow_json_returns_latest(self):
        """When multiple spec dirs exist, the most recent (reverse sorted) is returned."""
        old = self._write_wf(name="old-feature")
        # Override the date to be older
        data = workflow.load_workflow(old)
        data["feature"]["date"] = "2020-01-01"
        data["feature"]["dir"] = "docs/specs/2020-01-01-old-feature"
        old.unlink()
        old.parent.rmdir()
        new_old = self._write_wf(data=data, name="old-feature")

        new = self._write_wf(name="new-feature")
        found = workflow.find_workflow_json()
        self.assertIsNotNone(found)
        # Should find the one with the most recent date (new-feature)
        self.assertIn("new-feature", str(found))

    def test_find_workflow_json_no_specs_dir(self):
        # Remove the docs directory
        import shutil
        docs_dir = Path(self.temp_dir.name) / "docs"
        if docs_dir.exists():
            shutil.rmtree(docs_dir)
        self.assertIsNone(workflow.find_workflow_json())

    def test_find_workflow_json_empty_specs_dir(self):
        specs_dir = Path(self.temp_dir.name) / "docs" / "specs"
        specs_dir.mkdir(parents=True)
        self.assertIsNone(workflow.find_workflow_json())

    def test_find_workflow_json_no_workflow_in_subdirs(self):
        specs_dir = Path(self.temp_dir.name) / "docs" / "specs" / "2026-01-01-empty"
        specs_dir.mkdir(parents=True)
        self.assertIsNone(workflow.find_workflow_json())

    @patch.dict(os.environ, {}, clear=True)
    def test_find_workflow_json_fallback_git_common_dir(self):
        """When cwd has no specs, GIT_COMMON_DIR fallback should be checked."""
        # Create workflow in a "main repo" location
        main_repo = Path(self.temp_dir.name) / "main-repo"
        spec_dir = main_repo / "docs" / "specs" / "2026-01-01-fallback-feature"
        spec_dir.mkdir(parents=True)
        data = self._make_workflow_data(name="fallback-feature")
        data["feature"]["dir"] = "docs/specs/2026-01-01-fallback-feature"
        wf_path = spec_dir / "workflow.json"
        with open(wf_path, "w", encoding="utf-8") as f:
            json.dump(data, f)

        # Set GIT_COMMON_DIR to point to .git inside main-repo
        git_dir = main_repo / ".git"
        git_dir.mkdir(parents=True)
        with patch.dict(os.environ, {"GIT_COMMON_DIR": str(git_dir)}):
            # First, make cwd have no specs
            import shutil
            cwd_specs = Path(self.temp_dir.name) / "docs"
            if cwd_specs.exists():
                shutil.rmtree(cwd_specs)
            found = workflow.find_workflow_json()
        self.assertIsNotNone(found)
        self.assertIn("fallback-feature", str(found))

    @patch.dict(os.environ, {}, clear=True)
    def test_find_workflow_json_fallback_not_found(self):
        import shutil
        cwd_specs = Path(self.temp_dir.name) / "docs"
        if cwd_specs.exists():
            shutil.rmtree(cwd_specs)
        self.assertIsNone(workflow.find_workflow_json())


# ============================================================
# TestInit
# ============================================================

class TestInit(WorkflowTestBase):
    """Integration tests for cmd_init."""

    def _do_init(self, name="test-feature", **extra_args):
        args = self._make_args(name=name, **extra_args)
        return self._run_cmd(workflow.cmd_init, args)

    def test_init_creates_directory_structure(self):
        exit_code, output = self._do_init("my-feature")
        self.assertEqual(exit_code, 0)
        result = json.loads(output.strip())
        self.assertTrue(result["ok"])
        spec_dir = Path(self.temp_dir.name) / result["feature"]["dir"]
        self.assertTrue(spec_dir.exists())
        self.assertTrue((spec_dir / "workflow.json").exists())

    def test_init_first_stage_in_progress(self):
        exit_code, output = self._do_init("stage-test")
        self.assertEqual(exit_code, 0)
        result = json.loads(output.strip())
        wf_path = Path(self.temp_dir.name) / result["feature"]["dir"] / "workflow.json"
        data = workflow.load_workflow(wf_path)
        self.assertEqual(data["stages"]["worktree-setup"]["status"], "in_progress")
        self.assertIsNotNone(data["stages"]["worktree-setup"]["started_at"])

    def test_init_all_other_stages_pending(self):
        exit_code, output = self._do_init("pending-test")
        self.assertEqual(exit_code, 0)
        result = json.loads(output.strip())
        wf_path = Path(self.temp_dir.name) / result["feature"]["dir"] / "workflow.json"
        data = workflow.load_workflow(wf_path)
        for stage_name in workflow.STAGE_ORDER[1:]:
            self.assertEqual(
                data["stages"][stage_name]["status"], "pending",
                f"{stage_name} should be pending",
            )

    def test_init_all_15_stages_present(self):
        exit_code, output = self._do_init("all-stages-test")
        self.assertEqual(exit_code, 0)
        result = json.loads(output.strip())
        wf_path = Path(self.temp_dir.name) / result["feature"]["dir"] / "workflow.json"
        data = workflow.load_workflow(wf_path)
        self.assertEqual(len(data["stages"]), 15)
        for stage_name in workflow.STAGE_ORDER:
            self.assertIn(stage_name, data["stages"])

    def test_init_feature_metadata(self):
        exit_code, output = self._do_init("meta-test")
        self.assertEqual(exit_code, 0)
        result = json.loads(output.strip())
        wf_path = Path(self.temp_dir.name) / result["feature"]["dir"] / "workflow.json"
        data = workflow.load_workflow(wf_path)
        feat = data["feature"]
        self.assertEqual(feat["name"], "meta-test")
        self.assertEqual(feat["date"], date.today().isoformat())
        self.assertEqual(feat["branch"], f"feature/{date.today().isoformat()}-meta-test")

    def test_init_platform_stages_have_platforms(self):
        exit_code, output = self._do_init("plat-test")
        self.assertEqual(exit_code, 0)
        result = json.loads(output.strip())
        wf_path = Path(self.temp_dir.name) / result["feature"]["dir"] / "workflow.json"
        data = workflow.load_workflow(wf_path)
        for stage_name in workflow.STAGES_WITH_PLATFORMS:
            self.assertIn("platforms", data["stages"][stage_name])
            self.assertEqual(len(data["stages"][stage_name]["platforms"]), 4)

    def test_init_coding_platforms_has_review_loops(self):
        exit_code, output = self._do_init("coding-plat-test")
        self.assertEqual(exit_code, 0)
        result = json.loads(output.strip())
        wf_path = Path(self.temp_dir.name) / result["feature"]["dir"] / "workflow.json"
        data = workflow.load_workflow(wf_path)
        coding = data["stages"]["coding-platforms"]
        self.assertEqual(coding["review_loops"], 0)
        for plat in workflow.PLATFORM_NAMES:
            self.assertEqual(coding["platforms"][plat]["review_loops"], 0)

    def test_init_review_stages_have_review_loops(self):
        exit_code, output = self._do_init("review-test")
        self.assertEqual(exit_code, 0)
        result = json.loads(output.strip())
        wf_path = Path(self.temp_dir.name) / result["feature"]["dir"] / "workflow.json"
        data = workflow.load_workflow(wf_path)
        for stage_name in workflow.STAGES_WITH_REVIEW_LOOP:
            self.assertEqual(data["stages"][stage_name]["review_loops"], 0)

    def test_init_output_contains_current_stage(self):
        exit_code, output = self._do_init("output-test")
        self.assertEqual(exit_code, 0)
        result = json.loads(output.strip())
        self.assertEqual(result["current_stage"], "worktree-setup")

    def test_init_dir_already_exists_error(self):
        exit_code, _ = self._do_init("dup-test")
        self.assertEqual(exit_code, 0)
        exit_code2, output2 = self._do_init("dup-test")
        self.assertEqual(exit_code2, 1)
        self.assertIn("目录已存在", output2)

    def test_init_custom_branch(self):
        exit_code, output = self._do_init("branch-test", branch="custom/my-branch")
        self.assertEqual(exit_code, 0)
        result = json.loads(output.strip())
        wf_path = Path(self.temp_dir.name) / result["feature"]["dir"] / "workflow.json"
        data = workflow.load_workflow(wf_path)
        self.assertEqual(data["feature"]["branch"], "custom/my-branch")

    def test_init_custom_worktree_path(self):
        exit_code, output = self._do_init("wt-test", worktree_path="custom/wt/path")
        self.assertEqual(exit_code, 0)
        result = json.loads(output.strip())
        wf_path = Path(self.temp_dir.name) / result["feature"]["dir"] / "workflow.json"
        data = workflow.load_workflow(wf_path)
        self.assertEqual(data["feature"]["worktree_path"], "custom/wt/path")

    def test_init_metadata_version(self):
        exit_code, output = self._do_init("version-test")
        self.assertEqual(exit_code, 0)
        result = json.loads(output.strip())
        wf_path = Path(self.temp_dir.name) / result["feature"]["dir"] / "workflow.json"
        data = workflow.load_workflow(wf_path)
        self.assertEqual(data["metadata"]["version"], "1.0")

    def test_init_default_branch_format(self):
        exit_code, output = self._do_init("default-branch")
        self.assertEqual(exit_code, 0)
        result = json.loads(output.strip())
        wf_path = Path(self.temp_dir.name) / result["feature"]["dir"] / "workflow.json"
        data = workflow.load_workflow(wf_path)
        expected = f"feature/{date.today().isoformat()}-default-branch"
        self.assertEqual(data["feature"]["branch"], expected)


# ============================================================
# TestStatus
# ============================================================

class TestStatus(WorkflowTestBase):
    """Integration tests for cmd_status."""

    def test_status_no_workflow_json(self):
        with patch("workflow.find_workflow_json", return_value=None):
            exit_code, output = self._run_cmd(
                workflow.cmd_status, self._make_args()
            )
        self.assertEqual(exit_code, 1)
        self.assertIn("未找到 workflow.json", output)

    def test_status_json_output(self):
        wf_path = self._write_wf(current_stage_idx=0)
        with patch("workflow.find_workflow_json", return_value=wf_path):
            exit_code, output = self._run_cmd(
                workflow.cmd_status, self._make_args(json=True)
            )
        self.assertEqual(exit_code, 0)
        result = json.loads(output.strip().split("\n")[-1])
        self.assertTrue(result["ok"])
        self.assertEqual(result["current_stage"], "worktree-setup")
        self.assertEqual(result["current_stage_index"], 1)
        self.assertEqual(result["total_stages"], 15)
        self.assertIn("completed_stages", result)
        self.assertIn("skipped_stages", result)
        self.assertIn("pending_stages", result)
        self.assertIn("next_stage", result)
        self.assertTrue(result["can_advance"])
        self.assertIn("platform_details", result)

    def test_status_human_readable(self):
        wf_path = self._write_wf(current_stage_idx=0)
        with patch("workflow.find_workflow_json", return_value=wf_path):
            exit_code, output = self._run_cmd(
                workflow.cmd_status, self._make_args()
            )
        self.assertEqual(exit_code, 0)
        self.assertIn("需求:", output)
        self.assertIn("test-feature", output)
        self.assertIn("当前阶段:", output)

    def test_status_with_completed_stages(self):
        wf_path = self._write_wf(current_stage_idx=3)
        with patch("workflow.find_workflow_json", return_value=wf_path):
            exit_code, output = self._run_cmd(
                workflow.cmd_status, self._make_args(json=True)
            )
        self.assertEqual(exit_code, 0)
        result = json.loads(output.strip().split("\n")[-1])
        self.assertIn("worktree-setup", result["completed_stages"])
        self.assertIn("spec-writing", result["completed_stages"])
        self.assertIn("spec-review", result["completed_stages"])
        self.assertEqual(result["current_stage"], "spec-human-review")

    def test_status_can_advance_false_when_all_completed(self):
        data = self._make_workflow_data(current_stage_idx=0)
        for s in data["stages"].values():
            s["status"] = "completed"
        wf_path = self._write_wf(data=data)
        with patch("workflow.find_workflow_json", return_value=wf_path):
            exit_code, output = self._run_cmd(
                workflow.cmd_status, self._make_args(json=True)
            )
        self.assertEqual(exit_code, 0)
        result = json.loads(output.strip().split("\n")[-1])
        self.assertFalse(result["can_advance"])
        self.assertIsNone(result["current_stage"])

    def test_status_shows_skipped(self):
        data = self._make_workflow_data(current_stage_idx=13)
        data["stages"]["wiki-inclusion"]["status"] = "skipped"
        wf_path = self._write_wf(data=data)
        with patch("workflow.find_workflow_json", return_value=wf_path):
            exit_code, output = self._run_cmd(
                workflow.cmd_status, self._make_args(json=True)
            )
        self.assertEqual(exit_code, 0)
        result = json.loads(output.strip().split("\n")[-1])
        self.assertIn("wiki-inclusion", result["skipped_stages"])


# ============================================================
# TestAdvance
# ============================================================

class TestAdvance(WorkflowTestBase):
    """Integration tests for cmd_advance."""

    def _do_advance(self, **kwargs):
        args = self._make_args(**kwargs)
        return self._run_cmd(workflow.cmd_advance, args)

    def test_advance_no_workflow_json(self):
        with patch("workflow.find_workflow_json", return_value=None):
            exit_code, output = self._do_advance()
        self.assertEqual(exit_code, 1)
        self.assertIn("未找到 workflow.json", output)

    def test_advance_no_in_progress_stage(self):
        data = self._make_workflow_data(current_stage_idx=0)
        for s in data["stages"].values():
            s["status"] = "completed"
        wf_path = self._write_wf(data=data)
        with patch("workflow.find_workflow_json", return_value=wf_path):
            exit_code, output = self._do_advance()
        self.assertEqual(exit_code, 1)
        self.assertIn("没有 in_progress 的阶段", output)

    def test_advance_stage_mismatch(self):
        wf_path = self._write_wf(current_stage_idx=2)
        with patch("workflow.find_workflow_json", return_value=wf_path):
            exit_code, output = self._do_advance(stage="worktree-setup")
        self.assertEqual(exit_code, 1)
        self.assertIn("不匹配", output)

    def test_advance_normal_progression(self):
        wf_path = self._write_wf(current_stage_idx=0)
        with patch("workflow.find_workflow_json", return_value=wf_path):
            exit_code, output = self._do_advance()
        self.assertEqual(exit_code, 0)
        result = json.loads(output.strip().split("\n")[-1])
        self.assertTrue(result["ok"])
        self.assertEqual(result["completed"], "worktree-setup")
        self.assertEqual(result["advanced_to"], "spec-writing")

        data = workflow.load_workflow(wf_path)
        self.assertEqual(data["stages"]["worktree-setup"]["status"], "completed")
        self.assertIsNotNone(data["stages"]["worktree-setup"]["completed_at"])
        self.assertEqual(data["stages"]["spec-writing"]["status"], "in_progress")
        self.assertIsNotNone(data["stages"]["spec-writing"]["started_at"])

    def test_advance_mid_workflow(self):
        wf_path = self._write_wf(current_stage_idx=3)
        with patch("workflow.find_workflow_json", return_value=wf_path):
            exit_code, output = self._do_advance()
        self.assertEqual(exit_code, 0)
        result = json.loads(output.strip().split("\n")[-1])
        self.assertEqual(result["completed"], "spec-human-review")
        self.assertEqual(result["advanced_to"], "design-shared")

    def test_advance_to_last_stage(self):
        # wiki-inclusion (idx 13) → completed (idx 14)
        wf_path = self._write_wf(current_stage_idx=13)
        with patch("workflow.find_workflow_json", return_value=wf_path):
            exit_code, output = self._do_advance()
        self.assertEqual(exit_code, 0)
        result = json.loads(output.strip().split("\n")[-1])
        self.assertTrue(result["ok"])
        self.assertEqual(result["completed"], "wiki-inclusion")
        self.assertEqual(result["advanced_to"], "completed")

    def test_advance_from_completed_is_final(self):
        """Advancing from the last real stage (completed) shows all-done message."""
        wf_path = self._write_wf(current_stage_idx=14)
        with patch("workflow.find_workflow_json", return_value=wf_path):
            exit_code, output = self._do_advance()
        self.assertEqual(exit_code, 0)
        result = json.loads(output.strip().split("\n")[-1])
        self.assertTrue(result["ok"])
        self.assertEqual(result["completed"], "completed")
        self.assertIsNone(result["advanced_to"])
        self.assertIn("所有阶段已完成", result["message"])

    def test_advance_skip_wiki_inclusion(self):
        # wiki-inclusion must be pending for skip to work
        wf_path = self._write_wf(current_stage_idx=12)  # worktree-merge in_progress
        with patch("workflow.find_workflow_json", return_value=wf_path):
            exit_code, output = self._do_advance(skip="wiki-inclusion")
        self.assertEqual(exit_code, 0)
        result = json.loads(output.strip().split("\n")[-1])
        self.assertTrue(result["ok"])
        self.assertEqual(result["action"], "skipped")
        self.assertEqual(result["stage"], "wiki-inclusion")
        data = workflow.load_workflow(wf_path)
        self.assertEqual(data["stages"]["wiki-inclusion"]["status"], "skipped")

    def test_advance_skip_non_skippable(self):
        wf_path = self._write_wf(current_stage_idx=3)
        with patch("workflow.find_workflow_json", return_value=wf_path):
            exit_code, output = self._do_advance(skip="spec-writing")
        self.assertEqual(exit_code, 1)
        self.assertIn("不可跳过", output)

    def test_advance_skip_non_pending(self):
        data = self._make_workflow_data(current_stage_idx=0)
        data["stages"]["wiki-inclusion"]["status"] = "completed"
        wf_path = self._write_wf(data=data)
        with patch("workflow.find_workflow_json", return_value=wf_path):
            exit_code, output = self._do_advance(skip="wiki-inclusion")
        self.assertEqual(exit_code, 1)
        self.assertIn("不可跳过", output)

    def test_advance_platform_stage_blocked_by_incomplete(self):
        wf_path = self._write_wf(current_stage_idx=5)
        with patch("workflow.find_workflow_json", return_value=wf_path):
            exit_code, output = self._do_advance()
        self.assertEqual(exit_code, 1)
        self.assertIn("未完成的平台", output)

    def test_advance_platform_stage_all_completed(self):
        data = self._make_workflow_data(current_stage_idx=5)  # design-platforms
        for plat in workflow.PLATFORM_NAMES:
            data["stages"]["design-platforms"]["platforms"][plat]["status"] = "completed"
        wf_path = self._write_wf(data=data)
        with patch("workflow.find_workflow_json", return_value=wf_path):
            exit_code, output = self._do_advance()
        self.assertEqual(exit_code, 0)
        result = json.loads(output.strip().split("\n")[-1])
        self.assertEqual(result["advanced_to"], "design-review")

    def test_advance_platform_stage_with_skipped_platforms(self):
        data = self._make_workflow_data(current_stage_idx=5)
        for plat in ("backend", "ios", "android"):
            data["stages"]["design-platforms"]["platforms"][plat]["status"] = "completed"
        data["stages"]["design-platforms"]["platforms"]["web"]["status"] = "skipped"
        wf_path = self._write_wf(data=data)
        with patch("workflow.find_workflow_json", return_value=wf_path):
            exit_code, output = self._do_advance()
        self.assertEqual(exit_code, 0)

    def test_advance_skips_already_skipped_intermediate(self):
        """Advance past a skipped stage should skip over it cleanly."""
        data = self._make_workflow_data(current_stage_idx=12)  # worktree-merge
        data["stages"]["wiki-inclusion"]["status"] = "skipped"
        wf_path = self._write_wf(data=data)
        with patch("workflow.find_workflow_json", return_value=wf_path):
            exit_code, output = self._do_advance()
        self.assertEqual(exit_code, 0)
        result = json.loads(output.strip().split("\n")[-1])
        self.assertEqual(result["completed"], "worktree-merge")
        self.assertEqual(result["advanced_to"], "completed")

    def test_advance_auto_skip_is_dead_code(self):
        """Document that auto-skip (line 316) is unreachable in normal operation.

        get_current_stage only returns stages with status == "in_progress",
        so the check `data["stages"][current]["status"] == "skipped"` at
        line 316 can never be True. To trigger it, one would have to manually
        set a stage to "skipped" in the JSON while it's also "in_progress".
        """
        # Manually force a contradictory state
        data = self._make_workflow_data(current_stage_idx=13)
        data["stages"]["wiki-inclusion"]["status"] = "skipped"
        # get_current_stage will not find wiki-inclusion because it's "skipped"
        self.assertIsNone(workflow.get_current_stage(data))

    # -- qa-blackbox-testing specific tests --

    def test_advance_skip_qa_blackbox(self):
        """qa-blackbox-testing can be skipped with --skip."""
        wf_path = self._write_wf(current_stage_idx=10)  # code-human-review in_progress
        # Approve to advance to qa-blackbox-testing
        with patch("workflow.find_workflow_json", return_value=wf_path):
            exit_code, _ = self._run_cmd(
                workflow.cmd_human_review,
                self._make_args(stage="code-human-review", approve=True),
            )
            self.assertEqual(exit_code, 0)

        # Now qa-blackbox-testing should be in_progress (idx 11)
        data = workflow.load_workflow(wf_path)
        self.assertEqual(data["stages"]["qa-blackbox-testing"]["status"], "in_progress")

        # Skip it
        with patch("workflow.find_workflow_json", return_value=wf_path):
            exit_code, output = self._do_advance(skip="qa-blackbox-testing")
        self.assertEqual(exit_code, 0)
        result = json.loads(output.strip().split("\n")[-1])
        self.assertTrue(result["ok"])
        self.assertEqual(result["action"], "skipped")
        self.assertEqual(result["stage"], "qa-blackbox-testing")
        data = workflow.load_workflow(wf_path)
        self.assertEqual(data["stages"]["qa-blackbox-testing"]["status"], "skipped")

    def test_advance_qa_blackbox_normal_progression(self):
        """Advancing from qa-blackbox-testing goes to worktree-merge."""
        wf_path = self._write_wf(current_stage_idx=11)  # qa-blackbox-testing in_progress
        with patch("workflow.find_workflow_json", return_value=wf_path):
            exit_code, output = self._do_advance()
        self.assertEqual(exit_code, 0)
        result = json.loads(output.strip().split("\n")[-1])
        self.assertTrue(result["ok"])
        self.assertEqual(result["completed"], "qa-blackbox-testing")
        self.assertEqual(result["advanced_to"], "worktree-merge")


# ============================================================
# TestReviewLoop
# ============================================================

class TestReviewLoop(WorkflowTestBase):
    """Integration tests for cmd_review_loop."""

    def _do_review_loop(self, **kwargs):
        args = self._make_args(**kwargs)
        return self._run_cmd(workflow.cmd_review_loop, args)

    def test_review_loop_no_workflow_json(self):
        with patch("workflow.find_workflow_json", return_value=None):
            exit_code, output = self._do_review_loop(stage="spec-review")
        self.assertEqual(exit_code, 1)

    def test_review_loop_not_supported_stage(self):
        wf_path = self._write_wf(current_stage_idx=1)
        with patch("workflow.find_workflow_json", return_value=wf_path):
            exit_code, output = self._do_review_loop(
                stage="spec-writing", increment=True
            )
        self.assertEqual(exit_code, 1)
        self.assertIn("不支持 review-loop", output)

    def test_review_loop_increment_spec_review(self):
        wf_path = self._write_wf(current_stage_idx=2)
        with patch("workflow.find_workflow_json", return_value=wf_path):
            exit_code, output = self._do_review_loop(
                stage="spec-review", increment=True
            )
        self.assertEqual(exit_code, 0)
        result = json.loads(output.strip().split("\n")[-1])
        self.assertTrue(result["ok"])
        self.assertEqual(result["review_loops"], 1)
        data = workflow.load_workflow(wf_path)
        self.assertEqual(data["stages"]["spec-review"]["review_loops"], 1)

    def test_review_loop_no_increment_just_read(self):
        wf_path = self._write_wf(current_stage_idx=2)
        with patch("workflow.find_workflow_json", return_value=wf_path):
            exit_code, output = self._do_review_loop(stage="spec-review")
        self.assertEqual(exit_code, 0)
        result = json.loads(output.strip().split("\n")[-1])
        self.assertEqual(result["review_loops"], 0)
        data = workflow.load_workflow(wf_path)
        self.assertEqual(data["stages"]["spec-review"]["review_loops"], 0)

    def test_review_loop_multiple_increments(self):
        wf_path = self._write_wf(current_stage_idx=2)
        with patch("workflow.find_workflow_json", return_value=wf_path):
            for i in range(3):
                exit_code, _ = self._do_review_loop(
                    stage="spec-review", increment=True
                )
                self.assertEqual(exit_code, 0)
        data = workflow.load_workflow(wf_path)
        self.assertEqual(data["stages"]["spec-review"]["review_loops"], 3)

    def test_review_loop_coding_platforms_with_platform(self):
        data = self._make_workflow_data(current_stage_idx=9)
        wf_path = self._write_wf(data=data)
        with patch("workflow.find_workflow_json", return_value=wf_path):
            exit_code, output = self._do_review_loop(
                stage="coding-platforms", platform="ios", increment=True
            )
        self.assertEqual(exit_code, 0)
        result = json.loads(output.strip().split("\n")[-1])
        self.assertEqual(result["review_loops"], 1)
        self.assertEqual(result["platform"], "ios")
        data = workflow.load_workflow(wf_path)
        self.assertEqual(
            data["stages"]["coding-platforms"]["platforms"]["ios"]["review_loops"], 1
        )
        # Other platforms unaffected
        self.assertEqual(
            data["stages"]["coding-platforms"]["platforms"]["android"]["review_loops"], 0
        )

    def test_review_loop_coding_platforms_invalid_platform(self):
        wf_path = self._write_wf(current_stage_idx=9)
        with patch("workflow.find_workflow_json", return_value=wf_path):
            exit_code, output = self._do_review_loop(
                stage="coding-platforms", platform="linux", increment=True
            )
        self.assertEqual(exit_code, 1)
        self.assertIn("未知平台", output)

    def test_review_loop_platform_on_non_coding_stage(self):
        wf_path = self._write_wf(current_stage_idx=2)
        with patch("workflow.find_workflow_json", return_value=wf_path):
            exit_code, output = self._do_review_loop(
                stage="spec-review", platform="ios", increment=True
            )
        self.assertEqual(exit_code, 1)

    def test_review_loop_warning_at_max(self):
        data = self._make_workflow_data(current_stage_idx=2)
        data["stages"]["spec-review"]["review_loops"] = 2
        wf_path = self._write_wf(data=data)
        with patch("workflow.find_workflow_json", return_value=wf_path):
            exit_code, output = self._do_review_loop(
                stage="spec-review", increment=True
            )
        self.assertEqual(exit_code, 0)
        result = json.loads(output.strip().split("\n")[-1])
        self.assertEqual(result["review_loops"], 3)
        self.assertIn("warning", result)
        self.assertIn("上限", result["warning"])

    def test_review_loop_warning_above_max(self):
        data = self._make_workflow_data(current_stage_idx=2)
        data["stages"]["spec-review"]["review_loops"] = 3
        wf_path = self._write_wf(data=data)
        with patch("workflow.find_workflow_json", return_value=wf_path):
            exit_code, output = self._do_review_loop(
                stage="spec-review", increment=True
            )
        result = json.loads(output.strip().split("\n")[-1])
        self.assertEqual(result["review_loops"], 4)
        self.assertIn("warning", result)

    def test_review_loop_no_warning_below_max(self):
        data = self._make_workflow_data(current_stage_idx=2)
        data["stages"]["spec-review"]["review_loops"] = 1
        wf_path = self._write_wf(data=data)
        with patch("workflow.find_workflow_json", return_value=wf_path):
            exit_code, output = self._do_review_loop(
                stage="spec-review", increment=True
            )
        result = json.loads(output.strip().split("\n")[-1])
        self.assertEqual(result["review_loops"], 2)
        self.assertNotIn("warning", result)


# ============================================================
# TestHumanReview
# ============================================================

class TestHumanReview(WorkflowTestBase):
    """Integration tests for cmd_human_review."""

    def _do_hr(self, **kwargs):
        args = self._make_args(**kwargs)
        return self._run_cmd(workflow.cmd_human_review, args)

    def test_human_review_no_workflow_json(self):
        with patch("workflow.find_workflow_json", return_value=None):
            exit_code, output = self._do_hr(stage="spec-human-review", approve=True)
        self.assertEqual(exit_code, 1)

    def test_human_review_not_human_review_stage(self):
        wf_path = self._write_wf(current_stage_idx=1)
        with patch("workflow.find_workflow_json", return_value=wf_path):
            exit_code, output = self._do_hr(stage="spec-writing", approve=True)
        self.assertEqual(exit_code, 1)
        self.assertIn("不是人工确认阶段", output)

    # -- approve --

    def test_approve_spec_human_review(self):
        wf_path = self._write_wf(current_stage_idx=3)
        with patch("workflow.find_workflow_json", return_value=wf_path):
            exit_code, output = self._do_hr(stage="spec-human-review", approve=True)
        self.assertEqual(exit_code, 0)
        result = json.loads(output.strip().split("\n")[-1])
        self.assertEqual(result["action"], "approved")
        self.assertEqual(result["stage"], "spec-human-review")
        self.assertEqual(result["advanced_to"], "design-shared")

        data = workflow.load_workflow(wf_path)
        self.assertEqual(data["stages"]["spec-human-review"]["status"], "completed")
        self.assertEqual(data["stages"]["design-shared"]["status"], "in_progress")

    def test_approve_design_human_review(self):
        wf_path = self._write_wf(current_stage_idx=7)
        with patch("workflow.find_workflow_json", return_value=wf_path):
            exit_code, output = self._do_hr(
                stage="design-human-review", approve=True
            )
        self.assertEqual(exit_code, 0)
        result = json.loads(output.strip().split("\n")[-1])
        self.assertEqual(result["advanced_to"], "plan-platforms")

    def test_approve_code_human_review(self):
        wf_path = self._write_wf(current_stage_idx=10)
        with patch("workflow.find_workflow_json", return_value=wf_path):
            exit_code, output = self._do_hr(
                stage="code-human-review", approve=True
            )
        self.assertEqual(exit_code, 0)
        result = json.loads(output.strip().split("\n")[-1])
        self.assertEqual(result["advanced_to"], "qa-blackbox-testing")

    def test_approve_skips_intermediate_skipped(self):
        data = self._make_workflow_data(current_stage_idx=10)  # code-human-review
        data["stages"]["qa-blackbox-testing"]["status"] = "skipped"
        data["stages"]["worktree-merge"]["status"] = "skipped"
        wf_path = self._write_wf(data=data)
        with patch("workflow.find_workflow_json", return_value=wf_path):
            exit_code, output = self._do_hr(
                stage="code-human-review", approve=True
            )
        self.assertEqual(exit_code, 0)
        result = json.loads(output.strip().split("\n")[-1])
        self.assertEqual(result["advanced_to"], "wiki-inclusion")

    # -- reject (full) --

    def test_reject_spec_human_review(self):
        wf_path = self._write_wf(current_stage_idx=3)
        with patch("workflow.find_workflow_json", return_value=wf_path):
            exit_code, output = self._do_hr(stage="spec-human-review", reject=True)
        self.assertEqual(exit_code, 0)
        result = json.loads(output.strip().split("\n")[-1])
        self.assertEqual(result["action"], "rejected")
        self.assertEqual(result["back_to"], "spec-writing")

        data = workflow.load_workflow(wf_path)
        self.assertEqual(data["stages"]["spec-human-review"]["status"], "pending")
        self.assertEqual(data["stages"]["spec-writing"]["status"], "in_progress")
        # Review stage in between should be reset
        self.assertEqual(data["stages"]["spec-review"]["status"], "pending")
        self.assertEqual(data["stages"]["spec-review"]["review_loops"], 0)

    def test_reject_design_human_review(self):
        wf_path = self._write_wf(current_stage_idx=7)
        with patch("workflow.find_workflow_json", return_value=wf_path):
            exit_code, output = self._do_hr(
                stage="design-human-review", reject=True
            )
        self.assertEqual(exit_code, 0)
        result = json.loads(output.strip().split("\n")[-1])
        self.assertEqual(result["back_to"], "design-shared")

        data = workflow.load_workflow(wf_path)
        self.assertEqual(data["stages"]["design-shared"]["status"], "in_progress")
        self.assertEqual(data["stages"]["design-human-review"]["status"], "pending")

    def test_reject_code_human_review_full(self):
        wf_path = self._write_wf(current_stage_idx=10)
        with patch("workflow.find_workflow_json", return_value=wf_path):
            exit_code, output = self._do_hr(
                stage="code-human-review", reject=True
            )
        self.assertEqual(exit_code, 0)
        result = json.loads(output.strip().split("\n")[-1])
        self.assertEqual(result["back_to"], "coding-platforms")

        data = workflow.load_workflow(wf_path)
        self.assertEqual(data["stages"]["coding-platforms"]["status"], "in_progress")
        self.assertEqual(data["stages"]["coding-platforms"]["review_loops"], 0)

    # -- platform-level reject --

    def test_platform_reject_code_human_review(self):
        data = self._make_workflow_data(current_stage_idx=10)
        # Set coding-platforms as completed with all platforms completed
        data["stages"]["coding-platforms"]["status"] = "completed"
        data["stages"]["coding-platforms"]["completed_at"] = "2026-01-01T00:00:02+00:00"
        for plat in workflow.PLATFORM_NAMES:
            data["stages"]["coding-platforms"]["platforms"][plat]["status"] = "completed"
            data["stages"]["coding-platforms"]["platforms"][plat]["completed_at"] = (
                "2026-01-01T00:00:02+00:00"
            )
            data["stages"]["coding-platforms"]["platforms"][plat]["review_loops"] = 2
        wf_path = self._write_wf(data=data)

        with patch("workflow.find_workflow_json", return_value=wf_path):
            exit_code, output = self._do_hr(
                stage="code-human-review", platform="ios", reject=True
            )
        self.assertEqual(exit_code, 0)
        result = json.loads(output.strip().split("\n")[-1])
        self.assertEqual(result["action"], "rejected")
        self.assertEqual(result["platform"], "ios")

        data = workflow.load_workflow(wf_path)
        # Only ios should be reset
        self.assertEqual(
            data["stages"]["coding-platforms"]["platforms"]["ios"]["status"],
            "in_progress",
        )
        self.assertIsNone(
            data["stages"]["coding-platforms"]["platforms"]["ios"]["completed_at"]
        )
        # Other platforms should be untouched
        self.assertEqual(
            data["stages"]["coding-platforms"]["platforms"]["android"]["status"],
            "completed",
        )
        self.assertEqual(
            data["stages"]["coding-platforms"]["platforms"]["backend"]["status"],
            "completed",
        )
        # coding-platforms overall should be in_progress
        self.assertEqual(data["stages"]["coding-platforms"]["status"], "in_progress")

    def test_platform_reject_invalid_platform(self):
        wf_path = self._write_wf(current_stage_idx=10)
        with patch("workflow.find_workflow_json", return_value=wf_path):
            exit_code, output = self._do_hr(
                stage="code-human-review", platform="linux", reject=True
            )
        self.assertEqual(exit_code, 1)
        self.assertIn("未知平台", output)

    # -- known bugs --

    def test_bug_neither_approve_nor_reject(self):
        """Known Bug: cmd_human_review with neither --approve nor --reject silently
        does nothing and prints nothing."""
        wf_path = self._write_wf(current_stage_idx=3)
        with patch("workflow.find_workflow_json", return_value=wf_path):
            exit_code, output = self._do_hr(stage="spec-human-review")
        # No SystemExit raised, no output -- this is the current (buggy) behavior
        self.assertEqual(exit_code, 0)
        self.assertEqual(output.strip(), "")

    def test_bug_both_approve_and_reject(self):
        """Known Bug: When both --approve and --reject are passed, --approve wins
        silently because it's an if/elif chain."""
        wf_path = self._write_wf(current_stage_idx=3)
        with patch("workflow.find_workflow_json", return_value=wf_path):
            exit_code, output = self._do_hr(
                stage="spec-human-review", approve=True, reject=True
            )
        self.assertEqual(exit_code, 0)
        result = json.loads(output.strip().split("\n")[-1])
        self.assertEqual(result["action"], "approved")
        # reject was silently ignored


# ============================================================
# TestMarkPlatform
# ============================================================

class TestMarkPlatform(WorkflowTestBase):
    """Integration tests for cmd_mark_platform."""

    def _do_mark(self, **kwargs):
        args = self._make_args(**kwargs)
        return self._run_cmd(workflow.cmd_mark_platform, args)

    def test_mark_platform_no_workflow_json(self):
        with patch("workflow.find_workflow_json", return_value=None):
            exit_code, output = self._do_mark(
                stage="design-platforms", platform="ios", status="completed"
            )
        self.assertEqual(exit_code, 1)

    def test_mark_platform_not_platform_stage(self):
        wf_path = self._write_wf(current_stage_idx=1)
        with patch("workflow.find_workflow_json", return_value=wf_path):
            exit_code, output = self._do_mark(
                stage="spec-writing", platform="ios", status="completed"
            )
        self.assertEqual(exit_code, 1)
        self.assertIn("不是并行平台阶段", output)

    def test_mark_platform_invalid_platform(self):
        wf_path = self._write_wf(current_stage_idx=5)
        with patch("workflow.find_workflow_json", return_value=wf_path):
            exit_code, output = self._do_mark(
                stage="design-platforms", platform="linux", status="completed"
            )
        self.assertEqual(exit_code, 1)
        self.assertIn("未知平台", output)

    def test_mark_platform_completed(self):
        wf_path = self._write_wf(current_stage_idx=5)
        with patch("workflow.find_workflow_json", return_value=wf_path):
            exit_code, output = self._do_mark(
                stage="design-platforms", platform="ios", status="completed"
            )
        self.assertEqual(exit_code, 0)
        result = json.loads(output.strip().split("\n")[-1])
        self.assertEqual(result["new_status"], "completed")
        data = workflow.load_workflow(wf_path)
        self.assertEqual(
            data["stages"]["design-platforms"]["platforms"]["ios"]["status"], "completed"
        )
        self.assertIsNotNone(
            data["stages"]["design-platforms"]["platforms"]["ios"]["completed_at"]
        )

    def test_mark_platform_skipped(self):
        wf_path = self._write_wf(current_stage_idx=5)
        with patch("workflow.find_workflow_json", return_value=wf_path):
            exit_code, output = self._do_mark(
                stage="design-platforms", platform="web", status="skipped"
            )
        self.assertEqual(exit_code, 0)
        result = json.loads(output.strip().split("\n")[-1])
        self.assertEqual(result["new_status"], "skipped")
        data = workflow.load_workflow(wf_path)
        self.assertEqual(
            data["stages"]["design-platforms"]["platforms"]["web"]["status"], "skipped"
        )
        self.assertIsNotNone(
            data["stages"]["design-platforms"]["platforms"]["web"]["completed_at"]
        )

    def test_mark_platform_in_progress(self):
        wf_path = self._write_wf(current_stage_idx=5)
        with patch("workflow.find_workflow_json", return_value=wf_path):
            exit_code, output = self._do_mark(
                stage="design-platforms", platform="backend", status="in_progress"
            )
        self.assertEqual(exit_code, 0)
        data = workflow.load_workflow(wf_path)
        self.assertEqual(
            data["stages"]["design-platforms"]["platforms"]["backend"]["status"],
            "in_progress",
        )
        self.assertIsNotNone(
            data["stages"]["design-platforms"]["platforms"]["backend"]["started_at"]
        )

    def test_mark_platform_in_progress_preserves_started_at(self):
        data = self._make_workflow_data(current_stage_idx=5)
        data["stages"]["design-platforms"]["platforms"]["ios"]["started_at"] = (
            "2026-01-01T00:00:00+00:00"
        )
        wf_path = self._write_wf(data=data)
        with patch("workflow.find_workflow_json", return_value=wf_path):
            self._do_mark(stage="design-platforms", platform="ios", status="in_progress")
        data = workflow.load_workflow(wf_path)
        self.assertEqual(
            data["stages"]["design-platforms"]["platforms"]["ios"]["started_at"],
            "2026-01-01T00:00:00+00:00",
        )

    def test_mark_platform_failed(self):
        wf_path = self._write_wf(current_stage_idx=5)
        with patch("workflow.find_workflow_json", return_value=wf_path):
            exit_code, output = self._do_mark(
                stage="design-platforms", platform="ios", status="failed"
            )
        self.assertEqual(exit_code, 0)
        result = json.loads(output.strip().split("\n")[-1])
        self.assertEqual(result["new_status"], "failed")

    def test_mark_platform_pending(self):
        data = self._make_workflow_data(current_stage_idx=5)
        data["stages"]["design-platforms"]["platforms"]["ios"]["status"] = "completed"
        wf_path = self._write_wf(data=data)
        with patch("workflow.find_workflow_json", return_value=wf_path):
            exit_code, output = self._do_mark(
                stage="design-platforms", platform="ios", status="pending"
            )
        self.assertEqual(exit_code, 0)
        result = json.loads(output.strip().split("\n")[-1])
        self.assertEqual(result["new_status"], "pending")

    def test_mark_platform_all_done(self):
        data = self._make_workflow_data(current_stage_idx=5)
        for plat in ("backend", "android", "web"):
            data["stages"]["design-platforms"]["platforms"][plat]["status"] = "completed"
        wf_path = self._write_wf(data=data)
        with patch("workflow.find_workflow_json", return_value=wf_path):
            exit_code, output = self._do_mark(
                stage="design-platforms", platform="ios", status="completed"
            )
        self.assertEqual(exit_code, 0)
        result = json.loads(output.strip().split("\n")[-1])
        self.assertTrue(result["all_platforms_done"])
        self.assertEqual(result["remaining_platforms"], [])

    def test_mark_platform_some_remaining(self):
        data = self._make_workflow_data(current_stage_idx=5)
        data["stages"]["design-platforms"]["platforms"]["backend"]["status"] = "completed"
        wf_path = self._write_wf(data=data)
        with patch("workflow.find_workflow_json", return_value=wf_path):
            exit_code, output = self._do_mark(
                stage="design-platforms", platform="ios", status="completed"
            )
        self.assertEqual(exit_code, 0)
        result = json.loads(output.strip().split("\n")[-1])
        self.assertFalse(result["all_platforms_done"])
        self.assertIn("android", result["remaining_platforms"])
        self.assertIn("web", result["remaining_platforms"])

    def test_mark_platform_old_status_in_output(self):
        wf_path = self._write_wf(current_stage_idx=5)
        with patch("workflow.find_workflow_json", return_value=wf_path):
            exit_code, output = self._do_mark(
                stage="design-platforms", platform="ios", status="completed"
            )
        result = json.loads(output.strip().split("\n")[-1])
        self.assertEqual(result["old_status"], "pending")

    # -- known bugs --

    def test_bug_failed_does_not_clear_completed_at(self):
        """Known Bug: Marking failed after completed keeps the stale completed_at."""
        data = self._make_workflow_data(current_stage_idx=5)
        data["stages"]["design-platforms"]["platforms"]["ios"]["status"] = "completed"
        data["stages"]["design-platforms"]["platforms"]["ios"]["completed_at"] = (
            "2026-01-01T00:00:00+00:00"
        )
        wf_path = self._write_wf(data=data)
        with patch("workflow.find_workflow_json", return_value=wf_path):
            exit_code, output = self._do_mark(
                stage="design-platforms", platform="ios", status="failed"
            )
        self.assertEqual(exit_code, 0)
        data = workflow.load_workflow(wf_path)
        plat = data["stages"]["design-platforms"]["platforms"]["ios"]
        self.assertEqual(plat["status"], "failed")
        # Bug: completed_at is NOT cleared
        self.assertIsNotNone(plat.get("completed_at"))

    def test_bug_pending_does_not_clear_completed_at(self):
        """Known Bug: Marking pending after completed keeps the stale completed_at."""
        data = self._make_workflow_data(current_stage_idx=5)
        data["stages"]["design-platforms"]["platforms"]["ios"]["status"] = "completed"
        data["stages"]["design-platforms"]["platforms"]["ios"]["completed_at"] = (
            "2026-01-01T00:00:00+00:00"
        )
        wf_path = self._write_wf(data=data)
        with patch("workflow.find_workflow_json", return_value=wf_path):
            exit_code, output = self._do_mark(
                stage="design-platforms", platform="ios", status="pending"
            )
        self.assertEqual(exit_code, 0)
        data = workflow.load_workflow(wf_path)
        plat = data["stages"]["design-platforms"]["platforms"]["ios"]
        self.assertEqual(plat["status"], "pending")
        # Bug: completed_at is NOT cleared
        self.assertIsNotNone(plat.get("completed_at"))


# ============================================================
# TestFullWorkflow
# ============================================================

class TestFullWorkflow(WorkflowTestBase):
    """End-to-end integration tests simulating real workflow progression."""

    def test_full_workflow_15_stages(self):
        """Simulate advancing through all 15 stages."""
        # Phase 1: init
        exit_code, output = self._do_init("e2e-feature")
        self.assertEqual(exit_code, 0)
        result = json.loads(output.strip())
        wf_path = Path(self.temp_dir.name) / result["feature"]["dir"] / "workflow.json"

        # Phase 2: advance through non-platform stages
        with patch("workflow.find_workflow_json", return_value=wf_path):
            # advance: worktree-setup -> spec-writing
            exit_code, _ = self._run_cmd(workflow.cmd_advance, self._make_args())
            self.assertEqual(exit_code, 0)

            # advance: spec-writing -> spec-review
            exit_code, _ = self._run_cmd(workflow.cmd_advance, self._make_args())
            self.assertEqual(exit_code, 0)

            # review-loop on spec-review
            exit_code, _ = self._run_cmd(
                workflow.cmd_review_loop,
                self._make_args(stage="spec-review", increment=True),
            )
            self.assertEqual(exit_code, 0)

            # advance: spec-review -> spec-human-review
            exit_code, _ = self._run_cmd(workflow.cmd_advance, self._make_args())
            self.assertEqual(exit_code, 0)

            # human-review approve: spec-human-review -> design-shared
            exit_code, _ = self._run_cmd(
                workflow.cmd_human_review,
                self._make_args(stage="spec-human-review", approve=True),
            )
            self.assertEqual(exit_code, 0)

            # advance: design-shared -> design-platforms
            exit_code, _ = self._run_cmd(workflow.cmd_advance, self._make_args())
            self.assertEqual(exit_code, 0)

            # mark platforms for design-platforms
            for plat in ("backend", "ios", "android"):
                exit_code, _ = self._run_cmd(
                    workflow.cmd_mark_platform,
                    self._make_args(
                        stage="design-platforms", platform=plat, status="completed"
                    ),
                )
                self.assertEqual(exit_code, 0)
            # web skipped
            exit_code, _ = self._run_cmd(
                workflow.cmd_mark_platform,
                self._make_args(
                    stage="design-platforms", platform="web", status="skipped"
                ),
            )
            self.assertEqual(exit_code, 0)

            # advance: design-platforms -> design-review
            exit_code, _ = self._run_cmd(workflow.cmd_advance, self._make_args())
            self.assertEqual(exit_code, 0)

            # advance: design-review -> design-human-review
            exit_code, _ = self._run_cmd(workflow.cmd_advance, self._make_args())
            self.assertEqual(exit_code, 0)

            # human-review approve: design-human-review -> plan-platforms
            exit_code, _ = self._run_cmd(
                workflow.cmd_human_review,
                self._make_args(stage="design-human-review", approve=True),
            )
            self.assertEqual(exit_code, 0)

            # mark platforms for plan-platforms (web skipped)
            for plat in ("backend", "ios", "android"):
                exit_code, _ = self._run_cmd(
                    workflow.cmd_mark_platform,
                    self._make_args(
                        stage="plan-platforms", platform=plat, status="completed"
                    ),
                )
                self.assertEqual(exit_code, 0)
            exit_code, _ = self._run_cmd(
                workflow.cmd_mark_platform,
                self._make_args(
                    stage="plan-platforms", platform="web", status="skipped"
                ),
            )
            self.assertEqual(exit_code, 0)

            # advance: plan-platforms -> coding-platforms
            exit_code, _ = self._run_cmd(workflow.cmd_advance, self._make_args())
            self.assertEqual(exit_code, 0)

            # mark platforms for coding-platforms (web skipped)
            for plat in ("backend", "ios", "android"):
                exit_code, _ = self._run_cmd(
                    workflow.cmd_mark_platform,
                    self._make_args(
                        stage="coding-platforms", platform=plat, status="completed"
                    ),
                )
                self.assertEqual(exit_code, 0)
            exit_code, _ = self._run_cmd(
                workflow.cmd_mark_platform,
                self._make_args(
                    stage="coding-platforms", platform="web", status="skipped"
                ),
            )
            self.assertEqual(exit_code, 0)

            # advance: coding-platforms -> code-human-review
            exit_code, _ = self._run_cmd(workflow.cmd_advance, self._make_args())
            self.assertEqual(exit_code, 0)

            # human-review approve: code-human-review -> qa-blackbox-testing
            exit_code, _ = self._run_cmd(
                workflow.cmd_human_review,
                self._make_args(stage="code-human-review", approve=True),
            )
            self.assertEqual(exit_code, 0)

            # advance: qa-blackbox-testing -> worktree-merge
            exit_code, _ = self._run_cmd(workflow.cmd_advance, self._make_args())
            self.assertEqual(exit_code, 0)

            # advance: worktree-merge -> wiki-inclusion
            exit_code, _ = self._run_cmd(workflow.cmd_advance, self._make_args())
            self.assertEqual(exit_code, 0)

            # wiki-inclusion is now in_progress. Advance normally past it
            # (simulating full completion instead of skipping):
            exit_code, output = self._run_cmd(
                workflow.cmd_advance, self._make_args()
            )
            self.assertEqual(exit_code, 0)
            result = json.loads(output.strip().split("\n")[-1])
            self.assertEqual(result["completed"], "wiki-inclusion")
            # completed is the last stage in STAGE_ORDER, advancing from it
            # shows all-done message
            self.assertEqual(result["advanced_to"], "completed")

            # Advance from completed -> all done
            exit_code, output = self._run_cmd(
                workflow.cmd_advance, self._make_args()
            )
            self.assertEqual(exit_code, 0)
            result = json.loads(output.strip().split("\n")[-1])
            self.assertIsNone(result["advanced_to"])
            self.assertIn("所有阶段已完成", result["message"])

        # Verify final state
        data = workflow.load_workflow(wf_path)
        self.assertEqual(data["stages"]["completed"]["status"], "completed")
        for stage_name in workflow.STAGE_ORDER:
            if stage_name != "completed":
                self.assertIn(
                    data["stages"][stage_name]["status"],
                    ("completed", "skipped"),
                    f"{stage_name} should be completed or skipped",
                )

    def test_reject_and_retry_spec(self):
        """Test the full reject -> rewrite -> re-review -> re-approve cycle for spec."""
        exit_code, output = self._do_init("reject-retry")
        self.assertEqual(exit_code, 0)
        result = json.loads(output.strip())
        wf_path = Path(self.temp_dir.name) / result["feature"]["dir"] / "workflow.json"

        with patch("workflow.find_workflow_json", return_value=wf_path):
            # Advance to spec-human-review
            for _ in range(3):
                exit_code, _ = self._run_cmd(workflow.cmd_advance, self._make_args())
                self.assertEqual(exit_code, 0)

            # Reject
            exit_code, _ = self._run_cmd(
                workflow.cmd_human_review,
                self._make_args(stage="spec-human-review", reject=True),
            )
            self.assertEqual(exit_code, 0)

            data = workflow.load_workflow(wf_path)
            self.assertEqual(data["stages"]["spec-writing"]["status"], "in_progress")
            self.assertEqual(data["stages"]["spec-review"]["status"], "pending")
            self.assertEqual(data["stages"]["spec-review"]["review_loops"], 0)
            self.assertEqual(data["stages"]["spec-human-review"]["status"], "pending")

            # Rewrite and re-advance
            exit_code, _ = self._run_cmd(workflow.cmd_advance, self._make_args())
            self.assertEqual(exit_code, 0)
            exit_code, _ = self._run_cmd(workflow.cmd_advance, self._make_args())
            self.assertEqual(exit_code, 0)

            # Approve
            exit_code, _ = self._run_cmd(
                workflow.cmd_human_review,
                self._make_args(stage="spec-human-review", approve=True),
            )
            self.assertEqual(exit_code, 0)

            data = workflow.load_workflow(wf_path)
            self.assertEqual(data["stages"]["spec-human-review"]["status"], "completed")
            self.assertEqual(data["stages"]["design-shared"]["status"], "in_progress")

    def test_platform_reject_one_platform_only(self):
        """Platform-level reject only resets the specified platform."""
        exit_code, output = self._do_init("platform-reject")
        self.assertEqual(exit_code, 0)
        result = json.loads(output.strip())
        wf_path = Path(self.temp_dir.name) / result["feature"]["dir"] / "workflow.json"

        # Get to code-human-review with all coding platforms completed
        self._advance_to(wf_path, "coding-platforms")
        with patch("workflow.find_workflow_json", return_value=wf_path):
            for plat in workflow.PLATFORM_NAMES:
                self._run_cmd(
                    workflow.cmd_mark_platform,
                    self._make_args(
                        stage="coding-platforms", platform=plat, status="completed"
                    ),
                )
            self._run_cmd(workflow.cmd_advance, self._make_args())

            # Now at code-human-review, reject only ios
            exit_code, _ = self._run_cmd(
                workflow.cmd_human_review,
                self._make_args(stage="code-human-review", platform="ios", reject=True),
            )
            self.assertEqual(exit_code, 0)

        data = workflow.load_workflow(wf_path)
        # ios should be reset
        self.assertEqual(
            data["stages"]["coding-platforms"]["platforms"]["ios"]["status"],
            "in_progress",
        )
        # Others should remain completed
        self.assertEqual(
            data["stages"]["coding-platforms"]["platforms"]["android"]["status"],
            "completed",
        )
        self.assertEqual(
            data["stages"]["coding-platforms"]["platforms"]["backend"]["status"],
            "completed",
        )

    def _do_init(self, name):
        args = self._make_args(name=name)
        return self._run_cmd(workflow.cmd_init, args)


# ============================================================
# TestKnownBugs
# ============================================================

class TestKnownBugs(WorkflowTestBase):
    """Centralized documentation of all known bugs with @unittest.expectedFailure.

    Each test asserts the CORRECT behavior. Since the code currently has the bug,
    the test will fail, and unittest reports it as an "expected failure".
    When the bug is fixed, the test will pass (unexpected success), alerting us
    to remove the decorator.
    """

    @unittest.expectedFailure
    def test_bug_1_save_workflow_idempotency(self):
        """Bug 1: save_workflow always rewrites because updated_at is set before
        the comparison, making the idempotency check always detect a change."""
        wf_path = self._write_wf(name="bug1-test")
        data = workflow.load_workflow(wf_path)
        # Record the file's modification time
        original_mtime = wf_path.stat().st_mtime
        # Save with the exact same data (no changes)
        workflow.save_workflow(wf_path, data)
        new_mtime = wf_path.stat().st_mtime
        # Correct behavior: file should NOT be rewritten
        self.assertEqual(original_mtime, new_mtime,
                         "File should not be rewritten when data is unchanged")

    @unittest.expectedFailure
    def test_bug_2_human_review_neither_flag_should_error(self):
        """Bug 2: cmd_human_review with neither --approve nor --reject should
        error, not silently do nothing."""
        wf_path = self._write_wf(current_stage_idx=3)
        with patch("workflow.find_workflow_json", return_value=wf_path):
            exit_code, output = self._run_cmd(
                workflow.cmd_human_review, self._make_args(stage="spec-human-review")
            )
        # Correct behavior: should exit with error
        self.assertEqual(exit_code, 1,
                         "Should error when neither --approve nor --reject is given")

    @unittest.expectedFailure
    def test_bug_3_human_review_both_flags_should_error(self):
        """Bug 3: cmd_human_review with both --approve and --reject should error,
        not silently let --approve win."""
        wf_path = self._write_wf(current_stage_idx=3)
        with patch("workflow.find_workflow_json", return_value=wf_path):
            exit_code, output = self._run_cmd(
                workflow.cmd_human_review,
                self._make_args(
                    stage="spec-human-review", approve=True, reject=True
                ),
            )
        # Correct behavior: should exit with error for mutual exclusion
        self.assertEqual(exit_code, 1,
                         "Should error when both --approve and --reject are given")

    def test_bug_4_advance_auto_skip_dead_code(self):
        """Bug 4: The auto-skip branch in cmd_advance (lines 316-329) is dead code.

        get_current_stage only returns stages with status=="in_progress",
        so the check `data["stages"][current]["status"] == "skipped"` at
        line 316 can never be True under normal operation. If a stage is
        manually marked as "skipped" while also being "in_progress",
        get_current_stage won't find it and the code falls through to
        "没有 in_progress 的阶段" error instead.

        This is documented here as a known issue rather than an expectedFailure
        because the dead code doesn't cause incorrect behavior — it just
        never executes.
        """
        data = self._make_workflow_data(current_stage_idx=13)
        data["stages"]["wiki-inclusion"]["status"] = "skipped"
        # get_current_stage won't find this (status is "skipped", not "in_progress")
        self.assertIsNone(workflow.get_current_stage(data))

    @unittest.expectedFailure
    def test_bug_5_mark_platform_failed_clears_completed_at(self):
        """Bug 5: cmd_mark_platform marking failed should clear completed_at."""
        data = self._make_workflow_data(current_stage_idx=5)
        data["stages"]["design-platforms"]["platforms"]["ios"]["status"] = "completed"
        data["stages"]["design-platforms"]["platforms"]["ios"]["completed_at"] = (
            "2026-01-01T00:00:00+00:00"
        )
        wf_path = self._write_wf(data=data)
        with patch("workflow.find_workflow_json", return_value=wf_path):
            self._run_cmd(
                workflow.cmd_mark_platform,
                self._make_args(
                    stage="design-platforms", platform="ios", status="failed"
                ),
            )
        data = workflow.load_workflow(wf_path)
        plat = data["stages"]["design-platforms"]["platforms"]["ios"]
        # Correct behavior: completed_at should be cleared when status is not final
        self.assertIsNone(plat.get("completed_at"),
                          "completed_at should be cleared when marking failed")

    @unittest.expectedFailure
    def test_bug_6_platform_reject_resets_review_loops(self):
        """Bug 6: Platform-level reject should reset review_loops on that platform,
        matching the full-reject behavior."""
        data = self._make_workflow_data(current_stage_idx=10)
        data["stages"]["coding-platforms"]["status"] = "completed"
        data["stages"]["coding-platforms"]["completed_at"] = "2026-01-01T00:00:02+00:00"
        for plat in workflow.PLATFORM_NAMES:
            data["stages"]["coding-platforms"]["platforms"][plat]["status"] = "completed"
            data["stages"]["coding-platforms"]["platforms"][plat]["review_loops"] = 3
        wf_path = self._write_wf(data=data)

        with patch("workflow.find_workflow_json", return_value=wf_path):
            self._run_cmd(
                workflow.cmd_human_review,
                self._make_args(
                    stage="code-human-review", platform="ios", reject=True
                ),
            )
        data = workflow.load_workflow(wf_path)
        plat = data["stages"]["coding-platforms"]["platforms"]["ios"]
        # Correct behavior: review_loops should be reset to 0, matching full-reject
        self.assertEqual(plat.get("review_loops"), 0,
                         "Platform review_loops should be reset to 0 on reject")


# ============================================================
# Main
# ============================================================

if __name__ == "__main__":
    unittest.main()
