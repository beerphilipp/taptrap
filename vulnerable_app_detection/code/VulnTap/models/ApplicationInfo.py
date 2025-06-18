"""
Application Representation

Defines the `ApplicationInfo` dataclass, which encapsulates information
about an analyzed Android application. This structure is used to store
and serialize the results of the static analysis pipeline.

Author: Philipp Beer
"""

from dataclasses import dataclass, field
from typing import List
from VulnTap.models.ActivityInfo import ActivityInfo

@dataclass
class ApplicationInfo:
    
    apk_path: str = None
    target_sdk: int = None
    package_name: str = None
    is_enabled: bool = True
    permission: str = None
    protected_confirmation: bool = False
    activities: List[ActivityInfo] = field(default_factory=list)

    not_found_activities: List[str] = field(default_factory=list)

    override_animation_calls: List[str] = field(default_factory=list)

    start_time: str = None
    end_time: str = None
    exception: bool = False