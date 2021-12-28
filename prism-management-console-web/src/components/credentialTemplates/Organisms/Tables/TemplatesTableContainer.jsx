import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { observer } from 'mobx-react-lite';
import { useSession } from '../../../../hooks/useSession';
import { CONFIRMED } from '../../../../helpers/constants';
import CreateTemplateButton from '../../Atoms/Buttons/CreateTemplateButton';
import SimpleLoading from '../../../common/Atoms/SimpleLoading/SimpleLoading';
import EmptyComponent from '../../../common/Atoms/EmptyComponent/EmptyComponent';
import TemplatesTable from './TemplatesTable';
import noTemplatesPicture from '../../../../images/noTemplates.svg';
import { useTemplatesPageStore } from '../../../../hooks/useTemplatesPageStore';
import TemplatesSortControls from '../../Molecules/Headers/TemplatesSortControls';

const TemplatesTableContainer = observer(({ showTemplatePreview }) => {
  const { t } = useTranslation();
  const { accountStatus } = useSession();
  const {
    filteredTemplates,
    templateCategories,
    isLoading,
    filterSortingProps
  } = useTemplatesPageStore();
  const { hasFiltersApplied } = filterSortingProps;

  const noTemplates = !filteredTemplates?.length;

  const emptyProps = {
    photoSrc: noTemplatesPicture,
    model: t('templates.title'),
    isFilter: hasFiltersApplied,
    button: noTemplates && accountStatus === CONFIRMED ? <CreateTemplateButton /> : null
  };

  const renderContent = () => {
    if (isLoading) return <SimpleLoading size="md" />;
    if (noTemplates) return <EmptyComponent {...emptyProps} />;
    return (
      <>
        <div className="TemplatesTableOptions">
          <div className="LeftOptions">
            <TemplatesSortControls filterSortingProps={filterSortingProps} />
          </div>
        </div>
        <TemplatesTable
          credentialTemplates={filteredTemplates}
          templateCategories={templateCategories}
          isLoading={isLoading}
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
