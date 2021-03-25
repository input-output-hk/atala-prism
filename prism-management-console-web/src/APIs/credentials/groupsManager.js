import Logger from '../../helpers/Logger';
import { REQUEST_AUTH_TIMEOUT_MS } from '../../helpers/constants';
import { GroupsServicePromiseClient } from '../../protos/console_api_grpc_web_pb';

const {
  GetGroupsRequest,
  CreateGroupRequest,
  UpdateGroupRequest,
  DeleteGroupRequest
} = require('../../protos/console_api_pb');

async function getGroups(contactId) {
  const groupRequest = new GetGroupsRequest();
  if (contactId) groupRequest.setContactid(contactId);

  const { metadata, sessionError } = await this.auth.getMetadata(
    groupRequest,
    REQUEST_AUTH_TIMEOUT_MS
  );
  if (sessionError) return [];

  const response = await this.client.getGroups(groupRequest, metadata);

  return response.toObject().groupsList;
}

async function createGroup(groupName) {
  const request = new CreateGroupRequest();

  request.setName(groupName);

  const { metadata, sessionError } = await this.auth.getMetadata(request);
  if (sessionError) return {};

  const response = await this.client.createGroup(request, metadata);

  return response.getGroup().toObject();
}

async function updateGroup(groupId, { contactIdsToAdd, contactIdsToRemove, newName }) {
  const request = new UpdateGroupRequest();

  request.setGroupid(groupId);
  if (newName) request.setName(newName);
  request.setContactidstoaddList(contactIdsToAdd || []);
  request.setContactidstoremoveList(contactIdsToRemove || []);

  const { metadata, sessionError } = await this.auth.getMetadata(request);
  if (sessionError) return {};

  const response = await this.client.updateGroup(request, metadata);

  return response.toObject();
}

async function deleteGroup(groupId) {
  Logger.info(`deleting group with id: ${groupId}`);
  const request = new DeleteGroupRequest();

  request.setGroupid(groupId);

  const { metadata, sessionError } = await this.auth.getMetadata(request);
  if (sessionError) return {};

  return this.client.deleteGroup(request, metadata);
}

function GroupsManager(config, auth) {
  this.config = config;
  this.auth = auth;
  this.client = new GroupsServicePromiseClient(config.grpcClient, null, null);
}

GroupsManager.prototype.getGroups = getGroups;
GroupsManager.prototype.createGroup = createGroup;
GroupsManager.prototype.updateGroup = updateGroup;
GroupsManager.prototype.deleteGroup = deleteGroup;

export default GroupsManager;
