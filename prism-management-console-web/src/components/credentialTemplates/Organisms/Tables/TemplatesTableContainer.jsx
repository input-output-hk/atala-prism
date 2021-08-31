import React from 'react';
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

const TemplatesTableContainer = ({ tableProps, showTemplatePreview }) => {
  const { t } = useTranslation();
  const { accountStatus } = useSession();
  const { credentialTypes, templateCategories, isLoading, filterProps, sortingProps } = tableProps;

  const noTemplates = !credentialTypes?.length;

  const emptyProps = {
    photoSrc: noTemplatesPicture,
    model: t('templates.title'),
    isFilter: filterProps.name || filterProps.category || filterProps.lastEdited,
    button: noTemplates && accountStatus === CONFIRMED && <CreateTemplateButton />
  };

  const renderContent = () => {
    if (noTemplates && isLoading) return <SimpleLoading size="md" />;
    if (noTemplates) return <EmptyComponent {...emptyProps} />;
    return (
      <>
        <SortControls {...sortingProps} />
        <TemplatesTable
          credentialTypes={credentialTypes}
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
    credentialTypes: PropTypes.arrayOf(credentialTypeShape),
    templateCategories: PropTypes.arrayOf(templateCategoryShape).isRequired,
    isLoading: PropTypes.bool,
    filterProps: PropTypes.shape(templateFiltersShape),
    sortingProps: PropTypes.shape(templateSortingShape)
  }).isRequired,
  showTemplatePreview: PropTypes.func.isRequired
};

export default TemplatesTableContainer;
