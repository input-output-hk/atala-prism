import { GroupsServicePromiseClient } from '../../protos/cmanager_api_grpc_web_pb';

const { GetGroupsRequest, CreateGroupRequest } = require('../../protos/cmanager_api_pb');

async function getGroups() {
  const groupRequest = new GetGroupsRequest();

  const metadata = await this.auth.getMetadata(groupRequest);
  const response = await this.client.getGroups(groupRequest, metadata);

  return response.toObject().groupsList;
}

async function createGroup(groupName) {
  const request = new CreateGroupRequest();
  request.setName(groupName);

  const metadata = await this.auth.getMetadata(request);

  await this.client.createGroup(request, metadata);
}

function GroupsManager(config, auth) {
  this.config = config;
  this.auth = auth;
  this.client = new GroupsServicePromiseClient(config.grpcClient, null, null);
}

GroupsManager.prototype.getGroups = getGroups;
GroupsManager.prototype.createGroup = createGroup;

export default GroupsManager;
