import { GroupsServicePromiseClient } from '../../protos/cmanager_api_grpc_web_pb';

const { GetGroupsRequest, CreateGroupRequest } = require('../../protos/cmanager_api_pb');

async function getGroups() {
  const groupRequest = new GetGroupsRequest();

  const response = await this.client.getGroups(groupRequest, this.auth.getMetadata());

  return response.toObject().groupsList;
}

async function createGroup(groupName) {
  const request = new CreateGroupRequest();
  request.setName(groupName);

  await this.client.createGroup(request, this.auth.getMetadata());
}

function GroupsManager(config, auth) {
  this.config = config;
  this.auth = auth;
  this.client = new GroupsServicePromiseClient(config.grpcClient, null, null);
}

GroupsManager.prototype.getGroups = getGroups;
GroupsManager.prototype.createGroup = createGroup;

export default GroupsManager;
