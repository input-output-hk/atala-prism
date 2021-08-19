import React from 'react';
import PropTypes from 'prop-types';
import { withApi } from '../providers/withApi';
import CredentialTemplates from './CredentialTemplates';
import { useCredentialTypes, useTemplateCategories } from '../../hooks/useCredentialTypes';
import { useMockDataContext } from '../providers/MockDataProvider';

const CredentialTemplatesContainer = ({ api: { credentialTypesManager } }) => {
  const {
    filteredCredentialTypes,
    isLoading,
    isSearching,
    filterProps,
    sortingProps
  } = useCredentialTypes(credentialTypesManager);
  const { templateCategories } = useTemplateCategories(credentialTypesManager);
  const { mockData } = useMockDataContext();

  const tableProps = {
    credentialTypes:
      filteredCredentialTypes && filteredCredentialTypes.concat(mockData.credentialTypes),
    templateCategories,
    isLoading,
    isSearching,
    filterProps,
    sortingProps
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
