import React, { useContext } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { useSession } from '../../../providers/SessionContext';
import { CONFIRMED } from '../../../../helpers/constants';
import CreateTemplateButton from '../../Atoms/Buttons/CreateTemplateButton';
import SimpleLoading from '../../../common/Atoms/SimpleLoading/SimpleLoading';
import EmptyComponent from '../../../common/Atoms/EmptyComponent/EmptyComponent';
import TemplatesTable from './TemplatesTable';
import noTemplatesPicture from '../../../../images/noTemplates.svg';
import SortControls from '../../Molecules/Headers/SortControls';
import {
  credentialTypeShape,
  templateCategoryShape,
  templateFiltersShape,
  templateSortingShape
} from '../../../../helpers/propShapes';
import { UiStateContext } from '../../../../stores/ui/UiState';

const TemplatesTableContainer = ({ tableProps, showTemplatePreview }) => {
  const { t } = useTranslation();
  const { accountStatus } = useSession();
  const { credentialTemplates, templateCategories, isLoading } = tableProps;
  const { hasFiltersApplied, filteredTemplates } = useContext(UiStateContext).templateUiState;

  const noTemplates = !credentialTemplates?.length;

  const emptyProps = {
    photoSrc: noTemplatesPicture,
    model: t('templates.title'),
    isFilter: hasFiltersApplied,
    button: noTemplates && accountStatus === CONFIRMED && <CreateTemplateButton />
  };

  const renderContent = () => {
    if (noTemplates && isLoading) return <SimpleLoading size="md" />;
    if (noTemplates) return <EmptyComponent {...emptyProps} />;
    return (
      <>
        {/* <SortControls {...sortingProps} /> */}
        <TemplatesTable
          credentialTemplates={filteredTemplates}
          templateCategories={templateCategories}
          showTemplatePreview={showTemplatePreview}
        />
      </>
    );
  };

  return <div className="templatesContent">{renderContent()}</div>;
};

TemplatesTableContainer.propTypes = {
  tableProps: PropTypes.shape({
    credentialTemplates: PropTypes.arrayOf(credentialTypeShape),
    templateCategories: PropTypes.arrayOf(templateCategoryShape).isRequired,
    isLoading: PropTypes.bool,
    filterProps: PropTypes.shape(templateFiltersShape),
    sortingProps: PropTypes.shape(templateSortingShape)
  }).isRequired,
  showTemplatePreview: PropTypes.func.isRequired
};

export default TemplatesTableContainer;
