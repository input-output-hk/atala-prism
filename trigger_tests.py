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
                print(f"Pipeline has no workflow ready yet, "
                      f"waiting [{SECONDS_TO_WAIT}] seconds... Iteration nr: [{iteration}]")
            else:
                return text_response["items"][0]["id"]
        else:
            handle_error_response(workflow_details_response)
        time.sleep(SECONDS_TO_WAIT)
        iteration += 1
    raise RuntimeError("ERROR! There were no possibility to get workflow")


def get_finished_workflow_details(workflow_id):
    workflow_url = f"{CIRCLE_CI_URL}/workflow/{workflow_id}"
    iteration = 1
    while True:
        workflow_details_response = requests.get(workflow_url, auth=(token, ""), timeout=10, headers=HEADERS)
        if workflow_details_response.ok:
            text_response = json.loads(workflow_details_response.text)
            if text_response["stopped_at"] is None:
                print(f"Workflow is not finished yet, waiting... Iteration nr: [{iteration}]")
            else:
                status = text_response["status"]
                pipeline_number = text_response["pipeline_number"]
                url = f"https://app.circleci.com/pipelines/github/{ORG_NAME}" \
                      f"/{project_name}/{pipeline_number}/workflows/{workflow_id}"
                return status, url
        else:
            handle_error_response(workflow_details_response)
        time.sleep(SECONDS_TO_WAIT)
        iteration += 1


def check_workflow_result(details):
    status = details[0]
    url = details[1]
    print(f"Workflow is finished with [{status}] status. More info under: \n{url}")
    if status == "success":
        print("All jobs ended up successfully!")
    else:
        raise ValueError(f"ERROR: Workflow status is [{status}]! See the logs above.")


token = get_env_variable("CIRCLE_CI_TOKEN")
branch_to_trigger = get_env_variable("BRANCH_TO_TRIGGER")
project_name = get_env_variable("PROJECT_NAME_TO_TRIGGER")

project_slug = f"/{GITHUB}/{ORG_NAME}/{project_name}"
project_url = f"{CIRCLE_CI_URL}{PROJECT_ENDPOINT}{project_slug}"

new_pipeline_id = get_new_pipeline_id(branch_to_trigger)
new_workflow_id = get_workflow_id(new_pipeline_id)
workflow_details = get_finished_workflow_details(new_workflow_id)
check_workflow_result(workflow_details)
