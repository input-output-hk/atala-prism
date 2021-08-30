import React, { useContext, useEffect } from 'react';
import PropTypes from 'prop-types';
import { observer } from 'mobx-react-lite';
import { withApi } from '../providers/withApi';
import CredentialTemplates from './CredentialTemplates';
import { useTemplateCategories } from '../../hooks/useCredentialTypes';
import { PrismStoreContext } from '../../stores/domain/PrismStore';
import { UiStateContext } from '../../stores/ui/UiState';

const CredentialTemplatesContainer = observer(({ api: { credentialTypesManager } }) => {
  const { templateStore } = useContext(PrismStoreContext);
  const { templateUiState } = useContext(UiStateContext);
  const { credentialTemplates, fetchTemplates, isLoading } = templateStore;
  const { resetState } = templateUiState;

  useEffect(() => {
    resetState();
    fetchTemplates();
  }, [resetState, fetchTemplates]);

  const { templateCategories } = useTemplateCategories(credentialTypesManager);

  const tableProps = {
    credentialTemplates,
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
