import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { Button } from 'antd';
import { Link } from 'react-router-dom';
import PopOver from '../../../common/Organisms/Detail/PopOver';

import './_style.scss';

const baseProps = {
  type: 'link',
  style: { color: '#FF2D3B' },
  disabled: true
};

const ActionButtons = ({ id, setGroupToDelete, fullInfo }) => {
  const { t } = useTranslation();

  const menu = (
    <>
      <Button {...baseProps}>
        <Link to={`group/${id}`}>{t('groups.table.buttons.view')}</Link>
      </Button>
      <Button {...baseProps}>{t('groups.table.buttons.edit')}</Button>
      {fullInfo && (
        <Button {...baseProps} onClick={setGroupToDelete}>
          {t('groups.table.buttons.delete')}
        </Button>
      )}
    </>
  );

  return <PopOver content={menu} />;
};

ActionButtons.propTypes = {
  id: PropTypes.string.isRequired,
  setGroupToDelete: PropTypes.func.isRequired,
  fullInfo: PropTypes.bool.isRequired
};

export default ActionButtons;
