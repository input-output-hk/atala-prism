import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { useSession } from '../providers/SessionContext';
import {
  credentialTypeShape,
  templateCategoryShape,
  templateFiltersShape,
  templateSortingShape
} from '../../helpers/propShapes';
import { UNCONFIRMED } from '../../helpers/constants';
import WaitBanner from '../dashboard/Atoms/WaitBanner/WaitBanner';
import TemplateDetail from './Organisms/Drawers/TemplateDetail';
import ActionsHeader from './Molecules/Headers/ActionsHeader';
import TemplatesTableContainer from './Organisms/Tables/TemplatesTableContainer';
import './_style.scss';

const CredentialTemplates = ({ tableProps }) => {
  const { t } = useTranslation();
  const { accountStatus } = useSession();
  const [currentTemplate, setCurrentTemplate] = useState(false);
  const [showDrawer, setShowDrawer] = useState(false);

  const { templateCategories } = tableProps;

  const handleShowTemplatePreview = template => {
    setCurrentTemplate(template);
    setShowDrawer(true);
  };

  return (
    <div className="Wrapper PageContainer CredentialTemplatesContainer">
      {accountStatus === UNCONFIRMED && <WaitBanner />}
      <div className="ContentHeader">
        <div>
          <h1>{t('templates.title')}</h1>
        </div>
        <ActionsHeader templateCategories={templateCategories} />
      </div>
      <TemplatesTableContainer
        tableProps={tableProps}
        showTemplatePreview={handleShowTemplatePreview}
      />
      <TemplateDetail
        drawerInfo={{
          visible: showDrawer,
          onClose: () => setShowDrawer(false)
        }}
        templateData={currentTemplate}
      />
    </div>
  );
};

CredentialTemplates.propTypes = {
  tableProps: PropTypes.shape({
    CredentialTemplates: PropTypes.arrayOf(credentialTypeShape),
    templateCategories: PropTypes.arrayOf(templateCategoryShape),
    isLoading: PropTypes.bool,
    filterProps: PropTypes.shape(templateFiltersShape),
    sortingProps: PropTypes.shape(templateSortingShape)
  }).isRequired
};

export default CredentialTemplates;
