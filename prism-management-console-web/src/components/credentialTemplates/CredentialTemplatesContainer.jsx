import React, { useContext, useEffect } from 'react';
import PropTypes from 'prop-types';
import { observer } from 'mobx-react-lite';
import { withApi } from '../providers/withApi';
import CredentialTemplates from './CredentialTemplates';
import { useTemplateCategories } from '../../hooks/useCredentialTypes';
import { PrismStoreContext } from '../../stores/PrismStore';

const CredentialTemplatesContainer = observer(({ api: { credentialTypesManager } }) => {
  const { tempalteStore } = useContext(PrismStoreContext);
  const { filteredCredentialTypes, fetchTemplates, isLoading } = tempalteStore;

  useEffect(() => {
    fetchTemplates();
  }, [fetchTemplates]);

  const { templateCategories } = useTemplateCategories(credentialTypesManager);

  const tableProps = {
    credentialTypes: filteredCredentialTypes,
    templateCategories,
    isLoading
  };

  return <CredentialTemplates tableProps={tableProps} />;
});

CredentialTemplatesContainer.propTypes = {
  api: PropTypes.shape({
    credentialTypesManager: PropTypes.shape({
      getCredentialTypes: PropTypes.func
    }).isRequired
  }).isRequired
};

export default withApi(CredentialTemplatesContainer);
