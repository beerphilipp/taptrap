import pandas as pd
import sqlite3
import click
import os

RESULT = {}

"""
    Main function that runs the report generation.
    @param result_dir: The path to the results directory.
    @param report_dir: The path to the directory to generate the report in.
    @return: None
"""
@click.command()
@click.option("--result_dir", required=True, help="Path to the result directory.")
@click.option("--report_dir",required=True,help="Path to the directory to generate the report in.")
def main(result_dir: str, report_dir: str):
    extract_check_failed(result_dir)
    
    db = open_db(result_dir)
    
    extract_number_animations(db)
    extract_number_unique_animations(db)
    extract_number_unique_interpolators(db)

    analyze_number_animations_failed(db)
    analyze_alpha_score_min(db)
    analyze_scale_score_min(db)
    analyze_score_min_apps(db)
    analyze_unique_animations_extended_duration(db)

    save_output(report_dir)


def open_db(result_dir: str):
    # open the sqlite database
    db_path = os.path.join(result_dir, "animations.db")
    con = sqlite3.connect(db_path)
    return con.cursor()

def extract_check_failed(result_dir: str):
    try:
        joblog_path = os.path.join(result_dir, "logs", "joblog.log")
        joblog_df = pd.read_csv(joblog_path, sep='\t')
        if 'Exitval' not in joblog_df.columns:
            raise ValueError("The joblog does not contain the 'Exitval' column.")
        failed_count = joblog_df[joblog_df["Exitval"] != 0].shape[0]
        
        success_count = joblog_df[joblog_df["Exitval"] == 0].shape[0]
        RESULT["maltapNumberSuccessApps"] = f"{success_count:,}"
        percent = (100*(success_count / (success_count + failed_count)))
        RESULT["maltapNumberSuccessAppsPercent"] = f"{percent:.2f}"
    except Exception as e:
        print(e)
        print(f"x Exception during extraction of failed apps")


def extract_number_animations(db):
    query = "SELECT COUNT(*) AS unique_count FROM (SELECT DISTINCT package_name, file_name FROM anim)"
    number_animations = db.execute(query).fetchone()[0]
    RESULT["maltapNumberAnimations"] = f"{number_animations:,}"

def extract_number_unique_animations(db):
    query = "SELECT COUNT(DISTINCT hash) FROM anim"
    number_unique_animations = db.execute(query).fetchone()[0]
    RESULT["maltapNumberUniqueAnimations"] = f"{number_unique_animations:,}"

def extract_number_unique_interpolators(db):
    query = "SELECT COUNT(DISTINCT hash) FROM interpolator WHERE package_name != 'framework-res'"
    number_unique_interpolators = db.execute(query).fetchone()[0]

def analyze_number_animations_failed(db):
    query = "SELECT COUNT(*) FROM score WHERE alpha_score < 0"
    number_animations_failed = db.execute(query).fetchone()[0]
    RESULT["maltapNumberAnimationsFailed"] = f"{number_animations_failed:,}"

def analyze_alpha_score_min(db):
    query = "SELECT DISTINCT anim.package_name FROM score JOIN anim ON score.hash = anim.hash WHERE alpha_score >= 50"
    alpha_score_min = db.execute(query).fetchall()
    RESULT["maltapNumberAppsAlphaScoreMin"] = f"{len(alpha_score_min):,}"

def analyze_scale_score_min(db):
    query = "SELECT DISTINCT anim.package_name FROM score JOIN anim ON score.hash = anim.hash WHERE scale_score >= 50"
    scale_score_min = db.execute(query).fetchall()
    RESULT["maltapNumberAppsScaleScoreMin"] = f"{len(scale_score_min):,}"

def analyze_score_min_apps(db):
    query = "SELECT DISTINCT anim.package_name FROM score JOIN anim ON score.hash = anim.hash WHERE alpha_score >= 50 OR scale_score >= 50"
    score_min_apps = db.execute(query).fetchall()
    RESULT["maltapNumberAppsAnimationsScoreMin"] = f"{len(score_min_apps):,}"

def analyze_unique_animations_extended_duration(db):
    query = "SELECT COUNT(DISTINCT hash) FROM score WHERE animation_longer == 1"
    unique_animations_extended_duration = db.execute(query).fetchone()[0]
    RESULT["maltapNumberUniqueAnimationsExtendedDuration"] = f"{unique_animations_extended_duration:,}"
    
def apps_with_extended_duration(db):
    query = "SELECT DISTINCT anim.package_name FROM score JOIN anim ON score.hash = anim.hash WHERE animation_longer == 1"
    apps_with_extended_duration = db.execute(query).fetchall()
    RESULT["maltapNumberAppsWithExtendedDuration"] = f"{len(apps_with_extended_duration):,}"
    print(f"Package names: {[row[0] for row in apps_with_extended_duration]}")
    for row in apps_with_extended_duration:
        print(f"{row[0]}.apk")
        
def save_output(result_dir):
    with open(os.path.join(result_dir, "report.tex"), "w") as f:
        for key, value in RESULT.items():
            f.write(f"\\newcommand{{\\{key}}}{{{value}}}\n")

if __name__ == '__main__':
    main()