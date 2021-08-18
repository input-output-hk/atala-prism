import React from 'react';
import { PlusOutlined } from '@ant-design/icons';
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
          className: 'theme-secondary',
          onClick: redirectToCredentialTemplateCreation
        }}
        buttonText={t('templates.actions.createTemplate')}
        icon={<PlusOutlined />}
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
