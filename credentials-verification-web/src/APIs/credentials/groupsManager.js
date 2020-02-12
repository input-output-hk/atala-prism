import { GroupsServicePromiseClient } from '../../protos/credentials/credentialsManager_grpc_web_pb';

const { GetGroupsRequest } = require('../../protos/credentials/credentialsManager_pb');

const { config } = require('../config');

const groupsService = new GroupsServicePromiseClient(config.grpcClient, null, null);

export const getGroups = async () => {
  const groupRequest = new GetGroupsRequest();

  const response = await groupsService.getGroups(groupRequest, {
    userId: config.issuerId
  });

  return response.toObject().groupsList;
};
