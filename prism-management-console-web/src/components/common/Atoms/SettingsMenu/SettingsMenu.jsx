import React from 'react';
import PropTypes from 'prop-types';
import { DownOutlined } from '@ant-design/icons';
import { Avatar, Popover, Tooltip } from 'antd';
import { useTranslation } from 'react-i18next';
import CustomButton from '../CustomButton/CustomButton';
import { getInitials } from '../../../../helpers/genericHelpers';

import './_style.scss';

const MAX_DISPLAY_LENGTH = 10;

const SettingsMenu = ({ logout, name, logo }) => {
  const { t } = useTranslation();

  const content = (
    <div className="centerContent">
      <CustomButton
        buttonProps={{
          className: 'theme-primary',
          onClick: logout
        }}
        buttonText={t('menu.logout')}
      />
    </div>
  );

  const isLongName = name?.length > MAX_DISPLAY_LENGTH;

  return (
    <div className="SettingsMenu RightSide">
      <Popover content={content} trigger="click">
        <div className="userInfo">
          <div className="flex verticalAlign">
            {logo ? (
              <div>
                <img className="IconUniversity" src={logo} alt="Free University Tbilisi" />
              </div>
            ) : (
              <div className="logo">
                <Avatar style={{ color: '#FF2D3B', backgroundColor: '#FFFFFF' }}>
                  {getInitials(name)}
                </Avatar>
              </div>
            )}
            <div className="textContainer">
              <Tooltip title={isLongName ? name : ''}>
                <p className="UserName">{name}</p>
              </Tooltip>
            </div>
          </div>
          <DownOutlined />
        </div>
      </Popover>
    </div>
  );
};

SettingsMenu.defaultProps = {
  logo: null
};

SettingsMenu.propTypes = {
  logout: PropTypes.func.isRequired,
  logo: PropTypes.string,
  name: PropTypes.string.isRequired
};

export default SettingsMenu;
