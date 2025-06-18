"""
Result Aggregation Module

This module defines the `ResultAggregator` class, which handles writing
analysis results to disk in JSON format.

Author: Philipp Beer
"""

import os
import json

from VulnTap.models.ApplicationInfo import ApplicationInfo

class ResultAggregator:
    """
    This class is responsible for writing the results of the analysis to a file.
    """

    def write_to_file(self, output_dir: str, application_info: ApplicationInfo):
        """
        Writes the results of the analysis to a file.
        The file is named after the package name of the application.

        :param output_dir: The directory where the results should be written to.
        :param application_info: The results of the analysis.
        """
        # capitalization in package names matter. so if the file already exists, we add a _1 to the end of the file name.
        output_file = os.path.join(output_dir, f"{application_info.package_name}.json")
        cnt = 1
        while (os.path.exists(output_file)):
            output_file = os.path.join(output_dir, f"{application_info.package_name}_{cnt}.json")
            cnt += 1

        with open(output_file, "w") as f:
            json.dump(application_info, f, default=lambda o: o.__dict__, indent=4)