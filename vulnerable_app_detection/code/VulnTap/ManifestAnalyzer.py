"""
AndroidManifest Analyzer Module

This module defines the `ManifestAnalyzer` class, which extracts structured information 
from the AndroidManifest.xml of an APK file using `androguard`. It populates a 
provided `ApplicationInfo` instance with metadata about the application and its activities.

Author: Philipp Beer
"""

import logging

import lxml.etree as etree
import xml.etree.ElementTree as ET
import VulnTap.XmlUtils as XmlUtils
from VulnTap.models.ApplicationInfo import ApplicationInfo
from VulnTap.models.ActivityInfo import ActivityInfo

from androguard.core.apk import APK

class ManifestAnalyzer:
    """
    Analyzes the AndroidManifest.xml file of an APK and retrieves information about the application and its activities.
    """

    application_info: ApplicationInfo
    a: APK = None

    def __init__(self, application_info: ApplicationInfo, a: APK):
        self.application_info = application_info
        self.a = a

    def analyze(self):
        """
        Analyzes the AndroidManifest.xml file and retrieves information about the application and its activities.
        """
        logging.info(f"Analyzing manifest for {self.application_info.package_name}")
        self.get_application_info()

    
    def get_application_info(self) -> None:
        """
            Retrieves information from the <code>application</code> node in the AndroidManifest.xml file.
            This also transitively retrieves information about the activities in the app.
            See <a href="https://developer.android.com/guide/topics/manifest/application-element">this guide</a> for all tags that can be found in an <code>application</code> node.</p>
        """
        # Get the package name of the app
        self.application_info.package_name = self.a.get_package()
        if self.application_info.package_name is None or self.application_info.package_name == "":
            raise Exception("Could not find package name in manifest")

        # get whether the app is enabled
        manifest_xml: etree.Element = self.a.get_android_manifest_xml()
        application_element: etree.Element = manifest_xml.find("application")
        if application_element is None:
            raise Exception("Could not find application element in manifest")
        
        # Get the target SDK version of the app
        uses_sdk_nodes: list[etree.Element] = manifest_xml.findall("uses-sdk")
        if uses_sdk_nodes is not None and len(uses_sdk_nodes) >= 0:
            target_sdk_raw = XmlUtils.get_attribute_ignore_namespace(uses_sdk_nodes[0], "targetSdkVersion")
            if target_sdk_raw is not None:
                self.application_info.target_sdk = int(target_sdk_raw)
            else:
                self.application_info.target_sdk = 35 # assuming 35 (conservative) if we do not have any inof
        else:
            self.application_info.target_sdk = 35 # assuming 35 (conservative) if we do not have any info
        
        # Get whether the app is enabled - if an app is not enabled it cannot be launched
        is_enabled_raw = XmlUtils.get_attribute_ignore_namespace(application_element, "enabled")
        if is_enabled_raw is not None:
            self.application_info.is_enabled = is_enabled_raw.lower() == "true"

        # Get the android:permission attribute of the application node.
        # This can be used to restrict access to the application
        permission_raw = XmlUtils.get_attribute_ignore_namespace(application_element, "permission")
        if permission_raw is not None:
            self.application_info.permission = permission_raw

        activity_nodes: list[etree.Element] = application_element.findall("activity")
        if activity_nodes is None:
            raise Exception("Could not find any activity nodes in manifest")
        for activity_node in activity_nodes:
            activity_info: ActivityInfo = self.get_information_from_activity(activity_node)
            self.application_info.activities.append(activity_info)
    
    def get_information_from_activity(self, xml_node: etree.Element) -> ActivityInfo:
        """
        See <a href="https://developer.android.com/guide/topics/manifest/activity-element">this guide</a> for all tags that can be found in an <code>activity</code> node.
        
        :param xml_node: The XML node of the activity
        :return: An ActivityInfo object containing the information about the activity
        """
        activity_info: ActivityInfo = ActivityInfo()

        # Get the activity name
        activity_name = XmlUtils.get_attribute_ignore_namespace(xml_node, "name")
        if (activity_name is None):
            raise Exception("Could not find name attribute in activity node")
        if (activity_name.startswith(".")):
            # If the activity starts with a dot, it is a relative name, and we need to prepend the package name
            activity_name = self.application_info.package_name + activity_name
        activity_info.activity_name = activity_name

        # Get the document launch mode of the activity
        document_launch_mode = XmlUtils.get_attribute_ignore_namespace(xml_node, "documentLaunchMode")
        if document_launch_mode is not None:
            activity_info.document_launch_mode = document_launch_mode
        
        # Get whether the activity is enabled
        is_enabled = XmlUtils.get_attribute_ignore_namespace(xml_node, "enabled")
        if is_enabled is not None:
            activity_info.is_enabled = is_enabled.lower() == "true"

        # Get whether the activity is exported
        is_exported = XmlUtils.get_attribute_ignore_namespace(xml_node, "exported")
        activity_info.is_exported = is_exported

        # Get the launch mode of the activity
        launch_mode = XmlUtils.get_attribute_ignore_namespace(xml_node, "launchMode")
        if launch_mode is not None:
            # We retrieve the launch mode as an integer and have to convert it to the string representation
            match launch_mode:
                case "0":
                    launch_mode = "standard"
                case "1":
                    launch_mode = "singleTop"
                case "2":
                    launch_mode = "singleTask"
                case "3":
                    launch_mode = "singleInstance"
                case "4":
                    launch_mode = "singleInstancePerTask"
                case _:
                    raise Exception(f"Unknown launch mode {launch_mode}")
            activity_info.launch_mode = launch_mode
        
        # Get the permission of the activity
        permission = XmlUtils.get_attribute_ignore_namespace(xml_node, "permission")
        if permission is not None:
            activity_info.permission = permission

        # Retrieve whether the activity declares an intent filter
        intent_filter_nodes: list[ET.Element] = xml_node.findall("intent-filter")
        if intent_filter_nodes is not None:
            activity_info.declared_intent_filters = True
        
        # We also record the intent filters. This could be useful for further analysis
        activity_info.intent_filters = [ET.tostring(node, encoding='unicode') for node in intent_filter_nodes]
        
        return activity_info
