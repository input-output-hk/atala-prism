import { svgPathToEncodedBase64 } from '../../helpers/genericHelpers';

export const b64ImagePrefix = 'data:image/svg+xml;base64';

export const getCredentialTypeAttributes = async credentialList => {
  const { name, icon } = credentialList[0]?.credentialData.credentialTypeDetails;

  const encodedIcon = await svgPathToEncodedBase64(icon);

  return {
    credentialTypeName: name,
    credentialTypeIcon: encodedIcon,
    credentialTypeIconFormat: 'svg'
  };
};
