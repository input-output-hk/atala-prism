import { AdminServicePromiseClient } from '../../protos/admin_api_grpc_web_pb';
import { isDevEnv } from '../env';

const { PopulateDemoDatasetRequest } = require('../../protos/admin_api_pb');

function populateDemoDataset() {
  return this.client.populateDemoDataset(new PopulateDemoDatasetRequest(), null);
}

function isAdminSupported() {
  return isDevEnv(this.config.grpcClient);
}

function Admin(config) {
  this.config = config;
  this.client = new AdminServicePromiseClient(this.config.grpcClient, null, null);
}

Admin.prototype.populateDemoDataset = populateDemoDataset;
Admin.prototype.isAdminSupported = isAdminSupported;

export default Admin;
