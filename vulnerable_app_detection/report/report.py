import pandas as pd
import click
import os
import tqdm
import json

RESULT = {}

"""
    Main function that runs the report generation.
    @param result_dir: The path to the results directory.
    @param report_dir: The path to the directory to generate the report in.
    @return: None
"""
@click.command()
@click.option(
    "--result_dir", 
    required=True, 
    help="Path to the result directory.")
@click.option(
    "--report_dir",
    required=True,
    help="Path to the directory to generate the report in.",
)
def main(result_dir: str, report_dir: str):
    result_df = get_result_df(result_dir)

    result_df = check_failed_timeout(result_dir, result_df)
    df_apps = app_level_results(result_df)
    df_activities = activity_level_results(result_df)

    df_apps, df_activities = summary(df_apps, df_activities)

    save_output(df_apps, df_activities, report_dir)


"""
    Parses the JSON files in the results directory and returns a DataFrame with the results.

    @param result_dir: The path to the results directory.
    @return: A DataFrame with the results.
"""


def get_result_df(result_dir):
    result_files = [
        f
        for f in os.listdir(os.path.join(result_dir, "output"))
        if f.endswith(".json")
    ]
    parsed_json_files = []
    for json_file in result_files:
        with open(os.path.join(result_dir, "output", json_file), "r") as f:
            parsed_json_files.append(json.load(f))

    df = pd.json_normalize(parsed_json_files)
    return df


"""
    Checks for failed and timed out apps and returns those that succeeded.

    @param result_dir: The path to the results directory.
    @param df: The DataFrame with the results.
    @return: A DataFrame with the results of the apps that succeeded.
"""


def check_failed_timeout(result_dir, df):
    df_metrics = df.copy()
    joblog_path = os.path.join(result_dir, "logs", "joblog.log")
    joblog_df = pd.read_csv(joblog_path, sep="\t")
    if "Exitval" not in joblog_df.columns:
        raise ValueError("The joblog does not contain the 'Exitval' column.")

    failed_count = joblog_df[joblog_df["Exitval"] != 0].shape[0]
    failed_no_timeout_count = joblog_df[
        (joblog_df["Exitval"] != 0) & (joblog_df["Exitval"] != 124)].shape[0]
    timeout_count = joblog_df[joblog_df["Exitval"] == 124].shape[0]
    success_count = joblog_df[joblog_df["Exitval"] == 0].shape[0]
    total_count = joblog_df.shape[0]

    print(f"- Total number of apps (according to joblog): {total_count}")
    print(
        f"- Apps timed out (according to joblog): {timeout_count} ({timeout_count/total_count*100:.2f}%)"
    )

    n_success = len(df_metrics[df_metrics["exception"] == False])

    print(
        f"- Apps succeeded (according to joblog): {success_count} ({success_count/total_count*100:.2f}%)"
    )
    print(f"- Apps succeeded (according to results): {n_success}")

    if n_success != success_count:
        print(
            "! There is a mismatch between the number of successful apps in the joblog and the number of successful apps in the results."
        )

    # get all package names where joblog is successful
    successful_packages_joblog = joblog_df[joblog_df["Exitval"] == 0]["Command"].tolist()
    successful_packages_joblog = [x.split("/")[-1] for x in successful_packages_joblog]
    successful_packages_joblog = [x.split("_merged.apk ")[0] for x in successful_packages_joblog]
    successful_packages_joblog = [x.split(".apk ")[0] for x in successful_packages_joblog]
    
    # get the packages that timed out
    timed_out_packages = joblog_df[joblog_df["Exitval"] == 124]["Command"].tolist()
    timed_out_packages = [x.split("/")[-1] for x in timed_out_packages]
    timed_out_packages = [x.split("_merged.apk ")[0] for x in timed_out_packages]
    timed_out_packages = [x.split(".apk ")[0] for x in timed_out_packages]
    print(f"- Number of apps that timed out: {len(timed_out_packages)}")
    
    # in rare cases, the joblog might say it timed out, but the app had time to finish writing the results. exclude those from the timeout count and add them to the successful packages
    apps_joblog_timeout_but_result_success = set(timed_out_packages).intersection(
        set(df_metrics[df_metrics["exception"] == False]["package_name"])
    )
    
    if len(apps_joblog_timeout_but_result_success) > 0:
        print(
            f"- Number of apps that timed out but the results are successful: {len(apps_joblog_timeout_but_result_success)}"
        )
        # remove the apps from the timeout count
        timeout_count -= len(apps_joblog_timeout_but_result_success)
    

    # get all package names where the results are successful
    successful_packages_results = df_metrics[df_metrics['exception'] == False]["package_name"].tolist()

    # get the difference between the two lists
    
    difference = list(set(successful_packages_results) - set(successful_packages_joblog))
    print(f"- Number of apps where the results are successful but the joblog is not: {len(difference)}")
    print(difference)

    RESULT["vulntapAppsSuccess"] = f"{n_success:,}"
    RESULT["vulntapAppsSuccessPercent"] = f"{n_success/total_count*100:.2f}"
    RESULT["vulntapAppsTimeout"] = f"{timeout_count:,}"
    RESULT["vulntapAppsFailedWithoutTimeout"] = f"{failed_no_timeout_count:,}"

    df_metrics["duration"] = df_metrics["end_time"] - df_metrics["start_time"]
    avg_duration = df_metrics["duration"].mean()

    RESULT["vulntapAvgDuration"] = f"{round(avg_duration)}"
    return df_metrics[df_metrics["exception"] == False]


"""
    Checks basic app-level results, such as the number of activities of an app and adds it to a new column.

    @param df: The DataFrame with the results.
    @return: A DataFrame with the results of the apps.
"""
def app_level_results(df):
    df_apps = df.copy()
    df_apps["n_activities"] = df_apps["activities"].apply(lambda x: len(x))
    return df_apps


"""
    Checks activity-level results and whether they are vulnerable or not.
    @param df: The DataFrame with the results.
    @return: A DataFrame with the results of the activities.
"""
def activity_level_results(df):
    # Explode the activities column
    df_activities = df.copy()
    df_activities = df_activities[["package_name", "activities", "target_sdk"]]

    # get the ones that actually have activities
    df_activities = df_activities[
        df_activities["activities"].apply(lambda x: len(x) > 0)
    ]

    df_activities = df_activities.explode("activities")

    activities_normalized = pd.json_normalize(df_activities["activities"], max_level=0)

    # Combine back with packageName
    df_activities = df_activities.drop(columns=["activities"]).reset_index(drop=True)
    df_activities = pd.concat([df_activities, activities_normalized], axis=1)

    n_total_activities = len(df_activities)
    RESULT["vulntapAmountActivities"] = f"{n_total_activities:,}"

    # An activity is actually exported if:
    # 1. is_exported is true
    # 2. is_exported is not set, it has an intent filter and the target SDK is less than 31
    df_activities["actually_exported"] = (df_activities["is_exported"] == "true") | (
        (df_activities["is_exported"].isnull())
        & (df_activities["intent_filters"].apply(lambda x: len(x) > 0))
        & (df_activities["target_sdk"].apply(lambda x: x < 31))
    )
    n_exported_activities = len(
        df_activities[df_activities["actually_exported"] == True]
    )
    RESULT["vulntapAmountActivitiesExported"] = f"{n_exported_activities:,}"
    RESULT["vulntapAmountActivitiesExportedPercent"] = (
        f"{n_exported_activities/n_total_activities*100:.2f}"
    )

    # Check how many activities have a required permission
    n_no_permission_activities = len(
        df_activities[df_activities["permission"].apply(lambda x: x is None)]
    )
    RESULT["vulntapAmountActivitiesNoPermission"] = f"{n_no_permission_activities:,}"
    RESULT["vulntapAmountActivitiesNoPermissionPercent"] = (
        f"{n_no_permission_activities/n_total_activities*100:.2f}"
    )

    # Check how many activities are enabled
    n_enabled_activities = len(df_activities[df_activities["is_enabled"] == True])
    RESULT["vulntapAmountActivitiesEnabled"] = f"{n_enabled_activities:,}"
    RESULT["vulntapAmountActivitiesEnabledPercent"] = (
        f"{n_enabled_activities/n_total_activities*100:.2f}"
    )

    # Check if the activity is externally launchable
    df_activities["launchable"] = (
        (df_activities["actually_exported"] == True)
        & (df_activities["is_enabled"] == True)
        & (df_activities["permission"].apply(lambda x: x is None))
    )
    n_launchable_activities = len(df_activities[df_activities["launchable"] == True])
    RESULT["vulntapAmountActivitiesLaunchable"] = f"{n_launchable_activities:,}"
    RESULT["vulntapAmountActivitiesLaunchablePercent"] = (
        f"{n_launchable_activities/n_total_activities*100:.2f}"
    )

    # Check if the activity can be launched in the same task
    df_activities["same_task"] = df_activities["launch_mode"].apply(
        lambda x: x == "singleTop" or x == "standard"
    )
    n_same_task_activities = len(df_activities[df_activities["same_task"] == True])
    RESULT["vulntapAmountActivitiesSameTask"] = f"{n_same_task_activities:,}"
    RESULT["vulntapAmountActivitiesSameTaskPercent"] = (
        f"{n_same_task_activities/n_total_activities*100:.2f}"
    )

    # Check if the activity overrides onEnterAnimationComplete
    n_overrides_onenteranimationcomplete = len(
        df_activities[df_activities["overrides_on_enter_animation_complete"] == True]
    )
    RESULT["vulntapAmountActivitiesNoAnimationFinishWait"] = (
        f"{n_overrides_onenteranimationcomplete:,}"
    )
    RESULT["vulntapAmountActivitiesNoAnimationFinishWaitPercent"] = (
        f"{n_overrides_onenteranimationcomplete/n_total_activities*100:.2f}"
    )

    # Check if the activity uses overridePendingTransition
    df_activities["restrict_animation"] = df_activities[
        "animation_override_methods"
    ].apply(lambda x: len(x) > 0)
    activities_use_override_pending_transition = df_activities[
        "animation_override_methods"
    ].apply(lambda x: len(x) > 0)
    n_activities_use_override_pending_transition = len(
        df_activities[activities_use_override_pending_transition]
    )
    RESULT["vulntapAmountActivitiesAnimationRestriction"] = (
        f"{n_activities_use_override_pending_transition:,}"
    )
    RESULT["vulntapAmountActivitiesAnimationRestrictionPercent"] = (
        f"{n_activities_use_override_pending_transition/n_total_activities*100:.2f}"
    )

    # Check if the activity is vulnerable
    df_activities["vulnerable"] = (
        df_activities["launchable"]
        & df_activities["same_task"]
        & ~df_activities["restrict_animation"]
        & ~df_activities["overrides_on_enter_animation_complete"]
    )

    # Get how many activities are vulnerable
    n_vulnerable_activities = len(df_activities[df_activities["vulnerable"] == True])
    RESULT["vulntapAmountActivitiesVulnerable"] = f"{n_vulnerable_activities:,}"
    RESULT["vulntapAmountActivitiesVulnerablePercent"] = (
        f"{n_vulnerable_activities/n_total_activities*100:.2f}"
    )

    return df_activities

"""
    Calculates the summary of the results and adds it to the RESULT dictionary.
"""
def summary(df_apps, df_activities):
    n_apps = len(df_apps["package_name"].unique())

    # Get the amount of apps that have at least one activity that is externally launchable
    df_activities_launchable = df_activities[df_activities["launchable"] == True]
    n_apps_multiple_exported_activities = len(
        df_activities_launchable["package_name"].unique()
    )
    RESULT["vulntapAmountAppsMinOneActivityLaunchable"] = (
        f"{n_apps_multiple_exported_activities:,}"
    )
    RESULT["vulntapAmountAppsMinOneActivityLaunchablePercent"] = (
        f"{n_apps_multiple_exported_activities/n_apps*100:.2f}"
    )

    # Get the amount of apps that have at least one activity that is launchable same-task
    df_activities_same_task = df_activities[df_activities["same_task"] == True]
    n_apps_multiple_same_task_activities = len(
        df_activities_same_task["package_name"].unique()
    )
    RESULT["vulntapAmountAppsMinOneActivitySameTask"] = (
        f"{n_apps_multiple_same_task_activities:,}"
    )
    RESULT["vulntapAmountAppsMinOneActivitySameTaskPercent"] = (
        f"{n_apps_multiple_same_task_activities/n_apps*100:.2f}"
    )

    # Get the amount of apps that have at least one activity that restricts animations
    df_activities_no_animation_finish_wait = df_activities[
        df_activities["restrict_animation"] == True
    ]
    n_apps_multiple_no_animation_finish_wait_activities = len(
        df_activities_no_animation_finish_wait["package_name"].unique()
    )
    RESULT["vulntapAmountAppsMinOneActivityRestrictAnimation"] = (
        f"{n_apps_multiple_no_animation_finish_wait_activities:,}"
    )
    RESULT["vulntapAmountAppsMinOneActivityRestrictAnimationPercent"] = (
        f"{n_apps_multiple_no_animation_finish_wait_activities/n_apps*100:.2f}"
    )

    # Get the amount of apps that include an activity that waits for the animation to finish
    df_activities_no_animation_finish_wait = df_activities[
        df_activities["overrides_on_enter_animation_complete"] == True
    ]
    n_apps_multiple_no_animation_finish_wait_activities = len(
        df_activities_no_animation_finish_wait["package_name"].unique()
    )
    RESULT["vulntapAmountAppsMinOneActivityWaitAnimationFinish"] = (
        f"{n_apps_multiple_no_animation_finish_wait_activities:,}"
    )
    RESULT["vulntapAmountAppsMinOneActivityWaitAnimationFinishPercent"] = (
        f"{n_apps_multiple_no_animation_finish_wait_activities/n_apps*100:.2f}"
    )

    # Get the number of apps that have at least one activity that is vulnerable
    df_activities_vulnerable = df_activities[df_activities["vulnerable"] == True]
    n_apps_multiple_vulnerable_activities = len(
        df_activities_vulnerable["package_name"].unique()
    )
    vulnerable_apps_package_names = df_activities_vulnerable["package_name"].unique()
    RESULT["vulntapAmountAppsMinOneActivityVulnerable"] = (
        f"{n_apps_multiple_vulnerable_activities:,}"
    )
    RESULT["vulntapAmountAppsMinOneActivityVulnerablePercent"] = (
        f"{n_apps_multiple_vulnerable_activities/n_apps*100:.2f}"
    )

    # Set whether the app is vulnerable or not
    df_apps["vulnerable"] = df_apps["package_name"].apply(
        lambda x: x in vulnerable_apps_package_names
    )

    return (df_apps, df_activities)

"""
    Creates the report and a file containing all vulnerable apps and activities.
    @param df_apps: The DataFrame with the app-level results.
    @param df_activities: The DataFrame with the activity-level results.
    @param result_dir: The path to the directory to save the report in.
    @return: None 
"""
def save_output(df_apps, df_activities, result_dir):
    with open(os.path.join(result_dir, "report.tex"), "w") as f:
        for key, value in RESULT.items():
            f.write(f"\\newcommand{{\\{key}}}{{{value}}}\n")

    vulnerable_apps = {}
    
    for app in tqdm.tqdm(df_apps[df_apps["vulnerable"] == True]["package_name"].unique()):
        activities = df_activities[df_activities["package_name"] == app]
        vulnerable_activities = activities[activities["vulnerable"] == True]
        vulnerable_apps[app] = list(vulnerable_activities["activity_name"])

    with open(os.path.join(result_dir, "vulnerability.json"), "w") as f:
        json.dump(vulnerable_apps, f, indent=4)


if __name__ == "__main__":
    main()
