import json
import os
import time
import requests


def get_env_variable(variable_name):
    variable_value = os.getenv(variable_name)
    if variable_value is None or variable_value == "":
        raise NotImplementedError("There were no env variable exported for [{}]".format(variable_name))
    return variable_value


def trigger_new_build(project_name, branch):
    all_builds_url = "https://circleci.com/api/v1.1/project/github/input-output-hk/{}/build?circle-token={}"\
        .format(project_name, token)
    build_parameters = {"branch": branch}
    headers = {"Content-Type": "application/json"}
    new_build_response = requests.post(
        all_builds_url, data=json.dumps(build_parameters), timeout=10, headers=headers
    )
    if new_build_response.ok:
        print("A new build was triggered!")
    else:
        raise ValueError("Wrong response, {} received instead. Error: {}".format(new_build_response.status_code,
                                                                                 new_build_response.text))


def get_builds_numbers(branch):
    all_builds_url = "https://circleci.com/api/v1.1/projects?circle-token={}".format(token)
    branch = branch.replace("/", "%2F")
    is_empty = True
    while is_empty:
        all_projects_details_response = requests.get(all_builds_url, timeout=10)
        text_response = all_projects_details_response.text
        status_code = all_projects_details_response.status_code
        if status_code != 200:
            raise ValueError("ERROR: 200 was not returned, [{}] received instead. Error: {}"
                             .format(status_code, text_response))
        all_projects_details = json.loads(text_response)
        for projects_details in all_projects_details:
            if projects_details["reponame"] == project_name_to_trigger:
                running_builds_list = projects_details["branches"][branch]["running_builds"]
                list_size = len(running_builds_list)
                if list_size == 0:
                    print("Still waiting for build numbers...")
                    time.sleep(1)
                    continue
                else:
                    return running_builds_list


def get_jobs_results(project_name, builds_list):
    build_numbers = [build["build_num"] for build in builds_list]
    print("Getting {} jobs results from [{}] builds list".format(len(build_numbers), build_numbers))
    results = []
    for build_number in build_numbers:
        public_build_url = "https://circleci.com/gh/input-output-hk/{}/{}".format(project_name, build_number)
        print("URL for the build is: {}".format(public_build_url))
        build_url = "https://circleci.com/api/v1.1/project/github/input-output-hk/{}/{}?circle-token={}"\
            .format(project_name, build_number, token)
        text_response = None
        job_status = None
        iteration = 1
        while job_status == "running" or job_status is None:
            build_response = requests.get(build_url, timeout=10)
            status_code = build_response.status_code
            text_response = json.loads(build_response.text)
            if status_code != 200:
                raise ValueError("ERROR: 200 was not returned, {} received instead. Error: {}"
                                 .format(status_code, text_response))
            job_status = text_response["outcome"]
            print("Job status for build [{}] is [{}] for iteration nr {}".format(build_number, job_status, iteration))
            iteration += 1
            time.sleep(1)
        results.append(text_response["outcome"])
    return results


def check_jobs_results(results):
    if all(result == "success" for result in results):
        print("All jobs ended up successfully!")
    else:
        # TODO(ATA-3042): Re-enable failing here, to properly denote the workflow has failed.
        print("ERROR: A least one of the jobs did not return in success! See the logs above.")


token = get_env_variable("CIRCLE_CI_TOKEN")
branch_to_trigger = get_env_variable("BRANCH_TO_TRIGGER")
project_name_to_trigger = get_env_variable("PROJECT_NAME_TO_TRIGGER")

trigger_new_build(project_name_to_trigger, branch_to_trigger)
builds_numbers = get_builds_numbers(branch_to_trigger)
jobs_results = get_jobs_results(project_name_to_trigger, builds_numbers)
check_jobs_results(jobs_results)
