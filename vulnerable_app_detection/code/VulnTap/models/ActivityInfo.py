
"""
Activity Representation

Defines the `ActivityInfo` dataclass, which models relevant information
extracted from an Android activity declaration in the AndroidManifest.xml.

Author: Philipp Beer
"""

from dataclasses import dataclass, field
from typing import List


@dataclass
class ActivityInfo:

    activity_name: str = None
    is_alias: bool = False
    alias_for: str = None

    document_launch_mode: str = "none"
    launch_mode: str = "standard"
    is_enabled: bool = True
    is_exported: bool = False
    permission: str = None
    declared_intent_filters: bool = False
    intent_filters: List[str] = field(default_factory=list)
    is_ignored: bool = False

    overrides_on_enter_animation_complete: bool = False
    on_enter_animation_override: str = None

    animation_override_methods: List[str] = field(default_factory=list)