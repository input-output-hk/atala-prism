import { BROWSER_WALLET_INIT_DEFAULT_TIMEOUT_MS } from '../../helpers/constants';
import { GroupsServicePromiseClient } from '../../protos/console_api_grpc_web_pb';

const {
  GetGroupsRequest,
  CreateGroupRequest,
  UpdateGroupRequest
} = require('../../protos/console_api_pb');

async function getGroups(contactId) {
  const groupRequest = new GetGroupsRequest();
  if (contactId) groupRequest.setContactid(contactId);

  const metadata = await this.auth.getMetadata(
    groupRequest,
    BROWSER_WALLET_INIT_DEFAULT_TIMEOUT_MS
  );
  const response = await this.client.getGroups(groupRequest, metadata);

  return response.toObject().groupsList;
}

async function createGroup(groupName) {
  const request = new CreateGroupRequest();

  request.setName(groupName);

  const metadata = await this.auth.getMetadata(request, BROWSER_WALLET_INIT_DEFAULT_TIMEOUT_MS);
  const response = await this.client.createGroup(request, metadata);

  return response.getGroup().toObject();
}

async function updateGroup(groupId, contactIdsToAdd, contactIdsToRemove) {
  const request = new UpdateGroupRequest();

  request.setGroupid(groupId);
  request.setContactidstoaddList(contactIdsToAdd || []);
  request.setContactidstoremoveList(contactIdsToRemove || []);

  const metadata = await this.auth.getMetadata(request, BROWSER_WALLET_INIT_DEFAULT_TIMEOUT_MS);
  const response = await this.client.updateGroup(request, metadata);

  return response.toObject();
}

function GroupsManager(config, auth) {
  this.config = config;
  this.auth = auth;
  this.client = new GroupsServicePromiseClient(config.grpcClient, null, null);
}

GroupsManager.prototype.getGroups = getGroups;
GroupsManager.prototype.createGroup = createGroup;
GroupsManager.prototype.updateGroup = updateGroup;

export default GroupsManager;
