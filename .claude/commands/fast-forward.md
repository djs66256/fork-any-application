---
description: 自动推进 feature-workflow 直到完成，人工确认步骤直接通过
---

调用 Skill("feature-workflow")，推进需求「$ARGUMENTS」直到完成功能。流程中所有需要人类批准的步骤（spec-human-review、design-human-review、code-human-review）直接通过，不暂停等待用户确认。
