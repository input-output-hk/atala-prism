import React, { useState } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import { Menu } from 'antd';
import ProgressChecklist from './ProgressChecklist';
import CustomButton from '../../common/Atoms/CustomButton/CustomButton';

import './_style.scss';

const { SubMenu } = Menu;

const TutorialTool = ({ basicSteps, contacts, groups, credentials, onSkip }) => {
  const { t } = useTranslation();
  const [openKeys, setOpenKeys] = useState([]);

  return (
    <div className="tutorialContainer">
      <Menu mode="inline" openKeys={openKeys} onOpenChange={setOpenKeys} style={{ width: 256 }}>
        <SubMenu key="tutorialChecklist" title={t('tutorial.tutorialChecklist.title')}>
          <ProgressChecklist
            text={t('tutorial.tutorialChecklist.stepName.basicSteps')}
            percent={basicSteps}
          />
          <ProgressChecklist
            text={t('tutorial.tutorialChecklist.stepName.contacts')}
            percent={contacts}
          />
          <ProgressChecklist
            text={t('tutorial.tutorialChecklist.stepName.groups')}
            percent={groups}
          />
          <ProgressChecklist
            text={t('tutorial.tutorialChecklist.stepName.credentials')}
            percent={credentials}
          />
          <Menu.Item>
            <CustomButton
              buttonProps={{
                className: 'theme-outline',
                onClick: onSkip
              }}
              buttonText={t('tutorial.tutorialChecklist.skip')}
            />
          </Menu.Item>
        </SubMenu>
      </Menu>
    </div>
  );
};

TutorialTool.propTypes = {
  basicSteps: PropTypes.number.isRequired,
  contacts: PropTypes.number.isRequired,
  groups: PropTypes.number.isRequired,
  credentials: PropTypes.number.isRequired,
  onSkip: PropTypes.func.isRequired
};

export default TutorialTool;
