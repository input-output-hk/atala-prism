import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { useSession } from '../providers/SessionContext';
import { credentialTypeShape } from '../../helpers/propShapes';
import { CONFIRMED, UNCONFIRMED } from '../../helpers/constants';
import WaitBanner from '../dashboard/Atoms/WaitBanner/WaitBanner';
import CreateTemplateButton from './Buttons/CreateTemplateButton';
import EmptyComponent from '../common/Atoms/EmptyComponent/EmptyComponent';
import SimpleLoading from '../common/Atoms/SimpleLoading/SimpleLoading';
import noTemplatesPicture from '../../images/noTemplates.svg';
import './_style.scss';

const CredentialTemplates = ({ tableProps }) => {
  const { t } = useTranslation();
  const { accountStatus } = useSession();

  const { credentialTypes, isLoading } = tableProps;

  const noTemplates = !credentialTypes?.length;

  const emptyProps = {
    photoSrc: noTemplatesPicture,
    model: t('templates.title'),
    button: noTemplates && accountStatus === CONFIRMED && <CreateTemplateButton />
  };

  const renderContent = () => {
    if (noTemplates && isLoading) return <SimpleLoading size="md" />;
    if (noTemplates) return <EmptyComponent {...emptyProps} />;
    // FIXME: render templates table here
    return credentialTypes.map(ct => <div>{ct.toString()}</div>);
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
      {renderContent()}
    </div>
  );
};

CredentialTemplates.propTypes = {
  tableProps: PropTypes.shape({
    credentialTypes: PropTypes.arrayOf(credentialTypeShape),
    isLoading: PropTypes.bool
  }).isRequired
};

export default CredentialTemplates;
