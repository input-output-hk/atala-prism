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
  templateSortingShape
} from '../../../../helpers/propShapes';

const TemplatesTableContainer = ({ tableProps, showTemplatePreview }) => {
  const { t } = useTranslation();
  const { accountStatus } = useSession();
  const { credentialTypes, templateCategories, isLoading, sortingProps } = tableProps;

  const noTemplates = !credentialTypes?.length;

  const emptyProps = {
    photoSrc: noTemplatesPicture,
    model: t('templates.title'),
    button: noTemplates && accountStatus === CONFIRMED && <CreateTemplateButton />
  };

  if (noTemplates && isLoading) return <SimpleLoading size="md" />;
  if (noTemplates) return <EmptyComponent {...emptyProps} />;

  return (
    <div className="templatesContent">
      <SortControls {...sortingProps} />
      <TemplatesTable
        credentialTypes={credentialTypes}
        templateCategories={templateCategories}
        showTemplatePreview={showTemplatePreview}
      />
    </div>
  );
};

TemplatesTableContainer.propTypes = {
  tableProps: PropTypes.shape({
    credentialTypes: PropTypes.arrayOf(credentialTypeShape),
    templateCategories: PropTypes.arrayOf(templateCategoryShape).isRequired,
    isLoading: PropTypes.bool,
    sortingProps: PropTypes.shape(templateSortingShape)
  }).isRequired,
  showTemplatePreview: PropTypes.func.isRequired
};

export default TemplatesTableContainer;
