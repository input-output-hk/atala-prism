import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { observer } from 'mobx-react-lite';
import { useSession } from '../../hooks/useSession';
import { CONFIRMED, UNCONFIRMED } from '../../helpers/constants';
import WaitBanner from '../dashboard/Atoms/WaitBanner/WaitBanner';
import TemplateDetail from './Organisms/Drawers/TemplateDetail';
import ActionsHeader from './Molecules/Headers/ActionsHeader';
import TemplatesTableContainer from './Organisms/Tables/TemplatesTableContainer';

const CredentialTemplates = observer(() => {
  const { t } = useTranslation();
  const { accountStatus } = useSession();
  const [currentTemplate, setCurrentTemplate] = useState();
  const [showDrawer, setShowDrawer] = useState(false);

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
        {accountStatus === CONFIRMED && <ActionsHeader />}
      </div>
      <TemplatesTableContainer showTemplatePreview={handleShowTemplatePreview} />
      <TemplateDetail
        drawerInfo={{
          visible: showDrawer,
          onClose: () => setShowDrawer(false)
        }}
        templateData={currentTemplate}
      />
    </div>
  );
});

export default CredentialTemplates;
