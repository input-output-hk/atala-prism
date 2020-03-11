import { AdminServicePromiseClient } from '../../protos/admin_grpc_web_pb';
import { isDevEnv } from '../env';
import { config } from '../config';

const { PopulateDemoDatasetRequest } = require('../../protos/admin_pb');

const adminService = new AdminServicePromiseClient(config.grpcClient, null, null);

export const populateDemoDataset = () => {
  return adminService.populateDemoDataset(new PopulateDemoDatasetRequest(), null);
};

export const isAdminSupported = () => isDevEnv(config.grpcClient);
