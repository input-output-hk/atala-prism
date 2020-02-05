import {
  PopulateDemoDatasetRequest,
  AdminServicePromiseClient
} from '../../protos/admin/admin_grpc_web_pb';
import { isDevEnv } from '../env';
import { config } from '../config';

const adminService = new AdminServicePromiseClient(config.grpcClient, null, null);

export const populateDemoDataset = () => {
  return adminService.populateDemoDataset(new PopulateDemoDatasetRequest(), null);
};

export const isAdminSupported = () => isDevEnv(config.grpcClient);
