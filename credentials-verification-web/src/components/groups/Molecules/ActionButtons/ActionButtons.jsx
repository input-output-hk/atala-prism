import React, { Fragment } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { message } from 'antd';
import { Link } from 'react-router-dom';
import CustomButton from '../../../common/Atoms/CustomButton/CustomButton';

import './_style.scss';

const ActionButtons = ({ id, setGroupToDelete, fullInfo }) => {
  const { t } = useTranslation();

  return (
    <div className="ControlButtons">
      {fullInfo && (
        <Fragment>
          <CustomButton
            buttonProps={{
              onClick: setGroupToDelete,
              className: 'theme-link',
              disabled: true
            }}
            buttonText={t('groups.table.buttons.delete')}
          />
          <CustomButton
            buttonProps={{
              onClick: () => message.info(`The id to copy is ${id}`),
              className: 'theme-link',
              disabled: true
            }}
            buttonText={t('groups.table.buttons.copy')}
          />
        </Fragment>
      )}
      <Link disabled to={`group/${id}`}>
        {t('groups.table.buttons.view')}
      </Link>
    </div>
  );
};

ActionButtons.propTypes = {
  id: PropTypes.string.isRequired,
  setGroupToDelete: PropTypes.func.isRequired,
  fullInfo: PropTypes.bool.isRequired
};

export default ActionButtons;
