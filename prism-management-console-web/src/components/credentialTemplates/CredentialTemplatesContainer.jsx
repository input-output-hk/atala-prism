import React from 'react';
import PropTypes from 'prop-types';
import { withApi } from '../providers/withApi';
import CredentialTemplates from './CredentialTemplates';
import { useCredentialTypes } from '../../hooks/useCredentialTypes';

const CredentialTemplatesContainer = ({ api: { credentialTypesManager } }) => {
  const { credentialTypes, isLoading, isSearching } = useCredentialTypes(credentialTypesManager);

  const tableProps = {
    credentialTypes,
    isLoading,
    isSearching
  };

  return <CredentialTemplates tableProps={tableProps} />;
};

CredentialTemplatesContainer.propTypes = {
  api: PropTypes.shape({
    credentialTypesManager: PropTypes.shape({
      getCredentialTypes: PropTypes.func
    }).isRequired
  }).isRequired
};

export default withApi(CredentialTemplatesContainer);
