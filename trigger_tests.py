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
    text_response = json.loads(new_build_response.text)
    try:
        status_code = text_response["status"]
    except KeyError:
        raise ValueError("Cannot get \"status\" key! Error: {}".format(text_response))
    if status_code != 200:
        raise ValueError("200 was not returned, {} received instead. Error: {}".format(status_code, text_response))


def get_build_number(branch):
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
                    time.sleep(1)
                    continue
                else:
                    return running_builds_list[0]["build_num"]


def get_build_result(project_name, build_number):
    print("Getting build result for [{}] build number".format(build_number))
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
        print("Build status is [{}] for iteration nr {}".format(job_status, iteration))
        iteration += 1
        time.sleep(1)
    return text_response["outcome"]


def check_build_result(build_result):
    if build_result != "success":
        raise ValueError("ERROR: Job did not return in success! [{}] was returned!".format(build_result))


token = get_env_variable("CIRCLE_CI_TOKEN")
branch_to_trigger = get_env_variable("BRANCH_TO_TRIGGER")
project_name_to_trigger = get_env_variable("PROJECT_NAME_TO_TRIGGER")

trigger_new_build(project_name_to_trigger, branch_to_trigger)
build_nr = get_build_number(branch_to_trigger)
result = get_build_result(project_name_to_trigger, build_nr)
check_build_result(result)
