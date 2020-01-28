import React from 'react';
import { useTranslation } from 'react-i18next';
import PropTypes from 'prop-types';
import { Icon } from 'antd';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';
import { withRedirector } from '../../../providers/withRedirector';

const AddUsersButtons = ({ redirector: { redirectToBulkImport, redirectToStudentCreation } }) => {
  const { t } = useTranslation();

  return (
    <div className="ControlButtons">
      <CustomButton
        buttonProps={{
          disabled: true,
          className: 'theme-outline',
          onClick: redirectToBulkImport
        }}
        buttonText={t('connections.buttons.bulk')}
        icon={<Icon type="plus" />}
      />
      <CustomButton
        buttonProps={{
          className: 'theme-secondary',
          onClick: redirectToStudentCreation
        }}
        buttonText={t('connections.buttons.manual')}
        icon={<Icon type="plus" />}
      />
    </div>
  );
};

AddUsersButtons.propTypes = {
  redirector: PropTypes.shape({
    redirectToBulkImport: PropTypes.func,
    redirectToStudentCreation: PropTypes.func
  }).isRequired
};

export default withRedirector(AddUsersButtons);
