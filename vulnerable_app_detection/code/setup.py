from setuptools import setup, find_packages


setup(
    name="VulnTap",
    version="0.1.0",
    description="Analyze Android apps for TapTrap exploitability",
    author="TapTrap",
    packages=find_packages(),
    install_requires=[
        "click",
        "androguard>=4.1.2",
    ],
    entry_points={
        "console_scripts": [
            "vulntap-analyze=VulnTap.analyzer:main",
        ],
    },
    python_requires=">=3.7",
)