import { pick } from 'lodash';
import { ConsoleServicePromiseClient } from '../../protos/console_api_grpc_web_pb';

const { GetStatisticsRequest } = require('../../protos/console_api_pb');

async function getStatistics() {
  const request = new GetStatisticsRequest();

  const metadata = await this.auth.getMetadata(request);
  const response = await this.client.getStatistics(request, metadata);

  const result = await response.toObject();

  return {
    contacts: pick(result, [
      'numberofcontactspendingconnection',
      'numberofcontactsconnected',
      'numberofcontacts'
    ]),
    groups: pick(result, ['numberofgroups']),
    credentials: pick(result, [
      'numberofcredentialsindraft',
      'numberofcredentialspublished',
      'numberofcredentialsreceived'
    ])
  };
}

function SummaryManager(config, auth) {
  this.config = config;
  this.auth = auth;
  this.client = new ConsoleServicePromiseClient(this.config.grpcClient);
}

SummaryManager.prototype.getStatistics = getStatistics;

export default SummaryManager;
