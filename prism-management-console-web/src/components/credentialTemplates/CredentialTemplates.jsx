import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { useSession } from '../providers/SessionContext';
import { credentialTypeShape, templateCategoryShape } from '../../helpers/propShapes';
import { CONFIRMED, UNCONFIRMED } from '../../helpers/constants';
import WaitBanner from '../dashboard/Atoms/WaitBanner/WaitBanner';
import CreateTemplateButton from './Atoms/Buttons/CreateTemplateButton';
import EmptyComponent from '../common/Atoms/EmptyComponent/EmptyComponent';
import SimpleLoading from '../common/Atoms/SimpleLoading/SimpleLoading';
import noTemplatesPicture from '../../images/noTemplates.svg';
import TemplatesTable from './Organisms/TemplatesTable';
import TemplateDetail from './Organisms/TemplateDetail';
import './_style.scss';

const CredentialTemplates = ({ tableProps }) => {
  const { t } = useTranslation();
  const { accountStatus } = useSession();
  const [currentTemplate, setCurrentTemplate] = useState(false);
  const [showDrawer, setShowDrawer] = useState(false);

  const { credentialTypes, templateCategories, isLoading } = tableProps;

  const noTemplates = !credentialTypes?.length;

  const emptyProps = {
    photoSrc: noTemplatesPicture,
    model: t('templates.title'),
    button: noTemplates && accountStatus === CONFIRMED && <CreateTemplateButton />
  };

  const showTemplatePreview = template => {
    setCurrentTemplate(template);
    setShowDrawer(true);
  };

  const renderContent = () => {
    if (noTemplates && isLoading) return <SimpleLoading size="md" />;
    if (noTemplates) return <EmptyComponent {...emptyProps} />;
    return (
      <TemplatesTable
        credentialTypes={credentialTypes}
        templateCategories={templateCategories}
        showTemplatePreview={showTemplatePreview}
      />
    );
  };

  return (
    <div className="Wrapper PageContainer CredentialTemplatesContainer">
      {accountStatus === UNCONFIRMED && <WaitBanner />}
      <div className="ContentHeader">
        <div>
          <h1>{t('templates.title')}</h1>
        </div>
        {accountStatus === CONFIRMED && <CreateTemplateButton />}
      </div>
      <TemplateDetail
        drawerInfo={{
          visible: showDrawer,
          onClose: () => setShowDrawer(false)
        }}
        templateData={currentTemplate}
      />
      {renderContent()}
    </div>
  );
};

CredentialTemplates.propTypes = {
  tableProps: PropTypes.shape({
    credentialTypes: PropTypes.arrayOf(credentialTypeShape),
    templateCategories: PropTypes.arrayOf(templateCategoryShape).isRequired,
    isLoading: PropTypes.bool
  }).isRequired
};

export default CredentialTemplates;
