import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { withRedirector } from '../../../providers/withRedirector';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import './_style.scss';

const CreateTemplatesButtons = ({ redirector: { redirectToCredentialTemplateCreation } }) => {
  const { t } = useTranslation();

  return (
    <div className="MainOption">
      <CustomButton
        buttonProps={{
          className: 'theme-outline',
          onClick: redirectToCredentialTemplateCreation
        }}
        buttonText={t('templates.actions.createTemplate')}
      />
    </div>
  );
};

CreateTemplatesButtons.propTypes = {
  redirector: PropTypes.shape({
    redirectToCredentialTemplateCreation: PropTypes.func
  }).isRequired
};

export default withRedirector(CreateTemplatesButtons);
