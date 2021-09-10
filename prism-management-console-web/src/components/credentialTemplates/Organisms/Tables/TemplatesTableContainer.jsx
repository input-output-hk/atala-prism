import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { observer } from 'mobx-react-lite';
import { useSession } from '../../../providers/SessionContext';
import { CONFIRMED } from '../../../../helpers/constants';
import CreateTemplateButton from '../../Atoms/Buttons/CreateTemplateButton';
import SimpleLoading from '../../../common/Atoms/SimpleLoading/SimpleLoading';
import EmptyComponent from '../../../common/Atoms/EmptyComponent/EmptyComponent';
import TemplatesTable from './TemplatesTable';
import noTemplatesPicture from '../../../../images/noTemplates.svg';
import SortControls from '../../Molecules/Headers/SortControls';
import { useTemplateStore, useTemplateUiState } from '../../../../hooks/useTemplateStore';

const TemplatesTableContainer = observer(({ showTemplatePreview }) => {
  const { t } = useTranslation();
  const { accountStatus } = useSession();
  const { templateCategories, isLoading } = useTemplateStore();
  const { hasFiltersApplied, filteredTemplates } = useTemplateUiState();

  const noTemplates = !filteredTemplates?.length;

  const emptyProps = {
    photoSrc: noTemplatesPicture,
    model: t('templates.title'),
    isFilter: hasFiltersApplied,
    button: noTemplates && accountStatus === CONFIRMED && <CreateTemplateButton />
  };

  const renderContent = () => {
    if (isLoading) return <SimpleLoading size="md" />;
    if (noTemplates) return <EmptyComponent {...emptyProps} />;
    return (
      <>
        <SortControls />
        <TemplatesTable
          credentialTemplates={filteredTemplates}
          templateCategories={templateCategories}
          showTemplatePreview={showTemplatePreview}
        />
      </>
    );
  };

  return <div className="templatesContent">{renderContent()}</div>;
});

TemplatesTableContainer.propTypes = {
  showTemplatePreview: PropTypes.func.isRequired
};

export default TemplatesTableContainer;
