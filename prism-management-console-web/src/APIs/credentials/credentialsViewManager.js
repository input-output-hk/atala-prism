import { CredentialViewsServicePromiseClient } from '../../protos/cviews_api_grpc_web_pb';
import { GetCredentialViewTemplatesRequest } from '../../protos/cviews_api_pb';

async function getCredentialViewTemplates() {
  const req = new GetCredentialViewTemplatesRequest();
  const metadata = await this.auth.getMetadata(req);
  const res = await this.client.getCredentialViewTemplates(req, metadata);
  return res.getTemplatesList().map(template => template.toObject());
}

function CredentialsViewManager(config, auth) {
  this.config = config;
  this.auth = auth;
  this.client = new CredentialViewsServicePromiseClient(config.grpcClient, null, null);
}

CredentialsViewManager.prototype.getCredentialViewTemplates = getCredentialViewTemplates;

export default CredentialsViewManager;
