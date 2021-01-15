import React from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { Button } from 'antd';
import { Link } from 'react-router-dom';
import PopOver from '../../../common/Organisms/Detail/PopOver';

import './_style.scss';

const baseProps = {
  type: 'link',
  style: { color: '#FF2D3B' }
};

const ActionButtons = ({ id, setGroupToDelete, onCopy, fullInfo }) => {
  const { t } = useTranslation();

  const menu = (
    <>
      <Button {...baseProps} disabled>
        <Link to={`group/${id}`}>{t('groups.table.buttons.view')}</Link>
      </Button>
      <Button {...baseProps}>
        <Link to={`/groups/${id}/edit`}>{t('groups.table.buttons.edit')}</Link>
      </Button>
      <Button {...baseProps} onClick={onCopy}>
        {t('groups.table.buttons.copy')}
      </Button>
      {fullInfo && (
        <Button {...baseProps} disabled onClick={setGroupToDelete}>
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
  onCopy: PropTypes.func.isRequired,
  fullInfo: PropTypes.bool.isRequired
};

export default ActionButtons;
