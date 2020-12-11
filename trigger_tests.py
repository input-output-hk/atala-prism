import json
import os
import time
import requests

CIRCLE_CI_URL = "https://circleci.com/api/v2"
GITHUB = "gh"
ORG_NAME = "input-output-hk"
PROJECT_ENDPOINT = "/project"
HEADERS = {"Content-Type": "application/json"}
SECONDS_TO_WAIT = 10


def get_env_variable(variable_name):
    variable_value = os.getenv(variable_name)
    if variable_value is None or variable_value == "":
        raise NotImplementedError(f"There were no env variable exported for [{variable_name}]")
    return variable_value


def handle_error_response(response):
    raise ValueError(f"Wrong response, {response.status_code} received instead. Error: {response.text}")


def get_new_pipeline_id(branch):
    new_build_url = f"{project_url}/pipeline"
    build_parameters = {"branch": branch}
    new_pipeline_response = requests.post(
        new_build_url, auth=(token, ""), data=json.dumps(build_parameters), timeout=10, headers=HEADERS
    )
    if new_pipeline_response.ok:
        text_response = json.loads(new_pipeline_response.text)
        pipeline_id = text_response["id"]
        print(f"New pipeline id [{pipeline_id}] was triggered!")
        return pipeline_id
    else:
        handle_error_response(new_pipeline_response)


def get_workflow_id(pipeline_id):
    iteration = 1
    max_iteration = 10
    workflow_url = f"{CIRCLE_CI_URL}/pipeline/{pipeline_id}/workflow"
    while iteration < max_iteration:
        workflow_details_response = requests.get(workflow_url, auth=(token, ""), timeout=10, headers=HEADERS)
        if workflow_details_response.ok:
            text_response = json.loads(workflow_details_response.text)
            all_items = text_response["items"]
            if not all_items:
                print(f"Pipeline has no workflow ready yet, waiting... Iteration nr: [{iteration}]")
            else:
                return text_response["items"][0]["id"]
        else:
            handle_error_response(workflow_details_response)
        time.sleep(SECONDS_TO_WAIT)
        iteration += 1
    raise RuntimeError("ERROR! There were no possibility to get workflow")


def get_workflows_jobs_numbers(workflow_id):
    workflow_jobs_url = f"{CIRCLE_CI_URL}/workflow/{workflow_id}/job"
    workflow_jobs_details_response = requests.get(workflow_jobs_url, auth=(token, ""), timeout=10, headers=HEADERS)
    if workflow_jobs_details_response.ok:
        job_numbers = []
        text_response = json.loads(workflow_jobs_details_response.text)
        all_jobs = text_response["items"]
        for job in all_jobs:
            job_numbers.append(job["job_number"])
        return job_numbers
    else:
        handle_error_response(workflow_jobs_details_response)


def get_all_jobs_details(jobs_numbers):
    jobs_details = []
    for job_number in jobs_numbers:
        job_url = f"{project_url}/job/{job_number}"
        finished = False
        iteration = 1
        while not finished:
            job_details_response = requests.get(job_url, auth=(token, ""), timeout=10, headers=HEADERS)
            if job_details_response.ok:
                text_response = json.loads(job_details_response.text)
                finished = False if text_response["stopped_at"] is None else True
                name = text_response["name"]
                if finished:
                    web_url = text_response["web_url"]
                    status = text_response["status"]
                    jobs_details.append((name, web_url, status))
                    continue
                time.sleep(SECONDS_TO_WAIT)
                print(f"Waiting for [{name}] to be finished. Iteration nr: [{iteration}]. "
                      f"Waiting another [{SECONDS_TO_WAIT}] seconds...")
                iteration += 1
            else:
                handle_error_response(job_details_response)
    return jobs_details


def check_jobs_results(results):
    if all(result[2] == "success" for result in results):
        print("All jobs ended up successfully!")
    else:
        [print(f"Build name: [{result[0]}], url: [{result[1]}] resulted in [{result[2]}]") for result in results]
        raise ValueError("ERROR: A least one of the jobs did not return in success! See the logs above.")


token = get_env_variable("CIRCLE_CI_TOKEN")
branch_to_trigger = get_env_variable("BRANCH_TO_TRIGGER")
project_name = get_env_variable("PROJECT_NAME_TO_TRIGGER")

project_slug = f"/{GITHUB}/{ORG_NAME}/{project_name}"
project_url = f"{CIRCLE_CI_URL}{PROJECT_ENDPOINT}{project_slug}"

new_pipeline_id = get_new_pipeline_id(branch_to_trigger)
new_workflow_id = get_workflow_id(new_pipeline_id)
created_jobs_numbers = get_workflows_jobs_numbers(new_workflow_id)
jobs_results = get_all_jobs_details(created_jobs_numbers)
check_jobs_results(jobs_results)
