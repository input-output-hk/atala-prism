import Logger from '../../helpers/Logger';
import {
  GROUP_PAGE_SIZE,
  SORTING_DIRECTIONS,
  GROUP_SORTING_KEYS,
  REQUEST_AUTH_TIMEOUT_MS
} from '../../helpers/constants';
import { getProtoDate } from '../../helpers/formatters';
import { GroupsServicePromiseClient } from '../../protos/console_api_grpc_web_pb';
import {
  GetGroupsRequest,
  CreateGroupRequest,
  UpdateGroupRequest,
  DeleteGroupRequest
} from '../../protos/console_api_pb';

const { FilterBy, SortBy } = GetGroupsRequest;

const fieldKeys = {
  UNKNOWN: 0,
  NAME: 1,
  CREATED_AT: 2,
  NUMBER_OF_CONTACTS: 3
};

const sortByDirection = {
  ASCENDING: 1,
  DESCENDING: 2
};

async function getGroups({
  contactId,
  limit = GROUP_PAGE_SIZE,
  offset = 0,
  filter = {},
  sort = { field: GROUP_SORTING_KEYS.name, direction: SORTING_DIRECTIONS.ascending }
}) {
  const { name, createdBefore, createdAfter } = filter;
  const { field, direction } = sort;

  const filterBy = new FilterBy();
  filterBy.setName(name);
  filterBy.setContactId(contactId);

  if (createdBefore) {
    const createdBeforeDate = getProtoDate(createdBefore);
    filterBy.setCreatedBefore(createdBeforeDate);
  }

  if (createdAfter) {
    const createdAfterDate = getProtoDate(createdAfter);
    filterBy.setCreatedAfter(createdAfterDate);
  }

  const sortBy = new SortBy();
  sortBy.setField(fieldKeys[field]);
  sortBy.setDirection(sortByDirection[direction]);

  const groupRequest = new GetGroupsRequest();
  groupRequest.setLimit(limit);
  groupRequest.setOffset(offset);
  groupRequest.setFilterBy(filterBy);
  groupRequest.setSortBy(sortBy);

  const { metadata, sessionError } = await this.auth.getMetadata(
    groupRequest,
    REQUEST_AUTH_TIMEOUT_MS
  );
  if (sessionError) return [];

  const response = await this.client.getGroups(groupRequest, metadata);
  const { groupsList, totalNumberOfGroups } = response.toObject();
  Logger.info(`got (${groupsList.length}/${totalNumberOfGroups}) groups: `, groupsList);

  return { groupsList, totalNumberOfGroups };
}

async function getAllGroups() {
  const { totalNumberOfGroups } = await this.getGroups({});
  const { groupsList } = await this.getGroups({ limit: totalNumberOfGroups });

  return groupsList;
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

  request.setGroupId(groupId);
  if (newName) request.setName(newName);
  request.setContactIdsToAddList(contactIdsToAdd || []);
  request.setContactIdsToRemoveList(contactIdsToRemove || []);

  const { metadata, sessionError } = await this.auth.getMetadata(request);
  if (sessionError) return {};

  const response = await this.client.updateGroup(request, metadata);

  return response.toObject().groupsList;
}

async function deleteGroup(groupId) {
  Logger.info(`deleting group with id: ${groupId}`);
  const request = new DeleteGroupRequest();

  request.setGroupId(groupId);

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
GroupsManager.prototype.getAllGroups = getAllGroups;
GroupsManager.prototype.createGroup = createGroup;
GroupsManager.prototype.updateGroup = updateGroup;
GroupsManager.prototype.deleteGroup = deleteGroup;

export default GroupsManager;
